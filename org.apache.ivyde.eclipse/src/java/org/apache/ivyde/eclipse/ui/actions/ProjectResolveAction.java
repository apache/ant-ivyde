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
package org.apache.ivyde.eclipse.ui.actions;

import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;

public class ProjectResolveAction extends Action {
    IProject[] projects;
    
    public ProjectResolveAction(IProject[] projects) {
        this.projects = projects;
        this.setText("Resolve");
    }
    
    public void run() {
        final IProject[] finalProjects = projects;

        Job multipleResolveJob = new Job("Resolving dependencies") {
            protected IStatus run(IProgressMonitor monitor) {
                for (int i = 0; i < finalProjects.length; i++) {
                    IJavaProject javaProject = JavaCore.create(finalProjects[i]);
                    if (javaProject == null)
                        continue;

                    List/* <IvyClasspathContainer> */classpathContainers = IvyClasspathUtil
                            .getIvyClasspathContainers(javaProject);

                    Iterator containerIterator = classpathContainers.iterator();
                    while (containerIterator.hasNext()) {
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
                        IvyClasspathContainer container = (IvyClasspathContainer) containerIterator
                                .next();
                        container.launchResolve(false, true, subMonitor);
                    }
                }
                
                return Status.OK_STATUS;
            }
        };

        multipleResolveJob.setUser(true);
        multipleResolveJob.schedule();
    }
}
