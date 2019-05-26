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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Constant definitions for plug-in preferences.
 */
public final class PreferenceConstants {

    private PreferenceConstants() {
        // utility class
    }

    public static final String IVYSETTINGS_PATH = "ivy_conf_path";

    public static final String ORGANISATION = "ivy_org";

    public static final String ORGANISATION_URL = "ivy_org_url";

    public static final String ACCEPTED_TYPES = "acceptedTypes";

    public static final String SOURCES_TYPES = "sourceTypes";

    public static final String SOURCES_SUFFIXES = "sourceSuffixes";

    public static final String JAVADOC_TYPES = "javadocTypes";

    public static final String JAVADOC_SUFFIXES = "javadocSuffixes";

    public static final String P_BOOLEAN = "booleanPreference";

    public static final String P_CHOICE = "choicePreference";

    public static final String P_STRING = "stringPreference";

    public static final String DO_RETRIEVE_DEPRECATED = "do.retreive";

    public static final String RETRIEVE_PATTERN_DEPRECATED = "retreive.pattern";

    public static final String DO_RETRIEVE = "do.retrieve";

    public static final String RETRIEVE_PATTERN = "retrieve.pattern";

    public static final String RETRIEVE_SYNC = "retrieve.sync";

    public static final String RETRIEVE_CONFS = "retrieve.confs";

    public static final String RETRIEVE_TYPES = "retrieve.types";

    public static final String ALPHABETICAL_ORDER = "order.alphabetical";

    public static final String RESOLVE_IN_WORKSPACE = "resolveInWorkspace";

    public static final String TRANSITIVE_RESOLVE = "transitiveResolve";

    public static final String READ_OSGI_METADATA = "readOSGIMetadata";

    public static final String RESOLVE_BEFORE_LAUNCH = "resolveBeforeLaunch";

    public static final String USE_EXTENDED_RESOLVE_ID = "useExtendedResolveId";

    public static final String PROPERTY_FILES = "propertyFiles";

    public static final String IVY_USER_DIR = "ivuUserDir";

    public static final String LOAD_SETTINGS_ON_DEMAND = "loadSettingsOnDemand";

    public static final String RESOLVE_ON_STARTUP = "resolveOnStartup";

    public static final String AUTO_RESOLVE_ON_OPEN = "autoResolve.open";

    public static final String AUTO_RESOLVE_ON_CLOSE = "autoResolve.close";

    public static final String AUTO_RESOLVE_ON_CHANGE = "autoResolve.change";

    public static final String IVY_CONSOLE_LOG_LEVEL = "ivyConsole.logLevel";

    public static final String IVY_CONSOLE_IVYDE_LOG_LEVEL = "ivyConsole.ivyDELogLevel";

    public static final String OPEN_IVY_CONSOLE_ON_STARTUP = "ivyConsole.openOnStartup";

    public static final String IGNORE_BRANCH_ON_WORKSPACE_PROJECTS
        = "workspaceResolver.ignoreBranch";

    public static final String IGNORE_VERSION_ON_WORKSPACE_PROJECTS
        = "workspaceResolver.ignoreVersion";

    public static final String EDITOR_COLOR_XML_COMMENT = "editor.color.xmlComment";

    public static final String EDITOR_COLOR_PROC_INSTR = "editor.color.procInstr";

    public static final String EDITOR_COLOR_STRING = "editor.color.string";

    public static final String EDITOR_COLOR_DEFAULT = "editor.color.default";

    public static final String EDITOR_COLOR_TAG = "editor.color.tag";

    public static final String RETRIEVED_CLASSPATH = "retrievedClasspath";

    public static final String RETRIEVED_CLASSPATH_PATTERN = "retrievedClasspath.pattern";

    public static final String RETRIEVED_CLASSPATH_SYNC = "retrievedClasspath.sync";

    public static final String RETRIEVED_CLASSPATH_TYPES = "retrievedClasspath.types";

    public static final String MAP_IF_ONLY_ONE_SOURCE = "mapIfOnlyOneSource";

    public static final String MAP_IF_ONLY_ONE_JAVADOC = "mapIfOnlyOneJavadoc";

    public static final String OFFLINE = "offline";

    public static final String ERROR_POPUP = "error.popup";

    public static final Set<String> ALL = new HashSet<>();

    static {
        for (Field field : PreferenceConstants.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                try {
                    ALL.add((String) field.get(null));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
