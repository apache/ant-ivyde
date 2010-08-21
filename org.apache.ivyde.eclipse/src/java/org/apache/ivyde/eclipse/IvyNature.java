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
package org.apache.ivyde.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class IvyNature implements IProjectNature {

    public static final String IVY_NATURE = "org.apache.ivyde.eclipse.ivyNature";

    private IProject project;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void configure() throws CoreException {
        // nothin to do
    }

    public void deconfigure() throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        if (!javaProject.exists()) {
            return;
        }
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        List newEntries = new ArrayList();

        for (int i = 0; i < classpathEntries.length; i++) {
            if (!IvyClasspathUtil.isIvyClasspathContainer(classpathEntries[i].getPath())) {
                newEntries.add(classpathEntries[i]);
            }
        }

        if (newEntries.size() != classpathEntries.length) {
            IClasspathEntry[] newClasspathEntries = (IClasspathEntry[]) newEntries
                    .toArray(new IClasspathEntry[newEntries.size()]);
            javaProject.setRawClasspath(newClasspathEntries, null);
        }
    }

    public static boolean hasNature(IProject project) {
        try {
            return project.hasNature(IVY_NATURE);
        } catch (CoreException e) {
            IvyPlugin.log(IStatus.ERROR, "Unable to get the Ivy nature of the project "
                    + project.getName(), e);
            return false;
        }
    }

    public static void addNature(final IProject project) {
        try {
            if (hasNature(project)) {
                return;
            }

            final IProjectDescription description = project.getDescription();
            final String[] ids = description.getNatureIds();

            final String[] newIds = new String[ids == null ? 1 : ids.length + 1];
            if (ids != null) {
                System.arraycopy(ids, 0, newIds, 0, ids.length);
            }
            newIds[ids == null ? 0 : ids.length] = IVY_NATURE;

            description.setNatureIds(newIds);
            project.setDescription(description, null);
        } catch (Exception e) {
            IvyPlugin.log(IStatus.ERROR, "Failed to add Ivy dependency management on "
                    + project.getName(), e);
        }
    }

    public static void removeNature(final IProject project) {
        try {
            if (!hasNature(project)) {
                return;
            }

            final IProjectDescription description = project.getDescription();
            final String[] ids = description.getNatureIds();
            if (ids == null || ids.length == 0) {
                // wtf ? it has the Ivy nature but there is none ?
                return;
            }

            int i;
            for (i = 0; i < ids.length; i++) {
                if (IVY_NATURE.equals(ids[i])) {
                    break;
                }
            }
            if (i == ids.length) {
                // wtf ? it has the Ivy nature but we cannot find it ?
                return;
            }

            final String[] newIds = new String[ids.length - 1];
            if (i > 0) {
                System.arraycopy(ids, 0, newIds, 0, i);
            }
            if (i < ids.length - 1) {
                System.arraycopy(ids, i + 1, newIds, i, ids.length - i - 1);
            }

            description.setNatureIds(newIds);
            project.setDescription(description, null);
        } catch (Exception e) {
            IvyPlugin.log(IStatus.ERROR, "Failed to remove Ivy dependency management on "
                    + project.getName(), e);
        }
    }

}
