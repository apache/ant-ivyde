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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyNature;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.eclipse.retrieve.StandaloneRetrieveSetup;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class maps the IvyDE classpath container configuration into Eclipse objects representing
 * containers (IPath and IClasspathAttribute).
 */
public final class IvyClasspathContainerConfAdapter {

    private static final String UTF8_ERROR = "The UTF-8 encoding support is required"
            + " is decode the path of the container.";

    private static final String PROJECT_SCHEME_PREFIX = "project://";

    private static final int PROJECT_SCHEME_PREFIX_LENGTH = PROJECT_SCHEME_PREFIX.length();

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

        if (!FakeProjectManager.isFake(conf.getJavaProject())) {
            // ensure that the project has the Ivy nature
            IvyNature.addNature(conf.getJavaProject().getProject());
        }
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
        IvySettingsSetup settingsSetup = conf.getIvySettingsSetup();
        RetrieveSetup retrievedClasspathSetup = conf.getRetrievedClasspathSetup();

        String url = path.segment(1).substring(1);
        String[] parameters = url.split("&");
        conf.setAdvancedProjectSpecific(false);
        conf.setSettingsProjectSpecific(false);

        String ivyXmlPath = "ivy.xml";
        boolean doStandaloneRetrieve = false;
        boolean isRetrieveProjectSpecific = false;
        RetrieveSetup standaloneRetrieveSetup = new RetrieveSetup();

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
            } else if (parameter[0].equals("resolveBeforeLaunch")) {
                conf.setResolveBeforeLaunch(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("retrievedClasspath")) {
                conf.setRetrievedClasspath(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("retrievedClasspathPattern")) {
                retrievedClasspathSetup.setRetrievePattern(value);
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("retrievedClasspathSync")) {
                retrievedClasspathSetup.setRetrieveSync(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("retrievedClasspathTypes")) {
                retrievedClasspathSetup.setRetrieveTypes(value);
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("mapIfOnlyOneSource")) {
                mappingSetup.setMapIfOnlyOneSource(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            } else if (parameter[0].equals("mapIfOnlyOneJavadoc")) {
                mappingSetup.setMapIfOnlyOneJavadoc(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);

                // the following is the retrieve conf pre -IVYDE-56
                // from this conf should be build StandaloneRetrieveSetup

            } else if (parameter[0].equals("doRetrieve")) {
                // if the value is not actually "true" or "false", the Boolean class ensure to
                // return false, so it is fine
                doStandaloneRetrieve = Boolean.valueOf(value).booleanValue();
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrievePattern")) {
                standaloneRetrieveSetup.setRetrievePattern(value);
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveSync")) {
                standaloneRetrieveSetup.setRetrieveSync(Boolean.valueOf(value).booleanValue());
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveConfs")) {
                standaloneRetrieveSetup.setRetrieveConfs(value);
                isRetrieveProjectSpecific = true;
            } else if (parameter[0].equals("retrieveTypes")) {
                standaloneRetrieveSetup.setRetrieveTypes(value);
                isRetrieveProjectSpecific = true;
            }
            else if (parameter[0].equals("useExtendedResolveId")) {
                conf.setUseExtendedResolveId(Boolean.valueOf(value).booleanValue());
                conf.setAdvancedProjectSpecific(true);
            }
        }
        if (conf.isAdvancedProjectSpecific()) {
            // in this V1 version, it is just some paranoid check
            checkNonNullConf(conf);
        }

        // convert pre IVYDE-56 conf
        convertOldRetrieveConf(conf, isRetrieveProjectSpecific, doStandaloneRetrieve,
            standaloneRetrieveSetup, settingsSetup, ivyXmlPath);
    }

    private static void convertOldRetrieveConf(IvyClasspathContainerConfiguration conf,
            boolean isRetrieveProjectSpecific, boolean doStandaloneRetrieve,
            RetrieveSetup retrieveSetup, IvySettingsSetup settingsSetup, String ivyXmlPath) {
        if (conf.getJavaProject() == null) {
            // no project means no retrieve possible
            return;
        }

        StandaloneRetrieveSetup setup = new StandaloneRetrieveSetup();
        setup.setName("dependencies");
        setup.setIvySettingsSetup(settingsSetup);
        setup.setIvyXmlPath(ivyXmlPath);
        setup.setSettingsProjectSpecific(conf.isSettingsProjectSpecific());
        setup.setProject(conf.getJavaProject().getProject());

        IPreferenceStore prefStore = IvyPlugin.getDefault().getPreferenceStore();

        if (isRetrieveProjectSpecific) {
            if (!doStandaloneRetrieve) {
                return;
            }
        } else {
            if (!prefStore.getBoolean(PreferenceConstants.DO_RETRIEVE)) {
                return;
            }
            retrieveSetup = new RetrieveSetup();
            retrieveSetup.setRetrieveConfs(prefStore.getString(PreferenceConstants.RETRIEVE_CONFS));
            retrieveSetup.setRetrievePattern(prefStore
                    .getString(PreferenceConstants.RETRIEVE_PATTERN));
            retrieveSetup.setRetrieveSync(prefStore.getBoolean(PreferenceConstants.RETRIEVE_SYNC));
            retrieveSetup.setRetrieveTypes(prefStore.getString(PreferenceConstants.RETRIEVE_TYPES));
        }

        if (retrieveSetup.getRetrievePattern() == null) {
            retrieveSetup.setRetrievePattern(prefStore
                    .getString(PreferenceConstants.RETRIEVE_PATTERN));
        }

        setup.setRetrieveSetup(retrieveSetup);

        RetrieveSetupManager manager = IvyPlugin.getDefault().getRetrieveSetupManager();
        IProject project = conf.getJavaProject().getProject();
        List retrieveSetups;
        try {
            retrieveSetups = manager.getSetup(project);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        retrieveSetups.add(setup);
        try {
            manager.save(project, retrieveSetups);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Read old configuration that were based on relative urls, like: "file://./ivysettings.xml" or
     * "file:./ivysettings.xml", and also URL like "project:///ivysettings.xml".
     * <p>
     * It will be replaced by the Eclipse variable ${workspace_loc: ... }
     * 
     * @param value
     *            the value to read
     * @return
     */
    private static String readOldSettings(IvyClasspathContainerConfiguration conf, String value) {
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
            return value;
        }
        if (value.startsWith(PROJECT_SCHEME_PREFIX)) {
            String path = value.substring(PROJECT_SCHEME_PREFIX_LENGTH);
            if (path.startsWith("/")) {
                path = conf.getJavaProject().getProject().getName() + path;
            }
            return "${workspace_loc:" + path + "}";
        }
        if (value.startsWith("file://./") || value.startsWith("file:./")) {
            // CheckStyle:MagicNumber| OFF
            if (value.charAt(5) == '/') {
                value = value.substring(8);
            } else {
                value = value.substring(6);
            }
            // CheckStyle:MagicNumber| ON
            return "${workspace_loc:" + conf.getJavaProject().getProject().getName() + value + "}";
        }
        return value;
    }

    private static void checkNonNullConf(IvyClasspathContainerConfiguration conf) {
        ContainerMappingSetup mappingSetup = conf.getContainerMappingSetup();
        IvySettingsSetup settingsSetup = conf.getIvySettingsSetup();
        ContainerMappingSetup prefStoreMappingSetup = IvyPlugin.getPreferenceStoreHelper()
                .getContainerMappingSetup();
        if (settingsSetup.getRawPropertyFiles() == null) {
            settingsSetup.setPropertyFiles(IvyPlugin.getPreferenceStoreHelper()
                    .getIvySettingsSetup().getRawPropertyFiles());
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
                append(path, "ivySettingsPath", setup.getRawIvySettingsPath());
                append(path, "loadSettingsOnDemand", setup.isLoadSettingsOnDemand());
                append(path, "propertyFiles", setup.getRawPropertyFiles());
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
                append(path, "resolveBeforeLaunch", conf.isResolveBeforeLaunch());
                append(path, "retrievedClasspath", conf.isRetrievedClasspath());
                if (conf.isRetrievedClasspath()) {
                    RetrieveSetup retrieveSetup = conf.getRetrievedClasspathSetup();
                    append(path, "retrievedClasspathPattern", retrieveSetup.getRetrievePattern());
                    append(path, "retrievedClasspathSync", retrieveSetup.isRetrieveSync());
                    append(path, "retrievedClasspathTypes", retrieveSetup.getRetrieveTypes());
                }
                append(path, "mapIfOnlyOneSource", setup.isMapIfOnlyOneSource());
                append(path, "mapIfOnlyOneJavadoc", setup.isMapIfOnlyOneJavadoc());
                append(path, "useExtendedResolveId", conf.isUseExtendedResolveId());
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
