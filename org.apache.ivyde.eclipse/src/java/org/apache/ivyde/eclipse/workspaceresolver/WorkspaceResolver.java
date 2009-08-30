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
package org.apache.ivyde.eclipse.workspaceresolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.MetadataArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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

    private final IJavaProject resolvingJavaProject;

    private IJavaProject[] projects;

    public WorkspaceResolver(IJavaProject javaProject, IvySettings ivySettings) {
        this.resolvingJavaProject = javaProject;
        setName(javaProject.getElementName() + "-ivyde-workspace-resolver");
        setSettings(ivySettings);
        setCache(CACHE_NAME);

        try {
            projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
        } catch (JavaModelException e) {
            IvyPlugin.log(IStatus.ERROR, "JDT Error while resolving in workspace for "
                    + resolvingJavaProject.getElementName(), e);
        }
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        // Not much to do here - downloads are not required for workspace projects.
        DownloadReport dr = new DownloadReport();
        for (int i = 0; i < artifacts.length; i++) {
            final ArtifactDownloadReport adr = new ArtifactDownloadReport(artifacts[i]);
            dr.addArtifactReport(adr);

            // Only report java projects as downloaded
            if (artifacts[i].getType().equals(ECLIPSE_PROJECT_TYPE)) {
                Message.verbose("\t[WORKSPACE   ] " + artifacts[i]);
                adr.setDownloadStatus(DownloadStatus.NO);
                adr.setSize(0);
            } else {
                Message.verbose("\t[Eclipse Workspace resolver] "
                        + "cannot download non-project artifact: " + artifacts[i]);
                adr.setDownloadStatus(DownloadStatus.FAILED);
            }
        }
        return dr;
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException {

        ModuleRevisionId dependencyMrid = dd.getDependencyRevisionId();

        VersionMatcher versionMatcher = getSettings().getVersionMatcher();

        // Iterate over workspace to find Java project which has an Ivy
        // container for this dependency
        for (int i = 0; i < projects.length; i++) {
            IJavaProject javaProject = projects[i];
            if (resolvingJavaProject.equals(javaProject)) {
                // we don't want to introduce self dependency
                continue;
            }
            if (!javaProject.exists()) {
                continue;
            }
            List/* <IvyClasspathContainer> */containers = IvyClasspathUtil
                    .getIvyClasspathContainers(javaProject);
            Iterator/* <IvyClasspathContainer> */itContainer = containers.iterator();
            while (itContainer.hasNext()) {
                IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainer.next();
                ModuleDescriptor md;
                try {
                    md = ivycp.getConf().getCachedModuleDescriptor();
                } catch (IvyDEException e) {
                    IvyPlugin.log(IStatus.WARNING, "Resolve in workspace for '"
                            + resolvingJavaProject.getElementName() + "' cannot depend on "
                            + ivycp.getDescription() + " [" + e.getMessage() + "]", null);
                    continue;
                }

                if (!md.getModuleRevisionId().getModuleId().equals(dependencyMrid.getModuleId())) {
                    // it doesn't match org#module
                    continue;
                }

                // Found one; check if it is for the module we need
                if (versionMatcher.accept(dd.getDependencyRevisionId(), md)) {

                    Artifact af = new DefaultArtifact(md.getModuleRevisionId(), md
                            .getPublicationDate(), javaProject.getPath().toString(),
                            ECLIPSE_PROJECT_TYPE, ECLIPSE_PROJECT_EXTENSION);

                    DefaultModuleDescriptor workspaceMd = cloneMd(md, af);

                    MetadataArtifactDownloadReport madr = new MetadataArtifactDownloadReport(af);
                    madr.setDownloadStatus(DownloadStatus.SUCCESSFUL);
                    madr.setSearched(true);

                    return new ResolvedModuleRevision(this, this, workspaceMd, madr);
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
            for (int k = 0; k < allConfs.length; k++) {
                newMd.addConfiguration(allConfs[k]);
                newMd.addArtifact(allConfs[k].getName(), af);
            }
        }

        DependencyDescriptor[] dependencies = md.getDependencies();
        for (int k = 0; k < dependencies.length; k++) {
            newMd.addDependency(dependencies[k]);
        }

        ExcludeRule[] allExcludeRules = md.getAllExcludeRules();
        for (int k = 0; k < allExcludeRules.length; k++) {
            newMd.addExcludeRule(allExcludeRules[k]);
        }

        Map extraInfo = md.getExtraInfo();
        Iterator it = extraInfo.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            newMd.addExtraInfo((String) entry.getKey(), (String) entry.getValue());
        }

        License[] licenses = md.getLicenses();
        for (int k = 0; k < licenses.length; k++) {
            newMd.addLicense(licenses[k]);
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
