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

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.SettingsEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SettingsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /** the ID of the preference page */
    public static final String PEREFERENCE_PAGE_ID =
        "org.apache.ivyde.eclipse.ui.preferences.SettingsPreferencePage";

    private SettingsEditor settingsEditor;

    public SettingsPreferencePage() {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        // CheckStyle:MagicNumber| OFF
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        composite.setLayout(new GridLayout());

        settingsEditor = new SettingsEditor(composite, SWT.NONE);
        settingsEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        // CheckStyle:MagicNumber| ON

        initPreferences();

        return composite;
    }

    private void initPreferences() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
        settingsEditor.init(helper.getIvySettingsSetup());
    }

    public boolean performOk() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
        helper.setIvySettingsSetup(settingsEditor.getIvySettingsSetup());
        return true;
    }

    protected void performDefaults() {
        settingsEditor.init(PreferenceInitializer.DEFAULT_IVY_SETTINGS_SETUP);
    }
}
