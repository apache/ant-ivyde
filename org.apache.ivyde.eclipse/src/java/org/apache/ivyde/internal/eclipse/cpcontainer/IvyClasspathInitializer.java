/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.IvyDEMessage;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;

/**
 * Initializer the ivy class path container. It will create a container from the persisted class
 * path entries (the .classpath file), and then schedule the refresh of the container.
 */
public class IvyClasspathInitializer extends ClasspathContainerInitializer {

    public static final int ON_STARTUP_NOTHING = 0;

    public static final int ON_STARTUP_REFRESH = 1;

    public static final int ON_STARTUP_RESOLVE = 2;

    /**
     * Initialize the container with the "persisted" classpath entries, and then schedule the
     * refresh
     *
     * @param containerPath IPath
     * @param project IJavaProject
     * @throws CoreException if initialisation fails
     */
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (IvyClasspathContainerHelper.isIvyClasspathContainer(containerPath)) {

            IvyDEMessage.info("Initializing container " + containerPath);

            // try to get an existing one
            IClasspathContainer container = null;
            try {
                container = JavaCore.getClasspathContainer(containerPath, project);
            } catch (JavaModelException ex) {
                // unless there are issues with the JDT, this should never happen
                IvyPlugin.logError("Unable to get container for " + containerPath.toString(), ex);
                return;
            }

            try {
                boolean refresh = false;
                IvyClasspathContainerImpl ivycp = null;
                IClasspathEntry entry = IvyClasspathContainerHelper.getEntry(containerPath,
                    project);
                IClasspathAttribute[] attributes;
                if (entry != null) {
                    attributes = entry.getExtraAttributes();
                } else {
                    attributes = new IClasspathAttribute[0];
                }

                if (container instanceof IvyClasspathContainerImpl) {
                    IvyDEMessage.debug("Container already configured");
                    ivycp = (IvyClasspathContainerImpl) container;
                } else {
                    if (container == null) {
                        IvyDEMessage.debug("No saved container");
                        // try what the IvyDE plugin saved
                        IvyClasspathContainerSerializer serializer = IvyPlugin.getDefault()
                                .getIvyClasspathContainerSerializer();
                        Map<IPath, IvyClasspathContainer> containers = serializer.read(project);
                        if (containers != null) {
                            IvyDEMessage.debug("Found serialized containers");
                            ivycp = (IvyClasspathContainerImpl) containers.get(containerPath);
                        }
                        if (ivycp == null) {
                            IvyDEMessage.debug("No serialized containers match the expected container path");
                            // still bad luck or just a new classpath container
                            ivycp = new IvyClasspathContainerImpl(project, containerPath,
                                    new IClasspathEntry[0], attributes);
                            // empty, so force refresh at least
                            refresh = true;
                        }
                    } else {
                        IvyDEMessage.debug("Loading from a saved container");
                        // this might be the persisted one : reuse the persisted entries
                        ivycp = new IvyClasspathContainerImpl(project, containerPath,
                                container.getClasspathEntries(), attributes);
                    }
                }

                // FIXME : container path upgrade removed since it seems to make some trouble:
                // containers get either uninitialized or initialized twice...

                // recompute the path as it may have been "upgraded"
                // IPath updatedPath = IvyClasspathContainerConfAdapter.getPath(ivycp.getConf());
                // if (!updatedPath.equals(containerPath)) {
                // IvyDEMessage.verbose("Upgrading container path from " + containerPath + " to " +
                // updatedPath);
                // updateIvyDEContainerPath(project, entry, attributes, exported, updatedPath);
                // return;
                // }

                IvyDEMessage.verbose("Setting container in JDT model");

                JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                    new IClasspathContainer[] {ivycp}, null);

                int startupMode = IvyPlugin.getPreferenceStoreHelper().getResolveOnStartup();
                if (startupMode == ON_STARTUP_NOTHING) {
                    if (!refresh) {
                        IvyDEMessage.verbose("Doing nothing on startup");
                        // unless we force a refresh, actually do nothing
                        return;
                    }
                } else {
                    refresh = startupMode == ON_STARTUP_REFRESH;
                }

                if (refresh) {
                    IvyDEMessage.info("Scheduling a refresh of the container");
                } else {
                    IvyDEMessage.info("Scheduling a resolve of the container");
                }
                // now refresh the container to be synchronized with the ivy.xml
                ivycp.launchResolve(refresh, null);
            } catch (Exception ex) {
                IStatus status = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.OK,
                        "Unable to set container for " + containerPath.toString(), ex);
                throw new CoreException(status);
            }
        }
    }

    @SuppressWarnings("unused")
    private void updateIvyDEContainerPath(final IJavaProject project, final IClasspathEntry entry,
            final IClasspathAttribute[] attributes, final boolean exported, final IPath updatedPath) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    // update the classpath of the project by updating the IvyDE container
                    IClasspathEntry newEntry = JavaCore.newContainerEntry(updatedPath, null,
                        attributes, exported);
                    IClasspathEntry[] entries = project.getRawClasspath();
                    List<IClasspathEntry> newEntries = new ArrayList<>(Arrays.asList(entries));
                    for (int i = 0; i < newEntries.size(); i++) {
                        IClasspathEntry e = newEntries.get(i);
                        if (e == entry) {
                            newEntries.set(i, newEntry);
                            break;
                        }
                    }
                    entries = newEntries.toArray(new IClasspathEntry[newEntries.size()]);
                    project.setRawClasspath(entries, project.getOutputLocation(), null);
                } catch (JavaModelException e) {
                    IvyPlugin.logError("Unable to update the container path", e);
                }
            }
        });
    }

    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    public void requestClasspathContainerUpdate(final IPath containerPath,
            final IJavaProject project, final IClasspathContainer containerSuggestion)
            throws CoreException {
        new Job("IvyDE attachment updater") {
            protected IStatus run(IProgressMonitor monitor) {
                IvyPlugin.getDefault().getIvyAttachmentManager()
                        .updateAttachments(project, containerPath, containerSuggestion);
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public Object getComparisonID(IPath containerPath, IJavaProject project) {
        return containerPath;
    }
}
