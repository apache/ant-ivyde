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
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyMarkerManager;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.AdvancedSetupTab;
import org.apache.ivyde.internal.eclipse.ui.ClasspathSetupTab;
import org.apache.ivyde.internal.eclipse.ui.ConfTableViewer;
import org.apache.ivyde.internal.eclipse.ui.IvyFilePathText;
import org.apache.ivyde.internal.eclipse.ui.MappingSetupTab;
import org.apache.ivyde.internal.eclipse.ui.SettingsSetupTab;
import org.apache.ivyde.internal.eclipse.ui.ConfTableViewer.ConfTableListener;
import org.apache.ivyde.internal.eclipse.ui.IvyFilePathText.IvyXmlPathListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Editor of the classpath container configuration at the project level.
 */
public class IvydeContainerPage extends NewElementWizardPage implements IClasspathContainerPage,
        IClasspathContainerPageExtension {

    private IJavaProject project;

    private IvyFilePathText ivyFilePathText;

    private ConfTableViewer confTableViewer;

    private IvyClasspathContainerConfiguration conf;

    private IClasspathEntry entry;

    private TabFolder tabs;

    private boolean exported = false;

    private String oldIvyFile = null;

    private List<String> oldConfs = null;

    private IvyClasspathContainerState state;

    private SettingsSetupTab settingsSetupTab;

    private ClasspathSetupTab classpathSetupTab;

    private MappingSetupTab mappingSetupTab;

    private AdvancedSetupTab advancedSetupTab;

    /**
     * Constructor
     */
    public IvydeContainerPage() {
        super("IvyDE Container");
    }

    public IJavaProject getProject() {
        return project;
    }

    void checkCompleted() {
        String error = null;
        if (ivyFilePathText.getIvyFilePath().length() == 0) {
            error = "Choose an Ivy file";
        } else if (project != null) {
            error = checkConf();
        }
        setErrorMessage(error);
        setPageComplete(error == null);
    }

    /**
     * Check that the chosen configuration doesn't already exist within the current project.
     * <p>
     * The uniqueness is for xmlivyPath + conf
     * </p>
     *
     * @return String
     */
    private String checkConf() {
        String error = null;

        String ivyFilePath = ivyFilePathText.getIvyFilePath();
        List<String> selectedConfigurations = confTableViewer.getSelectedConfigurations();

        List<IvyClasspathContainer> containers = IvyClasspathContainerHelper
                .getContainers(project);
        if (containers == null) {
            return null;
        }

        for (IvyClasspathContainer container : containers) {
            IvyClasspathContainerConfiguration cpc = container.getConf();

            // first check that this is not the one we are editing
            if (cpc.getIvyXmlPath().equals(oldIvyFile) && oldConfs != null
                    && oldConfs.size() == cpc.getConfs().size()
                    && oldConfs.containsAll(cpc.getConfs())) {
                continue;
            }

            if (cpc.getIvyXmlPath().equals(ivyFilePath)) {
                if (selectedConfigurations.isEmpty() || selectedConfigurations.contains("*")
                        || cpc.getConfs().isEmpty() || cpc.getConfs().contains("*")) {
                    error = "A container already exists for the selected conf of "
                            + "the module descriptor";
                    break;
                } else {
                    List<String> list = new ArrayList<>(cpc.getConfs());
                    list.retainAll(selectedConfigurations);
                    if (!list.isEmpty()) {
                        error = "A container already exists for the selected conf of "
                                + "the module descriptor";
                        break;
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
        List<String> confs = confTableViewer.getSelectedConfigurations();
        if (confs.isEmpty()) {
            confs = Collections.singletonList("*");
        }
        conf.setConfs(confs);

        if (settingsSetupTab.isProjectSpecific()) {
            conf.setSettingsProjectSpecific(true);
            conf.setIvySettingsSetup(settingsSetupTab.getSettingsEditor().getIvySettingsSetup());
        } else {
            conf.setSettingsProjectSpecific(false);
        }

        if (classpathSetupTab.isProjectSpecific()) {
            conf.setClassthProjectSpecific(true);
            conf.setClasspathSetup(classpathSetupTab.getClasspathSetupEditor().getClasspathSetup());
        } else {
            conf.setClassthProjectSpecific(false);
        }

        if (mappingSetupTab.isProjectSpecific()) {
            conf.setMappingProjectSpecific(true);
            conf.setMappingSetup(mappingSetupTab.getMappingSetupEditor().getMappingSetup());
        } else {
            conf.setMappingProjectSpecific(false);
        }

        if (advancedSetupTab.isProjectSpecific()) {
            conf.setAdvancedProjectSpecific(true);
            conf.setAdvancedSetup(advancedSetupTab.getAdvancedSetupEditor().getAdvancedSetup());
        } else {
            conf.setAdvancedProjectSpecific(false);
        }

        IPath path = IvyClasspathContainerConfAdapter.getPath(conf);
        IClasspathAttribute[] atts = conf.getAttributes();

        entry = JavaCore.newContainerEntry(path, null, atts, exported);

        if (project != null) {
            try {
                IvyClasspathContainerImpl ivycp = new IvyClasspathContainerImpl(project, path,
                        new IClasspathEntry[0], atts);
                JavaCore.setClasspathContainer(path, new IJavaProject[] {project},
                    new IClasspathContainer[] {ivycp}, null);
                ivycp.launchResolve(false, null);
            } catch (JavaModelException e) {
                IvyPlugin.log(e);
            }
        }

        if (conf.getJavaProject() != null && oldIvyFile != null
                && !oldIvyFile.equals(conf.getIvyXmlPath())) {
            // changing the ivy.xml, remove old marker on the old file, if any
            IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
            ivyMarkerManager.removeMarkers(conf.getJavaProject().getProject(), oldIvyFile);
        }

        return true;
    }

    public IClasspathEntry getSelection() {
        return entry;
    }

    public void setSelection(IClasspathEntry entry) {
        if (entry == null) {
            conf = new IvyClasspathContainerConfiguration(project, "ivy.xml", true);
        } else {
            conf = new IvyClasspathContainerConfiguration(project, entry.getPath(), true,
                    entry.getExtraAttributes());
            exported = entry.isExported();
        }
        state = new IvyClasspathContainerState(conf);
        oldIvyFile = conf.getIvyXmlPath();
        oldConfs = conf.getConfs();
    }

    public void setSelection(IFile ivyfile) {
        conf = new IvyClasspathContainerConfiguration(project, ivyfile.getProjectRelativePath()
                .toString(), true);
        // if there is an ivysettings.xml file at the root of the project, configure the container
        // to use it
        if (project != null) {
            IResource settings = project.getProject().findMember(new Path("ivysettings.xml"));
            if (settings != null) {
                conf.setSettingsProjectSpecific(true);
                SettingsSetup setup = new SettingsSetup();
                setup.setIvySettingsPath("ivysettings.xml");
                conf.setIvySettingsSetup(setup);
            }
        }
        state = new IvyClasspathContainerState(conf);
    }

    public void createControl(Composite parent) {
        setTitle("IvyDE Managed Libraries");
        setDescription("Choose Ivy file and its configurations.");

        tabs = new TabFolder(parent, SWT.BORDER);
        tabs.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        TabItem mainTab = new TabItem(tabs, SWT.NONE);
        mainTab.setText("Main");
        mainTab.setControl(createMainTab(tabs));

        IProject p = project == null ? null : project.getProject();
        settingsSetupTab = new SettingsSetupTab(tabs, p) {
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

        classpathSetupTab = new ClasspathSetupTab(tabs, p);

        mappingSetupTab = new MappingSetupTab(tabs, p);

        advancedSetupTab = new AdvancedSetupTab(tabs, p);

        tabs.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ivyFilePathText.updateErrorMarker();
                settingsSetupTab.getSettingsEditor().updateErrorMarker();
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
        configComposite.setLayout(new GridLayout(2, false));
        configComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        ivyFilePathText = new IvyFilePathText(configComposite, SWT.NONE, project == null ? null
                : project.getProject());
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
            public void confTableUpdated(List<String> confs) {
                checkCompleted();
            }
        });

        // refresh
        Button refreshConf = new Button(configComposite, SWT.NONE);
        refreshConf.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false, 2, 1));
        refreshConf.setText("Reload the list of configurations");
        refreshConf.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                ModuleDescriptor md;
                try {
                    md = state.getModuleDescriptor();
                } catch (IvyDEException e) {
                    md = null;
                    e.show(IStatus.ERROR, "Ivy configuration error",
                        "The configurations of the ivy.xml file could not be retrieved: ");
                }
                confTableViewer.setModuleDescriptor(md);
            }
        });

        return composite;
    }

    private void loadFromConf() {
        ivyFilePathText.init(conf.getIvyXmlPath());

        settingsSetupTab.init(conf.isSettingsProjectSpecific(), conf.getIvySettingsSetup());
        confTableViewer.init(conf.getConfs()); // *after* settingsSetupTab.init()!
        classpathSetupTab.init(conf.isClassthProjectSpecific(), conf.getClasspathSetup());
        mappingSetupTab.init(conf.isMappingProjectSpecific(), conf.getMappingSetup());
        // project == null <==> container in a launch config: always resolve before launch
        advancedSetupTab.init(conf.isAdvancedProjectSpecific(), conf.getAdvancedSetup(),
            project == null);

        settingsSetupTab.projectSpecificChanged();
        classpathSetupTab.projectSpecificChanged();
        mappingSetupTab.projectSpecificChanged();
        advancedSetupTab.projectSpecificChanged();
    }

    public void initialize(IJavaProject p, IClasspathEntry[] currentEntries) {
        this.project = p;
    }

}
