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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;

/**
 * This class maps the IvyDE classpath container configuration into Eclipse objects representing
 * containers (IPath and IClasspathAttribute).
 */
public final class IvyClasspathContainerConfAdapter {

    private static final String UTF8_ERROR = "The UTF-8 encoding support is required"
            + " is decode the path of the container.";

    private static final String PROJECT_SCHEME_PREFIX = "project://";

    private IvyClasspathContainerConfAdapter() {
        // utility class
    }

    public static void load(IvyClasspathContainerConfiguration conf, IPath path,
            IClasspathAttribute[] attributes) {
        if (path.segmentCount() > 2) {
            loadV0(conf, path);
        } else {
            loadV1(conf, path);
        }
        conf.setAttributes(attributes);
    }

    /**
     * Load the pre-IVYDE-70 configuration
     * 
     * @param path
     *            the path of the container
     */
    private static void loadV0(IvyClasspathContainerConfiguration conf, IPath path) {
        // load some configuration that can be loaded
        conf.setIvyXmlPath(path.removeFirstSegments(1).removeLastSegments(1).toString());
        List confs = IvyClasspathUtil.split(path.lastSegment());
        if (confs.isEmpty()) {
            confs = Collections.singletonList("*");
        }
        conf.setConfs(confs);
        // the last part of the configuration coming from the preferences cannot be loaded due to
        // the bug described in IVYDE-70, so the configuration is let as the default one
    }

    /**
     * Load the post-IVYDE-70 configuration
     * 
     * @param path
     *            the path of the container
     */
    private static void loadV1(IvyClasspathContainerConfiguration conf, IPath path) {
        ContainerMappingSetup mappingSetup = conf.getContainerMappingSetup();
        RetrieveSetup retrieveSetup = conf.getRetrieveSetup();
        IvySettingsSetup settingsSetup = conf.getIvySettingsSetup();

        String url = path.segment(1).substring(1);
        String[] parameters = url.split("&");
        conf.setAdvancedProjectSpecific(false);
        conf.setRetrieveProjectSpecific(false);
        conf.setSettingsProjectSpecific(false);
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
                conf.setIvyXmlPath(value);
            } else if (parameter[0].equals("confs")) {
                List confs = IvyClasspathUtil.split(value);
                if (confs.isEmpty()) {
                    confs = Collections.singletonList("*");
                }
                conf.setConfs(confs);
            } else if (parameter[0].equals("ivySettingsPath")) {
                settingsSetup.setIvySettingsPath(readOldSettings(conf, value));
                conf.setSettingsProjectSpecific(true);
            } else if (parameter[0].equals("loadSettingsOnDemand")) {
                settingsSetup.setLoadSettingsOnDemand(Boolean.valueOf(value).booleanValue());
                conf.setSettingsProjectSpecific(true);
            } else if (parameter[0].equals("propertyFiles")) {
                settingsSetup.setPropertyFiles(IvyClasspathUtil.split(value));
                conf.setSettingsProjectSpecific(true);
            } else if (parameter[0].equals("doRetrieve")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                retrieveSetup.setDoRetrieve(Boolean.valueOf(value).booleanValue());
                conf.setRetrieveProjectSpecific(true);
            } else if (parameter[0].equals("retrievePattern")) {
                retrieveSetup.setRetrievePattern(value);
                conf.setRetrieveProjectSpecific(true);
            } else if (parameter[0].equals("retrieveSync")) {
                retrieveSetup.setRetrieveSync(Boolean.valueOf(value).booleanValue());
                conf.setRetrieveProjectSpecific(true);
            } else if (parameter[0].equals("retrieveConfs")) {
                retrieveSetup.setRetrieveConfs(value);
                conf.setRetrieveProjectSpecific(true);
            } else if (parameter[0].equals("retrieveTypes")) {
                retrieveSetup.setRetrieveTypes(value);
                conf.setRetrieveProjectSpecific(true);
            } else if (parameter[0].equals("acceptedTypes")) {
                mappingSetup.setAcceptedTypes(IvyClasspathUtil.split(value));
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("sourceTypes")) {
                mappingSetup.setSourceTypes(IvyClasspathUtil.split(value));
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("javadocTypes")) {
                mappingSetup.setJavadocTypes(IvyClasspathUtil.split(value));
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("sourceSuffixes")) {
                mappingSetup.setSourceSuffixes(IvyClasspathUtil.split(value));
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("javadocSuffixes")) {
                mappingSetup.setJavadocSuffixes(IvyClasspathUtil.split(value));
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("alphaOrder")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                conf.setAlphaOrder(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("resolveInWorkspace")) {
                conf.setResolveInWorkspace(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            }
        }
        if (conf.isAdvancedProjectSpecific()) {
            // in this V1 version, it is just some paranoid check
            checkNonNullConf(conf);
        }
        if (conf.isRetrieveProjectSpecific()) {
            if (retrieveSetup.getRetrievePattern() == null) {
                retrieveSetup.setRetrievePattern(IvyPlugin.getPreferenceStoreHelper()
                        .getRetrieveSetup().getRetrievePattern());
            }
        }
    }

    /**
     * Read old configuration that were based on relative urls, like: "file://./ivysettings.xml" or
     * "file:./ivysettings.xml". This kind of URL "project:///ivysettings.xml" should be used now.
     * 
     * @param value
     *            the value to read
     * @return
     */
    private static String readOldSettings(IvyClasspathContainerConfiguration conf, String value) {
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
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
        // the file doesn't exist, it is a relative path to the project.
        String urlpath = url.getPath();
        if (urlpath != null && urlpath.startsWith("./")) {
            urlpath = urlpath.substring(1);
        }
        return PROJECT_SCHEME_PREFIX + urlpath;
    }

    private static void checkNonNullConf(IvyClasspathContainerConfiguration conf) {
        ContainerMappingSetup mappingSetup = conf.getContainerMappingSetup();
        IvySettingsSetup settingsSetup = conf.getIvySettingsSetup();
        ContainerMappingSetup prefStoreMappingSetup = IvyPlugin.getPreferenceStoreHelper()
                .getContainerMappingSetup();
        if (settingsSetup.getPropertyFiles() == null) {
            settingsSetup.setPropertyFiles(IvyPlugin.getPreferenceStoreHelper()
                    .getIvySettingsSetup().getPropertyFiles());
        }
        if (mappingSetup.getAcceptedTypes() == null) {
            mappingSetup.setAcceptedTypes(prefStoreMappingSetup.getAcceptedTypes());
        }
        if (mappingSetup.getSourceTypes() == null) {
            mappingSetup.setSourceTypes(prefStoreMappingSetup.getSourceTypes());
        }
        if (mappingSetup.getJavadocTypes() == null) {
            mappingSetup.setJavadocTypes(prefStoreMappingSetup.getJavadocTypes());
        }
        if (mappingSetup.getSourceSuffixes() == null) {
            mappingSetup.setSourceSuffixes(prefStoreMappingSetup.getSourceSuffixes());
        }
        if (mappingSetup.getJavadocSuffixes() == null) {
            mappingSetup.setJavadocSuffixes(prefStoreMappingSetup.getJavadocSuffixes());
        }
    }

    public static IPath getPath(IvyClasspathContainerConfiguration conf) {
        StringBuffer path = new StringBuffer();
        path.append('?');
        path.append("ivyXmlPath=");
        try {
            path.append(URLEncoder.encode(conf.getIvyXmlPath(), "UTF-8"));
            append(path, "confs", conf.getConfs());
            if (conf.isSettingsProjectSpecific()) {
                IvySettingsSetup setup = conf.getIvySettingsSetup();
                append(path, "ivySettingsPath", setup.getIvySettingsPath());
                append(path, "loadSettingsOnDemand", setup.isLoadSettingsOnDemand());
                append(path, "propertyFiles", setup.getPropertyFiles());
            }
            if (conf.isRetrieveProjectSpecific()) {
                RetrieveSetup setup = conf.getRetrieveSetup();
                append(path, "doRetrieve", setup.isDoRetrieve());
                append(path, "retrievePattern", setup.getRetrievePattern());
                append(path, "retrieveSync", setup.isRetrieveSync());
                append(path, "retrieveConfs", setup.getRetrieveConfs());
                append(path, "retrieveTypes", setup.getRetrieveTypes());
            }
            if (conf.isAdvancedProjectSpecific()) {
                ContainerMappingSetup setup = conf.getContainerMappingSetup();
                append(path, "acceptedTypes", setup.getAcceptedTypes());
                append(path, "sourceTypes", setup.getSourceTypes());
                append(path, "javadocTypes", setup.getJavadocTypes());
                append(path, "sourceSuffixes", setup.getSourceSuffixes());
                append(path, "javadocSuffixes", setup.getJavadocSuffixes());
                append(path, "alphaOrder", conf.isAlphaOrder());
                append(path, "resolveInWorkspace", conf.isResolveInWorkspace());
            }
        } catch (UnsupportedEncodingException e) {
            IvyPlugin.log(IStatus.ERROR, UTF8_ERROR, e);
            throw new RuntimeException(UTF8_ERROR, e);
        }
        return new Path(IvyClasspathContainer.CONTAINER_ID).append(path.toString());
    }

    private static void append(StringBuffer path, String name, String value)
            throws UnsupportedEncodingException {
        path.append('&');
        path.append(name);
        path.append('=');
        path.append(URLEncoder.encode(value, "UTF-8"));
    }

    private static void append(StringBuffer path, String name, List values)
            throws UnsupportedEncodingException {
        append(path, name, IvyClasspathUtil.concat(values));
    }

    private static void append(StringBuffer path, String name, boolean value)
            throws UnsupportedEncodingException {
        append(path, name, Boolean.toString(value));
    }
}
