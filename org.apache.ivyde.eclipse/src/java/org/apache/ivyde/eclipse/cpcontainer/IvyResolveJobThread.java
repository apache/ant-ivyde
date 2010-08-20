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
import org.apache.ivy.core.IvyPatternHelper;
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
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyResolveJobThread extends Thread {

    private static final int MONITOR_LENGTH = 1000;

    private final Ivy ivy;

    private final IProgressMonitor monitor;

    private final IvyClasspathContainerConfiguration conf;

    private final ModuleDescriptor md;

    private final boolean usePreviousResolveIfExist;

    private IStatus status = null;

    private IClasspathEntry[] classpathEntries = null;

    private LinkedHashSet/* <ArtifactDownloadReport> */all;

    private List problemMessages;

    private String[] confs;

    private Map artifactsByDependency = new HashMap();

    public IvyResolveJobThread(IvyClasspathContainerConfiguration conf, Ivy ivy,
            ModuleDescriptor md, boolean usePreviousResolveIfExist, IProgressMonitor monitor) {
        this.ivy = ivy;
        this.md = md;
        this.usePreviousResolveIfExist = usePreviousResolveIfExist;
        this.monitor = monitor;
        this.conf = conf;
    }

    public IStatus getStatus() {
        return status;
    }

    public IClasspathEntry[] getClasspathEntries() {
        return classpathEntries;
    }

    public void run() {
        try {
            ivy.pushContext();
            IvyResolveJobListener ivyResolveJobListener = new IvyResolveJobListener(monitor);
            ivy.getEventManager().addIvyListener(ivyResolveJobListener);

            monitor.beginTask("resolving dependencies", MONITOR_LENGTH);
            monitor.setTaskName("resolving dependencies...");

            // context Classloader hook for commons logging used by httpclient
            // It will also be used by the SaxParserFactory in Ivy
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread()
                    .setContextClassLoader(IvyResolveJobThread.class.getClassLoader());
            try {
                computeConfs();

                if (usePreviousResolveIfExist) {
                    if (!resolveWithPrevious()) {
                        // resolve canceled
                        return;
                    }
                } else {
                    Message.info("\n\nIVYDE: calling resolve on " + conf.getIvyXmlPath() + "\n");
                    if (!resolve()) {
                        // resolve canceled
                        return;
                    }
                }

                IvyClasspathContainerMapper mapper = new IvyClasspathContainerMapper(monitor, ivy,
                        conf);

                warnIfDuplicates(mapper);

                classpathEntries = mapper.map(all, artifactsByDependency);
            } catch (ParseException e) {
                String errorMsg = "Error while parsing the ivy file " + conf.getIvyXmlPath() + "\n"
                        + e.getMessage();
                Message.error(errorMsg);
                status = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
                return;
            } catch (Exception e) {
                String errorMsg = "Error while resolving dependencies for " + conf.getIvyXmlPath()
                        + "\n" + e.getMessage();
                Message.error(errorMsg);
                status = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
                return;
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
                status = multiStatus;
                return;
            }

            status = Status.OK_STATUS;
        } catch (Throwable e) {
            status = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, "The resolve job of "
                    + conf + " has unexpectedly stopped", e);
        }
    }

    private void computeConfs() {
        Set configurations = new HashSet();
        configurations.addAll(conf.getConfs());
        if (conf.getInheritedDoRetrieve()) {
            configurations.addAll(Arrays.asList(conf.getInheritedRetrieveConfs().split(",")));
        }

        if (configurations.contains("*")) {
            confs = md.getConfigurationsNames();
        } else {
            confs = (String[]) configurations.toArray(new String[configurations.size()]);
        }
    }

    private boolean resolveWithPrevious() throws ParseException, IOException {
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
            return resolve();
        }
        return true;
    }

    private boolean resolve() throws ParseException, IOException {
        ResolveOptions resolveOption = new ResolveOptions().setConfs(confs);
        resolveOption.setValidate(ivy.getSettings().doValidate());
        ResolveReport report = ivy.resolve(md, resolveOption);
        problemMessages = report.getAllProblemMessages();

        all = new LinkedHashSet();
        for (int i = 0; i < confs.length; i++) {
            ConfigurationResolveReport configurationReport = report.getConfigurationReport(confs[i]);
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
            status = Status.CANCEL_STATUS;
            return false;
        }
        maybeRetrieve();
        return true;
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

    private void maybeRetrieve() throws IOException {
        if (!conf.getInheritedDoRetrieve()) {
            return;
        }
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
            return;
        }
        String pattern = conf.getJavaProject().getProject().getLocation().toPortableString() + "/"
                + conf.getInheritedRetrievePattern();
        monitor.setTaskName("retrieving dependencies in " + pattern);
        RetrieveOptions c = new RetrieveOptions();
        c.setSync(conf.getInheritedRetrieveSync());
        c.setConfs(conf.getInheritedRetrieveConfs().split(","));
        String inheritedRetrieveTypes = conf.getInheritedRetrieveTypes();
        if (inheritedRetrieveTypes != null && !inheritedRetrieveTypes.equals("*")) {
            c.setArtifactFilter(new ArtifactTypeFilter(IvyClasspathUtil
                    .split(inheritedRetrieveTypes)));
        }
        int numberOfItemsRetrieved = ivy.retrieve(md.getModuleRevisionId(), pattern, c);
        try {
            if (numberOfItemsRetrieved > 0 ){
                // Only refresh if we actually retrieved a file.
                monitor.setTaskName("refreshing after retrieve for pattern: " + pattern);
                String refreshPath = IvyPatternHelper.getTokenRoot(conf.getInheritedRetrievePattern());
                IFolder folder = conf.getJavaProject().getProject().getFolder(refreshPath);
                folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        } catch (CoreException e) {
            // we shouldn't get any conflict in resource changes notifications, the job running
            // this thread should be started with proper exclude rules
            throw new RuntimeException("Refresh after resolve is conflicting with another job", e);
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

}
