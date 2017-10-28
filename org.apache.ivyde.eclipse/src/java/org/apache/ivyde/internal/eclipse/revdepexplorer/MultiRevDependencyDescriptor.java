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
package org.apache.ivyde.internal.eclipse.revdepexplorer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;

public class MultiRevDependencyDescriptor {
    private final ModuleId moduleId;

    /**
     * Map<IvyClasspathContainer, Collection<DependencyDescriptor>>
     */
    private final Map dependenciesByContainer = new HashMap();

    private String newRevision;

    public MultiRevDependencyDescriptor(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    public int hashCode() {
        return getOrganization().hashCode() + getModule().hashCode()
                + dependenciesByContainer.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof MultiRevDependencyDescriptor) {
            MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) o;

            if (getOrganization().equals(mrdd.getOrganization())
                    && getModule().equals(mrdd.getModule())
                    && dependenciesByContainer.equals(mrdd.dependenciesByContainer)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasMultipleRevisions() {
        return getRevisions().length > 1;
    }

    /**
     * @param container
     *            classpath container
     * @param dependencyDescriptor
     *            current descriptor
     */
    public void addDependencyDescriptor(IvyClasspathContainerImpl container,
            DependencyDescriptor dependencyDescriptor) {
        Collection/* <DependencyDescriptor> */dependencies = (Collection) dependenciesByContainer
                .get(container);

        if (dependencies == null) {
            dependencies = new HashSet/* <DependencyDescriptor> */();
            dependenciesByContainer.put(container, dependencies);
        }

        dependencies.add(dependencyDescriptor);
    }

    /**
     * @return module name
     */
    public String getModule() {
        return moduleId.getName();
    }

    /**
     * @return organization name
     */
    public String getOrganization() {
        return moduleId.getOrganisation();
    }

    /**
     * @return all revisions
     */
    public String[] getRevisions() {
        Set/* <String> */revisions = new HashSet/* <String> */();

        /* Collection<Collection<DependencyDescriptor>> */
        Collection projectDependencyDescriptors = dependenciesByContainer.values();

        Iterator it = projectDependencyDescriptors.iterator();
        while (it.hasNext()) {
            Collection/* <DependencyDescriptor> */projectCollection = (Collection) it.next();
            Iterator descriptorIter = projectCollection.iterator();
            while (descriptorIter.hasNext()) {
                DependencyDescriptor descriptor = (DependencyDescriptor) descriptorIter.next();
                revisions.add(descriptor.getDependencyRevisionId().getRevision());
            }
        }

        return (String[]) revisions.toArray(new String[revisions.size()]);
    }

    /**
     * @return true if a new revision has been applied
     */
    public boolean hasNewRevision() {
        if (newRevision == null || "".equals(newRevision.trim())) {
            return false;
        }

        return true;
    }

    /**
     * @param newRevision
     *            new revision
     */
    public void setNewRevision(String newRevision) {
        this.newRevision = newRevision;
    }

    /**
     * @return new revision
     */
    public String getNewRevision() {
        return newRevision;
    }

    /**
     * @return all projects
     */
    public IvyClasspathContainerImpl[] getIvyClasspathContainers() {
        Collection containers = dependenciesByContainer.keySet();
        return (IvyClasspathContainerImpl[]) containers.toArray(new IvyClasspathContainerImpl[containers
                .size()]);
    }

    /**
     * @param container
     *            classpath container
     * @return true if there is a project match
     */
    public boolean isForContainer(IvyClasspathContainerImpl container) {
        IvyClasspathContainerImpl[] containers = getIvyClasspathContainers();
        for (int i = 0; i < containers.length; i++) {
            IvyClasspathContainerImpl currentContainer = containers[i];
            if (currentContainer.equals(container)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return ModuleId
     */
    public ModuleId getModuleId() {
        return moduleId;
    }

    /**
     * This returns the current revisions for a project.
     *
     * @param container
     *            container
     * @return revision
     */
    public String[] getRevisions(IvyClasspathContainerImpl container) {
        /* Collection<DependencyDescriptor> */
        Collection containerDependencyDescriptors = (Collection) dependenciesByContainer
                .get(container);

        if (containerDependencyDescriptors == null) {
            return new String[0];
        }

        Set/* <String> */revisions = new HashSet/* <String> */();

        Iterator iter = containerDependencyDescriptors.iterator();
        while (iter.hasNext()) {
            DependencyDescriptor descriptor = (DependencyDescriptor) iter.next();
            revisions.add(descriptor.getDependencyRevisionId().getRevision());
        }

        return (String[]) revisions.toArray(new String[revisions.size()]);
    }
}
