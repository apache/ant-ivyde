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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class PathEditor extends Composite {

    private final Text text;

    private final Button variableButton;

    private final Button browseFileSystem;

    private final Button browseWorkspace;

    private final IProject project;

    private final String defaultExtension;

    private Button browseProject;

    public PathEditor(Composite parent, int style, String label, IProject project, String defaultExtension) {
        super(parent, style);
        this.project = project;
        this.defaultExtension = defaultExtension;

        GridLayout layout = new GridLayout(2, false);
        setLayout(layout);

        Label l = new Label(this, SWT.NONE);
        l.setText(label);

        text = createText(this);
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                textUpdated();
            }
        });

        Composite buttons = new Composite(this, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 2, 1));
        // CheckStyle:MagicNumber| OFF
        layout = new GridLayout(project == null ? 4 : 5, false);
        // CheckStyle:MagicNumber| ON
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttons.setLayout(layout);

        boolean added = addButtons(buttons);

        if (project != null) {
            browseProject = new Button(buttons, SWT.NONE);
            browseProject.setLayoutData(new GridData(GridData.END, GridData.CENTER, !added, false));
            browseProject.setText("Project...");
            browseProject.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectInProject();
                }
            });
        }

        browseWorkspace = new Button(buttons, SWT.NONE);
        browseWorkspace.setLayoutData(new GridData(project == null ? GridData.END : GridData.CENTER, GridData.CENTER,
                !added && project == null, false));
        browseWorkspace.setText("Workspace...");
        browseWorkspace.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                selectInWorkspace();
            }
        });

        browseFileSystem = new Button(buttons, SWT.NONE);
        browseFileSystem
                .setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
        browseFileSystem.setText("File System...");
        browseFileSystem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                selectInFileSystem();
            }
        });

        variableButton = new Button(buttons, SWT.NONE);
        variableButton.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
        variableButton.setText("Variables...");
        variableButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                selectVariable();
            }
        });
    }

    private void selectInProject() {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
        dialog.setTitle("Select a file in the project:");
        dialog.setMessage("Select a file in the project:");
        // Filter to the project
        dialog.addFilter(new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IProject) {
                    return ((IProject) element).getName().equals(project.getName());
                }
                // we want a folder
                return defaultExtension != null || element instanceof IContainer;
            }
        });
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        // TODO try to preselect the current file
        dialog.open();
        Object[] results = dialog.getResult();
        if ((results != null) && (results.length > 0) && (results[0] instanceof IResource)) {
            IPath path = ((IResource) results[0]).getFullPath();
            setProjectLoc(path.removeFirstSegments(1).makeRelative().toString());
        }
    }

    private void selectInWorkspace() {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
        dialog.setTitle("Select a file in the workspace:");
        dialog.setMessage("Select a file in the workspace:");
        // Filter closed projects
        dialog.addFilter(new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IProject) {
                    return ((IProject) element).isAccessible();
                }
                // we want a folder
                return defaultExtension != null || element instanceof IContainer;
            }
        });
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        // TODO try to preselect the current file
        dialog.open();
        Object[] results = dialog.getResult();
        if (results != null && results.length > 0 && results[0] instanceof IResource) {
            IPath path = ((IResource) results[0]).getFullPath();
            if (project != null && path.segment(0).equals(project.getProject().getName())) {
                setProjectLoc(path.removeFirstSegments(1).makeRelative().toString());
            } else {
                String containerName = path.makeRelative().toString();
                setWorkspaceLoc("${workspace_loc:" + containerName + "}");
            }
        }
    }

    private void selectInFileSystem() {
        String file;
        if (defaultExtension == null) {
            // we want a folder
            DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
            file = dialog.open();
        } else {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            if (text != null) {
                dialog.setFileName(text.getText());
            }
            dialog.setFilterExtensions(new String[] {defaultExtension, "*"});
            file = dialog.open();
        }
        if (file != null) {
            setFile(file);
        }
    }

    private void selectVariable() {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if (variable != null) {
            addVariable(variable);
        }
    }

    protected void addVariable(String variable) {
        text.insert(variable);
        textUpdated();
    }

    protected void setFile(String file) {
        text.setText(file);
        textUpdated();
    }

    protected void setProjectLoc(String projectLoc) {
        text.setText(projectLoc);
        textUpdated();
    }

    protected void setWorkspaceLoc(String workspaceLoc) {
        text.setText(workspaceLoc);
        textUpdated();
    }

    protected void textUpdated() {
        // nothing to do
    }

    protected Text createText(Composite parent) {
        Text t = new Text(parent, SWT.BORDER);
        t.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        return t;
    }

    protected boolean addButtons(Composite buttons) {
        return false;
    }

    public Text getText() {
        return text;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEnabled(enabled);
        if (browseProject != null) {
            browseProject.setEnabled(enabled);
        }
        browseFileSystem.setEnabled(enabled);
        browseWorkspace.setEnabled(enabled);
        variableButton.setEnabled(enabled);

    }
}
