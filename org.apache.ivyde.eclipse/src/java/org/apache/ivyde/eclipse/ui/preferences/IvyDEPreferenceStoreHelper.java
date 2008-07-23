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

import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.jface.preference.IPreferenceStore;

public class IvyDEPreferenceStoreHelper {

    private final IPreferenceStore prefStore;

    public IvyDEPreferenceStoreHelper(IPreferenceStore prefStore) {
        this.prefStore = prefStore;
        setDefault();
    }

    public void setDefault() {
        prefStore.setDefault(PreferenceConstants.IVYSETTINGS_PATH, "");
        prefStore.setDefault(PreferenceConstants.ORGANISATION, "");
        prefStore.setDefault(PreferenceConstants.ORGANISATION_URL, "");
        prefStore.setDefault(PreferenceConstants.ACCEPTED_TYPES, "jar");
        prefStore.setDefault(PreferenceConstants.SOURCES_TYPES, "source");
        prefStore.setDefault(PreferenceConstants.JAVADOC_TYPES, "javadoc");
        prefStore.setDefault(PreferenceConstants.SOURCES_SUFFIXES, "-source,-sources,-src");
        prefStore.setDefault(PreferenceConstants.JAVADOC_SUFFIXES, "-javadoc,-javadocs,-doc,-docs");
        prefStore.setDefault(PreferenceConstants.DO_RETRIEVE, false);
        prefStore.setDefault(PreferenceConstants.RETRIEVE_PATTERN, "lib/[conf]/[artifact].[ext]");
        prefStore.setDefault(PreferenceConstants.ALPHABETICAL_ORDER, false);
    }

    public String getIvyOrg() {
        return prefStore.getString(PreferenceConstants.ORGANISATION);
    }

    public String getIvyOrgUrl() {
        return prefStore.getString(PreferenceConstants.ORGANISATION_URL);
    }

    public String getIvySettingsPath() {
        return prefStore.getString(PreferenceConstants.IVYSETTINGS_PATH);
    }

    public List getAcceptedTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.ACCEPTED_TYPES));
    }

    public List getSourceTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.SOURCES_TYPES));
    }

    public List getJavadocTypes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.JAVADOC_TYPES));
    }

    public List getSourceSuffixes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.SOURCES_SUFFIXES));
    }

    public List getJavadocSuffixes() {
        return IvyClasspathUtil.split(prefStore.getString(PreferenceConstants.JAVADOC_SUFFIXES));
    }

    public boolean getDoRetrieve() {
        return prefStore.getBoolean(PreferenceConstants.DO_RETRIEVE);
    }

    public String getRetrievePattern() {
        return prefStore.getString(PreferenceConstants.RETRIEVE_PATTERN);
    }

    public boolean getRetrieveSync() {
        return prefStore.getBoolean(PreferenceConstants.RETRIEVE_SYNC);
    }

    public boolean isAlphOrder() {
        return prefStore.getBoolean(PreferenceConstants.ALPHABETICAL_ORDER);
    }

}
