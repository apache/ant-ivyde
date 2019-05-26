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
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.internal.eclipse.IvyDEMessage;
import org.apache.ivyde.internal.eclipse.resolve.IvyResolver;
import org.apache.ivyde.internal.eclipse.resolve.ResolveResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class ClasspathEntriesResolver extends IvyResolver {

    private final IvyClasspathContainerConfiguration conf;

    private IClasspathEntry[] classpathEntries = null;

    private ResolveReport resolveReport;

    public ClasspathEntriesResolver(IvyClasspathContainer container, boolean usePreviousResolveIfExist) {
        super(container.getConf().getIvyXmlPath(), container.getConf().getConfs(), container.getConf()
                .getJavaProject() == null ? null : container.getConf().getJavaProject().getProject());
        this.conf = container.getConf();
        setUsePreviousResolveIfExist(conf.getInheritedAdvancedSetup().isUseExtendedResolveId());
        setUsePreviousResolveIfExist(usePreviousResolveIfExist);
        setTransitiveResolve(conf.getInheritedClasspathSetup().isTransitiveResolve());
        if (conf.getInheritedClasspathSetup().isRetrievedClasspath()) {
            RetrieveSetup setup = conf.getInheritedClasspathSetup().getRetrieveSetup();
            setRetrievePattern(setup.getRetrievePattern());
            setRetrieveSync(setup.isRetrieveSync());
            setRetrieveTypes(setup.getRetrieveTypes());
        }
    }

    protected void postResolveOrRefresh(Ivy ivy, ModuleDescriptor md, ResolveResult resolveResult,
            IProgressMonitor monitor) {
        IvyClasspathContainerMapper mapper = new IvyClasspathContainerMapper(monitor, ivy, conf,
                resolveResult);

        warnIfDuplicates(ivy, mapper, resolveResult.getArtifactReports());

        classpathEntries = mapper.map();
        resolveReport = resolveResult.getReport();
    }

    public IClasspathEntry[] getClasspathEntries() {
        return classpathEntries;
    }

    public ResolveReport getResolveReport() {
        return resolveReport;
    }

    /**
     * Trigger a warn if there are duplicates entries due to configuration conflict.
     * <p>
     * TODO: the algorithm can be more clever and find which configuration are conflicting.
     * </p>
     * @param ivy Ivy
     * @param mapper IvyClasspathContainerMapper
     * @param artifactReports Set&lt;ArtifactDownloadReport&gt;
     */
    private void warnIfDuplicates(Ivy ivy, IvyClasspathContainerMapper mapper, Set<ArtifactDownloadReport> artifactReports) {
        ArtifactDownloadReport[] reports = artifactReports.toArray(new ArtifactDownloadReport[0]);

        Set<ModuleId> duplicates = new HashSet<>();

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

        StringBuilder buffer = new StringBuilder("There are some duplicates entries due to conflicts"
                + " between the resolved configurations " + conf.getConfs());
        buffer.append(":\n  - ");
        Iterator<ModuleId> it = duplicates.iterator();
        while (it.hasNext()) {
            buffer.append(it.next());
            if (it.hasNext()) {
                buffer.append("\n  - ");
            }
        }
        IvyDEMessage.warn(buffer.toString());
    }

}
