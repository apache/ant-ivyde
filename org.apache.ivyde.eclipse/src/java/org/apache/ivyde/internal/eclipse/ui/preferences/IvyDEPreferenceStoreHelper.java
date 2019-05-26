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

import org.apache.ivyde.eclipse.cp.AdvancedSetup;
import org.apache.ivyde.eclipse.cp.ClasspathSetup;
import org.apache.ivyde.eclipse.cp.MappingSetup;
import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class IvyDEPreferenceStoreHelper {

    private final IPreferenceStore prefStore;

    public IvyDEPreferenceStoreHelper(IPreferenceStore prefStore) {
        this.prefStore = prefStore;
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

    public SettingsSetup getSettingsSetup() {
        SettingsSetup setup = new SettingsSetup();
        setup.setIvySettingsPath(prefStore.getString(PreferenceConstants.IVYSETTINGS_PATH));
        setup.setLoadSettingsOnDemand(prefStore
                .getBoolean(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND));
        setup.setIvyUserDir(prefStore.getString(PreferenceConstants.IVY_USER_DIR));
        setup.setPropertyFiles(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.PROPERTY_FILES)));
        return setup;
    }

    public void setSettingsSetup(SettingsSetup setup) {
        prefStore.setValue(PreferenceConstants.IVYSETTINGS_PATH, setup.getRawIvySettingsPath());
        prefStore.setValue(PreferenceConstants.PROPERTY_FILES,
            IvyClasspathUtil.concat(setup.getRawPropertyFiles()));
        prefStore.setValue(PreferenceConstants.IVY_USER_DIR, setup.getRawIvyUserDir());
        prefStore.setValue(PreferenceConstants.LOAD_SETTINGS_ON_DEMAND,
            setup.isLoadSettingsOnDemand());
    }

    public ClasspathSetup getClasspathSetup() {
        ClasspathSetup setup = new ClasspathSetup();
        setup.setResolveInWorkspace(prefStore.getBoolean(PreferenceConstants.RESOLVE_IN_WORKSPACE));
        setup.setTransitiveResolve(prefStore.getBoolean(PreferenceConstants.TRANSITIVE_RESOLVE));
        setup.setReadOSGiMetadata(prefStore.getBoolean(PreferenceConstants.READ_OSGI_METADATA));
        setup.setAcceptedTypes(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.ACCEPTED_TYPES)));
        setup.setAlphaOrder(prefStore.getBoolean(PreferenceConstants.ALPHABETICAL_ORDER));
        setup.setRetrievedClasspath(prefStore.getBoolean(PreferenceConstants.RETRIEVED_CLASSPATH));
        RetrieveSetup retrieveSetup = new RetrieveSetup();
        retrieveSetup.setRetrievePattern(prefStore
                .getString(PreferenceConstants.RETRIEVED_CLASSPATH_PATTERN));
        retrieveSetup.setRetrieveSync(prefStore
                .getBoolean(PreferenceConstants.RETRIEVED_CLASSPATH_SYNC));
        retrieveSetup.setRetrieveTypes(prefStore
                .getString(PreferenceConstants.RETRIEVED_CLASSPATH_TYPES));
        setup.setRetrieveSetup(retrieveSetup);
        return setup;
    }

    public void setClasspathSetup(ClasspathSetup setup) {
        prefStore.setValue(PreferenceConstants.RESOLVE_IN_WORKSPACE, setup.isResolveInWorkspace());
        prefStore.setValue(PreferenceConstants.TRANSITIVE_RESOLVE, setup.isTransitiveResolve());
        prefStore.setValue(PreferenceConstants.READ_OSGI_METADATA, setup.isReadOSGiMetadata());
        prefStore.setValue(PreferenceConstants.ACCEPTED_TYPES,
            IvyClasspathUtil.concat(setup.getAcceptedTypes()));
        prefStore.setValue(PreferenceConstants.ALPHABETICAL_ORDER, setup.isAlphaOrder());
        prefStore.setValue(PreferenceConstants.RETRIEVED_CLASSPATH, setup.isRetrievedClasspath());
        RetrieveSetup retrieveSetup = setup.getRetrieveSetup();
        prefStore.setValue(PreferenceConstants.RETRIEVED_CLASSPATH_PATTERN,
            retrieveSetup.getRetrievePattern());
        prefStore.setValue(PreferenceConstants.RETRIEVED_CLASSPATH_SYNC,
            retrieveSetup.isRetrieveSync());
        prefStore.setValue(PreferenceConstants.RETRIEVED_CLASSPATH_TYPES,
            retrieveSetup.getRetrieveTypes());
    }

    public MappingSetup getMappingSetup() {
        MappingSetup setup = new MappingSetup();
        setup.setSourceTypes(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.SOURCES_TYPES)));
        setup.setJavadocTypes(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.JAVADOC_TYPES)));
        setup.setSourceSuffixes(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.SOURCES_SUFFIXES)));
        setup.setJavadocSuffixes(IvyClasspathUtil.split(prefStore
                .getString(PreferenceConstants.JAVADOC_SUFFIXES)));
        setup.setMapIfOnlyOneSource(prefStore
                .getBoolean(PreferenceConstants.MAP_IF_ONLY_ONE_SOURCE));
        setup.setMapIfOnlyOneJavadoc(prefStore
                .getBoolean(PreferenceConstants.MAP_IF_ONLY_ONE_JAVADOC));
        return setup;
    }

    public void setMappingSetup(MappingSetup setup) {
        prefStore.setValue(PreferenceConstants.SOURCES_TYPES,
            IvyClasspathUtil.concat(setup.getSourceTypes()));
        prefStore.setValue(PreferenceConstants.JAVADOC_TYPES,
            IvyClasspathUtil.concat(setup.getJavadocTypes()));
        prefStore.setValue(PreferenceConstants.SOURCES_SUFFIXES,
            IvyClasspathUtil.concat(setup.getSourceSuffixes()));
        prefStore.setValue(PreferenceConstants.JAVADOC_SUFFIXES,
            IvyClasspathUtil.concat(setup.getJavadocSuffixes()));
        prefStore
                .setValue(PreferenceConstants.MAP_IF_ONLY_ONE_SOURCE, setup.isMapIfOnlyOneSource());
        prefStore.setValue(PreferenceConstants.MAP_IF_ONLY_ONE_JAVADOC,
            setup.isMapIfOnlyOneJavadoc());
    }

    public AdvancedSetup getAdvancedSetup() {
        AdvancedSetup setup = new AdvancedSetup();
        setup.setResolveBeforeLaunch(prefStore
                .getBoolean(PreferenceConstants.RESOLVE_BEFORE_LAUNCH));
        setup.setUseExtendedResolveId(prefStore
                .getBoolean(PreferenceConstants.USE_EXTENDED_RESOLVE_ID));
        return setup;
    }

    public void setAdvancedSetup(AdvancedSetup setup) {
        prefStore
                .setValue(PreferenceConstants.RESOLVE_BEFORE_LAUNCH, setup.isResolveBeforeLaunch());
        prefStore.setValue(PreferenceConstants.USE_EXTENDED_RESOLVE_ID,
            setup.isUseExtendedResolveId());
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

    public int getResolveOnStartup() {
        return prefStore.getInt(PreferenceConstants.RESOLVE_ON_STARTUP);
    }

    public void setResolveOnStartup(int resolveOnStartup) {
        prefStore.setValue(PreferenceConstants.RESOLVE_ON_STARTUP, resolveOnStartup);
    }

    public boolean getAutoResolveOnClose() {
        return prefStore.getBoolean(PreferenceConstants.AUTO_RESOLVE_ON_CLOSE);
    }

    public void setAutoResolveOnClose(boolean autoResolveOnOpen) {
        prefStore.setValue(PreferenceConstants.AUTO_RESOLVE_ON_CLOSE, autoResolveOnOpen);
    }

    public boolean getAutoResolveOnOpen() {
        return prefStore.getBoolean(PreferenceConstants.AUTO_RESOLVE_ON_OPEN);
    }

    public void setAutoResolveOnOpen(boolean autoResolveOnOpen) {
        prefStore.setValue(PreferenceConstants.AUTO_RESOLVE_ON_OPEN, autoResolveOnOpen);
    }

    public boolean getAutoResolveOnChange() {
        return prefStore.getBoolean(PreferenceConstants.AUTO_RESOLVE_ON_CHANGE);
    }

    public void setAutoResolveOnChange(boolean autoResolveChange) {
        prefStore.setValue(PreferenceConstants.AUTO_RESOLVE_ON_CHANGE, autoResolveChange);
    }

    public int getIvyConsoleLogLevel() {
        return prefStore.getInt(PreferenceConstants.IVY_CONSOLE_LOG_LEVEL);
    }

    public void setIvyConsoleLogLevel(int logLevel) {
        prefStore.setValue(PreferenceConstants.IVY_CONSOLE_LOG_LEVEL, logLevel);
    }

    public int getIvyConsoleIvyDELogLevel() {
        return prefStore.getInt(PreferenceConstants.IVY_CONSOLE_IVYDE_LOG_LEVEL);
    }

    public void setIvyConsoleIvyDELogLevel(int ivyDElogLevel) {
        prefStore.setValue(PreferenceConstants.IVY_CONSOLE_IVYDE_LOG_LEVEL, ivyDElogLevel);
    }

    public boolean isOpenIvyConsoleOnStartup() {
        return prefStore.getBoolean(PreferenceConstants.OPEN_IVY_CONSOLE_ON_STARTUP);
    }

    public void setOpenIvyConsoleOnStartup(boolean openOnStartup) {
        prefStore.setValue(PreferenceConstants.OPEN_IVY_CONSOLE_ON_STARTUP, openOnStartup);
    }

    public boolean getIgnoreBranchOnWorkspaceProjects() {
        return prefStore.getBoolean(PreferenceConstants.IGNORE_BRANCH_ON_WORKSPACE_PROJECTS);
    }

    public void setIgnoreBranchOnWorkspaceProjects(boolean ignoreBranchOnWorkspaceProjects) {
        prefStore.setValue(PreferenceConstants.IGNORE_BRANCH_ON_WORKSPACE_PROJECTS,
            ignoreBranchOnWorkspaceProjects);
    }

    public boolean getIgnoreVersionOnWorkspaceProjects() {
        return prefStore.getBoolean(PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS);
    }

    public void setIgnoreVersionOnWorkspaceProjects(boolean ignoreVersionOnWorkspaceProjects) {
        prefStore.setValue(PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS,
            ignoreVersionOnWorkspaceProjects);
    }

    public RGB getEditorColorXmlComment() {
        return PreferenceConverter
                .getColor(prefStore, PreferenceConstants.EDITOR_COLOR_XML_COMMENT);
    }

    public void setEditorColorXmlComment(RGB color) {
        PreferenceConverter
                .setValue(prefStore, PreferenceConstants.EDITOR_COLOR_XML_COMMENT, color);
    }

    public RGB getEditorColorProcInst() {
        return PreferenceConverter.getColor(prefStore, PreferenceConstants.EDITOR_COLOR_PROC_INSTR);
    }

    public void setEditorColorProcInst(RGB color) {
        PreferenceConverter.setValue(prefStore, PreferenceConstants.EDITOR_COLOR_PROC_INSTR, color);
    }

    public RGB getEditorColorString() {
        return PreferenceConverter.getColor(prefStore, PreferenceConstants.EDITOR_COLOR_STRING);
    }

    public void setEditorColorString(RGB color) {
        PreferenceConverter.setValue(prefStore, PreferenceConstants.EDITOR_COLOR_STRING, color);
    }

    public RGB getEditorColorDefault() {
        return PreferenceConverter.getColor(prefStore, PreferenceConstants.EDITOR_COLOR_DEFAULT);
    }

    public void setEditorColorDefault(RGB color) {
        PreferenceConverter.setValue(prefStore, PreferenceConstants.EDITOR_COLOR_DEFAULT, color);
    }

    public RGB getEditorColorTag() {
        return PreferenceConverter.getColor(prefStore, PreferenceConstants.EDITOR_COLOR_TAG);
    }

    public void setEditorColorTag(RGB color) {
        PreferenceConverter.setValue(prefStore, PreferenceConstants.EDITOR_COLOR_TAG, color);
    }

    public boolean isOffline() {
        return prefStore.getBoolean(PreferenceConstants.OFFLINE);
    }

    public void setOffline(boolean offline) {
        prefStore.setValue(PreferenceConstants.OFFLINE, offline);
    }

    public boolean isErrorPopup() {
        return prefStore.getBoolean(PreferenceConstants.ERROR_POPUP);
    }

    public void setErrorPopup(boolean errorPopup) {
        prefStore.setValue(PreferenceConstants.ERROR_POPUP, errorPopup);
    }

}
