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
package org.apache.ivyde.internal.eclipse.workspaceresolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.ManifestHeaderElement;
import org.apache.ivy.osgi.core.ManifestHeaderValue;
import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.IvyDEMessage;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * This is an Eclipse workspace Ivy resolver. When used with the custom IvyClasspathContainer
 * changes, this resolver will link dependent projects when they are open in the same workspace,
 * allowing full-fledged linked project functionality Eclipse provides, such as incremental
 * compilation, debugging, mouseover javadocs, and source browsing across projects.
 *
 * <b>How it works</b> During a resolve, it looks at all open projects in the workspace that have
 * Ivy containers. The <b>first</b> project that publishes the module on which the project being
 * resolved depends, will be picked and returned as a special type of artifact called "project".
 *
 * The IvyClasspathContainer will recognize the artifact as a project and put the eclipse project as
 * a dependent project within the classpath container of the parent.
 *
 * If you do not want a project to be linked as a dependency, close it or delete from the workspace.
 * As soon as you do that, any projects that were linked to it will automatically re-resolve (see
 * {@link WorkspaceResourceChangeListener}) and use the standard Ivy means of finding the
 * dependency.
 *
 * The {@link WorkspaceResourceChangeListener} will also auto-resolve when a new project is added or
 * opened, so opening a project will automatically link it into the currently open projects where
 * necessary.
 *
 * Since the resolver is not aware which module revision a project is publishing, it optimistically
 * matches any revision of the module.
 *
 * Since the resolver stops after finding the first open project which matches the module, having
 * multiple open versions of the same project in the workspace (for example, different branches) may
 * set the wrong version as a dependency. You are advised to only open the version of the project
 * which you want other projects in the workspace to depend on.
 *
 * NOTE: Transitive dependencies are not passed from the dependent project to the parent when
 * projects are linked. If you find you are missing some transitive dependencies, just set your
 * dependent eclipse project to export its ivy dependencies. (Project->Properties->Java Build
 * Path->Order and Export-> -> check the ivy container) This will only export the configuration that
 * project is using and not what a dependent project may ask for when it's being resolved. To do
 * that, this resolver will need to be modified to pass transitive dependencies along.
 */
public class WorkspaceResolver extends AbstractResolver {

    public static final String ECLIPSE_PROJECT_TYPE = "eclipse-project";

    public static final String ECLIPSE_PROJECT_EXTENSION = "eclipse-project";

    public static final String CACHE_NAME = "__ivyde-workspace-resolver-cache";

    public static final String IVYDE_WORKSPACE_ARTIFACTS = "IvyDEWorkspaceArtifacts";

    public static final String IVYDE_WORKSPACE_ARTIFACT_REPORTS = "IvyDEWorkspaceArtifactReports";

    private final IProject[] projects;

    private final boolean ignoreBranchOnWorkspaceProjects;

    private final boolean ignoreVersionOnWorkspaceProjects;

    private final boolean osgiResolveInWorkspaceAvailable;

    public WorkspaceResolver(IProject project, IvySettings ivySettings) {
        String projectName = project == null ? "<null>" : project.getName();
        setName(projectName + "-ivyde-workspace-resolver");
        setSettings(ivySettings);
        setCache(CACHE_NAME);

        projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        ignoreBranchOnWorkspaceProjects = IvyPlugin.getPreferenceStoreHelper()
                .getIgnoreBranchOnWorkspaceProjects();

        ignoreVersionOnWorkspaceProjects = IvyPlugin.getPreferenceStoreHelper()
                .getIgnoreVersionOnWorkspaceProjects();

        osgiResolveInWorkspaceAvailable = IvyPlugin.getDefault()
                .isIvyVersionGreaterOrEqual(2, 4, 0);
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        IvyContext context = IvyContext.getContext();
        Map<Artifact, Artifact> workspaceArtifacts = context.get(IVYDE_WORKSPACE_ARTIFACTS);
        Map<Artifact, ArtifactDownloadReport> workspaceReports = null;
        if (workspaceArtifacts != null) {
            workspaceReports = new HashMap<>();
            context.set(IVYDE_WORKSPACE_ARTIFACT_REPORTS, workspaceReports);
        }

        // Not much to do here - downloads are not required for workspace projects.
        DownloadReport dr = new DownloadReport();
        for (Artifact artifact : artifacts) {
            ArtifactDownloadReport adr = new ArtifactDownloadReport(artifact);
            dr.addArtifactReport(adr);
            // Only report java projects as downloaded
            if (artifact.getType().equals(ECLIPSE_PROJECT_TYPE)) {
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(0);
                Message.verbose("\t[IN WORKSPACE] " + artifact);
            } else if (workspaceArtifacts != null && workspaceArtifacts.containsKey(artifact)) {
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(0);
                // there is some 'forced' artifact by the dependency descriptor
                Artifact eclipseArtifact = workspaceArtifacts.get(artifact);
                ArtifactDownloadReport eclipseAdr = new ArtifactDownloadReport(eclipseArtifact);
                eclipseAdr.setDownloadStatus(DownloadStatus.NO);
                eclipseAdr.setSize(0);
                workspaceReports.put(artifact, eclipseAdr);
                Message.verbose("\t[IN WORKSPACE] " + eclipseArtifact);
            } else {
                adr.setDownloadStatus(DownloadStatus.FAILED);
                Message.verbose("\t[Eclipse Workspace resolver] "
                        + "cannot download non-project artifact: " + artifact);
            }
        }
        return dr;
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {
        IvyContext context = IvyContext.getContext();

        String contextId = "ivyde.workspaceresolver." + getName() + "."
                + dd.getDependencyRevisionId().toString();
        DependencyDescriptor parentDD = context.get(contextId);
        if (parentDD != null
                && parentDD.getDependencyRevisionId().equals(dd.getDependencyRevisionId())) {
            // this very workspace resolver has been already called for the same dependency
            // we are going into a circular dependency, let's return 'not found'
            return null;
        }
        context.set(contextId, dd);

        ModuleRevisionId dependencyMrid = dd.getDependencyRevisionId();
        String org = dependencyMrid.getModuleId().getOrganisation();
        String module = dependencyMrid.getModuleId().getName();

        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        // Iterate over workspace to find Java project which has an Ivy
        // container for this dependency
        for (IProject p : projects) {
            if (!p.exists()) {
                continue;
            }
            for (IvyClasspathContainer container : IvyClasspathContainerHelper.getContainers(p)) {
                ModuleDescriptor md = ((IvyClasspathContainerImpl) container).getState().getCachedModuleDescriptor();
                if (md == null) {
                    continue;
                }
                ModuleRevisionId candidateMrid = md.getModuleRevisionId();

                // search a match on the organization and the module name

                if (osgiResolveInWorkspaceAvailable && org.equals(BundleInfo.BUNDLE_TYPE)) {
                    // looking for an OSGi bundle via its symbolic name
                    if (!module.equals(md.getExtraInfoContentByTagName("Bundle-SymbolicName"))) {
                        // not found, skip to next
                        continue;
                    }
                } else if (osgiResolveInWorkspaceAvailable && org.equals(BundleInfo.PACKAGE_TYPE)) {
                    // looking for an OSGi bundle via its exported package
                    String exportedPackages = md.getExtraInfoContentByTagName("Export-Package");
                    if (exportedPackages == null) {
                        // not found, skip to next
                        continue;
                    }
                    boolean found = false;
                    String version = null;
                    ManifestHeaderValue exportElements = new ManifestHeaderValue(exportedPackages);
                    for (ManifestHeaderElement exportElement : exportElements.getElements()) {
                        if (exportElement.getValues().contains(module)) {
                            found = true;
                            version = exportElement.getAttributes().get("version");
                            break;
                        }
                    }
                    if (!found) {
                        // not found, skip to next
                        continue;
                    }
                    if (version == null) {
                        // no version means anything can match. Let's trick the version matcher by
                        // setting the exact expected version
                        version = dependencyMrid.getRevision();
                    }
                    md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(org, module,
                        version));
                } else {
                    if (!candidateMrid.getModuleId().equals(dependencyMrid.getModuleId())) {
                        // it doesn't match org#module, skip to next
                        continue;
                    }
                }

                IvyDEMessage.verbose("Workspace resolver found potential matching project "
                        + p.getName() + " with module " + candidateMrid + " for module "
                        + dependencyMrid);

                if (!ignoreBranchOnWorkspaceProjects) {
                    ModuleId mid = dependencyMrid.getModuleId();
                    String defaultBranch = getSettings().getDefaultBranch(mid);
                    String dependencyBranch = dependencyMrid.getBranch();
                    String candidateBranch = candidateMrid.getBranch();
                    if (dependencyBranch == null) {
                        dependencyBranch = defaultBranch;
                    }
                    if (candidateBranch == null) {
                        candidateBranch = defaultBranch;
                    }
                    if (dependencyBranch != candidateBranch) {
                        // Both cannot be null
                        if (dependencyBranch == null || candidateBranch == null) {
                            IvyDEMessage
                                    .verbose("\t\trejected since branches doesn't match (one is set, the other isn't)");
                            continue;
                        }
                        if (!dependencyBranch.equals(candidateBranch)) {
                            IvyDEMessage.verbose("\t\trejected since branches doesn't match");
                            continue;
                        }
                    }
                }

                // Found one; check if it is for the module we need
                if (ignoreVersionOnWorkspaceProjects
                        || md.getModuleRevisionId().getRevision().equals(Ivy.getWorkingRevision())
                        || versionMatcher.accept(dd.getDependencyRevisionId(), md)) {

                    if (ignoreVersionOnWorkspaceProjects) {
                        IvyDEMessage.verbose("\t\tmatched (version are ignored)");
                    } else {
                        IvyDEMessage.verbose("\t\tversion matched");
                    }

                    Artifact af = new DefaultArtifact(md.getModuleRevisionId(),
                            md.getPublicationDate(), p.getFullPath().toString(),
                            ECLIPSE_PROJECT_TYPE, ECLIPSE_PROJECT_EXTENSION);

                    DependencyArtifactDescriptor[] dArtifacts = dd.getAllDependencyArtifacts();
                    if (dArtifacts != null) {
                        // the dependency is declaring explicitly some artifacts to download
                        // we need to trick to and map these requested artifact by the Eclipse
                        // project

                        // we need the context which is used when downloading data, which is the
                        // parent of the current one so let's hack: popContext (the child),
                        // getContext (the parent), setVar, pushContext (child)
                        IvyContext currentContext = IvyContext.popContext();
                        IvyContext parentContext = IvyContext.getContext();
                        Map<Artifact, Artifact> workspaceArtifacts = parentContext.get(IVYDE_WORKSPACE_ARTIFACTS);
                        if (workspaceArtifacts == null) {
                            workspaceArtifacts = new HashMap<>();
                            parentContext.set(IVYDE_WORKSPACE_ARTIFACTS, workspaceArtifacts);
                        }
                        for (DependencyArtifactDescriptor dArtifact : dArtifacts) {
                            Artifact artifact = new MDArtifact(md, dArtifact.getName(),
                                    dArtifact.getType(), dArtifact.getExt(),
                                    dArtifact.getUrl(),
                                    dArtifact.getQualifiedExtraAttributes());
                            workspaceArtifacts.put(artifact, af);
                        }
                        IvyContext.pushContext(currentContext);
                    }

                    DefaultModuleDescriptor workspaceMd = cloneMd(md, af);

                    MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(af);
                    madr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
                    madr.setSearched(true);

                    return new ResolvedModuleRevision(this, this, workspaceMd, madr);
                } else {
                    IvyDEMessage.verbose("\t\treject as version didn't match");
                }
            }
        }

        // Didn't find module in any open project, proceed to other resolvers.
        return null;
    }

    private DefaultModuleDescriptor cloneMd(ModuleDescriptor md, Artifact af) {

        DefaultModuleDescriptor newMd = new DefaultModuleDescriptor(md.getModuleRevisionId(),
                "release", null, true);
        newMd.addConfiguration(new Configuration(ModuleDescriptor.DEFAULT_CONFIGURATION));
        newMd.setLastModified(System.currentTimeMillis());

        newMd.setDescription(md.getDescription());
        newMd.setHomePage(md.getHomePage());
        newMd.setLastModified(md.getLastModified());
        newMd.setPublicationDate(md.getPublicationDate());
        newMd.setResolvedPublicationDate(md.getResolvedPublicationDate());
        newMd.setStatus(md.getStatus());

        Configuration[] allConfs = md.getConfigurations();
        if (allConfs.length == 0) {
            newMd.addArtifact(ModuleDescriptor.DEFAULT_CONFIGURATION, af);
        } else {
            for (Configuration conf : allConfs) {
                newMd.addConfiguration(conf);
                newMd.addArtifact(conf.getName(), af);
            }
        }

        for (DependencyDescriptor dependency : md.getDependencies()) {
            newMd.addDependency(dependency);
        }

        for (ExcludeRule excludeRule : md.getAllExcludeRules()) {
            newMd.addExcludeRule(excludeRule);
        }

        newMd.getExtraInfos().addAll(md.getExtraInfos());

        for (License license : md.getLicenses()) {
            newMd.addLicense(license);
        }

        return newMd;
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException("publish not supported by " + getName());
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        return null;
    }
}
