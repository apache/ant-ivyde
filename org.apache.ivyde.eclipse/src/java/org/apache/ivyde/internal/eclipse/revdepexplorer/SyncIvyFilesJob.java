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
package org.apache.ivyde.internal.eclipse.revdepexplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.namespace.NamespaceTransformer;
import org.apache.ivy.plugins.parser.xml.UpdateOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorUpdater;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerState;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.xml.sax.SAXException;

/**
 * This job synchronizes all ivy files in a workspace according to the new revisions specified in
 * the ivy explorer.
 */
public class SyncIvyFilesJob extends WorkspaceJob {

    private final MultiRevDependencyDescriptor[] multiRevisionDependencies;

    /**
     * FIXME Here we seriously abuse the Ivy core API to allow us to preserve an info element
     * containing no revision attribute. Ivy code should be altered to allow us to preserve revision
     * (including the lack of its definition!).
     */
    private class RevisionPreservingNamespace extends Namespace {
        private class NullableRevisionModuleRevisionId extends ModuleRevisionId {
            private final String revision;

            public NullableRevisionModuleRevisionId(ModuleId moduleId, String revision) {
                super(moduleId, revision);
                this.revision = revision;
            }

            public String getRevision() {
                return revision;
            }
        }

        private class RevisionPreservingNamespaceTransformer implements NamespaceTransformer {
            public boolean isIdentity() {
                return false;
            }

            public ModuleRevisionId transform(ModuleRevisionId mrid) {
                if (mrid.getRevision().contains("working@")) {
                    return new NullableRevisionModuleRevisionId(mrid.getModuleId(), null);
                }
                return new ModuleRevisionId(mrid.getModuleId(), mrid.getRevision());
            }
        }

        public NamespaceTransformer getToSystemTransformer() {
            return new RevisionPreservingNamespaceTransformer();
        }
    }

    public SyncIvyFilesJob(MultiRevDependencyDescriptor[] multiRevisionDependencies) {
        super("Synchronizing Ivy Files");
        this.multiRevisionDependencies = multiRevisionDependencies;
    }

    protected IStatus executeJob(IProgressMonitor monitor) {
        MultiStatus errorStatuses = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Failed to update one or more Ivy files.  See details.", null);

        for (IvyClasspathContainer container : getIvyClasspathContainers()) {
            IvyClasspathContainerState ivycps = ((IvyClasspathContainerImpl) container).getState();

            ModuleDescriptor md = ivycps.getCachedModuleDescriptor();
            if (md == null) {
                continue;
            }

            Map<ModuleRevisionId, String> newRevisions = new HashMap<>();
            for (DependencyDescriptor dependency : md.getDependencies()) {
                for (MultiRevDependencyDescriptor multiRevision : multiRevisionDependencies) {
                    ModuleRevisionId dependencyRevisionId = dependency.getDependencyRevisionId();
                    if (dependency.getDependencyId().equals(multiRevision.getModuleId())
                            && multiRevision.hasNewRevision()
                            && multiRevision.isForContainer(container)) {
                        newRevisions.put(dependencyRevisionId,
                                multiRevision.getNewRevision());
                        break; // move on to the next dependency
                    }
                }
            }

            UpdateOptions updateOptions = new UpdateOptions().setResolvedRevisions(newRevisions)
                    .setReplaceInclude(false).setGenerateRevConstraint(false)
                    .setNamespace(new RevisionPreservingNamespace());

            File ivyFile;
            try {
                ivyFile = ivycps.getIvyFile();
            } catch (IvyDEException e) {
                errorStatuses.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                        "Fail to resolve the Ivy file", e));
                continue;
            }

            File ivyTempFile = new File(ivyFile.toString() + ".temp");
            try {
                XmlModuleDescriptorUpdater.update(ivyFile.toURI().toURL(), ivyTempFile,
                    updateOptions);
                saveChanges(container, ivyFile, ivyTempFile);
            } catch (MalformedURLException e) {
                errorStatuses.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                        "Failed to write Ivy file " + ivyFile + " (malformed URL)", e));
            } catch (IOException | SAXException e) {
                errorStatuses.add(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                        "Failed to write Ivy file " + ivyFile, e));
            } finally {
                ivyTempFile.delete();
            }
        }

        if (errorStatuses.getChildren().length > 0) {
            return errorStatuses;
        }
        return Status.OK_STATUS;
    }

    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IStatus status = Status.OK_STATUS;

        try {
            status = executeJob(monitor);
        } catch (OperationCanceledException ignore) {
            return Status.CANCEL_STATUS;
        }

        return status;
    }

    private Collection<IvyClasspathContainer> getIvyClasspathContainers() {
        Collection<IvyClasspathContainer> containers = new HashSet<>();

        for (MultiRevDependencyDescriptor multiRevision : multiRevisionDependencies) {
            if (multiRevision.hasNewRevision()) {
                containers.addAll(Arrays.asList(multiRevision.getIvyClasspathContainers()));
            }
        }

        return containers;
    }

    private void saveChanges(IvyClasspathContainer container, File permanentSaveTarget,
            File temporaryChanges) throws IOException {
        IvyClasspathContainerConfiguration conf = container.getConf();
        IFile virtualIvyFile = conf.getJavaProject().getProject().getFile(conf.getIvyXmlPath());
        IStatus writable = virtualIvyFile.getWorkspace().validateEdit(new IFile[] {virtualIvyFile},
            IWorkspace.VALIDATE_PROMPT);
        if (writable.isOK()) {
            FileWriter writer = new FileWriter(permanentSaveTarget, false);
            BufferedReader reader = new BufferedReader(new FileReader(temporaryChanges));
            while (reader.ready()) {
                writer.write(reader.readLine() + "\n");
            }
            writer.flush();
            writer.close();
            reader.close();
        }
    }
}
