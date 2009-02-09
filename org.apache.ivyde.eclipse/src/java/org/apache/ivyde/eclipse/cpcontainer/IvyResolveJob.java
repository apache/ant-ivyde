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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyResolveJob extends Job implements TransferListener, IvyListener {
    private static final int DOWNLOAD_MONITOR_LENGTH = 100;

    private static final int KILO_BITS_UNIT = 1024;

    private static final int MONITOR_LENGTH = 1000;

    private static final int WAIT_FOR_JOIN = 100;

    private long expectedTotalLength = 1;

    private long currentLength = 0;

    private IProgressMonitor monitor;

    private IProgressMonitor dlmonitor;

    private boolean usePreviousResolveIfExist;

    private int workPerArtifact = 100;

    private Ivy ivy;

    private final IvyClasspathContainerConfiguration conf;

    private final IvyClasspathContainer container;

    private ModuleDescriptor md;

    public IvyResolveJob(IvyClasspathContainer container, boolean usePreviousResolveIfExist) {
        super("Ivy resolve job of " + container.getConf());
        this.container = container;
        this.conf = container.getConf();
        this.usePreviousResolveIfExist = usePreviousResolveIfExist;
    }

    public void transferProgress(TransferEvent evt) {
        switch (evt.getEventType()) {
            case TransferEvent.TRANSFER_INITIATED:
                monitor.setTaskName("downloading " + evt.getResource());
                break;
            case TransferEvent.TRANSFER_STARTED:
                currentLength = 0;
                if (evt.isTotalLengthSet()) {
                    expectedTotalLength = evt.getTotalLength();
                    dlmonitor
                            .beginTask("downloading " + evt.getResource(), DOWNLOAD_MONITOR_LENGTH);
                }
                break;
            case TransferEvent.TRANSFER_PROGRESS:
                if (expectedTotalLength > 1) {
                    currentLength += evt.getLength();
                    int progress = (int) (currentLength * DOWNLOAD_MONITOR_LENGTH / expectedTotalLength);
                    dlmonitor.worked(progress);
                    monitor.subTask((currentLength / KILO_BITS_UNIT) + " / "
                            + (expectedTotalLength / KILO_BITS_UNIT) + "kB");
                }
                break;
            default:
        }
    }

    public void progress(IvyEvent event) {
        if (event instanceof TransferEvent) {
            if (dlmonitor != null) {
                transferProgress((TransferEvent) event);
            }
        } else if (event instanceof PrepareDownloadEvent) {
            PrepareDownloadEvent pde = (PrepareDownloadEvent) event;
            Artifact[] artifacts = pde.getArtifacts();
            if (artifacts.length > 0) {
                workPerArtifact = MONITOR_LENGTH / artifacts.length;
            }
        } else if (event instanceof StartArtifactDownloadEvent) {
            StartArtifactDownloadEvent evt = (StartArtifactDownloadEvent) event;
            monitor.setTaskName("downloading " + evt.getArtifact());
            if (dlmonitor != null) {
                dlmonitor.done();
            }
            dlmonitor = new SubProgressMonitor(monitor, workPerArtifact);
        } else if (event instanceof EndArtifactDownloadEvent) {
            if (dlmonitor != null) {
                dlmonitor.done();
            }
            monitor.subTask(" ");
            dlmonitor = null;
        } else if (event instanceof StartResolveDependencyEvent) {
            StartResolveDependencyEvent ev = (StartResolveDependencyEvent) event;
            monitor.subTask("resolving " + ev.getDependencyDescriptor().getDependencyRevisionId());
        } else if (event instanceof EndResolveDependencyEvent) {
            monitor.subTask(" ");
        }
    }

    private Map dependenciesAsMap(ResolveReport r) {
        Map result = new HashMap();
        for (Iterator it = r.getDependencies().iterator(); it.hasNext();) {
            IvyNode node = (IvyNode) it.next();
            result.put(node.getId(), node);
        }
        return result;
    }

    protected IStatus run(IProgressMonitor m) {
        Message.info("resolving dependencies of " + conf);
        this.monitor = m;
        final IStatus[] status = new IStatus[1];
        final IClasspathEntry[][] classpathEntries = new IClasspathEntry[1][];

        try {
            this.ivy = conf.getIvy();
            this.md = conf.getModuleDescriptor();
        } catch (IvyDEException e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, e.getMessage(), e);
        } catch (Throwable e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, "Unexpected error ["
                    + e.getClass().getCanonicalName() + "]: " + e.getMessage(), e);
        }

        Thread resolver = new Thread() {
            public void run() {
                try {
                    ivy.pushContext();
                    ivy.getEventManager().addIvyListener(IvyResolveJob.this);

                    monitor.beginTask("resolving dependencies", MONITOR_LENGTH);
                    monitor.setTaskName("resolving dependencies...");

                    String[] confs;
                    Collection/* <ArtifactDownloadReport> */all;
                    List problemMessages;

                    // context Classloader hook for commonlogging used by httpclient
                    ClassLoader old = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(
                        IvyResolveJob.class.getClassLoader());
                    try {
                        Map/*<ModuleRevisionId, IvyNode>*/ dependencies = Collections.EMPTY_MAP;
                        Set configurations = new HashSet();
                        configurations.addAll(conf.getConfs());
                        if (conf.getInheritedDoRetrieve()) {
                            configurations.addAll(Arrays.asList(conf.getInheritedRetrieveConfs().split(",")));
                        }

                        if (configurations.contains("*")) {
                            confs = md.getConfigurationsNames();
                        } else {
                            confs = (String[]) configurations.toArray(new String[configurations
                                    .size()]);
                        }

                        if (usePreviousResolveIfExist) {
                            all = new LinkedHashSet();

                            problemMessages = new ArrayList();
                            // we check if all required configurations have been
                            // resolved
                            for (int i = 0; i < confs.length; i++) {
                                File report = ivy.getResolutionCacheManager()
                                        .getConfigurationResolveReportInCache(
                                            ResolveOptions.getDefaultResolveId(md), confs[i]);
                                boolean resolved = false;
                                if (report.exists() && !conf.isResolveInWorkspace()) {
                                    // found a report, try to parse it.
                                    try {
                                        XmlReportParser parser = new XmlReportParser();
                                        parser.parse(report);
                                        all.addAll(Arrays.asList(parser.getArtifactReports()));
                                        resolved = true;
                                    } catch (ParseException e) {
                                        Message.info("\n\nIVYDE: Error while parsing the report "
                                                + report
                                                + ". Falling back by doing a resolve again.");
                                        // it fails, so let's try resolving
                                    }
                                }
                                if (!resolved) {
                                    // no resolve previously done for at least
                                    // one conf... we do it now
                                    Message.info("\n\nIVYDE: previous resolve of "
                                            + md.getModuleRevisionId().getModuleId()
                                            + " doesn't contain enough data: resolving again\n");
                                    ResolveOptions resolveOption = new ResolveOptions()
                                            .setConfs(confs);
                                    resolveOption.setValidate(ivy.getSettings().doValidate());
                                    ResolveReport r = ivy.resolve(md, resolveOption);
                                    all.addAll(Arrays.asList(r.getArtifactsReports(null, false)));
                                    confs = r.getConfigurations();
                                    dependencies = dependenciesAsMap(r);
                                    problemMessages.addAll(r.getAllProblemMessages());
                                    maybeRetrieve(md);

                                    break;
                                }
                            }
                        } else {
                            Message.info("\n\nIVYDE: calling resolve on " + conf.ivyXmlPath + "\n");
                            ResolveOptions resolveOption = new ResolveOptions().setConfs(confs);
                            resolveOption.setValidate(ivy.getSettings().doValidate());
                            ResolveReport report = ivy.resolve(md, resolveOption);
                            problemMessages = report.getAllProblemMessages();
                            all = new LinkedHashSet(Arrays.asList(report.getArtifactsReports(null,
                                false)));
                            confs = report.getConfigurations();

                            dependencies = dependenciesAsMap(report);

                            if (monitor.isCanceled()) {
                                status[0] = Status.CANCEL_STATUS;
                                return;
                            }

                            maybeRetrieve(md);
                        }

                        warnIfDuplicates(all);

                        classpathEntries[0] = artifacts2ClasspathEntries(all, dependencies);
                    } catch (ParseException e) {
                        String errorMsg = "Error while parsing the ivy file " + conf.ivyXmlPath
                                + "\n" + e.getMessage();
                        Message.error(errorMsg);
                        status[0] = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                                errorMsg, e);
                        return;
                    } catch (Exception e) {
                        String errorMsg = "Error while resolving dependencies for "
                                + conf.ivyXmlPath + "\n" + e.getMessage();
                        Message.error(errorMsg);
                        status[0] = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                                errorMsg, e);
                        return;
                    } finally {
                        Thread.currentThread().setContextClassLoader(old);
                        monitor.done();
                        ivy.getEventManager().removeIvyListener(IvyResolveJob.this);
                    }

                    if (!problemMessages.isEmpty()) {
                        MultiStatus multiStatus = new MultiStatus(
                                IvyPlugin.ID,
                                IStatus.ERROR,
                                "Impossible to resolve dependencies of " + md.getModuleRevisionId(),
                                null);
                        for (Iterator iter = problemMessages.iterator(); iter.hasNext();) {
                            multiStatus.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                                    (String) iter.next(), null));
                        }
                        status[0] = multiStatus;
                        return;
                    }

                    status[0] = Status.OK_STATUS;
                } catch (Throwable e) {
                    status[0] = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                            "The resolve job of " + conf + " has unexpectedly stopped", e);
                }
            }
        };

        try {
            resolver.start();
            while (true) {
                try {
                    resolver.join(WAIT_FOR_JOIN);
                } catch (InterruptedException e) {
                    ivy.interrupt(resolver);
                    return Status.CANCEL_STATUS;
                }
                synchronized (status) { // ensure proper sharing of done var
                    if (status[0] != null || !resolver.isAlive()) {
                        break;
                    }
                }
                if (monitor.isCanceled()) {
                    ivy.interrupt(resolver);
                    return Status.CANCEL_STATUS;
                }
            }
            if (status[0] == Status.OK_STATUS) {
                container.updateClasspathEntries(classpathEntries[0]);
            }
            setResolveStatus(status[0]);
            return status[0];
        } finally {
            container.resetJob();
            IvyPlugin.log(IStatus.INFO, "resolved dependencies of " + conf, null);
        }
    }

    private void setResolveStatus(IStatus status) {
        if (conf.javaProject != null) {
            IFile ivyFile = conf.javaProject.getProject().getFile(conf.ivyXmlPath);
            if (!ivyFile.exists()) {
                return;
            }
            try {
                ivyFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
                if (status == Status.OK_STATUS) {
                    return;
                }
                IMarker marker = ivyFile.createMarker(IMarker.PROBLEM);
                marker.setAttribute(IMarker.MESSAGE, status.getMessage());
                switch (status.getSeverity()) {
                    case IStatus.ERROR:
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                        break;
                    case IStatus.WARNING:
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                        break;
                    case IStatus.INFO:
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                        break;
                    default:
                        IvyPlugin.log(IStatus.WARNING, "Unsupported resolve status: "
                                + status.getSeverity(), null);
                }
            } catch (CoreException e) {
                IvyPlugin.log(e);
            }
        }
    }

    /**
     * Trigger a warn if there are duplicates entries due to configuration conflict.
     * <p>
     * TODO: the algorithm can be more clever and find which configuration are conflicting.
     * 
     * @param all
     *            the resolved artifacts
     */
    private void warnIfDuplicates(Collection/* <ArtifactDownloadReport> */all) {
        ArtifactDownloadReport[] reports = (ArtifactDownloadReport[]) all
                .toArray(new ArtifactDownloadReport[all.size()]);
        Set duplicates = new HashSet();
        for (int i = 0; i < reports.length - 1; i++) {
            if (accept(reports[i].getArtifact())) {
                ModuleRevisionId mrid1 = reports[i].getArtifact().getModuleRevisionId();
                for (int j = i + 1; j < reports.length; j++) {
                    if (accept(reports[j].getArtifact())) {
                        ModuleRevisionId mrid2 = reports[j].getArtifact().getModuleRevisionId();
                        if (mrid1.getModuleId().equals(mrid2.getModuleId())
                                && !mrid1.getRevision().equals(mrid2.getRevision())) {
                            duplicates.add(mrid1.getModuleId());
                            break;
                        }
                    }
                }
            }
        }
        if (!duplicates.isEmpty()) {
            StringBuffer buffer = new StringBuffer(
                    "There are some duplicates entries due to conflicts"
                            + " between the resolved configurations " + conf.confs);
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

    private void maybeRetrieve(ModuleDescriptor md) throws IOException {
        if (conf.getInheritedDoRetrieve()) {
            String pattern = conf.javaProject.getProject().getLocation().toPortableString() + "/"
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
            ivy.retrieve(md.getModuleRevisionId(), pattern, c);
        }
    }

    private IClasspathEntry[] artifacts2ClasspathEntries(
            Collection all, Map/*<ModuleRevisionId, IvyNode>*/ dependencies) {
        IClasspathEntry[] classpathEntries;
        Collection paths = new LinkedHashSet();

        Map idToJProject = new HashMap();
        if (conf.isResolveInWorkspace()) {
            try {
                IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot())
                        .getJavaProjects();
                for (int i = 0; i < projects.length; i++) {
                    IJavaProject javaProject = projects[i];
                    ModuleDescriptor md = findModuleDescriptor(javaProject);
                    if (md != null) {
                        idToJProject.put(md.getModuleRevisionId().getModuleId(), javaProject);
                    }
                }
            } catch (JavaModelException e) {
                IvyPlugin.log(IStatus.ERROR, "Error while listing the java projects,"
                        + " dependencies between java projects won't be used", e);
            }
        }

        for (Iterator iter = all.iterator(); iter.hasNext();) {
            ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();

            boolean usedProject = false;
            if (conf.isResolveInWorkspace()) {
                ModuleId moduleId = artifact.getArtifact().getModuleRevisionId().getModuleId();
                IJavaProject project = (IJavaProject) idToJProject.get(moduleId);
                if (project != null && project.exists()) {
                    IClasspathEntry entry = JavaCore.newProjectEntry(project.getPath());
                    if (entry != null && !paths.contains(entry)) {
                        paths.add(entry);
                    }
                    usedProject = true;
                }
            }

            if (!usedProject) {
                if (artifact.getLocalFile() != null && accept(artifact.getArtifact())) {
                    Path classpathArtifact = new Path(artifact.getLocalFile().getAbsolutePath());
                    Path sourcesArtifact = getSourcesArtifactPath(artifact, all, dependencies);
                    Path javadocArtifact = getJavadocArtifactPath(artifact, all, dependencies);
                    paths.add(JavaCore.newLibraryEntry(classpathArtifact, getSourceAttachment(
                        classpathArtifact, sourcesArtifact), getSourceAttachmentRoot(
                        classpathArtifact, sourcesArtifact), null, getExtraAttribute(
                        classpathArtifact, javadocArtifact), false));
                }
            }

        }
        classpathEntries = (IClasspathEntry[]) paths.toArray(new IClasspathEntry[paths.size()]);

        return classpathEntries;
    }

    /*
     * Finds and parses the ivy.xml file for the supplied project's classpath container
     */
    private ModuleDescriptor findModuleDescriptor(IJavaProject javaProject) {
        IvyClasspathContainer cp = IvyClasspathUtil.getIvyClasspathContainer(javaProject);
        if (cp == null) {
            return null;
        }
        try {
            return cp.getConf().getModuleDescriptor();
        } catch (IvyDEException e) {
            e.log(IStatus.WARNING, "IvyDE could not found out if the project "
                    + javaProject.getElementName() + " should be included in " + conf
                    + " classpath: ");
        }
        return null;
    }

    private Path getSourcesArtifactPath(ArtifactDownloadReport adr, Collection all, 
            Map/*<ModuleRevisionId, IvyNode>*/ dependencies) {
        Artifact artifact = adr.getArtifact();
        monitor.subTask("searching sources for " + artifact);
        for (Iterator iter = all.iterator(); iter.hasNext();) {
            ArtifactDownloadReport otherAdr = (ArtifactDownloadReport) iter.next();
            Artifact a = otherAdr.getArtifact();
            if (otherAdr.getLocalFile() != null
                    && isSourceArtifactName(artifact.getName(), a.getName())
                    && a.getId().getRevision().equals(artifact.getId().getRevision())
                    && isSources(a)) {
                return new Path(otherAdr.getLocalFile().getAbsolutePath());
            }
        }
        // we haven't found source artifact in resolved artifacts, 
        // let's look in the module declaring the artifact
        IvyNode node = (IvyNode) dependencies.get(artifact.getId().getModuleRevisionId());
        if (node != null) {
            Artifact[] artifacts = node.getDescriptor().getAllArtifacts();
            for (int i = 0; i < artifacts.length; i++) {
                Artifact metaArtifact = artifacts[i];
                if (isSourceArtifactName(artifact.getName(), metaArtifact.getName())
                    && isSources(metaArtifact)) {
                    // we've found the source artifact, let's provision it
                    ArtifactDownloadReport metaAdr = ivy.getResolveEngine().download(
                        metaArtifact, new DownloadOptions());
                    if (metaAdr.getLocalFile() != null && metaAdr.getLocalFile().exists()) {
                        return new Path(metaAdr.getLocalFile().getAbsolutePath());
                    }
                }
            }
        }
        return null;
    }

    private Path getJavadocArtifactPath(ArtifactDownloadReport adr, Collection all, 
            Map/*<ModuleRevisionId, IvyNode>*/ dependencies) {
        Artifact artifact = adr.getArtifact();
        monitor.subTask("searching javadoc for " + artifact);
        for (Iterator iter = all.iterator(); iter.hasNext();) {
            ArtifactDownloadReport otherAdr = (ArtifactDownloadReport) iter.next();
            Artifact a = otherAdr.getArtifact();
            if (otherAdr.getLocalFile() != null
                    && isJavadocArtifactName(artifact.getName(), a.getName())
                    && a.getModuleRevisionId().equals(artifact.getModuleRevisionId())
                    && a.getId().equals(artifact.getId()) && isJavadoc(a)) {
                return new Path(otherAdr.getLocalFile().getAbsolutePath());
            }
        }
        // we haven't found javadoc artifact in resolved artifacts, 
        // let's look in the module declaring the artifact
        IvyNode node = (IvyNode) dependencies.get(artifact.getId().getModuleRevisionId());
        if (node != null) {
            Artifact[] artifacts = node.getDescriptor().getAllArtifacts();
            for (int i = 0; i < artifacts.length; i++) {
                Artifact metaArtifact = artifacts[i];
                if (isJavadocArtifactName(artifact.getName(), metaArtifact.getName())
                    && isJavadoc(metaArtifact)) {
                    // we've found the javadoc artifact, let's provision it
                    ArtifactDownloadReport metaAdr = ivy.getResolveEngine().download(
                        metaArtifact, new DownloadOptions());
                    if (metaAdr.getLocalFile() != null && metaAdr.getLocalFile().exists()) {
                        return new Path(metaAdr.getLocalFile().getAbsolutePath());
                    }
                }
            }
        }
        return null;
    }

    private IPath getSourceAttachment(Path classpathArtifact, Path sourcesArtifact) {
        IPath sourceAttachment = IvyPlugin.getDefault().getPackageFragmentExtraInfo()
                .getSourceAttachment(classpathArtifact);
        if (sourceAttachment == null) {
            sourceAttachment = sourcesArtifact;
        }
        return sourceAttachment;
    }

    private IPath getSourceAttachmentRoot(Path classpathArtifact, Path sourcesArtifact) {
        IPath sourceAttachment = IvyPlugin.getDefault().getPackageFragmentExtraInfo()
                .getSourceAttachmentRoot(classpathArtifact);
        if (sourceAttachment == null && sourcesArtifact != null) {
            sourceAttachment = sourcesArtifact;
        }
        return sourceAttachment;
    }

    private IClasspathAttribute[] getExtraAttribute(Path classpathArtifact, Path javadocArtifact) {
        List result = new ArrayList();
        URL url = IvyPlugin.getDefault().getPackageFragmentExtraInfo().getDocAttachment(
            classpathArtifact);

        if (url == null) {
            Path path = javadocArtifact;
            if (path != null) {
                String u;
                try {
                    u = "jar:" + path.toFile().toURI().toURL().toExternalForm() + "!/";
                    try {
                        url = new URL(u);
                    } catch (MalformedURLException e) {
                        // this should not happen
                        IvyPlugin.log(IStatus.ERROR,
                            "The jar URL for the javadoc is not formed correctly " + u, e);
                    }
                } catch (MalformedURLException e) {
                    // this should not happen
                    IvyPlugin.log(IStatus.ERROR, "The path has not a correct URL: " + path, e);
                }
            }
        }

        if (url != null) {
            result.add(JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, url.toExternalForm()));
        }
        return (IClasspathAttribute[]) result.toArray(new IClasspathAttribute[result.size()]);
    }

    public boolean isJavadocArtifactName(String jar, String javadoc) {
        return isArtifactName(jar, javadoc, conf.getInheritedJavadocSuffixes());
    }

    public boolean isSourceArtifactName(String jar, String source) {
        return isArtifactName(jar, source, conf.getInheritedSourceSuffixes());
    }

    private boolean isArtifactName(String jar, String name, Collection/* <String> */suffixes) {
        if (name.equals(jar)) {
            return true;
        }
        Iterator it = suffixes.iterator();
        while (it.hasNext()) {
            if (name.equals(jar + it.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the artifact is an artifact which can be added to the classpath container
     * 
     * @param artifact
     *            the artifact to check
     * @return <code>true</code> if the artifact can be added
     */
    public boolean accept(Artifact artifact) {
        return conf.getInheritedAcceptedTypes().contains(artifact.getType())
                && !conf.getInheritedSourceTypes().contains(artifact.getType())
                && !conf.getInheritedJavadocTypes().contains(artifact.getType());
    }

    public boolean isSources(Artifact artifact) {
        return conf.getInheritedSourceTypes().contains(artifact.getType());
    }

    public boolean isJavadoc(Artifact artifact) {
        return conf.getInheritedJavadocTypes().contains(artifact.getType());
    }

}
