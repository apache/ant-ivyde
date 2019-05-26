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
package org.apache.ivyde.eclipse.cp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.IStructuredSelection;

public final class IvyClasspathContainerHelper {

    private IvyClasspathContainerHelper() {
        // utility class
    }

    /**
     * Get the Ivy classpath container from the selection in the Java package view
     *
     * @param selection
     *            the selection
     * @return IvyClasspathContainer
     */
    public static IvyClasspathContainer getContainer(IStructuredSelection selection) {
        if (selection == null) {
            return null;
        }
        for (Object element : selection.toList()) {
            IvyClasspathContainerImpl ivycp = IvyPlugin.adapt(element,
                IvyClasspathContainerImpl.class);
            if (ivycp != null) {
                return ivycp;
            }
            if (element instanceof ClassPathContainer) {
                // FIXME: we shouldn't check against internal JDT API but they are not adaptable to
                // useful class
                return IvyClasspathUtil.jdt2IvyCPC((ClassPathContainer) element);
            }
        }
        return null;
    }

    public static boolean isIvyClasspathContainer(IPath containerPath) {
        return containerPath.segment(0).equals(IvyClasspathContainer.ID);
    }

    /**
     * Search the Ivy classpath containers within the specified Java project
     *
     * @param javaProject
     *            the project to search into
     * @return the Ivy classpath container if found
     */
    public static List<IvyClasspathContainer> getContainers(IJavaProject javaProject) {
        List<IvyClasspathContainer> containers = new ArrayList<>();
        if (javaProject == null || !javaProject.exists()) {
            return containers;
        }
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path)) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        if (cp instanceof IvyClasspathContainer) {
                            containers.add((IvyClasspathContainer) cp);
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return containers;
    }

    public static List<IvyClasspathContainer> getContainersFromIvyFile(IFile ivyfile) {
        IJavaProject javaProject = JavaCore.create(ivyfile.getProject());
        List<IvyClasspathContainer> containers = new ArrayList<>();
        if (javaProject == null || !javaProject.exists()) {
            return containers;
        }
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path)) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        if (cp instanceof IvyClasspathContainerImpl) {
                            IvyClasspathContainerImpl ivycp = (IvyClasspathContainerImpl) cp;
                            if (ivycp.getConf().getIvyXmlPath()
                                    .equals(ivyfile.getProjectRelativePath().toString())) {
                                containers.add(ivycp);
                            }
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return containers;
    }

    public static List<IvyClasspathContainer> getContainersFromIvySettings(IFile ivySettings) {
        IJavaProject javaProject = JavaCore.create(ivySettings.getProject());
        List<IvyClasspathContainer> containers = new ArrayList<>();
        if (javaProject == null || !javaProject.exists()) {
            return containers;
        }
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path)) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        if (cp instanceof IvyClasspathContainerImpl) {
                            IvyClasspathContainerImpl ivycp = (IvyClasspathContainerImpl) cp;
                            ResolvedPath settingsPath = ivycp.getConf().getInheritedSettingsSetup()
                                    .getResolvedIvySettingsPath(ivycp.getConf().getProject());
                            if (settingsPath.getResolvedPath().equals(
                                ivySettings.getProjectRelativePath().toString())) {
                                containers.add(ivycp);
                            }
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return containers;
    }

    public static IvyClasspathContainer getContainer(IPath containerPath, IJavaProject javaProject) {
        IClasspathContainer cp;
        try {
            cp = JavaCore.getClasspathContainer(containerPath, javaProject);
        } catch (JavaModelException e) {
            IvyPlugin.log(e);
            return null;
        }
        if (!(cp instanceof IvyClasspathContainerImpl)) {
            IvyPlugin.logError("Expected an Ivy container but was " + cp.getClass().getName()
                    + " for path " + containerPath);
            return null;
        }
        return (IvyClasspathContainerImpl) cp;
    }

    /**
     * Search the Ivy classpath entry within the specified Java project with the specific path.
     *
     * @param containerPath
     *            the path of the container
     * @param javaProject
     *            the project to search into
     * @return the Ivy classpath container if found, otherwise return <code>null</code>
     */
    public static IClasspathEntry getEntry(IPath containerPath, IJavaProject javaProject) {
        if (javaProject == null || !javaProject.exists()) {
            return null;
        }
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
                        && containerPath.equals(entry.getPath())) {
                    return entry;
                }
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return null;
    }

    public static List<IvyClasspathContainer> getContainers(IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null && javaProject.exists()) {
            return getContainers(javaProject);
        }
        return Collections.emptyList();
    }

    /**
     * This will return all ivy projects in the workspace.
     *
     * @return collection of ivy projects
     */
    public static IProject[] getIvyProjectsInWorkspace() {
        Collection<IProject> ivyProjects = new HashSet<>();

        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (project.isOpen() && getContainers(project).size() > 0) {
                ivyProjects.add(project);
            }
        }

        return ivyProjects.toArray(new IProject[ivyProjects.size()]);
    }

}
