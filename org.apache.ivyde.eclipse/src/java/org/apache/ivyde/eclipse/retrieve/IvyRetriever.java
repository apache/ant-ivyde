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
package org.apache.ivyde.eclipse.retrieve;

import java.io.IOException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.IvyResolver;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.RefreshFolderJob;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;

public class IvyRetriever extends IvyResolver {

    private final StandaloneRetrieveSetup setup;

    public IvyRetriever(Ivy ivy, ModuleDescriptor md, boolean usePreviousResolveIfExist,
            IProgressMonitor monitor, StandaloneRetrieveSetup setup) {
        super(setup.getIvyXmlPath(), ivy, md, usePreviousResolveIfExist, monitor, IvyClasspathUtil
                .split(setup.getRetrieveSetup().getRetrieveConfs()), setup.getProject());
        this.setup = setup;
    }

    protected void postResolveOrRefresh() throws IOException {
        String pattern = project.getLocation().toPortableString() + "/"
                + setup.getRetrieveSetup().getRetrievePattern();
        RetrieveOptions c = new RetrieveOptions();
        c.setSync(setup.getRetrieveSetup().isRetrieveSync());
        c.setConfs(confs);
        String inheritedRetrieveTypes = setup.getRetrieveSetup().getRetrieveTypes();
        if (inheritedRetrieveTypes != null && !inheritedRetrieveTypes.equals("*")) {
            c.setArtifactFilter(new ArtifactTypeFilter(IvyClasspathUtil
                    .split(inheritedRetrieveTypes)));
        }

        int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, c);
        if (numberOfItemsRetrieved > 0) {
            // Only refresh if we actually retrieved a file.
            String refreshPath = IvyPatternHelper.getTokenRoot(setup.getRetrieveSetup()
                    .getRetrievePattern());
            IFolder folder = project.getFolder(refreshPath);
            RefreshFolderJob refreshFolderJob = new RefreshFolderJob(folder);
            refreshFolderJob.schedule();
        }
    }
}
