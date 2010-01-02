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
import org.apache.ivyde.eclipse.cpcontainer.RetrieveSetup;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

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
    }

    public static final boolean DEFAULT_DO_RETRIEVE = false;

    public static final String DEFAULT_RETRIEVE_PATTERN = "lib/[conf]/[artifact].[ext]";

    public static final boolean DEFAULT_RETRIEVE_SYNC = false;

    public static final String DEFAULT_RETRIEVE_CONFS = "*";

    public static final String DEFAULT_RETRIEVE_TYPES = "*";

    public static final RetrieveSetup DEFAULT_RETRIEVE_SETUP = new RetrieveSetup();

    static {
        DEFAULT_RETRIEVE_SETUP.setDoRetrieve(DEFAULT_DO_RETRIEVE);
        DEFAULT_RETRIEVE_SETUP.setRetrievePattern(DEFAULT_RETRIEVE_PATTERN);
        DEFAULT_RETRIEVE_SETUP.setRetrieveSync(DEFAULT_RETRIEVE_SYNC);
        DEFAULT_RETRIEVE_SETUP.setRetrieveConfs(DEFAULT_RETRIEVE_CONFS);
        DEFAULT_RETRIEVE_SETUP.setRetrieveTypes(DEFAULT_RETRIEVE_TYPES);
    }

    public static final boolean DEFAULT_ALPHABETICAL_ORDER = false;

    public static final boolean DEFAULT_RESOLVE_IN_WORKSPACE = false;

    public static final boolean DEFAULT_RESOLVE_BEFORE_LAUNCH = false;

    public static final int DEFAULT_RESOLVE_ON_STARTUP = 1;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CLOSE = true;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_OPEN = false;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CHANGE = true;
    
    public static final boolean DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS = false;
    
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

        store.setDefault(PreferenceConstants.DO_RETRIEVE, DEFAULT_DO_RETRIEVE);
        boolean b = store.getBoolean(PreferenceConstants.DO_RETRIEVE_DEPRECATED);
        if (b) {
            // not the default value, so it has been set
            // erase the deprecated preference and store the new one
            store.setValue(PreferenceConstants.DO_RETRIEVE_DEPRECATED, "");
            store.setValue(PreferenceConstants.DO_RETRIEVE, b);
        }

        store.setDefault(PreferenceConstants.RETRIEVE_PATTERN, DEFAULT_RETRIEVE_PATTERN);
        store.setDefault(PreferenceConstants.RETRIEVE_CONFS, DEFAULT_RETRIEVE_CONFS);
        store.setDefault(PreferenceConstants.RETRIEVE_TYPES, DEFAULT_RETRIEVE_TYPES);
        String s = store.getString(PreferenceConstants.RETRIEVE_PATTERN_DEPRECATED);
        if (s != null && s.length() != 0) {
            // not the default value, so it has been set
            // erase the deprecated preference and store the new one
            store.setValue(PreferenceConstants.RETRIEVE_PATTERN_DEPRECATED, "");
            store.setValue(PreferenceConstants.RETRIEVE_PATTERN, s);
        }

        store.setDefault(PreferenceConstants.RETRIEVE_SYNC, DEFAULT_RETRIEVE_SYNC);
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

        store.setDefault(PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS,
            DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS);
    }

}
