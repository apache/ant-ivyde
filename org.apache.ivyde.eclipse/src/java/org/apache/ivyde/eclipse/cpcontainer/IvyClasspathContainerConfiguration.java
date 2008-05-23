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
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * path: org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER? ivyXmlPath=ivy.xml &confs=default
 * &ivySettingsPath=file:///ivysetting.xml &acceptedTypes=jar &sourceTypes=source
 * &javadocTypes=javadoc &sourceSuffixes=-sources,-source,-src
 * &javadocSuffixes=-javadocs,-javadoc,-doc,-docs &doRetrieve=true
 * &retrievePattern=lib/[conf]/[artifact].[ext] &alphaOrder=true
 */
public class IvyClasspathContainerConfiguration {

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

    boolean alphaOrder;

    /**
     * Constructor
     * 
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> is not bind to a
     *            project
     * @param ivyXmlPath
     *            the path to the ivy.xml
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, String ivyXmlPath) {
        this.javaProject = javaProject;
        this.ivyXmlPath = ivyXmlPath;
    }

    /**
     * Constructor
     * 
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> is not bind to a
     *            project
     * @param path
     *            the path of the classpath container
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, IPath path) {
        this.javaProject = javaProject;
        if (path.segmentCount() > 2) {
            loadV0(path);
        } else {
            loadV1(path);
        }
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
        boolean isProjectSpecific = false;
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
                String message = "The UTF-8 encoding support is required is decode the path of the container.";
                IvyPlugin.log(IStatus.ERROR, message, e);
                throw new RuntimeException(message, e);
            }
            if (parameter[0].equals("ivyXmlPath")) {
                ivyXmlPath = value;
            } else if (parameter[0].equals("confs")) {
                confs = IvyClasspathUtil.split(value);
            } else if (parameter[0].equals("ivySettingsPath")) {
                ivySettingsPath = value;
                isProjectSpecific = true;
            } else if (parameter[0].equals("acceptedTypes")) {
                acceptedTypes = IvyClasspathUtil.split(value);
                isProjectSpecific = true;
            } else if (parameter[0].equals("sourceTypes")) {
                sourceTypes = IvyClasspathUtil.split(value);
                isProjectSpecific = true;
            } else if (parameter[0].equals("javadocTypes")) {
                javadocTypes = IvyClasspathUtil.split(value);
                isProjectSpecific = true;
            } else if (parameter[0].equals("sourceSuffixes")) {
                sourceSuffixes = IvyClasspathUtil.split(value);
                isProjectSpecific = true;
            } else if (parameter[0].equals("javadocSuffixes")) {
                javadocSuffixes = IvyClasspathUtil.split(value);
                isProjectSpecific = true;
            } else if (parameter[0].equals("doRetrieve")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                doRetrieve = Boolean.valueOf(value).booleanValue();
                isProjectSpecific = true;
            } else if (parameter[0].equals("retrievePattern")) {
                retrievePattern = value;
                isProjectSpecific = true;
            } else if (parameter[0].equals("alphaOrder")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                alphaOrder = Boolean.valueOf(value).booleanValue();
                isProjectSpecific = true;
            }
        }
        if (isProjectSpecific) {
            // in this V1 version, it is just some paranoid check
            checkNonNullConf();
        }
    }

    private void checkNonNullConf() {
        if (ivySettingsPath == null) {
            ivySettingsPath = IvyPlugin.getPreferenceStoreHelper().getIvySettingsPath();
        }
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
        if (retrievePattern == null) {
            retrievePattern = IvyPlugin.getPreferenceStoreHelper().getRetrievePattern();
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
                path.append("&doRetrieve=");
                path.append(URLEncoder.encode(Boolean.toString(doRetrieve), "UTF-8"));
                path.append("&retrievePattern=");
                path.append(URLEncoder.encode(retrievePattern, "UTF-8"));
                path.append("&alphaOrder=");
                path.append(URLEncoder.encode(Boolean.toString(alphaOrder), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            String message = "The UTF-8 encoding support is required is endecode the path of the container.";
            IvyPlugin.log(IStatus.ERROR, message, e);
            throw new RuntimeException(message, e);
        }
        return new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID).append(path.toString());
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public String getInheritedIvySettingsPath() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsPath();
        }
        if (javaProject == null || ivySettingsPath.trim().length() == 0) {
            return ivySettingsPath;
        }
        URL url;
        try {
            url = new URL(ivySettingsPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        File file = new File(url.getPath());
        if (file.exists()) {
            return ivySettingsPath;
        }
        // the file doesn't exist, so try to find out if it is a relative path to the project.
        IProject project = javaProject.getProject();
        File loc = project.getLocation().toFile();
        file = new File(loc, url.getPath());
        try {
            return file.toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection getInheritedAcceptedTypes() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getAcceptedTypes();
        }
        return acceptedTypes;
    }

    public Collection getInheritedSourceTypes() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getSourceTypes();
        }
        return sourceTypes;
    }

    public Collection getInheritedSourceSuffixes() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getSourceSuffixes();
        }
        return sourceSuffixes;
    }

    public Collection getInheritedJavadocTypes() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getJavadocTypes();
        }
        return javadocTypes;
    }

    public Collection getInheritedJavadocSuffixes() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getJavadocSuffixes();
        }
        return javadocSuffixes;
    }

    public boolean getInheritedDoRetrieve() {
        if (javaProject == null) {
            // no project means no retrieve possible
            return false;
        }
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getDoRetrieve();
        }
        return doRetrieve;
    }

    public String getInheritedRetrievePattern() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrievePattern();
        }
        return retrievePattern;
    }

    public boolean isInheritedAlphaOrder() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().isAlphOrder();
        }
        return alphaOrder;
    }

    public boolean isProjectSpecific() {
        return ivySettingsPath != null;
    }

    private String getInheritablePreferenceString(String value) {
        if (value == null || value.startsWith("[inherited]")) {
            return null;
        }
        return value;
    }

    private List getInheritablePreferenceList(String values) {
        if (values == null || values.startsWith("[inherited]")) {
            return null;
        }
        return IvyClasspathUtil.split(values);
    }

    File getIvyFile() {
        File file;
        if (javaProject != null) {
            IFile f = javaProject.getProject().getFile(ivyXmlPath);
            file = new File(f.getLocation().toOSString());
        } else {
            file = new File(ivyXmlPath);
        }
        return file;
    }

    public ModuleDescriptor getModuleDescriptorSafely(Ivy ivy) {
        File file = getIvyFile();
        try {
            return getModuleDescriptor(ivy);
        } catch (MalformedURLException e) {
            MessageDialog.openWarning(IvyPlugin.getActiveWorkbenchShell(), "Incorrect path",
                "The path to the ivy.xml file is incorrect: '" + file.getAbsolutePath()
                        + "'. \nError:" + e.getMessage());
            return null;
        } catch (ParseException e) {
            MessageDialog.openWarning(IvyPlugin.getActiveWorkbenchShell(), "Incorrect ivy file",
                "The ivy file '" + file.getAbsolutePath() + "' could not be parsed. \nError:"
                        + e.getMessage());
            return null;
        } catch (IOException e) {
            MessageDialog.openWarning(IvyPlugin.getActiveWorkbenchShell(), "Read error",
                "The ivy file '" + file.getAbsolutePath() + "' could not be read. \nError:"
                        + e.getMessage());
            return null;
        }
    }

    public ModuleDescriptor getModuleDescriptor(Ivy ivy) throws MalformedURLException,
            ParseException, IOException {
        return getModuleDescriptor(ivy, getIvyFile());
    }

    public ModuleDescriptor getModuleDescriptor(Ivy ivy, File ivyFile)
            throws MalformedURLException, ParseException, IOException {
        return ModuleDescriptorParserRegistry.getInstance().parseDescriptor(ivy.getSettings(),
            ivyFile.toURL(), false);
    }

}
