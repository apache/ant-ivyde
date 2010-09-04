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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.resolve.IvyResolver;
import org.apache.ivyde.eclipse.resolve.ResolveResult;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyClasspathResolver extends IvyResolver {

    private final IvyClasspathContainerConfiguration conf;

    private final IvyClasspathContainer ivycp;

    private IClasspathEntry[] classpathEntries = null;

    public IvyClasspathResolver(IvyClasspathContainer ivycp, boolean usePreviousResolveIfExist) {
        super(ivycp.getConf().getIvyXmlPath(), ivycp.getConf().getConfs(), ivycp.getConf()
                .getJavaProject().getProject());
        this.ivycp = ivycp;
        this.conf = ivycp.getConf();
        setUsePreviousResolveIfExist(usePreviousResolveIfExist);
        if (conf.isInheritedRetrievedClasspath()) {
            RetrieveSetup setup = conf.getInheritedRetrievedClasspathSetup();
            setRetrievePattern(setup.getRetrievePattern());
            setRetrieveSync(setup.isRetrieveSync());
            setRetrieveTypes(setup.getRetrieveTypes());
        }
    }

    protected void postResolveOrRefresh(Ivy ivy, ModuleDescriptor md, ResolveResult resolveResult,
            IProgressMonitor monitor) throws IOException {
        IvyClasspathContainerMapper mapper = new IvyClasspathContainerMapper(monitor, ivy, conf,
                resolveResult);

        warnIfDuplicates(ivy, mapper, resolveResult.getArtifactReports());

        classpathEntries = mapper.map();
    }

    /*
     * Actually set the classpath only after every resolve has been done. This will avoid having the
     * classpathsetter job blocked by this resolve job, and so have a popup blocking the end user of
     * doing anything.
     */
    public void postBatchResolve() {
        if (classpathEntries != null) {
            ivycp.updateClasspathEntries(classpathEntries);
        }
    }

    /**
     * Trigger a warn if there are duplicates entries due to configuration conflict.
     * <p>
     * TODO: the algorithm can be more clever and find which configuration are conflicting.
     * 
     */
    private void warnIfDuplicates(Ivy ivy, IvyClasspathContainerMapper mapper, Set artifactReports) {
        ArtifactDownloadReport[] reports = (ArtifactDownloadReport[]) artifactReports
                .toArray(new ArtifactDownloadReport[0]);

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

}
