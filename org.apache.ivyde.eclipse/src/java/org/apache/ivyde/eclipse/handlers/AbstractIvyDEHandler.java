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
package org.apache.ivyde.eclipse.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
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

        Map/* <IProject, Set<IvyClasspathContainer>> */projects = getProjectAndContainers((IStructuredSelection) selection);

        if (projects.size() > 0) {
            handleProjects(projects);
        }

        return null;
    }

    public static Map/* <IProject, Set<IvyClasspathContainer>> */getProjectAndContainers(
            IStructuredSelection selection) {
        Map/* <IProject, Set<IvyClasspathContainer>> */projects = new HashMap();

        Iterator it = selection.iterator();
        while (it.hasNext()) {
            Object element = it.next();
            if (element instanceof IWorkingSet) {
                IAdaptable[] elements = ((IWorkingSet) element).getElements();
                for (int i = 0; i < elements.length; i++) {
                    addElement(projects, elements[i]);
                }
            } else if (element instanceof ClassPathContainer) {
                IvyClasspathContainer ivycp = IvyClasspathUtil
                        .jdt2IvyCPC(((ClassPathContainer) element));
                IJavaProject javaProject = ivycp.getConf().getJavaProject();
                Set/* <IvyClasspathContainer> */cplist = (Set) projects.get(javaProject
                        .getProject());
                if (cplist == null) {
                    cplist = new HashSet();
                    projects.put(javaProject.getProject(), cplist);
                }
                cplist.add(ivycp);
            } else {
                addElement(projects, element);
            }
        }

        return projects;
    }

    private static void addElement(Map/* <IProject, Set<IvyClasspathContainer>> */projects,
            Object adaptableProject) {
        IProject project = null;
        if (adaptableProject instanceof IProject) {
            project = (IProject) adaptableProject;
        } else if (adaptableProject instanceof IAdaptable) {
            project = (IProject) ((IAdaptable) adaptableProject).getAdapter(IProject.class);
        }
        if (project != null) {
            // check that there is an IvyDE container
            List containers = IvyClasspathUtil.getIvyClasspathContainers(project);
            if (!containers.isEmpty()) {
                projects.put(project, new HashSet(containers));
            }
        }
    }

    protected void handleProjects(Map projects) {
        Iterator it = projects.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            Iterator itContainers = ((Set) entry.getValue()).iterator();
            while (itContainers.hasNext()) {
                handleContainer((IProject) entry.getKey(), (IvyClasspathContainer) itContainers
                        .next());
            }
        }
    }

    protected void handleContainer(IProject project, IvyClasspathContainer cp) {
        throw new UnsupportedOperationException();
    }

}
