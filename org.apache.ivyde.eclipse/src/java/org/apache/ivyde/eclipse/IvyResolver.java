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
package org.apache.ivyde.eclipse;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cpcontainer.IvyResolveJobListener;
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

    protected final Ivy ivy;

    protected final IProgressMonitor monitor;

    protected final ModuleDescriptor md;

    private final boolean usePreviousResolveIfExist;

    protected LinkedHashSet/* <ArtifactDownloadReport> */all;

    private List problemMessages;

    protected String[] confs;

    private Map artifactsByDependency = new HashMap();

    private final String ivyXmlPath;

    protected final IProject project;

    public IvyResolver(String ivyXmlPath, Ivy ivy, ModuleDescriptor md,
            boolean usePreviousResolveIfExist, IProgressMonitor monitor, List confInput,
            IProject project) {
        this.project = project;
        this.ivyXmlPath = ivyXmlPath;
        this.ivy = ivy;
        this.md = md;
        this.usePreviousResolveIfExist = usePreviousResolveIfExist;
        this.monitor = monitor;
        computeConfs(confInput);
    }

    public IStatus resolve() {
        try {
            ivy.pushContext();
            IvyResolveJobListener ivyResolveJobListener = new IvyResolveJobListener(monitor);
            ivy.getEventManager().addIvyListener(ivyResolveJobListener);

            monitor.beginTask("Resolve of " + toString(), MONITOR_LENGTH);
            monitor.setTaskName("Resolve of " + toString());

            // context Classloader hook for commons logging used by httpclient
            // It will also be used by the SaxParserFactory in Ivy
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(IvyResolver.class.getClassLoader());
            try {

                if (usePreviousResolveIfExist) {
                    IStatus status = resolveWithPrevious();
                    if (!status.isOK()) {
                        return status;
                    }
                } else {
                    Message.info("\n\nIVYDE: calling resolve on " + ivyXmlPath + "\n");
                    IStatus status = doResolve();
                    if (!status.isOK()) {
                        return status;
                    }
                }

                postResolveOrRefresh();
            } catch (ParseException e) {
                String errorMsg = "Error while parsing the ivy file " + ivyXmlPath + "\n"
                        + e.getMessage();
                Message.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Error while resolving dependencies for " + ivyXmlPath + "\n"
                        + e.getMessage();
                Message.error(errorMsg);
                return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
                monitor.done();
                ivy.getEventManager().removeIvyListener(ivyResolveJobListener);
            }

            if (!problemMessages.isEmpty()) {
                MultiStatus multiStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                        "Impossible to resolve dependencies of " + md.getModuleRevisionId(), null);
                for (Iterator iter = problemMessages.iterator(); iter.hasNext();) {
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

    protected void postResolveOrRefresh() throws IOException {
        // nothing to do by default
    }

    private void computeConfs(List/* <String> */confInput) {
        Set configurations = new HashSet();
        configurations.addAll(confInput);

        if (configurations.contains("*")) {
            confs = md.getConfigurationsNames();
        } else {
            confs = (String[]) configurations.toArray(new String[configurations.size()]);
        }
    }

    private IStatus resolveWithPrevious() throws ParseException, IOException {
        all = new LinkedHashSet();
        problemMessages = new ArrayList();
        // we check if all required configurations have been
        // resolved
        boolean parsingOk = true;
        for (int i = 0; i < confs.length && parsingOk; i++) {
            File report = ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(
                ResolveOptions.getDefaultResolveId(md), confs[i]);
            parsingOk = false;
            if (report.exists()) {
                // found a report, try to parse it.
                try {
                    XmlReportParser parser = new XmlReportParser();
                    parser.parse(report);
                    all.addAll(Arrays.asList(parser.getArtifactReports()));
                    parsingOk = true;
                    findAllArtifactOnRefresh(parser);
                } catch (ParseException e) {
                    Message.info("\n\nIVYDE: Error while parsing the report " + report
                            + ". Falling back by doing a resolve again.");
                    // it fails, so let's try resolving
                }
            }
        }
        if (!parsingOk) {
            // no resolve previously done for at least one conf... we do it now
            return doResolve();
        }
        return Status.OK_STATUS;
    }

    private IStatus doResolve() throws ParseException, IOException {
        ResolveOptions resolveOption = new ResolveOptions().setConfs(confs);
        resolveOption.setValidate(ivy.getSettings().doValidate());
        ResolveReport report = ivy.resolve(md, resolveOption);
        problemMessages = report.getAllProblemMessages();

        all = new LinkedHashSet();
        for (int i = 0; i < confs.length; i++) {
            ConfigurationResolveReport configurationReport = report
                    .getConfigurationReport(confs[i]);
            Set revisions = configurationReport.getModuleRevisionIds();
            for (Iterator it = revisions.iterator(); it.hasNext();) {
                ModuleRevisionId revId = (ModuleRevisionId) it.next();
                ArtifactDownloadReport[] aReports = configurationReport.getDownloadReports(revId);
                all.addAll(Arrays.asList(aReports));
            }
        }

        confs = report.getConfigurations();
        artifactsByDependency.putAll(getArtifactsByDependency(report));
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        postDoResolve(report);
        return Status.OK_STATUS;
    }

    protected void postDoResolve(ResolveReport report) throws IOException {
        // nothing to do by default
    }

    /**
     * Populate the map of artifact. The map should be populated by metadata in cache as this is
     * called in the refresh process.
     * 
     * @param parser
     * @throws ParseException
     */
    private void findAllArtifactOnRefresh(XmlReportParser parser) throws ParseException {
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
                artifactsByDependency.put(dependencyMrdis[iDep], dependency.getDescriptor()
                        .getAllArtifacts());
            }
        }
    }

    private Map/* <ModuleRevisionId, Artifact[]> */getArtifactsByDependency(ResolveReport r) {
        Map result = new HashMap();
        for (Iterator it = r.getDependencies().iterator(); it.hasNext();) {
            IvyNode node = (IvyNode) it.next();
            if (node.getDescriptor() != null) {
                result.put(node.getResolvedId(), node.getDescriptor().getAllArtifacts());
            }
        }
        return result;
    }

    public String toString() {
        return ivyXmlPath + confs + " in " + project.getName();
    }
}
