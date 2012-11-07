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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.ivyde.eclipse.IvyDEMessage;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class IvyAttachementManager {

    private static final String SRC_SUFFIX = "-src";

    private static final String SRCROOT_SUFFIX = "-srcroot";

    private static final String DOC_SUFFIX = "-doc";

    private Properties prop = new Properties();

    private File containersAttachementFile;

    public IvyAttachementManager(File containersAttachementFile) {
        this.containersAttachementFile = containersAttachementFile;
        if (!containersAttachementFile.exists()) {
            IvyDEMessage.verbose("Attachement properties file not found: nothing to load");
            return;
        }
        IvyDEMessage.verbose("Reading attachement properties");
        try {
            FileInputStream in = new FileInputStream(containersAttachementFile);
            try {
                prop.load(in);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        } catch (IOException ioe) {
            IvyPlugin.logWarn("IvyDE attachement properties could not be loaded", ioe);
        }
    }

    public void updateAttchements(IJavaProject project, IPath containerPath,
            IClasspathContainer containerSuggestion) {
        IvyDEMessage.verbose("Updating attachements on the container " + containerPath);
        
        Properties newProps = new Properties();

        IClasspathEntry[] newEntries = containerSuggestion.getClasspathEntries();
        for (int i = 0; i < newEntries.length; i++) {
            IClasspathEntry entry = newEntries[i];
            if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
                String path = entry.getPath().toPortableString();
                if (entry.getSourceAttachmentPath() != null) {
                    newProps.put(path + SRC_SUFFIX, entry.getSourceAttachmentPath()
                            .toPortableString());
                }
                if (entry.getSourceAttachmentRootPath() != null) {
                    newProps.put(path + SRCROOT_SUFFIX, entry.getSourceAttachmentRootPath()
                            .toPortableString());
                }
                String javadocUrl = getJavadocLocation(entry);
                if (javadocUrl != null) {
                    newProps.put(path + DOC_SUFFIX, javadocUrl);
                }
            }
        }

        IvyClasspathContainer ivycp = IvyClasspathUtil.getIvyClasspathContainer(containerPath,
            project);
        if (ivycp == null) {
            IvyDEMessage.error("The IvyDE container could not be found. Aborting updating attachements.");
            // something wrong happened, give up
            return;
        }
        IClasspathEntry[] existingEntries = ivycp.getClasspathEntries();
        for (int i = 0; i < existingEntries.length; i++) {
            IClasspathEntry entry = existingEntries[i];
            if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
                String path = entry.getPath().toPortableString();
                String value = (String) prop.get(path + SRC_SUFFIX);
                if (value != null && entry.getSourceAttachmentPath() != null
                        && value.equals(entry.getSourceAttachmentPath().toPortableString())) {
                    newProps.remove(path + SRC_SUFFIX);
                }
                value = (String) prop.get(path + SRCROOT_SUFFIX);
                if (value != null && entry.getSourceAttachmentRootPath() != null
                        && value.equals(entry.getSourceAttachmentRootPath().toPortableString())) {
                    newProps.remove(path + SRCROOT_SUFFIX);
                }
            }
        }

        // copy the actually new overrided properties
        prop.putAll(newProps);

        // now update the ivyde container for real
        ivycp.updateClasspathEntries(newEntries);

        // store the global result
        IvyDEMessage.verbose("Saving attachement properties");
        try {
            FileOutputStream out = new FileOutputStream(containersAttachementFile);
            try {
                prop.store(out, "");
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        } catch (IOException ioe) {
            IvyPlugin.logWarn("IvyDE attachement properties could not be saved", ioe);
        }
    }

    public IPath getSourceAttachment(IPath path) {
        String srcPath = prop.getProperty(path.toPortableString() + SRC_SUFFIX);
        if (srcPath != null && srcPath.length() != 0) {
            return new Path(srcPath);
        }
        return null;
    }

    public IPath getSourceAttachmentRoot(IPath path) {
        String srcPath = prop.getProperty(path.toPortableString() + SRCROOT_SUFFIX);
        if (srcPath != null && srcPath.length() != 0) {
            return new Path(srcPath);
        }
        return null;
    }

    public URL getDocAttachment(IPath path) {
        String srcPath = prop.getProperty(path.toPortableString() + DOC_SUFFIX);
        if (srcPath != null && srcPath.length() != 0) {
            try {
                return new URL(srcPath);
            } catch (MalformedURLException e) {
                IvyPlugin.logWarn("The path for the doc attachement is not a valid URL", e);
                return null;
            }
        }
        return null;
    }

    public IPath getSourceAttachment(IPath classpathArtifact, IPath sourcesArtifact) {
        IPath sourceAttachment = getSourceAttachment(classpathArtifact);
        if (sourceAttachment == null) {
            sourceAttachment = sourcesArtifact;
        }
        return sourceAttachment;
    }

    public IPath getSourceAttachmentRoot(IPath classpathArtifact, IPath sourcesArtifact) {
        IPath sourceAttachment = getSourceAttachmentRoot(classpathArtifact);
        if (sourceAttachment == null && sourcesArtifact != null) {
            sourceAttachment = sourcesArtifact;
        }
        return sourceAttachment;
    }

    public String getJavadocLocation(IClasspathEntry entry) {
        IClasspathAttribute[] attributes = entry.getExtraAttributes();
        for (int j = 0; j < attributes.length; j++) {
            IClasspathAttribute attribute = attributes[j];
            if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
                return attribute.getValue();
            }
        }
        return null;
    }

}
