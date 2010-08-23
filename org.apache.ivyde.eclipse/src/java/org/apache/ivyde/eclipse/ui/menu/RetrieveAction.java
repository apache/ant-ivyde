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
package org.apache.ivyde.eclipse.ui.menu;

import java.io.IOException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.RefreshFolderJob;
import org.apache.ivyde.eclipse.retrieve.StandaloneRetrieveSetup;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;

public class RetrieveAction extends Action {

    private final StandaloneRetrieveSetup retrieveSetup;

    private final IProject project;

    public RetrieveAction(IProject project, StandaloneRetrieveSetup retrieveSetup) {
        this.project = project;
        this.retrieveSetup = retrieveSetup;
    }

    public void run() {
        String pattern = project.getLocation().toPortableString() + "/"
                + retrieveSetup.getRetrieveSetup().getRetrievePattern();
        RetrieveOptions c = new RetrieveOptions();
        c.setSync(retrieveSetup.getRetrieveSetup().isRetrieveSync());
        c.setConfs(retrieveSetup.getRetrieveSetup().getRetrieveConfs().split(","));
        String inheritedRetrieveTypes = retrieveSetup.getRetrieveSetup().getRetrieveTypes();
        if (inheritedRetrieveTypes != null && !inheritedRetrieveTypes.equals("*")) {
            c.setArtifactFilter(new ArtifactTypeFilter(IvyClasspathUtil
                    .split(inheritedRetrieveTypes)));
        }
        Ivy ivy;
        ModuleDescriptor md;
        try {
            ivy = retrieveSetup.getState().getCachedIvy();
            md = retrieveSetup.getState().getCachedModuleDescriptor();
        } catch (IvyDEException e) {
            e.log(IStatus.ERROR, null);
            return;
        }
        try {
            int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, c);
            if (numberOfItemsRetrieved > 0) {
                // Only refresh if we actually retrieved a file.
                String refreshPath = IvyPatternHelper.getTokenRoot(retrieveSetup.getRetrieveSetup()
                        .getRetrievePattern());
                IFolder folder = project.getFolder(refreshPath);
                RefreshFolderJob refreshFolderJob = new RefreshFolderJob(folder);
                refreshFolderJob.schedule();
            }
        } catch (IOException e) {
            IvyPlugin.log(IStatus.ERROR, "Error while retrieving '" + retrieveSetup.getName()
                    + "' in " + retrieveSetup.getProject().getName(), e);
        }
        IvyPlugin.log(IStatus.INFO, "Sucessfull retrieve of '" + retrieveSetup.getName() + "' in "
                + retrieveSetup.getProject().getName(), null);
    }
}
