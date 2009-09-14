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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyResolveJob extends Job {

    private static final int WAIT_FOR_JOIN = 100;

    private boolean usePreviousResolveIfExist;

    private Ivy ivy;

    private final IvyClasspathContainerConfiguration conf;

    private final IvyClasspathContainer container;

    private IvyClasspathContainerState state;

    public IvyResolveJob(IvyClasspathContainer container, boolean usePreviousResolveIfExist) {
        super("Ivy resolve job of " + container.getConf());
        this.container = container;
        this.conf = container.getConf();
        this.state = container.getState();
        this.usePreviousResolveIfExist = usePreviousResolveIfExist;
    }

    protected IStatus run(IProgressMonitor monitor) {
        Message.info("resolving dependencies of " + conf);

        // Ivy use the SaxParserFactory, and we want it to instanciate the xerces parser which is in
        // the dependencies of IvyDE, so accessible via the current classloader
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IvyResolveJob.class.getClassLoader());
        ModuleDescriptor md;
        try {
            this.ivy = state.getIvy();
            // IVYDE-168 : Ivy needs the IvyContext in the threadlocal in order to found the
            // default branch
            ivy.pushContext();
            md = state.getModuleDescriptor(ivy);
        } catch (IvyDEException e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, e.getMessage(), e);
        } catch (Throwable e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, "Unexpected error ["
                    + e.getClass().getName() + "]: " + e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        IvyResolveJobThread resolver = new IvyResolveJobThread(conf, ivy, md,
                usePreviousResolveIfExist, monitor);

        try {
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
                container.updateClasspathEntries(resolver.getClasspathEntries());
            }
            setResolveStatus(resolver.getStatus());
            return resolver.getStatus();
        } finally {
            container.resetJob();
            IvyPlugin.log(IStatus.INFO, "resolved dependencies of " + conf, null);
        }
    }

    private void setResolveStatus(IStatus status) {
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
