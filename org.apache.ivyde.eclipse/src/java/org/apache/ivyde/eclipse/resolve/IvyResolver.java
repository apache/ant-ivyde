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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.IvyDEMessage;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.workspaceresolver.WorkspaceResolver;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyResolver {

    private boolean usePreviousResolveIfExist = false;

    private String[] confs;

    private final IProject project;

    private final List confInput;

    private String retrievePattern = null;

    private boolean retrieveSync = true;

    private String retrieveTypes = null;

    private final String ivyXmlPath;

    private boolean useCacheOnly = IvyPlugin.getPreferenceStoreHelper().isOffline();

    private boolean useExtendedResolveId = false;

    public IvyResolver(String ivyXmlPath, List confInput, IProject project) {
        this.ivyXmlPath = ivyXmlPath;
        this.confInput = confInput;
        this.project = project;
    }

    public void setUsePreviousResolveIfExist(boolean usePreviousResolveIfExist) {
        this.usePreviousResolveIfExist = usePreviousResolveIfExist;
    }

    public void setRetrievePattern(String retrievePattern) {
        this.retrievePattern = retrievePattern;
    }

    public void setRetrieveSync(boolean retrieveSync) {
        this.retrieveSync = retrieveSync;
    }

    public void setRetrieveTypes(String retrieveTypes) {
        this.retrieveTypes = retrieveTypes;
    }

    public void setUseCacheOnly(boolean useCacheOnly) {
        this.useCacheOnly = useCacheOnly;
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public IProject getProject() {
        return project;
    }

    public IStatus resolve(Ivy ivy, ModuleDescriptor md, IProgressMonitor monitor, int step) {
        computeConfs(confInput, md);
        try {
            ivy.pushContext();

            IvyDEMessage.info("Resolving " + toString());

            IvyResolveJobListener ivyResolveJobListener = new IvyResolveJobListener(monitor, step);
            ivy.getEventManager().addIvyListener(ivyResolveJobListener);

            monitor.setTaskName("Resolve of " + toString());

            ResolveResult result = new ResolveResult();

            // context Classloader hook for commons logging used by httpclient
            // It will also be used by the SaxParserFactory in Ivy
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(IvyResolver.class.getClassLoader());
            try {
                if (usePreviousResolveIfExist) {
                    result = resolveWithPrevious(ivy, md);
                } else {
                    result = doResolve(ivy, md);
                }

                if (result.getProblemMessages().isEmpty()) {
                    // only continue if we resolved correctly

                    IStatus retrieveStatus = maybeRetrieve(ivy, md, result, monitor);
                    if (!retrieveStatus.isOK()) {
                        return retrieveStatus;
                    }

                    postResolveOrRefresh(ivy, md, result, monitor);
                }
            } catch (ParseException e) {
                String errorMsg = "Error while parsing the ivy file from " + this.toString() + "\n"
                        + e.getMessage();
                IvyDEMessage.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Error while resolving dependencies for " + this.toString()
                        + "\n" + e.getMessage();
                IvyDEMessage.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
                ivy.getEventManager().removeIvyListener(ivyResolveJobListener);
            }

            if (!result.getProblemMessages().isEmpty()) {
                MultiStatus multiStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                        "Impossible to resolve dependencies of " + md.getModuleRevisionId(), null);
                for (Iterator iter = result.getProblemMessages().iterator(); iter.hasNext();) {
                    multiStatus.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                            (String) iter.next(), null));
                }
                return multiStatus;
            }

            return Status.OK_STATUS;
        } catch (Throwable e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, "The resolve job of "
                    + toString() + " has unexpectedly stopped", e);
        } finally {
            ivy.popContext();
        }
    }

    protected void postResolveOrRefresh(Ivy ivy, ModuleDescriptor md, ResolveResult resolveResult,
            IProgressMonitor monitor) throws IOException {
        // nothing to do by default
    }

    private void computeConfs(List/* <String> */confInput, ModuleDescriptor md) {
        Set configurations = new HashSet();
        configurations.addAll(confInput);

        if (configurations.contains("*")) {
            confs = md.getConfigurationsNames();
        } else {
            confs = (String[]) configurations.toArray(new String[configurations.size()]);
        }
    }

    private ResolveResult resolveWithPrevious(Ivy ivy, ModuleDescriptor md) throws ParseException,
            IOException {
        IvyDEMessage.verbose("Trying to read previous resolve report");

        ResolveResult result = new ResolveResult();

        // we check if all required configurations have been resolved
        for (int i = 0; i < confs.length; i++) {
            IvyDEMessage.verbose("Fetching the resolve report for configuration " + confs[i]);

            File report = ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(
                IvyClasspathUtil.buildResolveId(useExtendedResolveId, md), confs[i]);
            if (report.exists()) {
                // found a report, try to parse it.
                try {
                    XmlReportParser parser = new XmlReportParser();
                    parser.parse(report);
                    result.addArtifactReports(parser.getArtifactReports());
                    findAllArtifactOnRefresh(ivy, parser, result);
                } catch (ParseException e) {
                    IvyDEMessage.info("Error while parsing the report " + report
                            + ". Falling back by doing a resolve again.");
                    return doResolve(ivy, md);
                }
            }
        }

        return result;
    }

    private ResolveResult doResolve(Ivy ivy, ModuleDescriptor md) throws ParseException,
            IOException {
        IvyDEMessage.debug("Doing a full resolve...");
        ResolveOptions resolveOption = new ResolveOptions();
        resolveOption.setConfs(confs);
        resolveOption.setValidate(ivy.getSettings().doValidate());
        resolveOption.setUseCacheOnly(useCacheOnly);
        resolveOption.setResolveId(IvyClasspathUtil.buildResolveId(useExtendedResolveId, md));
        ResolveReport report = ivy.getResolveEngine().resolve(md, resolveOption);

        if (report.hasError()) {
            IvyDEMessage.verbose("Resolve ended with errors");
        } else {
            IvyDEMessage.verbose("Resolve successfull");
        }

        ResolveResult result = new ResolveResult(report);

        ArtifactDownloadReport[] artifactReports = report.getArtifactsReports(null, false);

        Map/* <Artifact, ArtifactDownloadReport> */workspaceArtifacts = (Map) IvyContext
                .getContext().get(WorkspaceResolver.IVYDE_WORKSPACE_ARTIFACT_REPORTS);
        if (workspaceArtifacts != null) {
            // some artifact were 'forced' by the dependency declaration, whereas they should be
            // switch by the eclipse project reference
            for (int i = 0; i < artifactReports.length; i++) {
                ArtifactDownloadReport eclipseArtifactReport = (ArtifactDownloadReport) workspaceArtifacts
                        .get(artifactReports[i].getArtifact());
                if (eclipseArtifactReport != null) {
                    // let's switch.
                    artifactReports[i] = eclipseArtifactReport;
                }
            }
        }
        result.addArtifactReports(artifactReports);

        collectArtifactsByDependency(report, result);

        return result;
    }

    /**
     * Populate the map of artifact. The map should be populated by metadata in cache as this is
     * called in the refresh process.
     * 
     * @param parser
     * @throws ParseException
     */
    private void findAllArtifactOnRefresh(Ivy ivy, XmlReportParser parser, ResolveResult result)
            throws ParseException {
        ModuleRevisionId[] dependencyMrdis = parser.getDependencyRevisionIds();
        IvyDEMessage.verbose("Resolve report parsed. Fetching artifacts of "
                + dependencyMrdis.length + " dependencie(s)");
        for (int iDep = 0; iDep < dependencyMrdis.length; iDep++) {
            DependencyResolver depResolver = ivy.getSettings().getResolver(dependencyMrdis[iDep]);
            DefaultDependencyDescriptor depDescriptor = new DefaultDependencyDescriptor(
                    dependencyMrdis[iDep], false);
            ResolveOptions options = new ResolveOptions();
            options.setRefresh(true);
            options.setUseCacheOnly(true);
            IvyDEMessage.debug("Fetching dependency " + dependencyMrdis[iDep]);
            ResolvedModuleRevision dependency = depResolver.getDependency(depDescriptor,
                new ResolveData(ivy.getResolveEngine(), options));
            if (dependency != null) {
                Artifact[] artifacts = dependency.getDescriptor().getAllArtifacts();
                IvyDEMessage.debug("Dependency " + dependencyMrdis[iDep] + " found: "
                        + artifacts.length + " artifact(s) found");
                result.putArtifactsForDep(dependencyMrdis[iDep], artifacts);
            } else {
                IvyDEMessage.debug("Dependency " + dependencyMrdis[iDep] + " not found");
            }
        }
    }

    private void collectArtifactsByDependency(ResolveReport r, ResolveResult result) {
        for (Iterator it = r.getDependencies().iterator(); it.hasNext();) {
            IvyNode node = (IvyNode) it.next();
            if (node.getDescriptor() != null) {
                result.putArtifactsForDep(node.getResolvedId(), node.getDescriptor()
                        .getAllArtifacts());
            }
        }
    }

    private IStatus maybeRetrieve(Ivy ivy, ModuleDescriptor md, ResolveResult result,
            IProgressMonitor monitor) throws IOException {
        if (retrievePattern == null || project == null) {
            IvyDEMessage.debug("No file retrieving configured");
            return Status.OK_STATUS;
        }

        String pattern = project.getLocation().toPortableString() + "/" + retrievePattern;

        IvyDEMessage.info("Retrieving files into " + pattern);

        monitor.setTaskName("retrieving dependencies in " + pattern);
        RetrieveOptions options = new RetrieveOptions();
        options.setSync(retrieveSync);
        if (!result.isPreviousUsed()) {
            options.setResolveId(result.getReport().getResolveId());
        }
        options.setConfs(confs);
        if (retrieveTypes != null && !retrieveTypes.equals("*")) {
            List typeList = IvyClasspathUtil.split(retrieveTypes);
            options.setArtifactFilter(new ArtifactTypeFilter(typeList));
        }
        options.setResolveId(IvyClasspathUtil.buildResolveId(useExtendedResolveId, md));

        String refreshPath = IvyPatternHelper.getTokenRoot(retrievePattern);
        if (retrieveSync && refreshPath.length() == 0) {
            // the root folder of the retrieve pattern is the the project itself
            // so let's prevent from deleting the entire project
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                    "The root of the retrieve pattern is the root folder of the project."
                            + " Your project would have then been entirely deleted."
                            + " Change your retrieve pattern to have a sub folder.", null);
        }

        // Actually do the retrieve
        // FIXME here we will parse a report we already have
        // with a better Java API, we could do probably better
        int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, options);

        IvyDEMessage.info(numberOfItemsRetrieved + " retrieved file(s)");

        if (numberOfItemsRetrieved > 0) {
            // Only refresh if we actually retrieved a file.
            IFolder retrieveFolder = project.getFolder(refreshPath);
            IvyDEMessage.verbose("Refreshing Eclipse folder " + retrieveFolder);
            RefreshFolderJob refreshFolderJob = new RefreshFolderJob(retrieveFolder);
            refreshFolderJob.schedule();
        }

        // recompute the files which has been copied to build a classpath
        String resolvedPattern = IvyPatternHelper.substituteVariables(pattern, ivy.getSettings()
                .getVariables());
        try {
            // FIXME same as above
            Map retrievedArtifacts = ivy.getRetrieveEngine().determineArtifactsToCopy(
                md.getModuleRevisionId(), resolvedPattern, options);
            result.setRetrievedArtifacts(retrievedArtifacts);
        } catch (ParseException e) {
            // ooops, failed to parse a report we already have...
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                    "failed to parse a resolve report in order to do the retrieve", e);
        }

        return Status.OK_STATUS;
    }

    /**
     * This function will be called by the {@link IvyResolveJob} after all resolve has been
     * accomplished. Note that this function will be called even if the resolve failed.
     */
    public void postBatchResolve() {
        // by default do nothing
    }

    public String toString() {
        return ivyXmlPath + confInput + (project == null ? "" : (" in " + project.getName()));
    }
}
