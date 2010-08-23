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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.workspaceresolver.WorkspaceResolver;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * This class is mapping the resolved artifacts between them. After a resolve process, this class
 * will build the classpath based on the retrieved artifacts and will search for sources and
 * javadocs and make them attached.
 */
public class IvyClasspathContainerMapper {

    private final IProgressMonitor monitor;

    private final Ivy ivy;

    private final IvyClasspathContainerConfiguration conf;

    private final Collection/* <ArtifactDownloadReport> */all;

    private final Map/* <ModuleRevisionId, Artifact[]> */artifactsByDependency;

    private final Map/*
                      * <ArtifactDownloadReport , Set<String>>
                      */retrievedArtifacts;

    public IvyClasspathContainerMapper(IProgressMonitor monitor, Ivy ivy,
            IvyClasspathContainerConfiguration conf, Collection all, Map artifactsByDependency,
            Map retrievedArtifacts) {
        this.monitor = monitor;
        this.ivy = ivy;
        this.conf = conf;
        this.all = all;
        this.artifactsByDependency = artifactsByDependency;
        this.retrievedArtifacts = retrievedArtifacts;
    }

    public IClasspathEntry[] map() {
        IClasspathEntry[] classpathEntries;
        Collection paths = new LinkedHashSet();

        for (Iterator iter = all.iterator(); iter.hasNext();) {
            ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();

            if (artifact.getType().equals(WorkspaceResolver.ECLIPSE_PROJECT_TYPE)) {
                // This is a java project in the workspace, add project path
                paths.add(JavaCore.newProjectEntry(new Path(artifact.getName()), true));
            } else if (artifact.getLocalFile() != null && accept(artifact.getArtifact())) {
                Path classpathArtifact = getArtifactPath(artifact);
                Path sourcesArtifact = getArtifactPath(artifact, sourceArtifactMatcher,
                    conf.isInheritedMapIfOnlyOneSource());
                Path javadocArtifact = getArtifactPath(artifact, javadocArtifactMatcher,
                    conf.isInheritedMapIfOnlyOneJavadoc());
                paths.add(JavaCore.newLibraryEntry(classpathArtifact,
                    getSourceAttachment(classpathArtifact, sourcesArtifact),
                    getSourceAttachmentRoot(classpathArtifact, sourcesArtifact), null,
                    getExtraAttribute(classpathArtifact, javadocArtifact), false));
            }

        }
        classpathEntries = (IClasspathEntry[]) paths.toArray(new IClasspathEntry[paths.size()]);

        return classpathEntries;
    }

    private Path getArtifactPath(ArtifactDownloadReport artifact) {
        if (retrievedArtifacts != null) {
            Set pathSet = (Set) retrievedArtifacts.get(artifact);
            if (pathSet != null && !pathSet.isEmpty()) {
                return new Path((String) pathSet.iterator().next());
            }
        }
        return new Path(artifact.getLocalFile().getAbsolutePath());
    }

    interface ArtifactMatcher {
        boolean matchName(String binaryName, String artifactName);

        boolean match(Artifact a);

        String getName();
    }

    private Path getArtifactPath(ArtifactDownloadReport adr, ArtifactMatcher matcher,
            boolean mapIfOnlyOne) {
        Artifact artifact = adr.getArtifact();
        monitor.subTask("searching " + matcher.getName() + " for " + artifact);
        for (Iterator iter = all.iterator(); iter.hasNext();) {
            ArtifactDownloadReport otherAdr = (ArtifactDownloadReport) iter.next();
            Artifact a = otherAdr.getArtifact();
            if (otherAdr.getLocalFile() != null
                    && matcher.matchName(artifact.getName(), a.getName())
                    && a.getModuleRevisionId().equals(artifact.getModuleRevisionId())
                    && matcher.match(a)) {
                return getArtifactPath(otherAdr);
            }
        }
        // we haven't found source artifact in resolved artifacts,
        // let's look in the module declaring the artifact
        ModuleRevisionId mrid = artifact.getId().getModuleRevisionId();
        Artifact[] artifacts = (Artifact[]) artifactsByDependency.get(mrid);
        if (artifacts != null) {
            Artifact foundArtifact = null;
            int nbFound = 0;
            for (int i = 0; i < artifacts.length; i++) {
                Artifact metaArtifact = artifacts[i];
                if (matcher.match(metaArtifact)) {
                    if (matcher.matchName(artifact.getName(), metaArtifact.getName())) {
                        // we've found a matching artifact, let's provision it
                        ArtifactDownloadReport metaAdr = ivy.getResolveEngine().download(
                            metaArtifact, new DownloadOptions());
                        if (metaAdr.getLocalFile() != null && metaAdr.getLocalFile().exists()) {
                            return getArtifactPath(metaAdr);
                        }
                    }
                    // keep a reference to the artifact so we could fall back
                    // to map-if-only-one
                    nbFound++;
                    foundArtifact = metaArtifact;
                }
            }
            if (mapIfOnlyOne) {
                // we haven't found artifact in the module declaring the artifact and having
                // a matching name.
                if (nbFound == 1) {
                    // If there is only 1 found artifact, it is the winner ;-)
                    ArtifactDownloadReport metaAdr = ivy.getResolveEngine().download(foundArtifact,
                        new DownloadOptions());
                    if (metaAdr.getLocalFile() != null && metaAdr.getLocalFile().exists()) {
                        return new Path(metaAdr.getLocalFile().getAbsolutePath());
                    }
                }
            }
        }

        return null;
    }

    private ArtifactMatcher sourceArtifactMatcher = new ArtifactMatcher() {
        public boolean matchName(String jar, String source) {
            return isArtifactName(jar, source, conf.getInheritedSourceSuffixes());
        }

        public boolean match(Artifact a) {
            return conf.getInheritedSourceTypes().contains(a.getType());
        }

        public String getName() {
            return "sources";
        }
    };

    private ArtifactMatcher javadocArtifactMatcher = new ArtifactMatcher() {
        public boolean matchName(String jar, String javadoc) {
            return isArtifactName(jar, javadoc, conf.getInheritedJavadocSuffixes());
        }

        public boolean match(Artifact a) {
            return conf.getInheritedJavadocTypes().contains(a.getType());
        }

        public String getName() {
            return "javadoc";
        }
    };

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
        URL url = IvyPlugin.getDefault().getPackageFragmentExtraInfo()
                .getDocAttachment(classpathArtifact);

        if (url == null) {
            Path path = javadocArtifact;
            if (path != null) {
                String u;
                try {
                    u = "jar:" + path.toFile().toURL().toExternalForm() + "!/";
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

}
