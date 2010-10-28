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
package org.apache.ivyde.eclipse.ui.preferences;

import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.ContainerMappingSetup;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.IvySettingsSetup;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.apache.ivyde.eclipse.ui.editors.xml.IXMLColorConstants;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_ORGANISATION = "";

    public static final String DEFAULT_ORGANISATION_URL = "";

    public static final String DEFAULT_IVYSETTINGS_PATH = "";

    public static final String DEFAULT_PROPERTY_FILES = "";

    public static final boolean DEFAULT_LOAD_SETTINGS_ON_DEMAND = false;

    public static final IvySettingsSetup DEFAULT_IVY_SETTINGS_SETUP = new IvySettingsSetup();

    static {
        DEFAULT_IVY_SETTINGS_SETUP.setIvySettingsPath(DEFAULT_IVYSETTINGS_PATH);
        DEFAULT_IVY_SETTINGS_SETUP.setLoadSettingsOnDemand(DEFAULT_LOAD_SETTINGS_ON_DEMAND);
        DEFAULT_IVY_SETTINGS_SETUP.setPropertyFiles(IvyClasspathUtil.split(DEFAULT_PROPERTY_FILES));
    }

    public static final String DEFAULT_ACCEPTED_TYPES = "jar,bundle,ejb,maven-plugin";

    public static final String DEFAULT_SOURCES_TYPES = "source";

    public static final String DEFAULT_JAVADOC_TYPES = "javadoc";

    public static final String DEFAULT_SOURCES_SUFFIXES = "-source,-sources,-src";

    public static final String DEFAULT_JAVADOC_SUFFIXES = "-javadoc,-javadocs,-doc,-docs";

    public static final boolean DEFAULT_MAP_IF_ONLY_ONE_SOURCE = false;

    public static final boolean DEFAULT_MAP_IF_ONLY_ONE_JAVADOC = false;
    
    public static final int DEFAULT_IVY_CONSOLE_LOG_MESSAGE = Message.MSG_INFO;

    public static final ContainerMappingSetup DEFAULT_CONTAINER_MAPPING_SETUP =
        new ContainerMappingSetup();

    static {
        DEFAULT_CONTAINER_MAPPING_SETUP.setAcceptedTypes(IvyClasspathUtil
                .split(DEFAULT_ACCEPTED_TYPES));
        DEFAULT_CONTAINER_MAPPING_SETUP.setSourceTypes(IvyClasspathUtil
                .split(DEFAULT_SOURCES_TYPES));
        DEFAULT_CONTAINER_MAPPING_SETUP.setJavadocTypes(IvyClasspathUtil
                .split(DEFAULT_JAVADOC_TYPES));
        DEFAULT_CONTAINER_MAPPING_SETUP.setSourceSuffixes(IvyClasspathUtil
                .split(DEFAULT_SOURCES_SUFFIXES));
        DEFAULT_CONTAINER_MAPPING_SETUP.setJavadocSuffixes(IvyClasspathUtil
                .split(DEFAULT_JAVADOC_SUFFIXES));
        DEFAULT_CONTAINER_MAPPING_SETUP.setMapIfOnlyOneSource(DEFAULT_MAP_IF_ONLY_ONE_SOURCE);
        DEFAULT_CONTAINER_MAPPING_SETUP.setMapIfOnlyOneJavadoc(DEFAULT_MAP_IF_ONLY_ONE_JAVADOC);
    }

    public static final boolean DEFAULT_ALPHABETICAL_ORDER = false;

    public static final boolean DEFAULT_RESOLVE_IN_WORKSPACE = false;

    public static final boolean DEFAULT_RESOLVE_BEFORE_LAUNCH = false;

    public static final int DEFAULT_RESOLVE_ON_STARTUP = 1;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CLOSE = true;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_OPEN = false;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CHANGE = true;

    public static final boolean DEFAULT_IGNORE_BRANCH_ON_WORKSPACE_PROJECTS = false;

    public static final boolean DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS = false;

    public static final boolean DEFAULT_RETRIEVED_CLASSPATH = false;

    private static final String DEFAULT_RETRIEVED_CLASSPATH_PATTERN =
        "lib/[artifact]-[revision].[ext]";

    private static final boolean DEFAULT_RETRIEVED_CLASSPATH_SYNC = false;

    private static final String DEFAULT_RETRIEVED_CLASSPATH_TYPES = "jar";

    public static final RetrieveSetup DEFAULT_RETRIEVED_CLASSPATH_SETUP = new RetrieveSetup();

    static {
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrievePattern(DEFAULT_RETRIEVED_CLASSPATH_PATTERN);
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrieveSync(DEFAULT_RETRIEVED_CLASSPATH_SYNC);
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrieveTypes(DEFAULT_RETRIEVED_CLASSPATH_TYPES);
    }

    public static final boolean DEFAULT_OFFLINE = false;

    public void initializeDefaultPreferences() {
        IPreferenceStore store = IvyPlugin.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.P_BOOLEAN, true);
        store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
        store.setDefault(PreferenceConstants.P_STRING, "Default value");

        store.setDefault(PreferenceConstants.IVYSETTINGS_PATH, DEFAULT_IVYSETTINGS_PATH);
        store.setDefault(PreferenceConstants.ORGANISATION, DEFAULT_ORGANISATION);
        store.setDefault(PreferenceConstants.ORGANISATION_URL, DEFAULT_ORGANISATION_URL);
        store.setDefault(PreferenceConstants.ACCEPTED_TYPES, DEFAULT_ACCEPTED_TYPES);
        store.setDefault(PreferenceConstants.SOURCES_TYPES, DEFAULT_SOURCES_TYPES);
        store.setDefault(PreferenceConstants.JAVADOC_TYPES, DEFAULT_JAVADOC_TYPES);
        store.setDefault(PreferenceConstants.SOURCES_SUFFIXES, DEFAULT_SOURCES_SUFFIXES);
        store.setDefault(PreferenceConstants.JAVADOC_SUFFIXES, DEFAULT_JAVADOC_SUFFIXES);
        store.setDefault(PreferenceConstants.MAP_IF_ONLY_ONE_SOURCE,
                DEFAULT_MAP_IF_ONLY_ONE_SOURCE);
        store.setDefault(PreferenceConstants.MAP_IF_ONLY_ONE_JAVADOC,
                DEFAULT_MAP_IF_ONLY_ONE_JAVADOC);

        store.setDefault(PreferenceConstants.ALPHABETICAL_ORDER, DEFAULT_ALPHABETICAL_ORDER);
        store.setDefault(PreferenceConstants.RESOLVE_IN_WORKSPACE, DEFAULT_RESOLVE_IN_WORKSPACE);
        store.setDefault(PreferenceConstants.RESOLVE_BEFORE_LAUNCH, DEFAULT_RESOLVE_BEFORE_LAUNCH);
        store.setDefault(PreferenceConstants.PROPERTY_FILES, DEFAULT_PROPERTY_FILES);
        store.setDefault(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND,
            DEFAULT_LOAD_SETTINGS_ON_DEMAND);
        store.setDefault(PreferenceConstants.RESOLVE_ON_STARTUP, DEFAULT_RESOLVE_ON_STARTUP);

        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_CLOSE, DEFAULT_AUTO_RESOLVE_ON_CLOSE);
        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_OPEN, DEFAULT_AUTO_RESOLVE_ON_OPEN);
        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_CHANGE,
            DEFAULT_AUTO_RESOLVE_ON_CHANGE);

        store.setDefault(PreferenceConstants.IVY_CONSOLE_LOG_LEVEL,
            DEFAULT_IVY_CONSOLE_LOG_MESSAGE);

        store.setDefault(PreferenceConstants.IGNORE_BRANCH_ON_WORKSPACE_PROJECTS,
            DEFAULT_IGNORE_BRANCH_ON_WORKSPACE_PROJECTS);

        store.setDefault(PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS,
            DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS);

        PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_COLOR_XML_COMMENT,
            IXMLColorConstants.XML_COMMENT);
        PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_COLOR_PROC_INSTR,
            IXMLColorConstants.PROC_INSTR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_COLOR_STRING,
            IXMLColorConstants.STRING);
        PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_COLOR_DEFAULT,
            IXMLColorConstants.DEFAULT);
        PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_COLOR_TAG,
            IXMLColorConstants.TAG);

        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH, DEFAULT_RETRIEVED_CLASSPATH);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_PATTERN,
            DEFAULT_RETRIEVED_CLASSPATH_PATTERN);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_SYNC,
            DEFAULT_RETRIEVED_CLASSPATH_SYNC);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_TYPES,
            DEFAULT_RETRIEVED_CLASSPATH_TYPES);

        store.setDefault(PreferenceConstants.OFFLINE, DEFAULT_OFFLINE);
    }

}
