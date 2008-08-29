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
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
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
    long _expectedTotalLength = 1;

    long _currentLength = 0;

    private IProgressMonitor _monitor;

    private IProgressMonitor _dlmonitor;

    private boolean _usePreviousResolveIfExist;

    private int _workPerArtifact = 100;

    Ivy ivy;

    final IvyClasspathContainerConfiguration conf;

    private final IvyClasspathContainer container;

    ModuleDescriptor md;

    public IvyResolveJob(IvyClasspathContainer container, boolean usePreviousResolveIfExist) {
        super("Resolve " + container.getConf() + " dependencies");
        this.container = container;
        this.conf = container.getConf();
        _usePreviousResolveIfExist = usePreviousResolveIfExist;
    }

    public void transferProgress(TransferEvent evt) {
        switch (evt.getEventType()) {
            case TransferEvent.TRANSFER_INITIATED:
                _monitor.setTaskName("downloading " + evt.getResource());
                break;
            case TransferEvent.TRANSFER_STARTED:
                _currentLength = 0;
                if (evt.isTotalLengthSet()) {
                    _expectedTotalLength = evt.getTotalLength();
                    _dlmonitor.beginTask("downloading " + evt.getResource(), 100);
                }
                break;
            case TransferEvent.TRANSFER_PROGRESS:
                if (_expectedTotalLength > 1) {
                    _currentLength += evt.getLength();
                    _dlmonitor.worked((int) (_currentLength * 100 / _expectedTotalLength));
                    _monitor.subTask((_currentLength / 1024) + " / "
                            + (_expectedTotalLength / 1024) + "kB");
                }
                break;
            default:
        }
    }

    public void progress(IvyEvent event) {
        if (event instanceof TransferEvent) {
            if (_dlmonitor != null) {
                transferProgress((TransferEvent) event);
            }
        } else if (event instanceof PrepareDownloadEvent) {
            PrepareDownloadEvent pde = (PrepareDownloadEvent) event;
            Artifact[] artifacts = pde.getArtifacts();
            if (artifacts.length > 0) {
                _workPerArtifact = 1000 / artifacts.length;
            }
        } else if (event instanceof StartArtifactDownloadEvent) {
            StartArtifactDownloadEvent evt = (StartArtifactDownloadEvent) event;
            _monitor.setTaskName("downloading " + evt.getArtifact());
            if (_dlmonitor != null) {
                _dlmonitor.done();
            }
            _dlmonitor = new SubProgressMonitor(_monitor, _workPerArtifact);
        } else if (event instanceof EndArtifactDownloadEvent) {
            if (_dlmonitor != null) {
                _dlmonitor.done();
            }
            _monitor.subTask(" ");
            _dlmonitor = null;
        } else if (event instanceof StartResolveDependencyEvent) {
            StartResolveDependencyEvent ev = (StartResolveDependencyEvent) event;
            _monitor.subTask("resolving " + ev.getDependencyDescriptor().getDependencyRevisionId());
        } else if (event instanceof EndResolveDependencyEvent) {
            _monitor.subTask(" ");
        }
    }

    private Map listDependencies(ResolveReport r) {
        Map result = new HashMap();
        Iterator it = r.getDependencies().iterator();
        while (it.hasNext()) {
            IvyNode node = (IvyNode) it.next();
            ModuleRevisionId moduleId = node.getId();
            result.put(moduleId.getName(), moduleId);
        }
        return result;
    }

    protected IStatus run(IProgressMonitor monitor) {
        Message.info("resolving dependencies of " + conf.ivyXmlPath);
        _monitor = monitor;
        final IStatus[] status = new IStatus[1];
        final IClasspathEntry[][] classpathEntries = new IClasspathEntry[1][];

        try {
            this.ivy = conf.getIvy();
            this.md = conf.getModuleDescriptor();
        } catch (IvyDEException e) {
            return new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, e.getMessage(), e);
        }

        Thread resolver = new Thread() {
            public void run() {
                try {
                    ivy.pushContext();
                    ivy.getEventManager().addIvyListener(IvyResolveJob.this);
    
                    _monitor.beginTask("resolving dependencies", 1000);
                    _monitor.setTaskName("resolving dependencies...");
    
                    String[] confs;
                    Collection/* <ArtifactDownloadReport> */all;
                    List problemMessages;
    
                    // context Classloader hook for commonlogging used by httpclient
                    ClassLoader old = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(IvyResolveJob.class.getClassLoader());
                    try {
                        Map dependencies = Collections.EMPTY_MAP;
                        if (_usePreviousResolveIfExist) {
                            if (conf.confs.size() == 1 && "*".equals(conf.confs.get(0))) {
                                confs = md.getConfigurationsNames();
                            } else {
                                confs = (String[]) conf.confs.toArray(new String[conf.confs.size()]);
                            }
    
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
                                                + report + ". Falling back by doing a resolve again.");
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
                                            .setConfs((String[]) conf.confs
                                                    .toArray(new String[conf.confs.size()]));
                                    resolveOption.setValidate(ivy.getSettings().doValidate());
                                    ResolveReport r = ivy.resolve(md, resolveOption);
                                    all.addAll(Arrays.asList(r.getArtifactsReports(null, false)));
                                    confs = r.getConfigurations();
                                    dependencies = listDependencies(r);
                                    problemMessages.addAll(r.getAllProblemMessages());
                                    maybeRetrieve(md, confs);
    
                                    break;
                                }
                            }
                        } else {
                            Message.info("\n\nIVYDE: calling resolve on " + conf.ivyXmlPath + "\n");
                            ResolveOptions resolveOption = new ResolveOptions()
                                    .setConfs((String[]) conf.confs.toArray(new String[conf.confs
                                            .size()]));
                            resolveOption.setValidate(ivy.getSettings().doValidate());
                            ResolveReport report = ivy.resolve(md, resolveOption);
                            problemMessages = report.getAllProblemMessages();
                            all = new LinkedHashSet(Arrays.asList(report.getArtifactsReports(null,
                                false)));
                            confs = report.getConfigurations();
    
                            dependencies = listDependencies(report);
    
                            if (_monitor.isCanceled()) {
                                status[0] = Status.CANCEL_STATUS;
                                return;
                            }
    
                            maybeRetrieve(md, confs);
                        }
    
                        warnIfDuplicates(all);
    
                        classpathEntries[0] = artifacts2ClasspathEntries(all, dependencies);
                    } catch (ParseException e) {
                        String errorMsg = "Error while parsing the ivy file " + conf.ivyXmlPath + "\n"
                                + e.getMessage();
                        Message.error(errorMsg);
                        status[0] = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
                        return;
                    } catch (Exception e) {
                        String errorMsg = "Error while resolving dependencies for " + conf.ivyXmlPath
                                + "\n" + e.getMessage();
                        Message.error(errorMsg);
                        status[0] = new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, errorMsg, e);
                        return;
                    } finally {
                        Thread.currentThread().setContextClassLoader(old);
                        _monitor.done();
                        ivy.getEventManager().removeIvyListener(IvyResolveJob.this);
                    }
    
                    if (!problemMessages.isEmpty()) {
                        MultiStatus multiStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
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
                        "The resolve job of "
                                + (conf.getJavaProject() == null ? "" : conf.getJavaProject()
                                        .getProject().getName()
                                        + "/") + conf.ivyXmlPath + " has unexpectedly stopped", e);                    
                }
            }
        };

        try {
            resolver.start();
            while (true) {
                try {
                    resolver.join(100);
                } catch (InterruptedException e) {
                    ivy.interrupt(resolver);
                    return Status.CANCEL_STATUS;
                }
                synchronized (status) { // ensure proper sharing of done var
                    if (status[0] != null || !resolver.isAlive()) {
                        break;
                    }
                }
                if (_monitor.isCanceled()) {
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
            container.job = null;
            IvyPlugin.log(IStatus.INFO, "resolved dependencies of "
                    + (conf.getJavaProject() == null ? "" : conf.getJavaProject().getProject()
                            .getName()
                            + "/") + conf.ivyXmlPath, null);
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
                    "There are some duplicates entries due to conflicts between the resolved configurations "
                            + conf.confs);
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

    private void maybeRetrieve(ModuleDescriptor md, String[] confs) throws IOException {
        if (conf.getInheritedDoRetrieve()) {
            String pattern = conf.javaProject.getProject().getLocation().toPortableString() + "/"
                    + conf.getInheritedRetrievePattern();
            _monitor.setTaskName("retrieving dependencies in " + pattern);
            RetrieveOptions c = new RetrieveOptions().setConfs(confs);
            c.setSync(conf.getInheritedRetrieveSync());
            ivy.retrieve(md.getModuleRevisionId(), pattern, c);
        }
    }

    private IClasspathEntry[] artifacts2ClasspathEntries(Collection all, Map dependencies) {
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
                String moduleName = moduleId.getName();
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) dependencies.get(moduleName);
                if (moduleRevisionId != null) {
                    IJavaProject project = (IJavaProject) idToJProject.get(moduleId);
                    if (project != null && project.exists() && project.isOpen()) {
                        IClasspathEntry entry = JavaCore.newProjectEntry(project.getPath());
                        if (entry != null && !paths.contains(entry)) {
                            paths.add(entry);
                        }
                        usedProject = true;
                    }
                }
            }

            if (!usedProject) {
                if (artifact.getLocalFile() != null && accept(artifact.getArtifact())) {
                    Path classpathArtifact = new Path(artifact.getLocalFile().getAbsolutePath());
                    Path sourcesArtifact = getSourcesArtifactPath(artifact, all);
                    Path javadocArtifact = getJavadocArtifactPath(artifact, all);
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

    private Path getSourcesArtifactPath(ArtifactDownloadReport adr, Collection all) {
        Artifact artifact = adr.getArtifact();
        _monitor.subTask("searching sources for " + artifact);
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
        if (shouldTestNonDeclaredSources()) {
            return getMetaArtifactPath(adr, "source", "sources");
        } else {
            return null;
        }
    }

    private Path getJavadocArtifactPath(ArtifactDownloadReport adr, Collection all) {
        Artifact artifact = adr.getArtifact();
        _monitor.subTask("searching javadoc for " + artifact);
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
        if (shouldTestNonDeclaredJavadocs()) {
            return getMetaArtifactPath(adr, "javadoc", "javadoc");
        } else {
            return null;
        }
    }

    /**
     * meta artifact (source or javadoc) not found in resolved artifacts, try to see if a non
     * declared one is available
     */
    private Path getMetaArtifactPath(ArtifactDownloadReport adr, String metaType,
            String metaClassifier) {
        Artifact artifact = adr.getArtifact();
        Map extraAtt = new HashMap(artifact.getExtraAttributes());
        extraAtt.put("classifier", metaClassifier);
        Artifact metaArtifact = new DefaultArtifact(artifact.getModuleRevisionId(), artifact
                .getPublicationDate(), artifact.getName(), metaType, "jar", extraAtt);
        RepositoryCacheManager cache = ivy.getSettings()
                .getResolver(artifact.getModuleRevisionId()).getRepositoryCacheManager();
        if (! (cache instanceof DefaultRepositoryCacheManager)) {
            /*
             * we're not using a default implementation of repository cache manager, so we don't
             * cache attempts to locate metadata artifacts
             */
            Path metaArtifactLocalPath = downloadMetaArtifact(adr, metaType, metaArtifact);
            if (metaArtifactLocalPath != null) {
                return metaArtifactLocalPath;
            }
            Message.info(metaType + " not found for " + artifact);
            Message.verbose(
                "Attempt not stored in cache because a non Default cache implementation is used.");
            return null;
        }
        
        File metaArtifactFile = ((DefaultRepositoryCacheManager) cache)
                                                .getArchiveFileInCache(metaArtifact);
        File attempt = new File(metaArtifactFile.getAbsolutePath() + ".notfound");
        if (metaArtifactFile.exists()) {
            return new Path(metaArtifactFile.getAbsolutePath());
        } else if (attempt.exists()) {
            return null;
        } 
        Path metaArtifactLocalPath = downloadMetaArtifact(adr, metaType, metaArtifact);
        if (metaArtifactLocalPath != null) {
            return metaArtifactLocalPath;
        }
        Message.info(metaType + " not found for " + artifact);
        /*
         * meta artifact not found, we store this information to avoid other attempts later
         */
        try {
            attempt.getParentFile().mkdirs();
            attempt.createNewFile();
        } catch (IOException e) {
            Message.error("impossible to create attempt file " + attempt + ": " + e);
        }
        return null;
    }

    private Path downloadMetaArtifact(ArtifactDownloadReport adr, String metaType,
            Artifact metaArtifact) {
        Artifact artifact = adr.getArtifact();
        Message.info("checking " + metaType + " for " + artifact );
        ArtifactOrigin origin = ivy.getResolveEngine().locate(metaArtifact);
        if (!ArtifactOrigin.isUnknown(origin)) {
            /*
             * fix for IVYDE-117: we need to check that the location of this metadata
             * artifact is different from the original artifact
             */
            if (adr.getArtifactOrigin() != null 
                    && (ArtifactOrigin.isUnknown(adr.getArtifactOrigin())
                            || !origin.getLocation()
                                .equals(adr.getArtifactOrigin().getLocation()))) {
                ArtifactDownloadReport metaAdr = ivy.getResolveEngine()
                                            .download(origin, new DownloadOptions());
                File localFile = metaAdr.getLocalFile();
                if (localFile != null && localFile.exists()) {
                    return new Path(localFile.getAbsolutePath());
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

    public boolean shouldTestNonDeclaredSources() {
        return true; // TODO: add settings for that
    }

    public boolean shouldTestNonDeclaredJavadocs() {
        return true; // TODO: add settings for that
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
