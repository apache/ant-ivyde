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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * path: org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER? ivyXmlPath=ivy.xml &confs=default
 * &ivySettingsPath=file:///ivysetting.xml &acceptedTypes=jar &sourceTypes=source
 * &javadocTypes=javadoc &sourceSuffixes=-sources,-source,-src
 * &javadocSuffixes=-javadocs,-javadoc,-doc,-docs &doRetrieve=true
 * &retrievePattern=lib/[conf]/[artifact].[ext] &alphaOrder=true
 */
public class IvyClasspathContainerConfiguration {

    private static final String UTF8_ERROR = "The UTF-8 encoding support is required"
            + " is decode the path of the container.";

    private static final String PROJECT_SCHEME_PREFIX = "project://";

    private static final int PROJECT_SCHEME_PREFIX_LENGTH = PROJECT_SCHEME_PREFIX.length();

    final IJavaProject javaProject;

    String ivyXmlPath;

    List/* <String> */confs = Arrays.asList(new String[] {"*"});

    String ivySettingsPath;

    List/* <String> */acceptedTypes;

    List/* <String> */sourceTypes;

    List/* <String> */javadocTypes;

    List/* <String> */sourceSuffixes;

    List/* <String> */javadocSuffixes;

    boolean doRetrieve;

    String retrievePattern;

    boolean retrieveSync = false;

    String retrieveConfs = "*";

    String retrieveTypes = "*";

    boolean alphaOrder;

    boolean resolveInWorkspace;

    private Ivy ivy;

    long ivySettingsLastModified = -1;

    boolean confOk;

    private final boolean editing;

    boolean isAdvancedProjectSpecific;

    boolean isRetrieveProjectSpecific;

    /**
     * Constructor
     * 
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> is not bind to a project
     * @param ivyXmlPath
     *            the path to the ivy.xml
     * @param editing
     *            if set to true, this bean will be used for edition purpose, so no need to trigger
     *            UI notification about some errors in there
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, String ivyXmlPath,
            boolean editing) {
        this.javaProject = javaProject;
        this.ivyXmlPath = ivyXmlPath;
        this.editing = editing;
    }

    /**
     * Constructor
     * 
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> is not bind to a project
     * @param path
     *            the path of the classpath container
     * @param editing
     *            if set to true, this bean will be used for edition purpose, so no need to trigger
     *            UI notification about some errors in there
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, IPath path, boolean editing) {
        this.javaProject = javaProject;
        this.editing = editing;
        if (path.segmentCount() > 2) {
            loadV0(path);
        } else {
            loadV1(path);
        }
    }

    public String toString() {
        return (javaProject == null ? "" : "project '" + javaProject.getProject().getName()
                + "' and ivy file '")
                + ivyXmlPath + (javaProject == null ? "" : "'");
    }

    /**
     * Load the pre-IVYDE-70 configuration
     * 
     * @param path
     *            the path of the container
     */
    private void loadV0(IPath path) {
        // load some configuration that can be loaded
        ivyXmlPath = path.removeFirstSegments(1).removeLastSegments(1).toString();
        confs = IvyClasspathUtil.split(path.lastSegment());
        // the last part of the configuration coming from the preferences cannot be loaded due to
        // the bug described in IVYDE-70, so the configuration is let as the default one
    }

    /**
     * Load the post-IVYDE-70 configuration
     * 
     * @param path
     *            the path of the container
     */
    private void loadV1(IPath path) {
        String url = path.segment(1).substring(1);
        String[] parameters = url.split("&");
        isAdvancedProjectSpecific = false;
        isRetrieveProjectSpecific = false;
        for (int i = 0; i < parameters.length; i++) {
            String[] parameter = parameters[i].split("=");
            if (parameter == null || parameter.length == 0) {
                continue;
            }
            String value;
            try {
                value = parameter.length > 1 ? URLDecoder.decode(parameter[1], "UTF-8") : "";
            } catch (UnsupportedEncodingException e) {
                // this should never never happen
                IvyPlugin.log(IStatus.ERROR, UTF8_ERROR, e);
                throw new RuntimeException(UTF8_ERROR, e);
            }
            if (parameter[0].equals("ivyXmlPath")) {
                ivyXmlPath = value;
            } else if (parameter[0].equals("confs")) {
                confs = IvyClasspathUtil.split(value);
            } else if (parameter[0].equals("ivySettingsPath")) {
                ivySettingsPath = readOldIvySettings(value);
            } else if (parameter[0].equals("doRetrieve")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                doRetrieve = Boolean.valueOf(value).booleanValue();
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrievePattern")) {
                retrievePattern = value;
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveSync")) {
                retrieveSync = Boolean.valueOf(value).booleanValue();
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveConfs")) {
                retrieveConfs = value;
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveTypes")) {
                retrieveTypes = value;
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("acceptedTypes")) {
                acceptedTypes = IvyClasspathUtil.split(value);
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("sourceTypes")) {
                sourceTypes = IvyClasspathUtil.split(value);
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("javadocTypes")) {
                javadocTypes = IvyClasspathUtil.split(value);
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("sourceSuffixes")) {
                sourceSuffixes = IvyClasspathUtil.split(value);
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("javadocSuffixes")) {
                javadocSuffixes = IvyClasspathUtil.split(value);
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("alphaOrder")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                alphaOrder = Boolean.valueOf(value).booleanValue();
                isAdvancedProjectSpecific = true;
            } else if (parameter[0].equals("resolveInWorkspace")) {
                resolveInWorkspace = Boolean.valueOf(value).booleanValue();
                isAdvancedProjectSpecific = true;
            }
        }
        if (isAdvancedProjectSpecific) {
            // in this V1 version, it is just some paranoid check
            checkNonNullConf();
        }
        if (isRetrieveProjectSpecific) {
            if (retrievePattern == null) {
                retrievePattern = IvyPlugin.getPreferenceStoreHelper().getRetrievePattern();
            }
        }
    }

    /**
     * Read old configuration that were based on relative urls, like: "file://./ivysettings.xml".
     * This kind of URL "project:///ivysettings.xml" should be used now.
     * 
     * @param value
     *            the value to read
     * @return
     */
    private String readOldIvySettings(String value) {
        if (javaProject == null) {
            return value;
        }
        URL url;
        try {
            url = new URL(value);
        } catch (MalformedURLException e) {
            return value;
        }
        File file = new File(url.getPath());
        if (file.exists()) {
            return value;
        }
        // the file doesn't exist, so try to find out if it is a relative path to the project.
        file = new File(javaProject.getProject().getFile(url.getPath()).getLocation().toOSString());
        return PROJECT_SCHEME_PREFIX + url.getPath();
    }

    private void checkNonNullConf() {
        if (acceptedTypes == null) {
            acceptedTypes = IvyPlugin.getPreferenceStoreHelper().getAcceptedTypes();
        }
        if (sourceTypes == null) {
            sourceTypes = IvyPlugin.getPreferenceStoreHelper().getSourceTypes();
        }
        if (javadocTypes == null) {
            javadocTypes = IvyPlugin.getPreferenceStoreHelper().getJavadocTypes();
        }
        if (sourceSuffixes == null) {
            sourceSuffixes = IvyPlugin.getPreferenceStoreHelper().getSourceSuffixes();
        }
        if (javadocSuffixes == null) {
            javadocSuffixes = IvyPlugin.getPreferenceStoreHelper().getJavadocSuffixes();
        }
    }

    public IPath getPath() {
        StringBuffer path = new StringBuffer();
        path.append('?');
        path.append("ivyXmlPath=");
        try {
            path.append(URLEncoder.encode(ivyXmlPath, "UTF-8"));
            path.append("&confs=");
            path.append(URLEncoder.encode(IvyClasspathUtil.concat(confs), "UTF-8"));
            if (ivySettingsPath != null) {
                path.append("&ivySettingsPath=");
                path.append(URLEncoder.encode(ivySettingsPath, "UTF-8"));
            }
            if (isRetrieveProjectSpecific) {
                path.append("&doRetrieve=");
                path.append(URLEncoder.encode(Boolean.toString(doRetrieve), "UTF-8"));
                path.append("&retrievePattern=");
                path.append(URLEncoder.encode(retrievePattern, "UTF-8"));
                path.append("&retrieveSync=");
                path.append(URLEncoder.encode(Boolean.toString(retrieveSync), "UTF-8"));
                path.append("&retrieveConfs=");
                path.append(URLEncoder.encode(retrieveConfs, "UTF-8"));
                path.append("&retrieveTypes=");
                path.append(URLEncoder.encode(retrieveTypes, "UTF-8"));
            }
            if (isAdvancedProjectSpecific) {
                path.append("&acceptedTypes=");
                path.append(URLEncoder.encode(IvyClasspathUtil.concat(acceptedTypes), "UTF-8"));
                path.append("&sourceTypes=");
                path.append(URLEncoder.encode(IvyClasspathUtil.concat(sourceTypes), "UTF-8"));
                path.append("&javadocTypes=");
                path.append(URLEncoder.encode(IvyClasspathUtil.concat(javadocTypes), "UTF-8"));
                path.append("&sourceSuffixes=");
                path.append(URLEncoder.encode(IvyClasspathUtil.concat(sourceSuffixes), "UTF-8"));
                path.append("&javadocSuffixes=");
                path.append(URLEncoder.encode(IvyClasspathUtil.concat(javadocSuffixes), "UTF-8"));
                path.append("&alphaOrder=");
                path.append(URLEncoder.encode(Boolean.toString(alphaOrder), "UTF-8"));
                path.append("&resolveInWorkspace=");
                path.append(URLEncoder.encode(Boolean.toString(this.resolveInWorkspace), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            IvyPlugin.log(IStatus.ERROR, UTF8_ERROR, e);
            throw new RuntimeException(UTF8_ERROR, e);
        }
        return new Path(IvyClasspathContainer.CONTAINER_ID).append(path.toString());
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public List getConfs() {
        return confs;
    }

    private void setConfStatus(IvyDEException e) {
        if (!editing && confOk != (e == null)) {
            confOk = (e == null);
            IvyPlugin.getDefault().getContainerDecorator().statusChaged(this);
            if (e != null) {
                setResolveStatus(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR, e
                        .getMessage(), e.getCause()));
            } else {
                setResolveStatus(Status.OK_STATUS);
            }
        }
    }

    public void setResolveStatus(IStatus status) {
        if (!editing && javaProject != null) {
            IFile ivyFile = javaProject.getProject().getFile(ivyXmlPath);
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

    public Ivy getIvy() throws IvyDEException {
        String settingsPath = getInheritedIvySettingsPath();
        if (settingsPath == null || settingsPath.trim().length() == 0) {
            // no settings specified, so take the default one
            if (ivy == null) {
                IvySettings ivySettings = new IvySettings();
                if (javaProject != null) {
                    ivySettings.setBaseDir(javaProject.getProject().getLocation().toFile());
                }
                try {
                    ivySettings.loadDefault();
                } catch (ParseException e) {
                    IvyDEException ex = new IvyDEException(
                            "Parsing error of the default Ivy settings",
                            "The default Ivy settings file could not be parsed (" + this.toString()
                                    + ")", e);
                    setConfStatus(ex);
                    throw ex;
                } catch (IOException e) {
                    IvyDEException ex = new IvyDEException(
                            "Read error of the default Ivy settings",
                            "The default Ivy settings file could not be read (" + this.toString()
                                    + ")", e);
                    setConfStatus(ex);
                    throw ex;
                }
                ivy = Ivy.newInstance(ivySettings);
            }
            setConfStatus(null);
            return ivy;
        }

        if (settingsPath.startsWith(PROJECT_SCHEME_PREFIX)) {
            int pathIndex = settingsPath.indexOf("/", PROJECT_SCHEME_PREFIX_LENGTH);
            String projectName = settingsPath.substring(PROJECT_SCHEME_PREFIX_LENGTH, pathIndex);
            String path = settingsPath.substring(pathIndex + 1);
            if (projectName.equals("")) {
                IFile f = javaProject.getProject().getFile(path);
                File file = f.getLocation().toFile();
                return getIvy(file);
            } else {
                try {
                    IJavaProject[] javaProjects = JavaCore.create(
                        ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
                    int i;
                    for (i = 0; i < javaProjects.length; i++) {
                        if (javaProjects[i].getProject().getName().equals(projectName)) {
                            break;
                        }
                    }
                    if (i == javaProjects.length) {
                        IvyDEException ex = new IvyDEException("Project '" + projectName
                                + "' not found", "The project name '" + projectName + "' from '"
                                + settingsPath + "' was not found (" + this.toString() + ")", null);
                        setConfStatus(ex);
                        throw ex;
                    }
                    IFile f = javaProjects[i].getProject().getFile(path);
                    File file = new File(f.getLocation().toOSString());
                    return getIvy(file);
                } catch (JavaModelException e) {
                    IvyDEException ex = new IvyDEException("The workspace is broken",
                            "The projects in the workspace could not be listed when resolving the settings ("
                                    + this.toString() + ")", null);
                    setConfStatus(ex);
                    throw ex;
                }
            }
        }
        // before returning the found ivy, try to refresh it if the settings changed
        URL url;
        try {
            url = new URL(settingsPath);
        } catch (MalformedURLException e) {
            IvyDEException ex = new IvyDEException("Incorrect url of the Ivy settings",
                    "The Ivy settings url '" + settingsPath + "' is incorrect (" + this.toString()
                            + ")", e);
            setConfStatus(ex);
            throw ex;
        }
        if (url.getProtocol().startsWith("file")) {
            File file = new File(url.getPath());
            return getIvy(file);
        } else {
            // an URL but not a file
            if (ivy == null || ivySettingsLastModified == -1) {
                IvySettings ivySettings = new IvySettings();
                if (javaProject != null) {
                    ivySettings.setBaseDir(javaProject.getProject().getLocation().toFile());
                }
                try {
                    ivySettings.load(url);
                    ivySettingsLastModified = 0;
                } catch (ParseException e) {
                    IvyDEException ex = new IvyDEException("Parsing error of the Ivy settings",
                            "The ivy settings file '" + settingsPath + "' could not be parsed ("
                                    + this.toString() + ")", e);
                    setConfStatus(ex);
                    throw ex;
                } catch (IOException e) {
                    IvyDEException ex = new IvyDEException("Read error of the Ivy settings",
                            "The ivy settings file '" + settingsPath + "' could not be read ("
                                    + this.toString() + ")", e);
                    setConfStatus(ex);
                    throw ex;
                }
                ivy = Ivy.newInstance(ivySettings);
            }
        }
        setConfStatus(null);
        return ivy;
    }

    private Ivy getIvy(File file) throws IvyDEException {
        if (!file.exists()) {
            IvyDEException ex = new IvyDEException("Ivy settings file not found",
                    "The Ivy settings file '" + ivySettingsPath + "' cannot be found ("
                            + this.toString() + ")", null);
            setConfStatus(ex);
            throw ex;
        }

        if (file.lastModified() != ivySettingsLastModified) {
            IvySettings ivySettings = new IvySettings();
            if (javaProject != null) {
                ivySettings.setBaseDir(javaProject.getProject().getLocation().toFile());
            }
            if (ivySettingsLastModified == -1) {
                Message.info("\n\n");
            } else {
                Message.info("\n\nIVYDE: ivysettings has changed, configuring ivy again\n");
            }
            try {
                ivySettings.load(file);
            } catch (ParseException e) {
                IvyDEException ex = new IvyDEException("Parsing error of the Ivy settings",
                        "The ivy settings file '" + ivySettingsPath + "' could not be parsed ("
                                + this.toString() + ")", e);
                setConfStatus(ex);
                throw ex;
            } catch (IOException e) {
                IvyDEException ex = new IvyDEException("Read error of the Ivy settings",
                        "The ivy settings file '" + ivySettingsPath + "' could not be read ("
                                + this.toString() + ")", e);
                setConfStatus(ex);
                throw ex;
            }
            ivy = Ivy.newInstance(ivySettings);
            ivySettingsLastModified = file.lastModified();
        }

        return ivy;
    }

    public String getInheritedIvySettingsPath() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsPath();
        }
        return ivySettingsPath;
    }

    public boolean getInheritedDoRetrieve() {
        if (javaProject == null) {
            // no project means no retrieve possible
            return false;
        }
        if (!isRetrieveProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getDoRetrieve();
        }
        return doRetrieve;
    }

    public String getInheritedRetrievePattern() {
        if (!isRetrieveProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrievePattern();
        }
        return retrievePattern;
    }

    public String getInheritedRetrieveConfs() {
        if (!isRetrieveProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrieveConfs();
        }
        return retrieveConfs;
    }

    public String getInheritedRetrieveTypes() {
        if (!isRetrieveProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrieveTypes();
        }
        return retrieveTypes;
    }

    public boolean getInheritedRetrieveSync() {
        if (!isRetrieveProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrieveSync();
        }
        return retrieveSync;
    }

    public Collection getInheritedAcceptedTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getAcceptedTypes();
        }
        return acceptedTypes;
    }

    public Collection getInheritedSourceTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getSourceTypes();
        }
        return sourceTypes;
    }

    public Collection getInheritedSourceSuffixes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getSourceSuffixes();
        }
        return sourceSuffixes;
    }

    public Collection getInheritedJavadocTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getJavadocTypes();
        }
        return javadocTypes;
    }

    public Collection getInheritedJavadocSuffixes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getJavadocSuffixes();
        }
        return javadocSuffixes;
    }

    public boolean isInheritedAlphaOrder() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isAlphOrder();
        }
        return alphaOrder;
    }

    public boolean isResolveInWorkspace() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isResolveInWorkspace();
        }
        return resolveInWorkspace;
    }

    public boolean isSettingsProjectSpecific() {
        return ivySettingsPath != null;
    }

    public boolean isAdvancedProjectSpecific() {
        return isAdvancedProjectSpecific;
    }

    public boolean isRetrieveProjectSpecific() {
        return isRetrieveProjectSpecific;
    }

    public File getIvyFile() {
        File file;
        if (javaProject != null) {
            IFile f = javaProject.getProject().getFile(ivyXmlPath);
            file = new File(f.getLocation().toOSString());
        } else {
            file = new File(ivyXmlPath);
        }
        return file;
    }

    public ModuleDescriptor getModuleDescriptor() throws IvyDEException {
        File file = getIvyFile();
        if (!file.exists()) {
            IvyDEException ex = new IvyDEException("Ivy file not found", "The ivy.xml file '"
                    + file.getAbsolutePath() + "' was not found (" + this.toString() + ")", null);
            setConfStatus(ex);
            throw ex;
        }
        try {
            Ivy i = getIvy();
            ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
                i.getSettings(), file.toURL(), false);
            setConfStatus(null);
            return md;
        } catch (MalformedURLException e) {
            IvyDEException ex = new IvyDEException("Incorrect URL of the Ivy file",
                    "The URL to the ivy.xml file is incorrect: '" + file.getAbsolutePath() + "' ("
                            + this.toString() + ")", e);
            setConfStatus(ex);
            throw ex;
        } catch (ParseException e) {
            IvyDEException ex = new IvyDEException("Parsing error of the Ivy file",
                    "The ivy file '" + file.getAbsolutePath() + "' could not be parsed ("
                            + this.toString() + ")", e);
            setConfStatus(ex);
            throw ex;
        } catch (IOException e) {
            IvyDEException ex = new IvyDEException("Read error of the Ivy file", "The ivy file '"
                    + file.getAbsolutePath() + "' could not be read (" + this.toString() + ")", e);
            setConfStatus(ex);
            throw ex;
        }
    }

}
