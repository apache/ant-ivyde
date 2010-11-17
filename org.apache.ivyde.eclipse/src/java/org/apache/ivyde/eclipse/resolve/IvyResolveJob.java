/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.eclipse.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.sort.ModuleDescriptorSorter;
import org.apache.ivy.core.sort.WarningNonMatchingVersionReporter;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.circular.WarnCircularDependencyStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivyde.eclipse.CachedIvy;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyMarkerManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyResolveJob extends Job {

    private static final int MONITOR_LENGTH = 1000;

    private static final int IVY_LOAD_LENGTH = 100;

    private static final int POST_RESOLVE_LENGTH = 100;

    private static final int WAIT_BEFORE_LAUNCH = 1000;

    private final List resolveQueue = new ArrayList();

    public IvyResolveJob() {
        super("IvyDE resolve");
        setUser(false);
        // computing the classpath is somehow building
        setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    }

    public IStatus launchRequest(ResolveRequest request, IProgressMonitor monitor) {
        synchronized (resolveQueue) {
            resolveQueue.add(request);
        }
        return run(monitor);
    }

    public void addRequest(ResolveRequest request) {
        synchronized (resolveQueue) {
            resolveQueue.add(request);
        }
        schedule(WAIT_BEFORE_LAUNCH);
    }

    protected IStatus run(IProgressMonitor monitor) {
        List toResolve;
        synchronized (resolveQueue) {
            toResolve = new ArrayList(resolveQueue);
            resolveQueue.clear();
        }

        monitor.beginTask("Loading ivy descriptors", MONITOR_LENGTH);

        Map/* <ModuleDescriptor, ResolveRequest> */inworkspaceModules = new LinkedHashMap();
        List/* <ResolveRequest> */otherModules = new ArrayList();
        Map/* <ResolveRequest, Ivy> */ivys = new HashMap();
        Map/* <ResolveRequest, ModuleDescriptor> */mds = new HashMap();

        MultiStatus errorsStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Some projects fail to be resolved", null);

        int step = IVY_LOAD_LENGTH / toResolve.size();

        // Ivy use the SaxParserFactory, and we want it to instanciate the xerces parser which is in
        // the dependencies of IvyDE, so accessible via the current classloader
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IvyResolveJob.class.getClassLoader());
        try {
            Iterator itRequests = toResolve.iterator();
            while (itRequests.hasNext()) {
                ResolveRequest request = (ResolveRequest) itRequests.next();
                monitor.subTask("loading " + request.getResolver().toString());
                IProject project = request.getResolver().getProject();
                if (!FakeProjectManager.isFake(project) && !project.isAccessible()) {
                    // closed project, skip it
                    monitor.worked(step);
                    continue;
                }
                CachedIvy cachedIvy = request.getCachedIvy();
                Ivy ivy;
                try {
                    ivy = cachedIvy.getIvy();
                } catch (IvyDEException e) {
                    cachedIvy.setErrorMarker(e);
                    errorsStatus.add(e.asStatus(IStatus.ERROR, "Failed to configure Ivy for "
                            + request));
                    monitor.worked(step);
                    continue;
                }
                cachedIvy.setErrorMarker(null);
                ivys.put(request, ivy);
                // IVYDE-168 : Ivy needs the IvyContext in the threadlocal in order to found the
                // default branch
                ivy.pushContext();
                ModuleDescriptor md;
                try {
                    md = cachedIvy.getModuleDescriptor(ivy);
                } catch (IvyDEException e) {
                    cachedIvy.setErrorMarker(e);
                    errorsStatus.add(e.asStatus(IStatus.ERROR, "Failed to load the descriptor for "
                            + request));
                    monitor.worked(step);
                    continue;
                } finally {
                    ivy.popContext();
                }
                cachedIvy.setErrorMarker(null);
                mds.put(request, md);
                if (request.isInWorkspace()) {
                    inworkspaceModules.put(md, request);
                } else {
                    otherModules.add(request);
                }
                monitor.worked(step);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        step = (MONITOR_LENGTH - IVY_LOAD_LENGTH - POST_RESOLVE_LENGTH) / toResolve.size();

        if (!inworkspaceModules.isEmpty()) {
            // for the modules which are using the workspace resolver, make sure
            // we resolve them in the correct order

            // The version matcher used will be the one configured for the first project
            ResolveRequest request = (ResolveRequest) inworkspaceModules.values().iterator().next();
            VersionMatcher versionMatcher = ((Ivy) ivys.get(request)).getSettings()
                    .getVersionMatcher();

            WarningNonMatchingVersionReporter vReporter = new WarningNonMatchingVersionReporter();
            CircularDependencyStrategy circularDependencyStrategy = WarnCircularDependencyStrategy
                    .getInstance();
            ModuleDescriptorSorter sorter = new ModuleDescriptorSorter(inworkspaceModules.keySet(),
                    versionMatcher, vReporter, circularDependencyStrategy);
            List sortedModuleDescriptors = sorter.sortModuleDescriptors();

            Iterator it = sortedModuleDescriptors.iterator();
            while (it.hasNext()) {
                request = (ResolveRequest) inworkspaceModules.get(it.next());
                Ivy ivy = (Ivy) ivys.get(request);
                ModuleDescriptor md = (ModuleDescriptor) mds.get(request);
                boolean canceled = launchResolveThread(request, monitor, step, errorsStatus, ivy,
                    md);
                if (canceled) {
                    return Status.CANCEL_STATUS;
                }
            }
        }

        if (!otherModules.isEmpty()) {
            Iterator it = otherModules.iterator();
            while (it.hasNext()) {
                ResolveRequest request = (ResolveRequest) it.next();
                Ivy ivy = (Ivy) ivys.get(request);
                ModuleDescriptor md = (ModuleDescriptor) mds.get(request);
                boolean canceled = launchResolveThread(request, monitor, step, errorsStatus, ivy,
                    md);
                if (canceled) {
                    return Status.CANCEL_STATUS;
                }
            }
        }

        step = POST_RESOLVE_LENGTH / toResolve.size();

        monitor.setTaskName("Post resolve");

        // launch every post batch resolve
        Iterator itRequests = toResolve.iterator();
        while (itRequests.hasNext()) {
            ResolveRequest request = (ResolveRequest) itRequests.next();
            monitor.setTaskName(request.getResolver().toString());
            request.getResolver().postBatchResolve();
            monitor.worked(step);
        }

        if (errorsStatus.getChildren().length != 0) {
            return errorsStatus;
        }

        return Status.OK_STATUS;
    }

    private boolean launchResolveThread(ResolveRequest request, final IProgressMonitor monitor,
            final int step, MultiStatus errorsStatus, final Ivy ivy, final ModuleDescriptor md) {

        final IStatus[] status = new IStatus[1];

        final IvyResolver resolver = request.getResolver();
        Runnable resolveRunner = new Runnable() {
            public void run() {
                status[0] = resolver.resolve(ivy, md, monitor, step);
            }
        };

        IvyRunner ivyRunner = new IvyRunner();
        if (ivyRunner.launchIvyThread(resolveRunner, ivy, monitor)) {
            return true;
        }

        IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
        ivyMarkerManager.setResolveStatus(status[0], resolver.getProject(),
            resolver.getIvyXmlPath());

        switch (status[0].getCode()) {
            case IStatus.CANCEL:
                return true;
            case IStatus.OK:
            case IStatus.INFO:
                break;
            case IStatus.ERROR:
                errorsStatus.add(status[0]);
                break;
            default:
                IvyPlugin.log(IStatus.WARNING, "Unknown IStatus: " + status[0].getCode(), null);
        }

        return false;
    }

}
