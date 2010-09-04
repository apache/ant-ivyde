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
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
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

    private static final int MONITOR_LENGTH = 1000;

    private boolean usePreviousResolveIfExist = false;

    private String[] confs;

    private final IProject project;

    private final List confInput;

    private String retrievePattern = null;

    private boolean retrieveSync = true;

    private String retrieveTypes = null;

    private final String ivyXmlPath;

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

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public IProject getProject() {
        return project;
    }

    public IStatus resolve(Ivy ivy, ModuleDescriptor md, IProgressMonitor monitor) {
        computeConfs(confInput, md);
        try {
            ivy.pushContext();
            IvyResolveJobListener ivyResolveJobListener = new IvyResolveJobListener(monitor);
            ivy.getEventManager().addIvyListener(ivyResolveJobListener);

            monitor.beginTask("Resolve of " + toString(), MONITOR_LENGTH);
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

                maybeRetrieve(ivy, md, result, monitor);

                postResolveOrRefresh(ivy, md, result, monitor);
            } catch (ParseException e) {
                String errorMsg = "Error while parsing the ivy file from " + this.toString() + "\n"
                        + e.getMessage();
                Message.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Error while resolving dependencies for " + this.toString()
                        + "\n" + e.getMessage();
                Message.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
                monitor.done();
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
        ResolveResult result = new ResolveResult();

        // we check if all required configurations have been resolved
        for (int i = 0; i < confs.length; i++) {
            File report = ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(
                ResolveOptions.getDefaultResolveId(md), confs[i]);
            if (report.exists()) {
                // found a report, try to parse it.
                try {
                    XmlReportParser parser = new XmlReportParser();
                    parser.parse(report);
                    result.addArtifactReports(parser.getArtifactReports());
                    findAllArtifactOnRefresh(ivy, parser, result);
                } catch (ParseException e) {
                    Message.info("\n\nIVYDE: Error while parsing the report " + report
                            + ". Falling back by doing a resolve again.");
                    // it fails, so let's try resolving for all configuration
                    return doResolve(ivy, md);
                }
            }
        }

        return result;
    }

    private ResolveResult doResolve(Ivy ivy, ModuleDescriptor md) throws ParseException,
            IOException {
        ResolveOptions resolveOption = new ResolveOptions().setConfs(confs);
        resolveOption.setValidate(ivy.getSettings().doValidate());
        ResolveReport report = ivy.resolve(md, resolveOption);

        ResolveResult result = new ResolveResult(report);

        for (int i = 0; i < confs.length; i++) {
            ConfigurationResolveReport configurationReport = report
                    .getConfigurationReport(confs[i]);
            Set revisions = configurationReport.getModuleRevisionIds();
            for (Iterator it = revisions.iterator(); it.hasNext();) {
                ModuleRevisionId revId = (ModuleRevisionId) it.next();
                result.addArtifactReports(configurationReport.getDownloadReports(revId));
            }
        }

        confs = report.getConfigurations();
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
        for (int iDep = 0; iDep < dependencyMrdis.length; iDep++) {
            DependencyResolver depResolver = ivy.getSettings().getResolver(dependencyMrdis[iDep]);
            DefaultDependencyDescriptor depDescriptor = new DefaultDependencyDescriptor(
                    dependencyMrdis[iDep], false);
            ResolveOptions options = new ResolveOptions();
            options.setRefresh(true);
            options.setUseCacheOnly(true);
            ResolvedModuleRevision dependency = depResolver.getDependency(depDescriptor,
                new ResolveData(ivy.getResolveEngine(), options));
            if (dependency != null) {
                result.putArtifactsForDep(dependencyMrdis[iDep], dependency.getDescriptor()
                        .getAllArtifacts());
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

    private void maybeRetrieve(Ivy ivy, ModuleDescriptor md, ResolveResult result,
            IProgressMonitor monitor) throws IOException {
        if (result.isPreviousUsed() || retrievePattern == null
                || FakeProjectManager.isFake(project)) {
            return;
        }

        String pattern = project.getLocation().toPortableString() + "/" + retrievePattern;
        monitor.setTaskName("retrieving dependencies in " + pattern);
        RetrieveOptions options = new RetrieveOptions();
        options.setSync(retrieveSync);
        options.setResolveId(result.getReport().getResolveId());
        options.setConfs(confs);
        if (retrieveTypes != null && !retrieveTypes.equals("*")) {
            List typeList = IvyClasspathUtil.split(retrieveTypes);
            options.setArtifactFilter(new ArtifactTypeFilter(typeList));
        }

        // Actually do the retrieve
        // FIXME here we will parse a report we already have
        // with a better Java API, we could do probably better
        int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, options);
        if (numberOfItemsRetrieved > 0) {
            // Only refresh if we actually retrieved a file.
            String refreshPath = IvyPatternHelper.getTokenRoot(retrievePattern);
            IFolder folder = project.getFolder(refreshPath);
            RefreshFolderJob refreshFolderJob = new RefreshFolderJob(folder);
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
            IvyPlugin.log(IStatus.ERROR,
                "failed to parse a resolve report in order to do the retrieve", e);
        }
    }

    /**
     * This function will be called by the {@link IvyResolveJob} after all resolve has been
     * accomplished. Note that this function will be called even if the resolve failed.
     */
    public void postBatchResolve() {
        // by default do nothing
    }

    public String toString() {
        return ivyXmlPath + confInput + " in " + project.getName();
    }
}
