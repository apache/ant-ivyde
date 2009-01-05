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

import java.util.Collection;
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.jface.preference.IPreferenceStore;

public class IvyDEPreferenceStoreHelper {

    public static final String DEFAULT_IVYSETTINGS_PATH = "";

    public static final String DEFAULT_ORGANISATION = "";

    public static final String DEFAULT_ORGANISATION_URL = "";

    public static final String DEFAULT_ACCEPTED_TYPES = "jar,bundle,ejb,maven-plugin";

    public static final String DEFAULT_SOURCES_TYPES = "source";

    public static final String DEFAULT_JAVADOC_TYPES = "javadoc";

    public static final String DEFAULT_SOURCES_SUFFIXES = "-source,-sources,-src";

    public static final String DEFAULT_JAVADOC_SUFFIXES = "-javadoc,-javadocs,-doc,-docs";

    public static final boolean DEFAULT_DO_RETRIEVE = false;

    public static final String DEFAULT_RETRIEVE_PATTERN = "lib/[conf]/[artifact].[ext]";

    public static final boolean DEFAULT_RETRIEVE_SYNC = false;

    public static final String DEFAULT_RETRIEVE_CONFS = "*";

    public static final String DEFAULT_RETRIEVE_TYPES = "*";

    public static final boolean DEFAULT_ALPHABETICAL_ORDER = false;

    public static final boolean DEFAULT_RESOLVE_IN_WORKSPACE = false;

    public static final String DEFAULT_PROPERTY_FILES = "";

    public static final boolean DEFAULT_LOAD_SETTINGS_ON_DEMAND = false;

    private final IPreferenceStore prefStore;

    public IvyDEPreferenceStoreHelper(IPreferenceStore prefStore) {
        this.prefStore = prefStore;
        setDefault();
    }

    public void setDefault() {
        prefStore.setDefault(PreferenceConstants.IVYSETTINGS_PATH, DEFAULT_IVYSETTINGS_PATH);
        prefStore.setDefault(PreferenceConstants.ORGANISATION, DEFAULT_ORGANISATION);
        prefStore.setDefault(PreferenceConstants.ORGANISATION_URL, DEFAULT_ORGANISATION_URL);
        prefStore.setDefault(PreferenceConstants.ACCEPTED_TYPES, DEFAULT_ACCEPTED_TYPES);
        prefStore.setDefault(PreferenceConstants.SOURCES_TYPES, DEFAULT_SOURCES_TYPES);
        prefStore.setDefault(PreferenceConstants.JAVADOC_TYPES, DEFAULT_JAVADOC_TYPES);
        prefStore.setDefault(PreferenceConstants.SOURCES_SUFFIXES, DEFAULT_SOURCES_SUFFIXES);
        prefStore.setDefault(PreferenceConstants.JAVADOC_SUFFIXES, DEFAULT_JAVADOC_SUFFIXES);

        prefStore.setDefault(PreferenceConstants.DO_RETRIEVE, DEFAULT_DO_RETRIEVE);
        boolean b = prefStore.getBoolean(PreferenceConstants.DO_RETRIEVE_DEPRECATED);
        if (b) {
            // not the default value, so it has been set
            // erase the deprecated preference and store the new one
            prefStore.setValue(PreferenceConstants.DO_RETRIEVE_DEPRECATED, "");
            prefStore.setValue(PreferenceConstants.DO_RETRIEVE, b);
        }

        prefStore.setDefault(PreferenceConstants.RETRIEVE_PATTERN, DEFAULT_RETRIEVE_PATTERN);
        prefStore.setDefault(PreferenceConstants.RETRIEVE_CONFS, DEFAULT_RETRIEVE_CONFS);
        prefStore.setDefault(PreferenceConstants.RETRIEVE_TYPES, DEFAULT_RETRIEVE_TYPES);
        String s = prefStore.getString(PreferenceConstants.RETRIEVE_PATTERN_DEPRECATED);
        if (s != null && s.length() != 0) {
            // not the default value, so it has been set
            // erase the deprecated preference and store the new one
            prefStore.setValue(PreferenceConstants.RETRIEVE_PATTERN_DEPRECATED, "");
            prefStore.setValue(PreferenceConstants.RETRIEVE_PATTERN, s);
        }

        prefStore.setDefault(PreferenceConstants.RETRIEVE_SYNC, DEFAULT_RETRIEVE_SYNC);
        prefStore.setDefault(PreferenceConstants.ALPHABETICAL_ORDER, DEFAULT_ALPHABETICAL_ORDER);
        prefStore
                .setDefault(PreferenceConstants.RESOLVE_IN_WORKSPACE, DEFAULT_RESOLVE_IN_WORKSPACE);
        prefStore.setDefault(PreferenceConstants.PROPERTY_FILES, DEFAULT_PROPERTY_FILES);
        prefStore.setDefault(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND,
            DEFAULT_LOAD_SETTINGS_ON_DEMAND);
    }

    public String getIvyOrg() {
        return prefStore.getString(PreferenceConstants.ORGANISATION);
    }

    public void setIvyOrg(String org) {
        prefStore.setValue(PreferenceConstants.ORGANISATION, org);
    }

    public String getIvyOrgUrl() {
        return prefStore.getString(PreferenceConstants.ORGANISATION_URL);
    }

    public void setIvyOrgUrl(String url) {
        prefStore.setValue(PreferenceConstants.ORGANISATION_URL, url);
    }

    public String getIvySettingsPath() {
        return prefStore.getString(PreferenceConstants.IVYSETTINGS_PATH);
    }

    public void setIvySettingsPath(String path) {
        prefStore.setValue(PreferenceConstants.IVYSETTINGS_PATH, path);
    }

    public List getAcceptedTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.ACCEPTED_TYPES));
    }

    public void setAcceptedTypes(Collection acceptedTypes) {
        prefStore.setValue(PreferenceConstants.ACCEPTED_TYPES, IvyClasspathUtil
                .concat(acceptedTypes));
    }

    public List getSourceTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.SOURCES_TYPES));
    }

    public void setSourceTypes(Collection sourceTypes) {
        prefStore.setValue(PreferenceConstants.SOURCES_TYPES, IvyClasspathUtil.concat(sourceTypes));
    }

    public List getJavadocTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.JAVADOC_TYPES));
    }

    public void setJavadocTypes(Collection javadocTypes) {
        prefStore
                .setValue(PreferenceConstants.JAVADOC_TYPES, IvyClasspathUtil.concat(javadocTypes));
    }

    public List getSourceSuffixes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.SOURCES_SUFFIXES));
    }

    public void setSourceSuffixes(Collection sourceSuffixes) {
        prefStore.setValue(PreferenceConstants.SOURCES_SUFFIXES, IvyClasspathUtil
                .concat(sourceSuffixes));
    }

    public List getJavadocSuffixes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.JAVADOC_SUFFIXES));
    }

    public void setJavadocSuffixes(Collection javadocSuffixes) {
        prefStore.setValue(PreferenceConstants.JAVADOC_SUFFIXES, IvyClasspathUtil
                .concat(javadocSuffixes));
    }

    public boolean getDoRetrieve() {
        return prefStore.getBoolean(PreferenceConstants.DO_RETRIEVE);
    }

    public void setDoRetrieve(boolean doretrieve) {
        prefStore.setValue(PreferenceConstants.DO_RETRIEVE, doretrieve);
    }

    public String getRetrievePattern() {
        return prefStore.getString(PreferenceConstants.RETRIEVE_PATTERN);
    }

    public void setRetrievePattern(String pattern) {
        prefStore.setValue(PreferenceConstants.RETRIEVE_PATTERN, pattern);
    }

    public boolean getRetrieveSync() {
        return prefStore.getBoolean(PreferenceConstants.RETRIEVE_SYNC);
    }

    public void setRetrieveSync(boolean sync) {
        prefStore.setValue(PreferenceConstants.RETRIEVE_SYNC, sync);
    }

    public String getRetrieveConfs() {
        return prefStore.getString(PreferenceConstants.RETRIEVE_CONFS);
    }

    public void setRetrieveConfs(String confs) {
        prefStore.setValue(PreferenceConstants.RETRIEVE_CONFS, confs);
    }

    public String getRetrieveTypes() {
        return prefStore.getString(PreferenceConstants.RETRIEVE_TYPES);
    }

    public void setRetrieveTypes(String types) {
        prefStore.setValue(PreferenceConstants.RETRIEVE_TYPES, types);
    }

    public boolean isAlphOrder() {
        return prefStore.getBoolean(PreferenceConstants.ALPHABETICAL_ORDER);
    }

    public void setAlphOrder(boolean alpha) {
        prefStore.setValue(PreferenceConstants.ALPHABETICAL_ORDER, alpha);
    }

    public boolean isResolveInWorkspace() {
        return prefStore.getBoolean(PreferenceConstants.RESOLVE_IN_WORKSPACE);
    }

    public void setResolveInWorkspace(boolean inWorkspace) {
        prefStore.setValue(PreferenceConstants.RESOLVE_IN_WORKSPACE, inWorkspace);
    }

    public String getOrganization() {
        return prefStore.getString(PreferenceConstants.ORGANISATION);
    }

    public void setOrganization(String org) {
        prefStore.setValue(PreferenceConstants.ORGANISATION, org);
    }

    public String getOrganizationUrl() {
        return prefStore.getString(PreferenceConstants.ORGANISATION_URL);
    }

    public void setOrganizationUrl(String url) {
        prefStore.setValue(PreferenceConstants.ORGANISATION_URL, url);
    }

    public List getPropertyFiles() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.PROPERTY_FILES));
    }

    public void setPropertyFiles(List files) {
        prefStore.setValue(PreferenceConstants.PROPERTY_FILES, IvyClasspathUtil.concat(files));
    }

    public boolean isLoadSettingsOnDemand() {
        return prefStore.getBoolean(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND);
    }

    public void setLoadSettingsOnDemand(boolean onDemand) {
        prefStore.setValue(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND, onDemand);
    }

}
