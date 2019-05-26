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
package org.apache.ivyde.internal.eclipse.revdepexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.eclipse.core.resources.IProject;

public final class IvyUtil {

    private IvyUtil() {
        // utility class
    }

    public static MultiRevDependencyDescriptor[] getDependencyDescriptorsByProjects(IProject[] projects) {
        // a temporary cache of multi-revision dependency descriptors
        Map<ModuleId, MultiRevDependencyDescriptor> mdMap = new HashMap<>();

        for (IProject project : projects) {
            for (IvyClasspathContainer container : IvyClasspathContainerHelper.getContainers(project)) {
                ModuleDescriptor md = ((IvyClasspathContainerImpl) container).getState().getCachedModuleDescriptor();
                if (md == null) {
                    continue;
                }
                for (DependencyDescriptor descriptor : md.getDependencies()) {
                    MultiRevDependencyDescriptor syncabledd = mdMap.get(descriptor.getDependencyId());

                    if (syncabledd == null) {
                        syncabledd = new MultiRevDependencyDescriptor(
                                descriptor.getDependencyId());

                        mdMap.put(descriptor.getDependencyId(), syncabledd);
                    }

                    syncabledd.addDependencyDescriptor(container, descriptor);
                }
            }
        }

        List<MultiRevDependencyDescriptor> sorted = new ArrayList<>(mdMap.values());

        Collections.sort(sorted, new Comparator<MultiRevDependencyDescriptor>() {
            public int compare(MultiRevDependencyDescriptor desc1, MultiRevDependencyDescriptor desc2) {
                int equal = desc1.getOrganization().compareTo(desc2.getOrganization());
                if (equal == 0) {
                    equal = desc1.getModule().compareTo(desc2.getModule());
                }
                return equal;
            }
        });

        return sorted.toArray(new MultiRevDependencyDescriptor[sorted.size()]);
    }

    /**
     * This returns a list of multi-revision dependency descriptors which is a grouping of the
     * revisions under and organization and module.
     *
     * @return multi-revision dependency descriptors
     */
    public static MultiRevDependencyDescriptor[] getAllDependencyDescriptorsInWorkspace() {
        return getDependencyDescriptorsByProjects(IvyClasspathContainerHelper.getIvyProjectsInWorkspace());
    }
}
