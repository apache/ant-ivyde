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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.AcceptedSuffixesTypesComposite;
import org.apache.ivyde.eclipse.ui.ConfTableViewer;
import org.apache.ivyde.eclipse.ui.IvyFilePathText;
import org.apache.ivyde.eclipse.ui.IvySettingsTab;
import org.apache.ivyde.eclipse.ui.RetrieveComposite;
import org.apache.ivyde.eclipse.ui.ConfTableViewer.ConfTableListener;
import org.apache.ivyde.eclipse.ui.IvyFilePathText.IvyXmlPathListener;
import org.apache.ivyde.eclipse.ui.preferences.ClasspathPreferencePage;
import org.apache.ivyde.eclipse.ui.preferences.IvyDEPreferenceStoreHelper;
import org.apache.ivyde.eclipse.ui.preferences.RetrievePreferencePage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Editor of the classpath container configuration at the project level.
 */
public class IvydeContainerPage extends NewElementWizardPage implements IClasspathContainerPage,
        IClasspathContainerPageExtension {

    private IJavaProject project;

    private IvyFilePathText ivyFilePathText;

    private ConfTableViewer confTableViewer;

    private Combo alphaOrderCheck;

    private Button resolveInWorkspaceCheck;

    private Button resolveBeforeLaunchCheck;

    private Button advancedProjectSpecificButton;

    private Link advancedGeneralSettingsLink;

    private IvyClasspathContainerConfiguration conf;

    private IClasspathEntry entry;

    private TabFolder tabs;

    private RetrieveComposite retrieveComposite;

    private AcceptedSuffixesTypesComposite acceptedSuffixesTypesComposite;

    private Button retrieveProjectSpecificButton;

    private Link retrieveGeneralSettingsLink;

    private boolean exported = false;

    private String oldIvyFile = null;

    private List oldConfs = null;

    private IvyClasspathContainerState state;

    private IvySettingsTab settingsTab;

    /**
     * Constructor
     */
    public IvydeContainerPage() {
        super("IvyDE Container");
    }

    void checkCompleted() {
        String error = null;
        if (ivyFilePathText.getIvyFilePath().length() == 0) {
            error = "Choose an ivy file";
        } else if (project != null) {
            error = checkConf();
        }
        setErrorMessage(error);
        setPageComplete(error == null);
    }

    /**
     * Check that the chosen configuration doesn't already exist within the current project
     * <p>
     * The uniqueness is for xmlivyPath + conf
     * 
     * @return
     */
    private String checkConf() {
        String error = null;

        String ivyFilePath = ivyFilePathText.getIvyFilePath();
        List selectedConfigurations = confTableViewer.getSelectedConfigurations();

        List/* <IvyClasspathContainer> */containers = IvyClasspathUtil
                .getIvyClasspathContainers(project);
        if (containers == null) {
            return null;
        }

        Iterator/* <IvyClasspathContainer> */itContainers = containers.iterator();
        while (error == null && itContainers.hasNext()) {
            IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainers.next();
            IvyClasspathContainerConfiguration cpc = ivycp.getConf();

            // first check that this is not the one we are editing
            if (oldIvyFile != null && cpc.getIvyXmlPath().equals(oldIvyFile) && oldConfs != null
                    && oldConfs.size() == cpc.getConfs().size()
                    && oldConfs.containsAll(cpc.getConfs())) {
                continue;
            }

            if (cpc.getIvyXmlPath().equals(ivyFilePath)) {
                if (selectedConfigurations.isEmpty() || selectedConfigurations.contains("*")
                        || cpc.getConfs().isEmpty() || cpc.getConfs().contains("*")) {
                    error = "A container already exists for the selected conf of "
                            + "the module descriptor";
                } else {
                    ArrayList list = new ArrayList(cpc.getConfs());
                    list.retainAll(selectedConfigurations);
                    if (!list.isEmpty()) {
                        error = "A container already exists for the selected conf of "
                                + "the module descriptor";
                    }
                }
            }
        }

        return error;
    }

    void checkIvyXmlPath() {
        ModuleDescriptor md;
        try {
            md = state.getModuleDescriptor();
            ivyFilePathText.setIvyXmlError(null);
        } catch (IvyDEException e) {
            md = null;
            ivyFilePathText.setIvyXmlError(e);
        }
        confTableViewer.setModuleDescriptor(md);
        checkCompleted();
    }

    public boolean finish() {
        List confs = confTableViewer.getSelectedConfigurations();
        if (confs.isEmpty()) {
            confs = Collections.singletonList("*");
        }
        conf.setConfs(confs);

        if (settingsTab.isProjectSpecific()) {
            conf.setSettingsProjectSpecific(true);
            conf.setIvySettingsSetup(settingsTab.getSettingsEditor().getIvySettingsSetup());
        } else {
            conf.setSettingsProjectSpecific(false);
        }

        if (retrieveProjectSpecificButton.getSelection()) {
            conf.setRetrieveProjectSpecific(true);
            conf.setRetrieveSetup(retrieveComposite.getRetrieveSetup());
        } else {
            conf.setRetrieveProjectSpecific(false);
        }
        if (advancedProjectSpecificButton.getSelection()) {
            conf.setAdvancedProjectSpecific(true);
            conf
                    .setContainerMappingSetup(acceptedSuffixesTypesComposite
                            .getContainerMappingSetup());
            conf.setAlphaOrder(alphaOrderCheck.getSelectionIndex() == 1);
            conf.setResolveInWorkspace(resolveInWorkspaceCheck.getSelection());
            conf.setResolveBeforeLaunch(resolveBeforeLaunchCheck.getSelection());
        } else {
            conf.setAdvancedProjectSpecific(false);
        }

        IPath path = IvyClasspathContainerConfAdapter.getPath(conf);
        IClasspathAttribute[] atts = conf.getAttributes();

        entry = JavaCore.newContainerEntry(path, null, atts, exported);

        try {
            IvyClasspathContainer ivycp = new IvyClasspathContainer(project, path,
                    new IClasspathEntry[0], atts);
            JavaCore.setClasspathContainer(path, new IJavaProject[] {project},
                new IClasspathContainer[] {ivycp}, null);
            ivycp.launchResolve(false, null);
        } catch (JavaModelException e) {
            IvyPlugin.log(e);
        }

        return true;
    }

    public IJavaProject getProject() {
        return project;
    }

    public IClasspathEntry getSelection() {
        return entry;
    }

    public void setSelection(IClasspathEntry entry) {
        checkProject();
        if (entry == null) {
            conf = new IvyClasspathContainerConfiguration(project, "ivy.xml", true);
        } else {
            conf = new IvyClasspathContainerConfiguration(project, entry.getPath(), true, entry
                    .getExtraAttributes());
            exported = entry.isExported();
        }
        state = new IvyClasspathContainerState(conf);
        oldIvyFile = conf.getIvyXmlPath();
        oldConfs = conf.getConfs();
    }

    public void setSelection(IFile ivyfile) {
        checkProject();
        conf = new IvyClasspathContainerConfiguration(project, ivyfile.getProjectRelativePath()
                .toString(), true);
        state = new IvyClasspathContainerState(conf);
    }

    private void checkProject() {
        if (project == null) {
            project = FakeProjectManager.createPlaceholderProject();
        }
    }

    public void createControl(Composite parent) {
        setTitle("IvyDE Managed Libraries");
        setDescription("Choose ivy file and its configurations.");

        tabs = new TabFolder(parent, SWT.BORDER);
        tabs.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        TabItem mainTab = new TabItem(tabs, SWT.NONE);
        mainTab.setText("Main");
        mainTab.setControl(createMainTab(tabs));

        settingsTab = new IvySettingsTab(tabs) {
            protected void settingsUpdated() {
                try {
                    conf.setSettingsProjectSpecific(isProjectSpecific());
                    conf.setIvySettingsSetup(getSettingsEditor().getIvySettingsSetup());
                    state.setIvySettingsLastModified(-1);
                    state.getIvy();
                    getSettingsEditor().setSettingsError(null);
                    checkIvyXmlPath();
                } catch (IvyDEException e) {
                    getSettingsEditor().setSettingsError(e);
                }
            }
        };

        TabItem retrieveTab = new TabItem(tabs, SWT.NONE);
        retrieveTab.setText("Retrieve");
        retrieveTab.setControl(createRetrieveTab(tabs));

        TabItem advancedTab = new TabItem(tabs, SWT.NONE);
        advancedTab.setText("Advanced");
        advancedTab.setControl(createAdvancedTab(tabs));

        tabs.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ivyFilePathText.updateErrorMarker();
                settingsTab.getSettingsEditor().updateErrorMarker();
            }
        });

        setControl(tabs);

        loadFromConf();
        checkCompleted();
        tabs.setFocus();
    }

    private Control createMainTab(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        // CheckStyle:MagicNumber| OFF
        Composite configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout(3, false));
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        // Label for ivy file field
        Label pathLabel = new Label(configComposite, SWT.NONE);
        pathLabel.setText("Ivy File");

        ivyFilePathText = new IvyFilePathText(configComposite, SWT.NONE, project.getProject());
        ivyFilePathText.addListener(new IvyXmlPathListener() {
            public void ivyXmlPathUpdated(String path) {
                conf.setIvyXmlPath(path);
                checkIvyXmlPath();
            }
        });
        ivyFilePathText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2,
                1));

        // Label for ivy configurations field
        Label confLabel = new Label(configComposite, SWT.NONE);
        confLabel.setText("Configurations");

        // table for configuration selection
        confTableViewer = new ConfTableViewer(configComposite, SWT.NONE);
        confTableViewer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        confTableViewer.addListener(new ConfTableListener() {
            public void confTableUpdated(List confs) {
                checkCompleted();
            }
        });

        // refresh
        Button refreshConf = new Button(configComposite, SWT.NONE);
        refreshConf.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
        refreshConf.setText("Refresh");
        refreshConf.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                ModuleDescriptor md;
                try {
                    md = state.getModuleDescriptor();
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

    private Control createRetrieveTab(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite headerComposite = new Composite(composite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        retrieveProjectSpecificButton = new Button(headerComposite, SWT.CHECK);
        retrieveProjectSpecificButton.setText("Enable project specific settings");
        retrieveProjectSpecificButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFieldsStatusRetrieve();
            }
        });

        retrieveGeneralSettingsLink = new Link(headerComposite, SWT.NONE);
        retrieveGeneralSettingsLink.setFont(composite.getFont());
        retrieveGeneralSettingsLink.setText("<A>Configure Workspace Settings...</A>");
        retrieveGeneralSettingsLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
                    RetrievePreferencePage.PEREFERENCE_PAGE_ID, null, null);
                dialog.open();
            }
        });
        retrieveGeneralSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        Label horizontalLine = new Label(headerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        Composite configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout());
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        retrieveComposite = new RetrieveComposite(configComposite, SWT.NONE);
        retrieveComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        return composite;
    }

    private Control createAdvancedTab(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Composite headerComposite = new Composite(composite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        advancedProjectSpecificButton = new Button(headerComposite, SWT.CHECK);
        advancedProjectSpecificButton.setText("Enable project specific settings");
        advancedProjectSpecificButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFieldsStatusAdvanced();
            }
        });

        advancedGeneralSettingsLink = new Link(headerComposite, SWT.NONE);
        advancedGeneralSettingsLink.setFont(composite.getFont());
        advancedGeneralSettingsLink.setText("<A>Configure Workspace Settings...</A>");
        advancedGeneralSettingsLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
                    ClasspathPreferencePage.PEREFERENCE_PAGE_ID, null, null);
                dialog.open();
            }
        });
        advancedGeneralSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        Label horizontalLine = new Label(headerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        Composite configComposite = new Composite(composite, SWT.NONE);
        configComposite.setLayout(new GridLayout(3, false));
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        resolveInWorkspaceCheck = new Button(configComposite, SWT.CHECK);
        resolveInWorkspaceCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 3, 1));
        resolveInWorkspaceCheck.setText("Resolve dependencies in workspace");
        resolveInWorkspaceCheck
                .setToolTipText("Will replace jars on the classpath with workspace projects");

        resolveBeforeLaunchCheck = new Button(configComposite, SWT.CHECK);
        resolveBeforeLaunchCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 3, 1));
        resolveBeforeLaunchCheck.setText("Resolve before launch");
        resolveBeforeLaunchCheck
                .setToolTipText("Trigger a resolve before each run of any kind of java launch configuration");

        Label label = new Label(configComposite, SWT.NONE);
        label.setText("Order of the classpath entries:");

        alphaOrderCheck = new Combo(configComposite, SWT.READ_ONLY);
        alphaOrderCheck
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        alphaOrderCheck.setToolTipText("Order of the artifacts in the classpath container");
        alphaOrderCheck.add("From the ivy.xml");
        alphaOrderCheck.add("Lexical");

        acceptedSuffixesTypesComposite = new AcceptedSuffixesTypesComposite(configComposite,
                SWT.NONE);
        acceptedSuffixesTypesComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
                true, false, 3, 1));

        return composite;
    }

    private void loadFromConf() {
        ivyFilePathText.init(conf.getIvyXmlPath());
        confTableViewer.init(conf.getConfs());

        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();

        settingsTab.init(conf.isSettingsProjectSpecific(), conf.getIvySettingsSetup());

        if (conf.isRetrieveProjectSpecific()) {
            retrieveProjectSpecificButton.setSelection(true);
            retrieveComposite.init(conf.getRetrieveSetup());
        } else {
            retrieveProjectSpecificButton.setSelection(false);
            retrieveComposite.init(helper.getRetrieveSetup());
        }

        if (conf.isAdvancedProjectSpecific()) {
            advancedProjectSpecificButton.setSelection(true);
            acceptedSuffixesTypesComposite.init(conf.getContainerMappingSetup());
            alphaOrderCheck.select(conf.isAlphaOrder() ? 1 : 0);
            resolveInWorkspaceCheck.setSelection(conf.isResolveInWorkspace());
            resolveBeforeLaunchCheck.setSelection(conf.isResolveBeforeLaunch());
        } else {
            advancedProjectSpecificButton.setSelection(false);
            acceptedSuffixesTypesComposite.init(helper.getContainerMappingSetup());
            alphaOrderCheck.select(helper.isAlphOrder() ? 1 : 0);
            resolveInWorkspaceCheck.setSelection(helper.isResolveInWorkspace());
            resolveBeforeLaunchCheck.setSelection(helper.isResolveBeforeLaunch());
        }

        settingsTab.updateFieldsStatusSettings();
        updateFieldsStatusRetrieve();
        updateFieldsStatusAdvanced();
    }

    void updateFieldsStatusRetrieve() {
        boolean projectSpecific = retrieveProjectSpecificButton.getSelection();
        conf.setRetrieveProjectSpecific(projectSpecific);
        retrieveGeneralSettingsLink.setEnabled(!projectSpecific);
        retrieveComposite.setEnabled(projectSpecific);
    }

    void updateFieldsStatusAdvanced() {
        boolean projectSpecific = advancedProjectSpecificButton.getSelection();
        conf.setAdvancedProjectSpecific(projectSpecific);
        advancedGeneralSettingsLink.setEnabled(!projectSpecific);
        acceptedSuffixesTypesComposite.setEnabled(projectSpecific);
        alphaOrderCheck.setEnabled(projectSpecific);
        resolveInWorkspaceCheck.setEnabled(projectSpecific);
        resolveBeforeLaunchCheck.setEnabled(projectSpecific);
    }

    public void initialize(IJavaProject p, IClasspathEntry[] currentEntries) {
        this.project = p;
    }

}
