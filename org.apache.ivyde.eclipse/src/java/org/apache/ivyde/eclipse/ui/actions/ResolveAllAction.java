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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ResolveAllAction implements IWorkbenchWindowActionDelegate {
    /**
     * The constructor.
     */
    public ResolveAllAction() {
    }

    /**
     * The action has been activated. The argument of the method represents the 'real' action
     * sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
        Job resolveAllJob = new Job("Resolve all dependencies") {
            protected IStatus run(IProgressMonitor monitor) {
                IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
                IJavaProject[] projects;
                try {
                    projects = model.getJavaProjects();
                } catch (JavaModelException e) {
                    return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                            "Unable to get the list of available java projects", e);
                }
                List containers = new ArrayList();
                for (int i = 0; i < projects.length; i++) {
                    IvyClasspathContainer cp = IvyClasspathUtil.getIvyClasspathContainer(projects[i]);
                    if (cp != null) {
                        containers.add(cp);
                    }
                }
                monitor.beginTask("Resolve all dependencies", containers.size());
                for (Iterator iter = containers.iterator(); iter.hasNext();) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
                    IvyClasspathContainer cp = (IvyClasspathContainer) iter.next();
                    cp.resolve(subMonitor);
                }

                return Status.OK_STATUS;
            }
        };

        resolveAllJob.setUser(true);
        resolveAllJob.schedule();
    }

    /**
     * Selection in the workbench has been changed. We can change the state of the 'real' action
     * here if we want, but this can only happen after the delegate has been created.
     * 
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * We can use this method to dispose of any system resources we previously allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell for the message
     * dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
    }
}
