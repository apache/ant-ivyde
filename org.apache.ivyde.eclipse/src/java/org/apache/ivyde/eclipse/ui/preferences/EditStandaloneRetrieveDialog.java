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

import org.apache.ivyde.eclipse.cpcontainer.StandaloneRetrieveSetup;
import org.apache.ivyde.eclipse.ui.IvyFilePathText;
import org.apache.ivyde.eclipse.ui.IvySettingsTab;
import org.apache.ivyde.eclipse.ui.RetrieveComposite;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class EditStandaloneRetrieveDialog extends Dialog {

    private final IProject project;

    private Text nameText;

    private StandaloneRetrieveSetup retrieveSetup;

    private IvyFilePathText ivyFilePathText;

    private RetrieveComposite retrieveComposite;

    private IvySettingsTab settingsTab;

    private StandaloneRetrieveSetup setup;

    protected EditStandaloneRetrieveDialog(Shell parentShell, IProject project,
            StandaloneRetrieveSetup retrieveSetup) {
        super(parentShell);
        this.project = project;
        this.retrieveSetup = retrieveSetup;
    }

    protected Control createDialogArea(Composite parent) {
        TabFolder tabs = new TabFolder(parent, SWT.BORDER);

        TabItem mainTab = new TabItem(tabs, SWT.NONE);
        mainTab.setText("Main");
        mainTab.setControl(createMainTab(tabs));

        settingsTab = new IvySettingsTab(tabs) {
            protected void settingsUpdated() {
                super.settingsUpdated();
            }
        };

        nameText.setText(retrieveSetup.getName());
        settingsTab.init(retrieveSetup.isSettingProjectSpecific(),
            retrieveSetup.getIvySettingsSetup());
        ivyFilePathText.init(retrieveSetup.getIvyXmlPath());
        retrieveComposite.init(retrieveSetup.getRetrieveSetup());

        return tabs;
    }

    private Control createMainTab(Composite parent) {
        Composite body = new Composite(parent, SWT.NONE);
        body.setLayout(new GridLayout(1, false));

        Composite nameComposite = new Composite(body, SWT.NONE);
        nameComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        nameComposite.setLayout(new GridLayout(2, false));

        Label nameLabel = new Label(nameComposite, SWT.NONE);
        nameLabel.setText("Name: ");
        nameLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));

        nameText = new Text(nameComposite, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        ivyFilePathText = new IvyFilePathText(parent, SWT.NONE, project);
        ivyFilePathText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        retrieveComposite = new RetrieveComposite(body, SWT.NONE);
        retrieveComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        return body;
    }
    protected void okPressed() {
        setup = new StandaloneRetrieveSetup();
        setup.setName(nameText.getText());
        setup.setSettingsProjectSpecific(settingsTab.isProjectSpecific());
        setup.setIvySettingsSetup(settingsTab.getSettingsEditor().getIvySettingsSetup());
        setup.setIvyXmlPath(ivyFilePathText.getIvyFilePath());
        setup.setRetrieveSetup(retrieveComposite.getRetrieveSetup());
        super.okPressed();
    }

    public StandaloneRetrieveSetup getStandaloneRetrieveSetup() {
        return setup;
    }
}
