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

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
     */
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (IvyClasspathUtil.isIvyClasspathContainer(containerPath)) {

            // try to get an existing one
            IClasspathContainer container = null;
            try {
                container = JavaCore.getClasspathContainer(containerPath, project);
            } catch (JavaModelException ex) {
                // unless there are issues with the JDT, this should never happen
                IvyPlugin.log(IStatus.ERROR, "Unable to get container for "
                        + containerPath.toString(), ex);
                return;
            }

            try {
                IvyClasspathContainer ivycp;

                if (container instanceof IvyClasspathContainer) {
                    ivycp = (IvyClasspathContainer) container;
                } else {

                    IClasspathEntry entry =
                            IvyClasspathUtil.getIvyClasspathEntry(containerPath, project);
                    IClasspathAttribute[] attributes = new IClasspathAttribute[0];
                    if (entry != null) {
                        attributes = entry.getExtraAttributes();
                    }

                    if (container == null) {
                        ivycp = new IvyClasspathContainer(project, containerPath,
                                new IClasspathEntry[0], attributes);
                    } else {
                        // this might be the persisted one : reuse the persisted entries
                        ivycp = new IvyClasspathContainer(project, containerPath, container
                                .getClasspathEntries(), attributes);
                    }                    
                }

                // set the container
                JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                    new IClasspathContainer[] {ivycp}, null);

                int startupMode = IvyPlugin.getPreferenceStoreHelper().getResolveOnStartup();
                if (startupMode == ON_STARTUP_NOTHING) {
                    return;
                }
                boolean refresh = startupMode == ON_STARTUP_REFRESH;

                // now refresh the container to be synchronized with the ivy.xml
                ivycp.launchResolve(refresh, false, null);
            } catch (Exception ex) {
                IStatus status = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.OK,
                        "Unable to set container for " + containerPath.toString(), ex);
                throw new CoreException(status);
            }
        }
    }

    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return false;
    }

    public Object getComparisonID(IPath containerPath, IJavaProject project) {
        return project.getProject().getName() + "/" + containerPath;
    }
}
