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
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.IvyMultiResolveJob;
import org.eclipse.core.resources.ResourcesPlugin;
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

    public void run(IAction action) {
        IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        IJavaProject[] projects;
        try {
            projects = model.getJavaProjects();
        } catch (JavaModelException e) {
            // TODO deal with it properly
            return;
        }

        List allContainers = new ArrayList();
        for (int i = 0; i < projects.length; i++) {
            allContainers.addAll(IvyClasspathUtil.getIvyClasspathContainers(projects[i]));
        }

        Job resolveAllJob = new IvyMultiResolveJob(allContainers);
        resolveAllJob.setUser(true);
        resolveAllJob.schedule();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // nothing to do
    }

    public void dispose() {
        // nothing to do
    }

    public void init(IWorkbenchWindow window) {
        // nothing to do
    }
}
