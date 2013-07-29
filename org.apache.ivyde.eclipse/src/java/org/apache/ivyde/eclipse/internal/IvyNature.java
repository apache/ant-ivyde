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
package org.apache.ivyde.eclipse.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class IvyNature implements IProjectNature {

    private IProject project;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void configure() throws CoreException {
        // nothing to do
    }

    public void deconfigure() throws CoreException {
        IJavaProject javaProject = JavaCore.create(project);
        if (!javaProject.exists()) {
            return;
        }
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        List newEntries = new ArrayList();

        for (int i = 0; i < classpathEntries.length; i++) {
            if (!IvyClasspathContainerHelper.isIvyClasspathContainer(classpathEntries[i].getPath())) {
                newEntries.add(classpathEntries[i]);
            }
        }

        if (newEntries.size() != classpathEntries.length) {
            IClasspathEntry[] newClasspathEntries = (IClasspathEntry[]) newEntries
                    .toArray(new IClasspathEntry[newEntries.size()]);
            javaProject.setRawClasspath(newClasspathEntries, null);
        }

        IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
        ivyMarkerManager.removeMarkers(project);
    }

}
