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
package org.apache.ivyde.eclipse.cpcontainer;

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
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyMarkerManager;
import org.apache.ivyde.eclipse.IvyPlugin;
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

    private static final int WAIT_FOR_JOIN = 100;

    private final List resolveQueue = new ArrayList();

    public IvyResolveJob() {
        super("IvyDE resolve job");
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
        schedule(1000);
    }

    protected IStatus run(IProgressMonitor monitor) {
        List toResolve;
        synchronized (resolveQueue) {
            toResolve = new ArrayList(resolveQueue);
            resolveQueue.clear();
        }

        Map/* <ModuleDescriptor, ResolveRequest> */inworkspaceModules = new LinkedHashMap();
        List/* <ResolveRequest> */otherModules = new ArrayList();
        Map/* <ResolveRequest, Ivy> */ivys = new HashMap();
        Map/* <ResolveRequest, ModuleDescriptor> */mds = new HashMap();

        MultiStatus errorsStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Some projects fail to be resolved", null);

        // Ivy use the SaxParserFactory, and we want it to instanciate the xerces parser which is in
        // the dependencies of IvyDE, so accessible via the current classloader
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IvyResolveJob.class.getClassLoader());
        try {
            Iterator itRequests = toResolve.iterator();
            while (itRequests.hasNext()) {
                ResolveRequest request = (ResolveRequest) itRequests.next();
                IvyClasspathContainerState state = request.getContainer().getState();
                Ivy ivy;
                try {
                    ivy = state.getIvy();
                } catch (IvyDEException e) {
                    state.setErrorMarker(e);
                    errorsStatus.add(e.asStatus(IStatus.ERROR, "Failed to configure Ivy for "
                            + request));
                    continue;
                }
                state.setErrorMarker(null);
                ivys.put(request, ivy);
                // IVYDE-168 : Ivy needs the IvyContext in the threadlocal in order to found the
                // default branch
                ivy.pushContext();
                ModuleDescriptor md;
                try {
                    md = state.getModuleDescriptor(ivy);
                } catch (IvyDEException e) {
                    state.setErrorMarker(e);
                    errorsStatus.add(e.asStatus(IStatus.ERROR, "Failed to load the descriptor for "
                            + request));
                    continue;
                } finally {
                    ivy.popContext();
                }
                state.setErrorMarker(null);
                mds.put(request, md);
                if (request.getContainer().getConf().isInheritedResolveInWorkspace()) {
                    inworkspaceModules.put(md, request);
                } else {
                    otherModules.add(request);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        if (!inworkspaceModules.isEmpty()) {
            // for the modules which are using the workspace resolver, make sure
            // we resolve them in the correct order

            // The version matcher used will be the one configured for the first project
            ResolveRequest request = (ResolveRequest) inworkspaceModules.values().iterator().next();
            VersionMatcher versionMatcher = ((Ivy) ivys.get(request)).getSettings()
                    .getVersionMatcher();

            WarningNonMatchingVersionReporter nonMatchingVersionReporter = new WarningNonMatchingVersionReporter();
            CircularDependencyStrategy circularDependencyStrategy = WarnCircularDependencyStrategy
                    .getInstance();
            ModuleDescriptorSorter sorter = new ModuleDescriptorSorter(inworkspaceModules.keySet(),
                    versionMatcher, nonMatchingVersionReporter, circularDependencyStrategy);
            List sortedModuleDescriptors = sorter.sortModuleDescriptors();

            Iterator it = sortedModuleDescriptors.iterator();
            while (it.hasNext()) {
                request = (ResolveRequest) inworkspaceModules.get(it.next());
                Ivy ivy = (Ivy) ivys.get(request);
                ModuleDescriptor md = (ModuleDescriptor) mds.get(request);
                boolean canceled = launchResolveThread(request, monitor, errorsStatus, ivy, md);
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
                boolean canceled = launchResolveThread(request, monitor, errorsStatus, ivy, md);
                if (canceled) {
                    return Status.CANCEL_STATUS;
                }
            }
        }

        if (errorsStatus.getChildren().length != 0) {
            return errorsStatus;
        }

        return Status.OK_STATUS;
    }

    private boolean launchResolveThread(ResolveRequest request, IProgressMonitor monitor,
            MultiStatus errorsStatus, Ivy ivy, ModuleDescriptor md) {
        IStatus jobStatus = launchResolveThread(request, monitor, ivy, md);
        switch (jobStatus.getCode()) {
            case IStatus.CANCEL:
                return true;
            case IStatus.OK:
            case IStatus.INFO:
                break;
            case IStatus.ERROR:
                errorsStatus.add(jobStatus);
                break;
            default:
                IvyPlugin.log(IStatus.WARNING, "Unknown IStatus: " + jobStatus.getCode(), null);
        }
        return false;
    }

    private IStatus launchResolveThread(ResolveRequest request, IProgressMonitor monitor, Ivy ivy,
            ModuleDescriptor md) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        boolean usePreviousResolveIfExist = request.isUsePreviousResolveIfExist();
        IvyClasspathContainerConfiguration conf = request.getContainer().getConf();
        final IvyClasspathResolver resolver = new IvyClasspathResolver(conf, ivy, md,
                usePreviousResolveIfExist, monitor);
        final IStatus[] status = new IStatus[1];
        Thread resolverThread = new Thread(new Runnable() {
            public void run() {
                status[0] = resolver.resolve();
            }
        });
        resolverThread.setName("IvyDE resolver thread");

        resolverThread.start();
        while (true) {
            try {
                resolverThread.join(WAIT_FOR_JOIN);
            } catch (InterruptedException e) {
                ivy.interrupt(resolverThread);
                return Status.CANCEL_STATUS;
            }
            if (status[0] != null || !resolverThread.isAlive()) {
                break;
            }
            if (monitor.isCanceled()) {
                ivy.interrupt(resolverThread);
                return Status.CANCEL_STATUS;
            }
        }
        if (status[0] == Status.OK_STATUS) {
            request.getContainer().updateClasspathEntries(resolver.getClasspathEntries());
        }
        IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
        ivyMarkerManager.setResolveStatus(status[0], conf.getJavaProject().getProject(),
            conf.getIvyXmlPath());
        return status[0];

    }

}
