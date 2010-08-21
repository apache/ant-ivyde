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
import org.apache.ivy.plugins.version.LatestVersionMatcher;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

        // Ivy use the SaxParserFactory, and we want it to instanciate the xerces parser which is in
        // the dependencies of IvyDE, so accessible via the current classloader
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IvyResolveJob.class.getClassLoader());
        try {
            Iterator itRequests = toResolve.iterator();
            while (itRequests.hasNext()) {
                ResolveRequest request = (ResolveRequest) itRequests.next();
                IvyClasspathContainerState state = request.getContainer().getState();
                Ivy ivy = state.getIvy();
                // IVYDE-168 : Ivy needs the IvyContext in the threadlocal in order to found the
                // default branch
                ivy.pushContext();
                ModuleDescriptor md;
                try {
                    md = state.getModuleDescriptor(ivy);
                } finally {
                    ivy.popContext();
                }
                if (request.getContainer().getConf().isInheritedResolveInWorkspace()) {
                    inworkspaceModules.put(md, request);
                } else {
                    otherModules.add(request);
                }
            }
        } catch (IvyDEException e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, e.getMessage(), e);
        } catch (Throwable e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, "Unexpected error ["
                    + e.getClass().getName() + "]: " + e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        MultiStatus errorsStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Some projects fail to be resolved", null);

        if (!inworkspaceModules.isEmpty()) {
            // for the modules which are using the workspace resolver, make sure
            // we resolve them in the correct order
            // TODO which version matcher we should actually used here ?
            VersionMatcher versionMatcher = new LatestVersionMatcher();
            WarningNonMatchingVersionReporter nonMatchingVersionReporter = new WarningNonMatchingVersionReporter();
            CircularDependencyStrategy circularDependencyStrategy = WarnCircularDependencyStrategy
                    .getInstance();
            ModuleDescriptorSorter sorter = new ModuleDescriptorSorter(inworkspaceModules.keySet(),
                    versionMatcher, nonMatchingVersionReporter, circularDependencyStrategy);
            List sortedModuleDescriptors = sorter.sortModuleDescriptors();

            Iterator it = sortedModuleDescriptors.iterator();
            while (it.hasNext()) {
                ResolveRequest request = (ResolveRequest) inworkspaceModules.get(it.next());
                boolean canceled = launchResolveThread(request, monitor, errorsStatus);
                if (canceled) {
                    return Status.CANCEL_STATUS;
                }
            }
        }

        if (!otherModules.isEmpty()) {
            Iterator it = otherModules.iterator();
            while (it.hasNext()) {
                ResolveRequest request = (ResolveRequest) it.next();
                boolean canceled = launchResolveThread(request, monitor, errorsStatus);
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
            MultiStatus errorsStatus) {
        IStatus jobStatus = launchResolveThread(request, monitor);
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

    private IStatus launchResolveThread(ResolveRequest request, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        IvyClasspathContainerConfiguration conf = request.getContainer().getConf();
        Ivy ivy;
        ModuleDescriptor md;
        try {
            // here we expect to find the ivy and the md we have just computed
            ivy = request.getContainer().getState().getCachedIvy();
            md = request.getContainer().getState().getCachedModuleDescriptor();
        } catch (IvyDEException e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, "Unexpected error");
        }
        boolean usePreviousResolveIfExist = request.isUsePreviousResolveIfExist();
        IvyResolveJobThread resolver = new IvyResolveJobThread(conf, ivy, md,
                usePreviousResolveIfExist, monitor);
        resolver.setName("IvyDE resolver thread");

        resolver.start();
        while (true) {
            try {
                resolver.join(WAIT_FOR_JOIN);
            } catch (InterruptedException e) {
                ivy.interrupt(resolver);
                return Status.CANCEL_STATUS;
            }
            if (resolver.getStatus() != null || !resolver.isAlive()) {
                break;
            }
            if (monitor.isCanceled()) {
                ivy.interrupt(resolver);
                return Status.CANCEL_STATUS;
            }
        }
        if (resolver.getStatus() == Status.OK_STATUS) {
            request.getContainer().updateClasspathEntries(resolver.getClasspathEntries());
        }
        setResolveStatus(conf, resolver.getStatus());
        return resolver.getStatus();

    }

    private void setResolveStatus(IvyClasspathContainerConfiguration conf, IStatus status) {
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
            return;
        }
        IFile ivyFile = conf.getJavaProject().getProject().getFile(conf.getIvyXmlPath());
        if (!ivyFile.exists()) {
            return;
        }
        try {
            ivyFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            if (status == Status.OK_STATUS) {
                return;
            }
            IMarker marker = ivyFile.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.MESSAGE, status.getMessage());
            switch (status.getSeverity()) {
                case IStatus.ERROR:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    break;
                case IStatus.WARNING:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                    break;
                case IStatus.INFO:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                    break;
                default:
                    IvyPlugin.log(IStatus.WARNING, "Unsupported resolve status: "
                            + status.getSeverity(), null);
            }
        } catch (CoreException e) {
            IvyPlugin.log(e);
        }
    }

}
