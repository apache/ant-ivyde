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
package org.apache.ivyde.eclipse.cpcontainer;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.ConfTableViewer;
import org.apache.ivyde.eclipse.ui.IvyFilePathText;
import org.apache.ivyde.eclipse.ui.SettingsPathText;
import org.apache.ivyde.eclipse.ui.IvyFilePathText.IvyXmlPathListener;
import org.apache.ivyde.eclipse.ui.SettingsPathText.SettingsPathListener;
import org.apache.ivyde.eclipse.ui.preferences.IvyDEPreferenceStoreHelper;
import org.apache.ivyde.eclipse.ui.preferences.IvyPreferencePage;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class IvydeContainerPage extends NewElementWizardPage implements IClasspathContainerPage,
        IClasspathContainerPageExtension {

    private IJavaProject project;

    private IvyFilePathText ivyFilePathText;

    private ConfTableViewer confTableViewer;

    private SettingsPathText settingsText;

    private Text acceptedTypesText;

    private Text sourcesTypesText;

    private Text sourcesSuffixesText;

    private Text javadocTypesText;

    private Text javadocSuffixesText;

    private Button doRetrieveButton;

    private Text retrievePatternText;

    private Combo alphaOrderCheck;

    private Button resolveInWorkspaceCheck;

    private Button projectSpecificButton;

    private Link generalSettingsLink;

    private Composite configComposite;

    private IvyClasspathContainerConfiguration conf;

    private IClasspathEntry entry;

    private Button retrieveSyncButton;

    private TabItem mainTab;

    private TabFolder tabs;

    private TabItem advancedTab;

    /**
     * Constructor
     */
    public IvydeContainerPage() {
        super("IvyDE Container");
    }

    void checkCompleted() {
        String error;
        if (ivyFilePathText.getIvyFilePath().length() == 0) {
            error = "Choose an ivy file";
        } else {
            error = null;
        }
        setErrorMessage(error);
        setPageComplete(error == null);
    }

    void checkIvyXmlPath() {
        ModuleDescriptor md;
        try {
            md = conf.getModuleDescriptor();
            ivyFilePathText.setIvyXmlError(null);
        } catch (IvyDEException e) {
            md = null;
            ivyFilePathText.setIvyXmlError(e);
        }
        confTableViewer.setModuleDescriptor(md);
        checkCompleted();
    }

    public boolean finish() {
        conf.confs = confTableViewer.getSelectedConfigurations();
        if (projectSpecificButton.getSelection()) {
            conf.ivySettingsPath = settingsText.getSettingsPath();
            conf.acceptedTypes = IvyClasspathUtil.split(acceptedTypesText.getText());
            conf.sourceTypes = IvyClasspathUtil.split(sourcesTypesText.getText());
            conf.javadocTypes = IvyClasspathUtil.split(javadocTypesText.getText());
            conf.sourceSuffixes = IvyClasspathUtil.split(sourcesSuffixesText.getText());
            conf.javadocSuffixes = IvyClasspathUtil.split(javadocSuffixesText.getText());
            conf.doRetrieve = doRetrieveButton.getSelection();
            conf.retrievePattern = retrievePatternText.getText();
            conf.retrieveSync = retrieveSyncButton.getSelection();
            conf.alphaOrder = alphaOrderCheck.getSelectionIndex() == 1;
            conf.resolveInWorkspace = resolveInWorkspaceCheck.getSelection();
        } else {
            conf.ivySettingsPath = null;
        }
        entry = JavaCore.newContainerEntry(conf.getPath());
        return true;
    }

    public IClasspathEntry getSelection() {
        return entry;
    }

    public void setSelection(IClasspathEntry entry) {
        if (entry == null) {
            conf = new IvyClasspathContainerConfiguration(project, "ivy.xml", true);
        } else {
            conf = new IvyClasspathContainerConfiguration(project, entry.getPath(), true);
        }
    }

    public void createControl(Composite parent) {
        setTitle("IvyDE Managed Libraries");
        setDescription("Choose ivy file and its configurations.");

        tabs = new TabFolder(parent, SWT.BORDER);
        tabs.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        mainTab = new TabItem(tabs, SWT.NONE);
        mainTab.setText("Main");
        mainTab.setControl(createMainTab(tabs));

        tabs.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ivyFilePathText.updateErrorMarker();
                settingsText.updateErrorMarker();
            }
        });

        advancedTab = new TabItem(tabs, SWT.NONE);
        advancedTab.setText("Advanced");
        advancedTab.setControl(createAdvancedTab(tabs));

        setControl(tabs);

        loadFromConf();
        checkCompleted();
        tabs.setFocus();
    }

    private Control createMainTab(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        // Label for ivy file field
        Label pathLabel = new Label(composite, SWT.NONE);
        pathLabel.setText("Ivy File");

        ivyFilePathText = new IvyFilePathText(composite, SWT.NONE, project);
        ivyFilePathText.addListener(new IvyXmlPathListener() {
            public void ivyXmlPathUpdated(String path) {
                conf.ivyXmlPath = path;
                checkIvyXmlPath();
            }
        });
        ivyFilePathText
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        // Label for ivy configurations field
        Label confLabel = new Label(composite, SWT.NONE);
        confLabel.setText("Configurations");

        // table for configuration selection
        confTableViewer = new ConfTableViewer(composite, SWT.NONE);
        confTableViewer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        // refresh
        Button refreshConf = new Button(composite, SWT.NONE);
        refreshConf.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
        refreshConf.setText("Refresh");
        refreshConf.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                ModuleDescriptor md;
                try {
                    md = conf.getModuleDescriptor();
                } catch (IvyDEException e) {
                    md = null;
                    e.show(IStatus.ERROR, "Ivy configuration error",
                        "The configurations of the ivy.xml could not be retrieved: ");
                }
                confTableViewer.setModuleDescriptor(md);
            }
        });

        return composite;
    }

    private Control createAdvancedTab(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite headerComposite = new Composite(composite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        projectSpecificButton = new Button(headerComposite, SWT.CHECK);
        projectSpecificButton.setText("Enable project specific settings");
        projectSpecificButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFieldsStatus();
                conf.ivySettingsPath = settingsText.getSettingsPath();
                settingsUpdated();
            }
        });

        generalSettingsLink = new Link(headerComposite, SWT.NONE);
        generalSettingsLink.setFont(composite.getFont());
        generalSettingsLink.setText("<A>Configure Workspace Settings...</A>");
        generalSettingsLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
                    IvyPreferencePage.PEREFERENCE_PAGE_ID, null, null);
                dialog.open();
            }
        });
        generalSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        Label horizontalLine = new Label(headerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout(3, false));
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        Label label = new Label(configComposite, SWT.NONE);
        label.setText("Ivy settings path:");

        settingsText = new SettingsPathText(configComposite, SWT.NONE);
        settingsText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        settingsText.addListener(new SettingsPathListener() {
            public void settingsPathUpdated(String path) {
                conf.ivySettingsPath = path;
                settingsUpdated();
            }
        });

        label = new Label(configComposite, SWT.NONE);
        label.setText("Accepted types:");

        acceptedTypesText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        acceptedTypesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2,
                1));
        acceptedTypesText.setToolTipText("Comma separated list of artifact types"
                + " to use in IvyDE Managed Dependencies Library.\n" + "Example: jar, zip");

        label = new Label(configComposite, SWT.NONE);
        label.setText("Sources types:");

        sourcesTypesText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        sourcesTypesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false,
                2, 1));
        sourcesTypesText
                .setToolTipText("Comma separated list of artifact types to be used as sources.\n"
                        + "Example: source, src");

        label = new Label(configComposite, SWT.NONE);
        label.setText("Sources suffixes:");

        sourcesSuffixesText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        sourcesSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false,
                2, 1));
        sourcesSuffixesText
                .setToolTipText("Comma separated list of suffixes to match sources to artifacts.\n"
                        + "Example: -source, -src");

        label = new Label(configComposite, SWT.NONE);
        label.setText("Javadoc types:");

        javadocTypesText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        javadocTypesText
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        javadocTypesText
                .setToolTipText("Comma separated list of artifact types to be used as javadoc.\n"
                        + "Example: javadoc.");

        label = new Label(configComposite, SWT.NONE);
        label.setText("Javadoc suffixes:");

        javadocSuffixesText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        javadocSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false,
                2, 1));
        javadocSuffixesText
                .setToolTipText("Comma separated list of suffixes to match javadocs to artifacts.\n"
                        + "Example: -javadoc, -doc");

        doRetrieveButton = new Button(configComposite, SWT.CHECK);
        doRetrieveButton.setText("Do retrieve after resolve");
        doRetrieveButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3,
                1));

        label = new Label(configComposite, SWT.NONE);
        label.setText("Retrieve pattern:");

        retrievePatternText = new Text(configComposite, SWT.SINGLE | SWT.BORDER);
        retrievePatternText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false,
                2, 1));
        retrievePatternText.setEnabled(doRetrieveButton.getSelection());
        retrievePatternText.setToolTipText("Example: lib/[conf]/[artifact].[ext]\n"
                + "To copy artifacts in folder named lib without revision by folder"
                + " named like configurations");

        retrieveSyncButton = new Button(configComposite, SWT.CHECK);
        retrieveSyncButton.setText("Delete old retrieved artifacts");
        retrieveSyncButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false,
                3, 1));
        retrieveSyncButton.setEnabled(doRetrieveButton.getSelection());

        doRetrieveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                retrievePatternText.setEnabled(doRetrieveButton.getSelection());
                retrieveSyncButton.setEnabled(doRetrieveButton.getSelection());
            }
        });

        label = new Label(configComposite, SWT.NONE);
        label.setText("Order of the classpath entries:");

        alphaOrderCheck = new Combo(configComposite, SWT.READ_ONLY);
        alphaOrderCheck
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        alphaOrderCheck.setToolTipText("Order of the artifacts in the classpath container");
        alphaOrderCheck.add("From the ivy.xml");
        alphaOrderCheck.add("Lexical");

        resolveInWorkspaceCheck = new Button(this.configComposite, SWT.CHECK);
        resolveInWorkspaceCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 2, 1));
        resolveInWorkspaceCheck.setText("Resolve dependencies in workspace");
        resolveInWorkspaceCheck
                .setToolTipText("Will replace jars on the classpath with workspace projects");

        return composite;
    }

    void settingsUpdated() {
        try {
            conf.ivySettingsLastModified = -1;
            conf.getIvy();
            settingsText.setSettingsError(null);
        } catch (IvyDEException e) {
            settingsText.setSettingsError(e);
        }
        checkIvyXmlPath();
    }

    private void loadFromConf() {
        ivyFilePathText.init(conf.ivyXmlPath);
        confTableViewer.init(conf.confs);

        if (conf.isProjectSpecific()) {
            projectSpecificButton.setSelection(true);
            settingsText.init(conf.ivySettingsPath);
            acceptedTypesText.setText(IvyClasspathUtil.concat(conf.acceptedTypes));
            sourcesTypesText.setText(IvyClasspathUtil.concat(conf.sourceTypes));
            sourcesSuffixesText.setText(IvyClasspathUtil.concat(conf.sourceSuffixes));
            javadocTypesText.setText(IvyClasspathUtil.concat(conf.javadocTypes));
            javadocSuffixesText.setText(IvyClasspathUtil.concat(conf.javadocSuffixes));
            doRetrieveButton.setSelection(conf.doRetrieve);
            retrievePatternText.setText(conf.retrievePattern);
            retrieveSyncButton.setSelection(conf.retrieveSync);
            alphaOrderCheck.select(conf.alphaOrder ? 1 : 0);
            resolveInWorkspaceCheck.setSelection(this.conf.resolveInWorkspace);
        } else {
            projectSpecificButton.setSelection(false);
            IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
            settingsText.init(helper.getIvySettingsPath());
            acceptedTypesText.setText(IvyClasspathUtil.concat(helper.getAcceptedTypes()));
            sourcesTypesText.setText(IvyClasspathUtil.concat(helper.getSourceTypes()));
            sourcesSuffixesText.setText(IvyClasspathUtil.concat(helper.getSourceSuffixes()));
            javadocTypesText.setText(IvyClasspathUtil.concat(helper.getJavadocTypes()));
            javadocSuffixesText.setText(IvyClasspathUtil.concat(helper.getJavadocSuffixes()));
            doRetrieveButton.setSelection(helper.getDoRetrieve());
            retrievePatternText.setText(helper.getRetrievePattern());
            retrieveSyncButton.setSelection(helper.getRetrieveSync());
            alphaOrderCheck.select(helper.isAlphOrder() ? 1 : 0);
            resolveInWorkspaceCheck.setSelection(helper.isResolveInWorkspace());
        }

        updateFieldsStatus();
    }

    void updateFieldsStatus() {
        boolean projectSpecific = projectSpecificButton.getSelection();
        generalSettingsLink.setEnabled(!projectSpecific);
        configComposite.setEnabled(projectSpecific);
        settingsText.setEnabled(projectSpecific);
        acceptedTypesText.setEnabled(projectSpecific);
        sourcesTypesText.setEnabled(projectSpecific);
        sourcesSuffixesText.setEnabled(projectSpecific);
        javadocTypesText.setEnabled(projectSpecific);
        javadocSuffixesText.setEnabled(projectSpecific);
        doRetrieveButton.setEnabled(projectSpecific);
        retrievePatternText.setEnabled(doRetrieveButton.getSelection() && projectSpecific);
        retrieveSyncButton.setEnabled(doRetrieveButton.getSelection() && projectSpecific);
        alphaOrderCheck.setEnabled(projectSpecific);
        resolveInWorkspaceCheck.setEnabled(projectSpecific);
    }

    public void initialize(IJavaProject p, IClasspathEntry[] currentEntries) {
        this.project = p;
    }

}
