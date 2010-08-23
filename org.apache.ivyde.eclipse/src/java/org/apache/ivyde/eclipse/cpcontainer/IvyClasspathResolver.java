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
package org.apache.ivyde.eclipse.cpcontainer;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.IvyResolver;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyClasspathResolver extends IvyResolver {

    private final IvyClasspathContainerConfiguration conf;

    private IClasspathEntry[] classpathEntries = null;

    private LinkedHashSet/* <ArtifactDownloadReport> */all;

    private Map artifactsByDependency = new HashMap();

    /**
     * Mapping of resolved artifact to their retrieved path, <code>null</code> if there were no
     * retrieve
     * <p>
     * The paths may be relative It shouldn't be an issue has every relative path should be relative
     * to the eclipse project FIXME: not sure why the Ivy API is returning a set of paths...
     */
    private Map/* <ArtifactDownloadReport, Set<String>> */retrievedArtifacts = null;

    public IvyClasspathResolver(IvyClasspathContainerConfiguration conf, Ivy ivy,
            ModuleDescriptor md, boolean usePreviousResolveIfExist, IProgressMonitor monitor) {
        super(conf.getIvyXmlPath(), ivy, md, usePreviousResolveIfExist, monitor, conf.getConfs(),
                conf.getJavaProject().getProject());
        this.conf = conf;
    }

    public IClasspathEntry[] getClasspathEntries() {
        return classpathEntries;
    }

    protected void postResolveOrRefresh() {
        IvyClasspathContainerMapper mapper = new IvyClasspathContainerMapper(monitor, ivy, conf,
                all, artifactsByDependency, retrievedArtifacts);

        warnIfDuplicates(mapper);

        classpathEntries = mapper.map();
    }

    protected void postDoResolve(ResolveReport report) throws IOException {
        maybeRetrieve(report);
    }

    /**
     * Trigger a warn if there are duplicates entries due to configuration conflict.
     * <p>
     * TODO: the algorithm can be more clever and find which configuration are conflicting.
     * 
     */
    private void warnIfDuplicates(IvyClasspathContainerMapper mapper) {
        ArtifactDownloadReport[] reports = (ArtifactDownloadReport[]) all
                .toArray(new ArtifactDownloadReport[all.size()]);

        Set duplicates = new HashSet();

        for (int i = 0; i < reports.length - 1; i++) {
            if (!mapper.accept(reports[i].getArtifact())) {
                continue;
            }

            ModuleRevisionId mrid1 = reports[i].getArtifact().getModuleRevisionId();

            for (int j = i + 1; j < reports.length; j++) {
                if (!mapper.accept(reports[j].getArtifact())) {
                    continue;
                }
                ModuleRevisionId mrid2 = reports[j].getArtifact().getModuleRevisionId();
                if (mrid1.getModuleId().equals(mrid2.getModuleId())
                        && !mrid1.getRevision().equals(mrid2.getRevision())) {
                    duplicates.add(mrid1.getModuleId());
                    break;
                }
            }
        }

        if (duplicates.isEmpty()) {
            return;
        }

        StringBuffer buffer = new StringBuffer("There are some duplicates entries due to conflicts"
                + " between the resolved configurations " + conf.getConfs());
        buffer.append(":\n  - ");
        Iterator it = duplicates.iterator();
        while (it.hasNext()) {
            buffer.append(it.next());
            if (it.hasNext()) {
                buffer.append("\n  - ");
            }
        }
        ivy.getLoggerEngine().log(buffer.toString(), Message.MSG_WARN);
    }

    private void maybeRetrieve(ResolveReport report) throws IOException {
        if (!conf.isInheritedRetrievedClasspath()) {
            return;
        }
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
            return;
        }
        RetrieveSetup setup = conf.getInheritedRetrievedClasspathSetup();
        IProject project = conf.getJavaProject().getProject();
        String pattern = project.getLocation().toPortableString() + "/"
                + setup.getRetrievePattern();
        monitor.setTaskName("retrieving dependencies in " + pattern);
        RetrieveOptions options = new RetrieveOptions();
        options.setSync(setup.isRetrieveSync());
        options.setResolveId(report.getResolveId());
        options.setConfs(confs);
        String inheritedRetrieveTypes = setup.getRetrieveTypes();
        if (inheritedRetrieveTypes != null && !inheritedRetrieveTypes.equals("*")) {
            options.setArtifactFilter(new ArtifactTypeFilter(IvyClasspathUtil
                    .split(inheritedRetrieveTypes)));
        }

        // Actually do the retrieve
        // FIXME here we will parse a report we already have
        // with a better Java API, we could do probably better
        int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        if (numberOfItemsRetrieved > 0) {
            // Only refresh if we actually retrieved a file.
            String refreshPath = IvyPatternHelper.getTokenRoot(setup.getRetrievePattern());
            IFolder folder = project.getFolder(refreshPath);
            RefreshFolderJob refreshFolderJob = new RefreshFolderJob(folder);
            refreshFolderJob.schedule();
        }

        // recompute the files which has been copied to build a classpath
        String resolvedPattern = IvyPatternHelper.substituteVariables(pattern, ivy.getSettings()
                .getVariables());
        try {
            // FIXME same as above
            retrievedArtifacts = ivy.getRetrieveEngine().determineArtifactsToCopy(
                md.getModuleRevisionId(), resolvedPattern, options);
        } catch (ParseException e) {
            // ooops, failed to parse a report we already have...
            IvyPlugin.log(IStatus.ERROR,
                "failed to parse a resolve report in order to do the retrieve", e);
            return;
        }
        all = new LinkedHashSet(retrievedArtifacts.keySet());
    }

}
