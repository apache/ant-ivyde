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
package org.apache.ivyde.internal.eclipse.retrieve;

import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.resources.IProject;

public class StandaloneRetrieveSetup {

    private boolean resolveInWorkspace;

    private String name = "dependencies";

    private SettingsSetup settingsSetup = new SettingsSetup();

    private String ivyXmlPath = "ivy.xml";

    private RetrieveSetup retrieveSetup = new RetrieveSetup();

    private boolean settingsProjectSpecific;

    private IProject project;

    private final StandaloneRetrieveSetupState state = new StandaloneRetrieveSetupState(this);

    public StandaloneRetrieveSetupState getState() {
        return state;
    }

    public boolean isResolveInWorkspace() {
        return resolveInWorkspace;
    }

    public void setResolveInWorkspace(boolean resolveInWorkspace) {
        this.resolveInWorkspace = resolveInWorkspace;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SettingsSetup getSettingsSetup() {
        return settingsSetup;
    }

    public void setSettingsSetup(SettingsSetup settingsSetup) {
        this.settingsSetup = settingsSetup;
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public void setIvyXmlPath(String ivyXmlPath) {
        this.ivyXmlPath = ivyXmlPath;
    }

    public RetrieveSetup getRetrieveSetup() {
        return retrieveSetup;
    }

    public void setRetrieveSetup(RetrieveSetup retrieveSetup) {
        this.retrieveSetup = retrieveSetup;
    }

    public boolean isSettingProjectSpecific() {
        return settingsProjectSpecific;
    }

    public void setSettingsProjectSpecific(boolean isSettingsProjectSpecific) {
        this.settingsProjectSpecific = isSettingsProjectSpecific;
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public String toString() {
        return name != null ? name : retrieveSetup.getRetrievePattern();
    }

    public SettingsSetup getInheritedSettingSetup() {
        if (!settingsProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getSettingsSetup();
        }
        return settingsSetup;

    }

}
