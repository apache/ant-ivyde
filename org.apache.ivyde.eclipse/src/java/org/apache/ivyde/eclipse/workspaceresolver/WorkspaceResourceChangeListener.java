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
package org.apache.ivyde.eclipse.workspaceresolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This ResourceChangeListener detects when projects linked in as Ivy dependencies are closed. When
 * necessary, it will re-resolve Ivy projects which had the dependent project linked into the Ivy
 * container before it was closed.
 */
public class WorkspaceResourceChangeListener implements IResourceChangeListener {

    public void resourceChanged(IResourceChangeEvent event) {

        try {
            if (event.getType() == IResourceChangeEvent.PRE_CLOSE
                    || event.getType() == IResourceChangeEvent.PRE_DELETE) {
                if (!IvyPlugin.getPreferenceStoreHelper().getAutoResolveOnClose()) {
                    return;
                }
                IResource res = event.getResource();
                IProject project;
                switch (res.getType()) {
                    case IResource.FOLDER:
                        project = ((IFolder) res).getProject();
                        break;
                    case IResource.FILE:
                        project = ((IFile) res).getProject();
                        break;
                    case IResource.PROJECT:
                        project = (IProject) res;
                        break;
                    default:
                        return;
                }
                try {
                    if (project.hasNature(JavaCore.NATURE_ID)) {
                        projectClosed(JavaCore.create(project));
                    }
                } catch (CoreException e) {
                    // project doesn't exist or is not open: ignore
                }
            } else if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                if (!IvyPlugin.getPreferenceStoreHelper().getAutoResolveOnOpen()) {
                    return;
                }
                projectOpened(event);
            }
        } catch (OperationCanceledException oce) {
            IvyPlugin.log(IStatus.CANCEL,
                "Ivy update of dependent proejects affected by project close operation canceled",
                null);
        }
    }

    private void projectClosed(final IJavaProject javaProject) throws JavaModelException {
        // Check if one of Ivy projects is being removed
        List containers = IvyClasspathUtil.getIvyClasspathContainers(javaProject);
        if (containers.isEmpty()) {
            return;
        }

        // Found an Ivy container in this project -- notify dependent projects
        // to perform fresh resolve

        List affectedContainers = getAffectedContainers(javaProject.getPath());

        Iterator it = affectedContainers.iterator();
        while (it.hasNext()) {
            IvyClasspathContainer ivycp = (IvyClasspathContainer) it.next();
            ivycp.launchResolve(false, null);
        }
    }

    private void projectOpened(IResourceChangeEvent event) {

        // Find out if a project was opened.
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }

        final Collection projects = new LinkedHashSet();
        IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
        for (int i = 0; i < projDeltas.length; ++i) {
            IResourceDelta projDelta = projDeltas[i];
            if ((projDelta.getFlags() & IResourceDelta.OPEN) == 0) {
                continue;
            }
            IResource resource = projDeltas[i].getResource();
            if (!(resource instanceof IProject)) {
                continue;
            }
            IJavaProject javaProject = JavaCore.create((IProject) resource);
            List/* <IvyClasspathContainer> */containers = IvyClasspathUtil
                    .getIvyClasspathContainers(javaProject);
            Iterator/* <IvyClasspathContainer> */itContainer = containers.iterator();
            while (itContainer.hasNext()) {
                IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainer.next();
                if (!ivycp.getConf().isInheritedResolveInWorkspace()) {
                    continue;
                }
                projects.add(resource);
            }
        }

        if (projects.size() == 0) {
            return;
        }

        // Let's try to be nice and use the workspace method to schedule resolves in
        // dependent projects after the open operation has finished.
        List allContainers = getAllContainersExcludingProjects(projects);

        Iterator it = allContainers.iterator();
        while (it.hasNext()) {
            IvyClasspathContainer ivycp = (IvyClasspathContainer) it.next();
            ivycp.launchResolve(false, null);
        }
    }

    /**
     * Return the IvyDE container which include the specified project path as ivy dependency
     */
    private List getAffectedContainers(IPath projectPath) {
        List/* <IvyClasspathContainer> */allContainers = new ArrayList();

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IJavaProject[] projects;
        try {
            projects = JavaCore.create(root).getJavaProjects();
        } catch (JavaModelException e) {
            // something bad happend in the JDT...
            IvyPlugin.log(e);
            return allContainers;
        }

        for (int i = 0; i < projects.length; i++) {
            IJavaProject javaProject = projects[i];
            List/* <IvyClasspathContainer> */containers = IvyClasspathUtil
                    .getIvyClasspathContainers(javaProject);
            Iterator/* <IvyClasspathContainer> */itContainer = containers.iterator();
            while (itContainer.hasNext()) {
                IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainer.next();
                IClasspathEntry[] containerEntries = ivycp.getClasspathEntries();
                for (int j = 0; j < containerEntries.length; j++) {
                    IClasspathEntry containerEntry = containerEntries[j];
                    if (containerEntry == null
                            || containerEntry.getEntryKind() != IClasspathEntry.CPE_PROJECT
                            || !containerEntry.getPath().equals(projectPath)) {
                        continue;
                    }
                    allContainers.add(ivycp);
                    break;
                }
            }
        }

        return allContainers;
    }

    private List getAllContainersExcludingProjects(Collection sourceProjects) {
        List/* <IvyClasspathContainer> */allContainers = new ArrayList();

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IJavaProject[] projects;
        try {
            projects = JavaCore.create(root).getJavaProjects();
        } catch (JavaModelException e) {
            // something bad happend in the JDT...
            IvyPlugin.log(e);
            return allContainers;
        }

        for (int i = 0; i < projects.length; i++) {
            if (!sourceProjects.contains(projects[i])) {
                allContainers.addAll(IvyClasspathUtil.getIvyClasspathContainers(projects[i]));
            }
        }

        return allContainers;
    }

}
