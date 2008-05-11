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

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

/**
 * path:
 * org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER/ivy.xml/conf/ivysetting.xml/acceptedTypes/sourceTypes/javadocTypes/sourceSuffixes/javadocSuffixes/doRetrieve/retrievePattern/order
 */
public class IvyClasspathContainerConfiguration {

    IJavaProject javaProject;

    String ivyXmlPath;

    List/* <String> */confs = Arrays.asList(new String[] {"default"});

    String ivySettingsPath;

    List/* <String> */acceptedTypes;

    List/* <String> */sourceTypes;

    List/* <String> */javadocTypes;

    List/* <String> */sourceSuffixes;

    List/* <String> */javadocSuffixes;

    boolean doRetrieve;

    String retrievePattern;

    boolean alphaOrder;

    ModuleDescriptor md;

    public IvyClasspathContainerConfiguration(IJavaProject javaProject, String ivyXmlPath,
            List confs) {
        this.javaProject = javaProject;
        this.ivyXmlPath = ivyXmlPath;
        this.confs = confs;
    }

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
                // TODO this should not happend, but if it happend it can break eclipse, a project
                // can be displayed abnormally. This exception should be raised at the UI level,
                // either in the error log or in an popup to the user
                throw new RuntimeException(e);
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
            // in this V1 version, it is just some paranoïd check
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
            // TODO this should not happend, but if it happend it can break eclipse, a project
            // can be displayed abnormally. This exception should be raised at the UI level,
            // either in the error log or in an popup to the user
            throw new RuntimeException(e);
        }
        return new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID).append(path.toString());
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public String getInheritedIvySettingsPath() {
        if (ivySettingsPath == null) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsPath();
        }
        if (javaProject == null) {
            return ivySettingsPath;
        }
        IProject project = javaProject.getProject();
        File loc = project.getLocation().toFile();
        return new File(loc, ivySettingsPath).getAbsolutePath();
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

    public void resolveModuleDescriptor() throws ParseException, MalformedURLException, IOException {
        URL url;
        md = null;
        if (javaProject != null) {
            IFile file = javaProject.getProject().getFile(ivyXmlPath);
            url = new File(file.getLocation().toOSString()).toURL();
        } else {
            url = new File(ivyXmlPath).toURL();
        }
        md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
            IvyPlugin.getIvy(getInheritedIvySettingsPath()).getSettings(), url, false);
    }

}
