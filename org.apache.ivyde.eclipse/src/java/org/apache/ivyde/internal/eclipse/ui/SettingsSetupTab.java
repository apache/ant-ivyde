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
package org.apache.ivyde.internal.eclipse.ui;

import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.SettingsSetupEditor.SettingsEditorListener;
import org.apache.ivyde.internal.eclipse.ui.preferences.SettingsSetupPreferencePage;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class SettingsSetupTab extends AbstractSetupTab {

    private SettingsSetupEditor settingsEditor;

    public SettingsSetupTab(TabFolder tabs, IProject project) {
        super(tabs, "Settings", SettingsSetupPreferencePage.PREFERENCE_PAGE_ID, project);
    }

    protected Composite createSetupEditor(Composite configComposite, IProject project) {
        settingsEditor = new SettingsSetupEditor(configComposite, SWT.NONE, project);
        settingsEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        settingsEditor.addListener(new SettingsEditorListener() {
            public void settingsEditorUpdated(SettingsSetup setup) {
                settingsUpdated();
            }
        });

        return settingsEditor;
    }

    public void init(boolean isProjectSpecific, SettingsSetup setup) {
        init(isProjectSpecific);
        if (isProjectSpecific) {
            settingsEditor.init(setup);
        } else {
            settingsEditor.init(IvyPlugin.getPreferenceStoreHelper().getSettingsSetup());
        }
    }

    protected void settingsUpdated() {
        // nothing to do
    }

    public SettingsSetupEditor getSettingsEditor() {
        return settingsEditor;
    }

}
