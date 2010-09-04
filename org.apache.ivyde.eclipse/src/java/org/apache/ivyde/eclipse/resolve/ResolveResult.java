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
package org.apache.ivyde.eclipse.resolve;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;

/**
 * Container of result of an Ivy resolve and maybe retrieve
 */
public class ResolveResult {

    private final boolean previousUsed;

    private Set/* <ArtifactDownloadReport> */artifactReports = new LinkedHashSet();

    private Set problemMessages = new HashSet();

    private final ResolveReport report;

    private Map/* <ModuleRevisionId, Artifact[]> */artifactsByDependency = new HashMap();

    /**
     * Mapping of resolved artifact to their retrieved path, <code>null</code> if there were no
     * retrieve
     * <p>
     * The paths may be relative It shouldn't be an issue has every relative path should be relative
     * to the eclipse project FIXME: not sure why the Ivy API is returning a set of paths...
     */
    private Map/* <ArtifactDownloadReport, Set<String>> */retrievedArtifacts;

    /**
     * Constructor to be used when the resolve have been refreshed
     */
    ResolveResult() {
        report = null;
        previousUsed = true;
    }

    /**
     * Constructor to be used based on the fresh resolve report
     */
    ResolveResult(ResolveReport report) {
        this.report = report;
        previousUsed = false;
        problemMessages = new HashSet(report.getAllProblemMessages());
    }

    /**
     * 
     * @return <code>true</code> if the refresh has been successful
     */
    public boolean isPreviousUsed() {
        return previousUsed;
    }

    /**
     * 
     * @return the report from the resolve, <code>null</code> if there was a successful refresh
     */
    public ResolveReport getReport() {
        return report;
    }

    /**
     * Get the list of errors of the resolve. They will be used to build a {@link MultiStatus}.
     * 
     * @return the list of error message
     */
    public Set/* <String> */getProblemMessages() {
        return problemMessages;
    }

    void addArtifactReports(ArtifactDownloadReport[] reports) {
        artifactReports.addAll(Arrays.asList(reports));
    }

    /**
     * 
     * @return the reports of the artifacts resolved
     */
    public Set/* <ArtifactDownloadReport> */getArtifactReports() {
        return artifactReports;
    }

    void putArtifactsForDep(ModuleRevisionId mrid, Artifact[] artifacts) {
        artifactsByDependency.put(mrid, artifacts);
    }

    /**
     * 
     * @return the reports of the artifacts by dependency
     */
    public Map /* <ModuleRevisionId, Artifact[]> */getArtifactsByDependency() {
        return artifactsByDependency;
    }

    void setRetrievedArtifacts(Map retrievedArtifacts) {
        this.retrievedArtifacts = retrievedArtifacts;
    }

    /**
     * 
     * @return the path(s) of the retrieved artifacts
     */
    public Map/* <ArtifactDownloadReport, Set<String>> */getRetrievedArtifacts() {
        return retrievedArtifacts;
    }
}
