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
package org.apache.ivyde.eclipse.ui;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvySettingsSetup;
import org.apache.ivyde.eclipse.ui.SettingsEditor.SettingsEditorListener;
import org.apache.ivyde.eclipse.ui.preferences.SettingsPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class IvySettingsTab {

    private Button settingsProjectSpecificButton;

    private Link mainGeneralSettingsLink;

    private SettingsEditor settingsEditor;

    public IvySettingsTab(TabFolder tabs) {
        TabItem settingsTab = new TabItem(tabs, SWT.NONE);
        settingsTab.setText("Settings");
        settingsTab.setControl(createSettingsTab(tabs));
    }

    private Control createSettingsTab(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite headerComposite = new Composite(composite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        settingsProjectSpecificButton = new Button(headerComposite, SWT.CHECK);
        settingsProjectSpecificButton.setText("Enable project specific settings");
        settingsProjectSpecificButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFieldsStatusSettings();
                settingsUpdated();
            }
        });

        mainGeneralSettingsLink = new Link(headerComposite, SWT.NONE);
        mainGeneralSettingsLink.setFont(headerComposite.getFont());
        mainGeneralSettingsLink.setText("<A>Configure Workspace Settings...</A>");
        mainGeneralSettingsLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(parent
                        .getShell(), SettingsPreferencePage.PEREFERENCE_PAGE_ID, null, null);
                dialog.open();
            }
        });
        mainGeneralSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        Label horizontalLine = new Label(headerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        // CheckStyle:MagicNumber| OFF
        Composite configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout(3, false));
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        settingsEditor = new SettingsEditor(configComposite, SWT.NONE);
        settingsEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        settingsEditor.addListener(new SettingsEditorListener() {
            public void settingsEditorUpdated(IvySettingsSetup setup) {
                settingsUpdated();
            }
        });

        return composite;
    }

    public void init(boolean isProjectSpecific, IvySettingsSetup setup) {
        if (isProjectSpecific) {
            settingsProjectSpecificButton.setSelection(true);
            settingsEditor.init(setup);
        } else {
            settingsProjectSpecificButton.setSelection(false);
            settingsEditor.init(IvyPlugin.getPreferenceStoreHelper().getIvySettingsSetup());
            settingsEditor.setEnabled(false);
        }
    }

    public boolean isProjectSpecific() {
        return settingsProjectSpecificButton.getSelection();
    }

    public void updateFieldsStatusSettings() {
        boolean projectSpecific = settingsProjectSpecificButton.getSelection();
        mainGeneralSettingsLink.setEnabled(!projectSpecific);
        settingsEditor.setEnabled(projectSpecific);
    }

    protected void settingsUpdated() {
        // nothing to do
    }

    public SettingsEditor getSettingsEditor() {
        return settingsEditor;
    }

}