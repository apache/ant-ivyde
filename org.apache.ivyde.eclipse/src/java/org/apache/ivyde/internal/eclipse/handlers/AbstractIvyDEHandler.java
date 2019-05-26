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
package org.apache.ivyde.internal.eclipse.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class AbstractIvyDEHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        Map<IProject, Set<IvyClasspathContainer>> projects = getProjectAndContainers((IStructuredSelection) selection);

        if (projects.size() > 0) {
            handleProjects(projects);
        }

        return null;
    }

    public static Map<IProject, Set<IvyClasspathContainer>> getProjectAndContainers(
            IStructuredSelection selection) {
        Map<IProject, Set<IvyClasspathContainer>> projects = new HashMap<>();

        for (Object element : selection.toList()) {
            if (element instanceof IWorkingSet) {
                for (IAdaptable elem : ((IWorkingSet) element).getElements()) {
                    addElement(projects, elem);
                }
            } else if (element instanceof ClassPathContainer) {
                IvyClasspathContainerImpl ivycp = IvyClasspathUtil
                        .jdt2IvyCPC(((ClassPathContainer) element));
                IJavaProject javaProject = ivycp.getConf().getJavaProject();
                if (javaProject != null) {
                    Set<IvyClasspathContainer> containers = projects.get(javaProject
                            .getProject());
                    if (containers == null) {
                        containers = new HashSet<>();
                        projects.put(javaProject.getProject(), containers);
                    }
                    containers.add(ivycp);
                }
            } else {
                addElement(projects, element);
            }
        }
        return projects;
    }

    private static void addElement(Map<IProject, Set<IvyClasspathContainer>> projects,
            Object adaptableProject) {
        IProject project = null;
        if (adaptableProject instanceof IProject) {
            project = (IProject) adaptableProject;
        } else if (adaptableProject instanceof IAdaptable) {
            project = (IProject) ((IAdaptable) adaptableProject).getAdapter(IProject.class);
        }
        if (project != null) {
            // check that there is an IvyDE container
            List<IvyClasspathContainer> containers = IvyClasspathContainerHelper.getContainers(project);
            if (!containers.isEmpty()) {
                projects.put(project, new HashSet<>(containers));
            }
        }
    }

    protected void handleProjects(Map<IProject, Set<IvyClasspathContainer>> projects) {
        for (Entry<IProject, Set<IvyClasspathContainer>> entry : projects.entrySet()) {
            IProject project = entry.getKey();
            for (IvyClasspathContainer container : entry.getValue()) {
                handleContainer(project, container);
            }
        }
    }

    protected void handleContainer(IProject project, IvyClasspathContainer container) {
        throw new UnsupportedOperationException();
    }

}
