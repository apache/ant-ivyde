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
package org.apache.ivyde.eclipse.revdepexplorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

public final class IvyUtil {

    private IvyUtil() {
        // utility class
    }

    /**
     * This will return all ivy projects in the workspace <br>
     * 
     * @return collection of ivy projects
     */
    public static IProject[] getIvyProjectsInWorkspace() {
        Collection/* <IProject> */ivyProjects = new HashSet();

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (int i = 0; i < projects.length; i++) {
            if (projects[i].isOpen()
                    && IvyClasspathUtil.getIvyClasspathContainers(projects[i]).size() > 0) {
                ivyProjects.add(projects[i]);
            }
        }

        return (IProject[]) ivyProjects.toArray(new IProject[ivyProjects.size()]);
    }

    public static MultiRevisionDependencyDescriptor[] getDependencyDescriptorsByProjects(
            IProject[] projects) {
        // a temporary cache of multi-revision dependency descriptors
        Map/* <ModuleId, MultiRevisionDependencyDescriptor> */mdMap = new HashMap();

        for (int i = 0; i < projects.length; i++) {
            List containers = IvyClasspathUtil.getIvyClasspathContainers(projects[i]);
            Iterator containerIter = containers.iterator();

            while (containerIter.hasNext()) {
                IvyClasspathContainer container = (IvyClasspathContainer) containerIter.next();
                ModuleDescriptor md = container.getState().getCachedModuleDescriptor();
                if (md == null) {
                    continue;
                }
                DependencyDescriptor[] descriptors = md.getDependencies();
                for (int j = 0; j < descriptors.length; j++) {
                    DependencyDescriptor descriptor = descriptors[j];
                    MultiRevisionDependencyDescriptor syncabledd = (MultiRevisionDependencyDescriptor) mdMap
                            .get(descriptor.getDependencyId());

                    if (syncabledd == null) {
                        syncabledd = new MultiRevisionDependencyDescriptor(
                                descriptor.getDependencyId());

                        mdMap.put(descriptor.getDependencyId(), syncabledd);
                    }

                    syncabledd.addDependencyDescriptor(container, descriptor);
                }
            }
        }

        List/* <MultiRevisionDependencyDescriptor> */sorted = new ArrayList(mdMap
                .values());

        Collections.sort(sorted, new Comparator/* <MultiRevisionDependencyDescriptor> */() {
            public int compare(Object o1, Object o2) {
                MultiRevisionDependencyDescriptor desc1 = (MultiRevisionDependencyDescriptor) o1;
                MultiRevisionDependencyDescriptor desc2 = (MultiRevisionDependencyDescriptor) o2;

                int equal = desc1.getOrganization().compareTo(desc2.getOrganization());
                if (equal == 0) {
                    equal = desc1.getModule().compareTo(desc2.getModule());
                }

                return equal;

            }
        });

        return (MultiRevisionDependencyDescriptor[]) sorted
                .toArray(new MultiRevisionDependencyDescriptor[sorted.size()]);
    }

    /**
     * This returns a list of multi-revision dependency descriptors which is a grouping of the
     * revisions under and organization and module <br>
     * 
     * @return multi-revision dependency descriptors
     */
    public static MultiRevisionDependencyDescriptor[] getAllDependencyDescriptorsInWorkspace() {
        return getDependencyDescriptorsByProjects(getIvyProjectsInWorkspace());
    }
}
