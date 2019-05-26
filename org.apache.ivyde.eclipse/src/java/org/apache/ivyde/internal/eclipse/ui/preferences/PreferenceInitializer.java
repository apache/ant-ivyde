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
package org.apache.ivyde.internal.eclipse.ui.preferences;

import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cp.AdvancedSetup;
import org.apache.ivyde.eclipse.cp.ClasspathSetup;
import org.apache.ivyde.eclipse.cp.MappingSetup;
import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathInitializer;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.IXMLColorConstants;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_ORGANISATION = "";

    public static final String DEFAULT_ORGANISATION_URL = "";

    public static final String DEFAULT_IVYSETTINGS_PATH = "";

    public static final String DEFAULT_IVY_USER_DIR = "";

    public static final String DEFAULT_PROPERTY_FILES = "";

    public static final boolean DEFAULT_LOAD_SETTINGS_ON_DEMAND = false;

    public static final SettingsSetup DEFAULT_SETTINGS_SETUP = new SettingsSetup();

    static {
        DEFAULT_SETTINGS_SETUP.setIvySettingsPath(DEFAULT_IVYSETTINGS_PATH);
        DEFAULT_SETTINGS_SETUP.setLoadSettingsOnDemand(DEFAULT_LOAD_SETTINGS_ON_DEMAND);
        DEFAULT_SETTINGS_SETUP.setIvyUserDir(DEFAULT_IVY_USER_DIR);
        DEFAULT_SETTINGS_SETUP.setPropertyFiles(IvyClasspathUtil.split(DEFAULT_PROPERTY_FILES));
    }

    public static final String DEFAULT_ACCEPTED_TYPES = "jar,bundle,ejb,maven-plugin";

    public static final boolean DEFAULT_ALPHABETICAL_ORDER = false;

    public static final boolean DEFAULT_RESOLVE_IN_WORKSPACE = false;

    public static final boolean DEFAULT_TRANSITIVE_RESOLVE = true;

    private static final boolean DEFAULT_READ_OSGI_METADATA = false;

    public static final boolean DEFAULT_RETRIEVED_CLASSPATH = false;

    private static final String DEFAULT_RETRIEVED_CLASSPATH_PATTERN = "lib/[artifact]-[revision].[ext]";

    private static final boolean DEFAULT_RETRIEVED_CLASSPATH_SYNC = false;

    private static final String DEFAULT_RETRIEVED_CLASSPATH_TYPES = "jar";

    public static final RetrieveSetup DEFAULT_RETRIEVED_CLASSPATH_SETUP = new RetrieveSetup();

    static {
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrievePattern(DEFAULT_RETRIEVED_CLASSPATH_PATTERN);
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrieveSync(DEFAULT_RETRIEVED_CLASSPATH_SYNC);
        DEFAULT_RETRIEVED_CLASSPATH_SETUP.setRetrieveTypes(DEFAULT_RETRIEVED_CLASSPATH_TYPES);
    }

    public static final ClasspathSetup DEFAULT_CLASSPATH_SETUP = new ClasspathSetup();

    static {
        DEFAULT_CLASSPATH_SETUP.setAcceptedTypes(IvyClasspathUtil.split(DEFAULT_ACCEPTED_TYPES));
        DEFAULT_CLASSPATH_SETUP.setAlphaOrder(DEFAULT_ALPHABETICAL_ORDER);
        DEFAULT_CLASSPATH_SETUP.setResolveInWorkspace(DEFAULT_RESOLVE_IN_WORKSPACE);
        DEFAULT_CLASSPATH_SETUP.setTransitiveResolve(DEFAULT_TRANSITIVE_RESOLVE);
        DEFAULT_CLASSPATH_SETUP.setReadOSGiMetadata(DEFAULT_READ_OSGI_METADATA);
        DEFAULT_CLASSPATH_SETUP.setRetrievedClasspath(DEFAULT_RETRIEVED_CLASSPATH);
        DEFAULT_CLASSPATH_SETUP.setRetrieveSetup(DEFAULT_RETRIEVED_CLASSPATH_SETUP);
    }

    public static final String DEFAULT_SOURCES_TYPES = "source";

    public static final String DEFAULT_JAVADOC_TYPES = "javadoc";

    public static final String DEFAULT_SOURCES_SUFFIXES = "-source,-sources,-src";

    public static final String DEFAULT_JAVADOC_SUFFIXES = "-javadoc,-javadocs,-doc,-docs";

    public static final boolean DEFAULT_MAP_IF_ONLY_ONE_SOURCE = false;

    public static final boolean DEFAULT_MAP_IF_ONLY_ONE_JAVADOC = false;

    public static final int DEFAULT_IVY_CONSOLE_LOG_MESSAGE = Message.MSG_INFO;

    public static final int DEFAULT_IVY_CONSOLE_IVYDE_LOG_MESSAGE = Message.MSG_INFO;

    public static final boolean DEFAULT_OPEN_IVY_CONSOLE_ON_STARTUP = false;

    public static final MappingSetup DEFAULT_MAPPING_SETUP = new MappingSetup();

    static {
        DEFAULT_MAPPING_SETUP.setSourceTypes(IvyClasspathUtil.split(DEFAULT_SOURCES_TYPES));
        DEFAULT_MAPPING_SETUP.setJavadocTypes(IvyClasspathUtil.split(DEFAULT_JAVADOC_TYPES));
        DEFAULT_MAPPING_SETUP.setSourceSuffixes(IvyClasspathUtil.split(DEFAULT_SOURCES_SUFFIXES));
        DEFAULT_MAPPING_SETUP.setJavadocSuffixes(IvyClasspathUtil.split(DEFAULT_JAVADOC_SUFFIXES));
        DEFAULT_MAPPING_SETUP.setMapIfOnlyOneSource(DEFAULT_MAP_IF_ONLY_ONE_SOURCE);
        DEFAULT_MAPPING_SETUP.setMapIfOnlyOneJavadoc(DEFAULT_MAP_IF_ONLY_ONE_JAVADOC);
    }

    public static final boolean DEFAULT_RESOLVE_BEFORE_LAUNCH = false;

    public static final boolean DEFAULT_USE_EXTENDED_RESOLVE_ID = false;

    public static final AdvancedSetup DEFAULT_ADVANCED_SETUP = new AdvancedSetup();

    static {
        DEFAULT_ADVANCED_SETUP.setResolveBeforeLaunch(DEFAULT_RESOLVE_BEFORE_LAUNCH);
        DEFAULT_ADVANCED_SETUP.setUseExtendedResolveId(DEFAULT_USE_EXTENDED_RESOLVE_ID);
    }

    public static final int DEFAULT_RESOLVE_ON_STARTUP = IvyClasspathInitializer.ON_STARTUP_NOTHING;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CLOSE = true;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_OPEN = false;

    public static final boolean DEFAULT_AUTO_RESOLVE_ON_CHANGE = true;

    public static final boolean DEFAULT_IGNORE_BRANCH_ON_WORKSPACE_PROJECTS = false;

    public static final boolean DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS = false;

    public static final boolean DEFAULT_OFFLINE = false;

    public static final boolean DEFAULT_ERROR_POPUP = true;

    public void initializeDefaultPreferences() {
        IPreferenceStore store = IvyPlugin.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.P_BOOLEAN, true);
        store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
        store.setDefault(PreferenceConstants.P_STRING, "Default value");

        store.setDefault(PreferenceConstants.IVYSETTINGS_PATH, DEFAULT_IVYSETTINGS_PATH);
        store.setDefault(PreferenceConstants.IVY_USER_DIR, DEFAULT_IVY_USER_DIR);
        store.setDefault(PreferenceConstants.ORGANISATION, DEFAULT_ORGANISATION);
        store.setDefault(PreferenceConstants.ORGANISATION_URL, DEFAULT_ORGANISATION_URL);
        store.setDefault(PreferenceConstants.ACCEPTED_TYPES, DEFAULT_ACCEPTED_TYPES);
        store.setDefault(PreferenceConstants.SOURCES_TYPES, DEFAULT_SOURCES_TYPES);
        store.setDefault(PreferenceConstants.JAVADOC_TYPES, DEFAULT_JAVADOC_TYPES);
        store.setDefault(PreferenceConstants.SOURCES_SUFFIXES, DEFAULT_SOURCES_SUFFIXES);
        store.setDefault(PreferenceConstants.JAVADOC_SUFFIXES, DEFAULT_JAVADOC_SUFFIXES);
        store.setDefault(PreferenceConstants.MAP_IF_ONLY_ONE_SOURCE, DEFAULT_MAP_IF_ONLY_ONE_SOURCE);
        store.setDefault(PreferenceConstants.MAP_IF_ONLY_ONE_JAVADOC,
            DEFAULT_MAP_IF_ONLY_ONE_JAVADOC);

        store.setDefault(PreferenceConstants.ALPHABETICAL_ORDER, DEFAULT_ALPHABETICAL_ORDER);
        store.setDefault(PreferenceConstants.RESOLVE_IN_WORKSPACE, DEFAULT_RESOLVE_IN_WORKSPACE);
        store.setDefault(PreferenceConstants.TRANSITIVE_RESOLVE, DEFAULT_TRANSITIVE_RESOLVE);
        store.setDefault(PreferenceConstants.READ_OSGI_METADATA, DEFAULT_READ_OSGI_METADATA);
        store.setDefault(PreferenceConstants.RESOLVE_BEFORE_LAUNCH, DEFAULT_RESOLVE_BEFORE_LAUNCH);
        store.setDefault(PreferenceConstants.PROPERTY_FILES, DEFAULT_PROPERTY_FILES);
        store.setDefault(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND,
            DEFAULT_LOAD_SETTINGS_ON_DEMAND);
        store.setDefault(PreferenceConstants.RESOLVE_ON_STARTUP, DEFAULT_RESOLVE_ON_STARTUP);

        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_CLOSE, DEFAULT_AUTO_RESOLVE_ON_CLOSE);
        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_OPEN, DEFAULT_AUTO_RESOLVE_ON_OPEN);
        store.setDefault(PreferenceConstants.AUTO_RESOLVE_ON_CHANGE, DEFAULT_AUTO_RESOLVE_ON_CHANGE);

        store.setDefault(PreferenceConstants.IVY_CONSOLE_LOG_LEVEL, DEFAULT_IVY_CONSOLE_LOG_MESSAGE);
        store.setDefault(PreferenceConstants.IVY_CONSOLE_IVYDE_LOG_LEVEL, DEFAULT_IVY_CONSOLE_IVYDE_LOG_MESSAGE);
        store.setDefault(PreferenceConstants.OPEN_IVY_CONSOLE_ON_STARTUP, DEFAULT_OPEN_IVY_CONSOLE_ON_STARTUP);

        store.setDefault(PreferenceConstants.IGNORE_BRANCH_ON_WORKSPACE_PROJECTS,
            DEFAULT_IGNORE_BRANCH_ON_WORKSPACE_PROJECTS);

        store.setDefault(PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS,
            DEFAULT_IGNORE_VERSION_ON_WORKSPACE_PROJECTS);

        store.setDefault(PreferenceConstants.EDITOR_COLOR_XML_COMMENT,
            asString(IXMLColorConstants.XML_COMMENT));
        store.setDefault(PreferenceConstants.EDITOR_COLOR_PROC_INSTR,
            asString(IXMLColorConstants.PROC_INSTR));
        store.setDefault(PreferenceConstants.EDITOR_COLOR_STRING,
            asString(IXMLColorConstants.STRING));
        store.setDefault(PreferenceConstants.EDITOR_COLOR_DEFAULT,
            asString(IXMLColorConstants.DEFAULT));
        store.setDefault(PreferenceConstants.EDITOR_COLOR_TAG, asString(IXMLColorConstants.TAG));

        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH, DEFAULT_RETRIEVED_CLASSPATH);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_PATTERN,
            DEFAULT_RETRIEVED_CLASSPATH_PATTERN);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_SYNC,
            DEFAULT_RETRIEVED_CLASSPATH_SYNC);
        store.setDefault(PreferenceConstants.RETRIEVED_CLASSPATH_TYPES,
            DEFAULT_RETRIEVED_CLASSPATH_TYPES);

        store.setDefault(PreferenceConstants.OFFLINE, DEFAULT_OFFLINE);
        store.setDefault(PreferenceConstants.ERROR_POPUP, DEFAULT_ERROR_POPUP);
    }

    private String asString(RGB value) {
        return StringConverter.asString(value);
    }
}
