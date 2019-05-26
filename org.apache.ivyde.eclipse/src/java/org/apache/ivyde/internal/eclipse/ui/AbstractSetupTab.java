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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

public abstract class AbstractSetupTab {

    private final Button projectSpecificButton;

    private final Link mainGeneralSettingsLink;

    private final Composite setupEditor;

    public AbstractSetupTab(final TabFolder tabs, String title, final String preferencePageId, IProject project) {
        TabItem tab = new TabItem(tabs, SWT.NONE);
        tab.setText(title);

        Composite composite = new Composite(tabs, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite headerComposite = new Composite(composite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        projectSpecificButton = new Button(headerComposite, SWT.CHECK);
        projectSpecificButton.setText("Enable project specific settings");
        projectSpecificButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                projectSpecificChanged();
            }
        });

        mainGeneralSettingsLink = new Link(headerComposite, SWT.NONE);
        mainGeneralSettingsLink.setFont(headerComposite.getFont());
        mainGeneralSettingsLink.setText("<A>Configure Workspace Settings...</A>");
        mainGeneralSettingsLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(tabs.getShell(),
                    preferencePageId, null, null);
                dialog.open();
            }
        });
        mainGeneralSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        Label horizontalLine = new Label(headerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        // CheckStyle:MagicNumber| OFF
        Composite configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout());
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        setupEditor = createSetupEditor(configComposite, project);

        tab.setControl(composite);
    }

    protected abstract Composite createSetupEditor(Composite configComposite, IProject project);

    public void init(boolean isProjectSpecific) {
        if (isProjectSpecific) {
            projectSpecificButton.setSelection(true);
        } else {
            projectSpecificButton.setSelection(false);
            setupEditor.setEnabled(false);
        }
    }

    public boolean isProjectSpecific() {
        return projectSpecificButton.getSelection();
    }

    public void projectSpecificChanged() {
        boolean projectSpecific = projectSpecificButton.getSelection();
        mainGeneralSettingsLink.setEnabled(!projectSpecific);
        setupEditor.setEnabled(projectSpecific);
    }

}
