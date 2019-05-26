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
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.apache.ivyde.eclipse.IvyNatureHelper;
import org.apache.ivyde.eclipse.cp.AdvancedSetup;
import org.apache.ivyde.eclipse.cp.ClasspathSetup;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.apache.ivyde.eclipse.cp.MappingSetup;
import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.internal.eclipse.retrieve.StandaloneRetrieveSetup;
import org.apache.ivyde.internal.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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

        if (conf.getJavaProject() != null) {
            // ensure that the project has the Ivy nature
            IvyNatureHelper.addNature(conf.getJavaProject().getProject());
        }
    }

    /**
     * Load the pre-IVYDE-70 configuration
     *
     * @param conf IvyClasspathContainerConfiguration
     * @param path
     *            the path of the container
     */
    private static void loadV0(IvyClasspathContainerConfiguration conf, IPath path) {
        // load some configuration that can be loaded
        conf.setIvyXmlPath(path.removeFirstSegments(1).removeLastSegments(1).toString());
        List<String> confs = IvyClasspathUtil.split(path.lastSegment());
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
     * @param conf IvyClasspathContainerConfiguration
     * @param path
     *            the path of the container
     */
    private static void loadV1(IvyClasspathContainerConfiguration conf, IPath path) {
        SettingsSetup settingsSetup = conf.getIvySettingsSetup();
        ClasspathSetup classpathSetup = conf.getClasspathSetup();
        MappingSetup mappingSetup = conf.getMappingSetup();
        AdvancedSetup advancedSetup = conf.getAdvancedSetup();

        conf.setAdvancedProjectSpecific(false);
        conf.setSettingsProjectSpecific(false);

        String ivyXmlPath = "ivy.xml";
        boolean doStandaloneRetrieve = false;
        boolean isRetrieveProjectSpecific = false;
        RetrieveSetup standaloneRetrieveSetup = new RetrieveSetup();

        for (String keyValue : path.segment(1).substring(1).split("&")) {
            String[] parameter = keyValue.split("=");
            if (parameter == null || parameter.length == 0) {
                continue;
            }
            String value;
            try {
                value = parameter.length > 1 ? URLDecoder.decode(parameter[1], "UTF-8") : "";
            } catch (UnsupportedEncodingException e) {
                // this should never never happen
                IvyPlugin.logError(UTF8_ERROR, e);
                throw new RuntimeException(UTF8_ERROR, e);
            }
            switch (parameter[0]) {
                case "project":
                    if (conf.getJavaProject() == null && value.trim().length() != 0) {
                        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                        IProject project = root.getProject(value.trim());
                        IJavaProject javaProject = JavaCore.create(project);
                        conf.setProject(javaProject);
                    }
                    break;
                case "ivyXmlPath":
                    ivyXmlPath = value;
                    conf.setIvyXmlPath(value);
                    break;
                case "confs":
                    List<String> confs = IvyClasspathUtil.split(value);
                    if (confs.isEmpty()) {
                        confs = Collections.singletonList("*");
                    }
                    conf.setConfs(confs);
                    break;
                case "ivySettingsPath":
                    settingsSetup.setIvySettingsPath(readOldSettings(conf, value));
                    conf.setSettingsProjectSpecific(true);
                    break;
                case "loadSettingsOnDemand":
                    settingsSetup.setLoadSettingsOnDemand(Boolean.valueOf(value));
                    conf.setSettingsProjectSpecific(true);
                    break;
                case "ivyUserDir":
                    settingsSetup.setIvyUserDir(value);
                    conf.setSettingsProjectSpecific(true);
                    break;
                case "propertyFiles":
                    settingsSetup.setPropertyFiles(IvyClasspathUtil.split(value));
                    conf.setSettingsProjectSpecific(true);
                    break;
                case "acceptedTypes":
                    classpathSetup.setAcceptedTypes(IvyClasspathUtil.split(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "sourceTypes":
                    mappingSetup.setSourceTypes(IvyClasspathUtil.split(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                case "javadocTypes":
                    mappingSetup.setJavadocTypes(IvyClasspathUtil.split(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                case "sourceSuffixes":
                    mappingSetup.setSourceSuffixes(IvyClasspathUtil.split(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                case "javadocSuffixes":
                    mappingSetup.setJavadocSuffixes(IvyClasspathUtil.split(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                case "alphaOrder":
                    // if the value is not actually "true" or "false", the Boolean class ensure to
                    // return false, so it is fine
                    classpathSetup.setAlphaOrder(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "resolveInWorkspace":
                    classpathSetup.setResolveInWorkspace(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "transitiveResolve":
                    classpathSetup.setTransitiveResolve(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "readOSGiMetadata":
                    classpathSetup.setReadOSGiMetadata(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "resolveBeforeLaunch":
                    advancedSetup.setResolveBeforeLaunch(Boolean.valueOf(value));
                    conf.setAdvancedProjectSpecific(true);
                    break;
                case "retrievedClasspath":
                    classpathSetup.setRetrievedClasspath(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "retrievedClasspathPattern":
                    classpathSetup.getRetrieveSetup().setRetrievePattern(value);
                    conf.setClassthProjectSpecific(true);
                    break;
                case "retrievedClasspathSync":
                    classpathSetup.getRetrieveSetup().setRetrieveSync(Boolean.valueOf(value));
                    conf.setClassthProjectSpecific(true);
                    break;
                case "retrievedClasspathTypes":
                    classpathSetup.getRetrieveSetup().setRetrieveTypes(value);
                    conf.setClassthProjectSpecific(true);
                    break;
                case "mapIfOnlyOneSource":
                    mappingSetup.setMapIfOnlyOneSource(Boolean.valueOf(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                case "mapIfOnlyOneJavadoc":
                    mappingSetup.setMapIfOnlyOneJavadoc(Boolean.valueOf(value));
                    conf.setMappingProjectSpecific(true);
                    break;
                // the following is the retrieve conf pre -IVYDE-56
                // from this conf should be build StandaloneRetrieveSetup
                case "doRetrieve":
                    // if the value is not actually "true" or "false", the Boolean class ensure to
                    // return false, so it is fine
                    doStandaloneRetrieve = Boolean.valueOf(value);
                    isRetrieveProjectSpecific = true;
                    break;
                case "retrievePattern":
                    standaloneRetrieveSetup.setRetrievePattern(value);
                    isRetrieveProjectSpecific = true;
                    break;
                case "retrieveSync":
                    standaloneRetrieveSetup.setRetrieveSync(Boolean.valueOf(value));
                    isRetrieveProjectSpecific = true;
                    break;
                case "retrieveConfs":
                    standaloneRetrieveSetup.setRetrieveConfs(value);
                    isRetrieveProjectSpecific = true;
                    break;
                case "retrieveTypes":
                    standaloneRetrieveSetup.setRetrieveTypes(value);
                    isRetrieveProjectSpecific = true;
                    break;
                case "useExtendedResolveId":
                    advancedSetup.setUseExtendedResolveId(Boolean.valueOf(value));
                    conf.setAdvancedProjectSpecific(true);
                    break;
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
            RetrieveSetup retrieveSetup, SettingsSetup settingsSetup, String ivyXmlPath) {
        if (conf.getJavaProject() == null) {
            // no project means no retrieve possible
            return;
        }

        StandaloneRetrieveSetup setup = new StandaloneRetrieveSetup();
        setup.setName("dependencies");
        setup.setSettingsSetup(settingsSetup);
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
        List<StandaloneRetrieveSetup> retrieveSetups;
        try {
            retrieveSetups = manager.getSetup(project);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            IvyPlugin.logError(e.getMessage(), e);
            return;
        }
        retrieveSetups.add(setup);
        try {
            manager.save(project, retrieveSetups);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            IvyPlugin.logError(e.getMessage(), e);
        }
    }

    /**
     * Read old configuration that were based on relative URLs, like: "file://./ivysettings.xml" or
     * "file:./ivysettings.xml", and also URL like "project:///ivysettings.xml".
     * <p>
     * It will be replaced by the Eclipse variable ${workspace_loc: ... }
     * </p>
     *
     * @param conf IvyClasspathContainerConfiguration
     * @param value
     *            the value to read
     * @return
     *            Eclipse variable
     */
    private static String readOldSettings(IvyClasspathContainerConfiguration conf, String value) {
        if (conf.getJavaProject() == null) {
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
        ClasspathSetup classpathSetup = conf.getClasspathSetup();
        ClasspathSetup prefStoreClasspathSetup = IvyPlugin.getPreferenceStoreHelper()
                .getClasspathSetup();
        MappingSetup mappingSetup = conf.getMappingSetup();
        SettingsSetup settingsSetup = conf.getIvySettingsSetup();
        MappingSetup prefStoreMappingSetup = IvyPlugin.getPreferenceStoreHelper().getMappingSetup();
        if (settingsSetup.getRawIvyUserDir() == null) {
            settingsSetup.setIvyUserDir(IvyPlugin.getPreferenceStoreHelper().getSettingsSetup()
                    .getRawIvyUserDir());
        }
        if (settingsSetup.getRawPropertyFiles() == null) {
            settingsSetup.setPropertyFiles(IvyPlugin.getPreferenceStoreHelper().getSettingsSetup()
                    .getRawPropertyFiles());
        }
        if (classpathSetup.getAcceptedTypes() == null) {
            classpathSetup.setAcceptedTypes(prefStoreClasspathSetup.getAcceptedTypes());
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
        IJavaProject javaProject = conf.getJavaProject();
        try {
            /*
             * Implementation note about why the project is serialized in the path. This is related
             * to https://issues.apache.org/jira/browse/IVYDE-237
             *
             * For some reason, when we add a project to the source path of a launch configuration,
             * any IvyDE container involved of that project lose its reference to its project. Then
             * when the JDT call the IvyDERuntimeClasspathEntryResolver to resolve the source of
             * that container, the IRuntimeClasspathEntry doesn't reference a Java project. In most
             * case, an IvyDE classpath container reference an ivy.xml relatively to the project. So
             * in that context, the classpath cannot be resolved without a reference to the project
             * in the path of the container.
             *
             * Another reason for having the project in the path of the container, is to make the
             * path unique. Again the source path in a launch configuration would consider two
             * containers with exactly the configurations the same, even if the
             * IRuntimeClasspathEntry reference different projects.
             *
             * To reproduce the issue, some test project is available and configured accordingly.
             * See in the test folder of the IvyDE project, check out the project 'jetty' and
             * 'jetty-webapp'.
             */
            path.append("project=");
            if (javaProject != null) {
                path.append(URLEncoder.encode(javaProject.getElementName(), "UTF-8"));
            }
            path.append("&ivyXmlPath=");
            path.append(URLEncoder.encode(conf.getIvyXmlPath(), "UTF-8"));
            append(path, "confs", conf.getConfs());
            if (conf.isSettingsProjectSpecific()) {
                SettingsSetup setup = conf.getIvySettingsSetup();
                append(path, "ivySettingsPath", setup.getRawIvySettingsPath());
                append(path, "loadSettingsOnDemand", setup.isLoadSettingsOnDemand());
                append(path, "ivyUserDir", setup.getRawIvyUserDir());
                append(path, "propertyFiles", setup.getRawPropertyFiles());
            }
            if (conf.isClassthProjectSpecific()) {
                ClasspathSetup setup = conf.getClasspathSetup();
                append(path, "acceptedTypes", setup.getAcceptedTypes());
                append(path, "alphaOrder", setup.isAlphaOrder());
                append(path, "resolveInWorkspace", setup.isResolveInWorkspace());
                append(path, "transitiveResolve", setup.isTransitiveResolve());
                append(path, "readOSGiMetadata", setup.isReadOSGiMetadata());
                append(path, "retrievedClasspath", setup.isRetrievedClasspath());
                if (setup.isRetrievedClasspath()) {
                    RetrieveSetup retrieveSetup = setup.getRetrieveSetup();
                    append(path, "retrievedClasspathPattern", retrieveSetup.getRetrievePattern());
                    append(path, "retrievedClasspathSync", retrieveSetup.isRetrieveSync());
                    append(path, "retrievedClasspathTypes", retrieveSetup.getRetrieveTypes());
                }
            }
            if (conf.isMappingProjectSpecific()) {
                MappingSetup setup = conf.getMappingSetup();
                append(path, "sourceTypes", setup.getSourceTypes());
                append(path, "javadocTypes", setup.getJavadocTypes());
                append(path, "sourceSuffixes", setup.getSourceSuffixes());
                append(path, "javadocSuffixes", setup.getJavadocSuffixes());
                append(path, "mapIfOnlyOneSource", setup.isMapIfOnlyOneSource());
                append(path, "mapIfOnlyOneJavadoc", setup.isMapIfOnlyOneJavadoc());
            }
            if (conf.isAdvancedProjectSpecific()) {
                AdvancedSetup setup = conf.getAdvancedSetup();
                append(path, "resolveBeforeLaunch", setup.isResolveBeforeLaunch());
                append(path, "useExtendedResolveId", setup.isUseExtendedResolveId());
            }
        } catch (UnsupportedEncodingException e) {
            IvyPlugin.logError(UTF8_ERROR, e);
            throw new RuntimeException(UTF8_ERROR, e);
        }
        return new Path(IvyClasspathContainer.ID).append(path.toString());
    }

    private static void append(StringBuffer path, String name, String value)
            throws UnsupportedEncodingException {
        path.append('&');
        path.append(name);
        path.append('=');
        path.append(URLEncoder.encode(value, "UTF-8"));
    }

    private static void append(StringBuffer path, String name, List<String> values)
            throws UnsupportedEncodingException {
        append(path, name, IvyClasspathUtil.concat(values));
    }

    private static void append(StringBuffer path, String name, boolean value)
            throws UnsupportedEncodingException {
        append(path, name, Boolean.toString(value));
    }
}
