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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

/**
 * This job synchronizes all ivy files in a workspace according to the new revisions specified in
 * the ivy explorer.
 */
public class SyncIvyFilesJob extends WorkspaceJob {

    private MultiRevisionDependencyDescriptor[] multiRevisionDependencies;

    public SyncIvyFilesJob(MultiRevisionDependencyDescriptor[] multiRevisionDependencies) {
        super("Synchronizing Ivy Files");
        this.multiRevisionDependencies = multiRevisionDependencies;
    }

    protected IStatus executeJob(IProgressMonitor monitor) {
        MultiStatus errorStatuses = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Failed to update one or more Ivy files.  See details.", null);

        IvyClasspathContainer[] containers = getIvyClasspathContainers();
        for (int i = 0; i < containers.length; i++) {
            IvyClasspathContainer container = containers[i];

            EditableModuleDescriptor moduleDescriptor;
            try {
                moduleDescriptor = new EditableModuleDescriptor(container.getState()
                        .getModuleDescriptor());
            } catch (IvyDEException e) {
                errorStatuses
                        .add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                                "Failed to get module descriptor at "
                                        + container.getConf().getIvyXmlPath(), e));
                continue;
            }
            Collection/* <MultiRevisionDependencyDescriptor> */newRevisions = getNewRevisions(container);

            Iterator multiRevisionIter = newRevisions.iterator();
            while (multiRevisionIter.hasNext()) {
                MultiRevisionDependencyDescriptor newRevision = (MultiRevisionDependencyDescriptor) multiRevisionIter
                        .next();

                DependencyDescriptor dependencyDescriptors[] = moduleDescriptor.getDependencies();
                for (int j = 0; j < dependencyDescriptors.length; j++) {
                    DependencyDescriptor dependencyDescriptor = dependencyDescriptors[j];
                    if (newRevision.getModuleId().equals(dependencyDescriptor.getDependencyId())) {
                        EditableDependencyDescriptor editableDependencyDescriptor = new EditableDependencyDescriptor(
                                dependencyDescriptor);
                        editableDependencyDescriptor.setRevision(newRevision.getNewRevision());
                        moduleDescriptor.removeDependency(dependencyDescriptor);
                        moduleDescriptor.addDependency(editableDependencyDescriptor);
                    }
                }
            }

            try {
                IvyClasspathUtil.toIvyFile(moduleDescriptor, container);
            } catch (ParseException e) {
                errorStatuses.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                        "Failed to write Ivy file " + container.getState().getIvyFile().getPath(),
                        e));
            } catch (IOException e) {
                errorStatuses.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                        "Failed to write Ivy file " + container.getState().getIvyFile().getPath(),
                        e));
            }
        }

        if (errorStatuses.getChildren().length > 0) {
            return errorStatuses;
        }
        return Status.OK_STATUS;
    }

    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IStatus status = Status.OK_STATUS;

        try {
            status = executeJob(monitor);
        } catch (OperationCanceledException ignore) {
            return Status.CANCEL_STATUS;
        }

        return status;
    }

    private IvyClasspathContainer[] getIvyClasspathContainers() {
        Collection/* <IvyClasspathContainer> */containers = new HashSet();

        for (int i = 0; i < multiRevisionDependencies.length; i++) {
            MultiRevisionDependencyDescriptor multiRevision = multiRevisionDependencies[i];
            if (multiRevision.hasNewRevision()) {
                containers.addAll(Arrays.asList(multiRevision.getIvyClasspathContainers()));
            }
        }

        return (IvyClasspathContainer[]) containers.toArray(new IvyClasspathContainer[containers
                .size()]);
    }

    /**
     * Return the new revision changes for a given project <br>
     * 
     * @param project
     *            project
     * @return multiRevision descriptors
     */
    private Collection/* <MultiRevisionDependencyDescriptor> */getNewRevisions(
            IvyClasspathContainer container) {
        Collection/* <MultiRevisionDependencyDescriptor> */list = new ArrayList();

        for (int i = 0; i < multiRevisionDependencies.length; i++) {
            MultiRevisionDependencyDescriptor multiRevision = multiRevisionDependencies[i];
            if (multiRevision.hasNewRevision() && multiRevision.isForContainer(container)) {
                list.add(multiRevision);
            }
        }

        return list;
    }
}
