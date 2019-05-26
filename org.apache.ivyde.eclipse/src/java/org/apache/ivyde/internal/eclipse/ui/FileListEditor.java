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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class FileListEditor extends Composite {

    private final ListViewer filelist;

    private List<String> files = new ArrayList<>();

    private Button edit;

    private final Button add;

    private Button remove;

    private final Button up;

    private final Button down;

    public FileListEditor(Composite parent, int style,  String label, final String labelPopup,
            final IProject project, final String defaultExtension) {
        super(parent, style);
        setLayout(new GridLayout(3, false));

        Label l = new Label(this, SWT.NONE);
        l.setText(label);
        l.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false));

        filelist = new ListViewer(this, SWT.BORDER);
        filelist.getList().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        filelist.setContentProvider(ArrayContentProvider.getInstance());
        filelist.setLabelProvider(new LabelProvider());
        filelist.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                edit.setEnabled(!event.getSelection().isEmpty());
                remove.setEnabled(!event.getSelection().isEmpty());
                updateUpDownEnableButtons(true);
            }
        });

        Composite buttons = new Composite(this, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
        // CheckStyle:MagicNumber| OFF
        GridLayout layout = new GridLayout(1, false);
        // CheckStyle:MagicNumber| ON
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttons.setLayout(layout);

        edit = new Button(buttons, SWT.PUSH);
        edit.setText("Edit");
        edit.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        edit.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PathEditorDialog dialog = new PathEditorDialog(getShell(), labelPopup, project,
                        defaultExtension);
                String selection = (String) ((IStructuredSelection) filelist.getSelection()).getFirstElement();
                dialog.init(selection);
                if (dialog.open() == Window.OK) {
                    int i = getSelectedConfigurationIndex(selection);
                    files.set(i, dialog.getFile());
                    filelist.refresh();
                    fileListUpdated();
                }
            }
        });

        add = new Button(buttons, SWT.PUSH);
        add.setText("Add");
        add.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        add.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PathEditorDialog dialog = new PathEditorDialog(getShell(), labelPopup, project,
                        defaultExtension);
                if (dialog.open() == Window.OK) {
                    files.add(dialog.getFile());
                    filelist.refresh();
                    fileListUpdated();
                }
            }
        });

        remove = new Button(buttons, SWT.PUSH);
        remove.setText("Remove");
        remove.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        remove.addSelectionListener(new SelectionAdapter() {
            @SuppressWarnings("unchecked")
            public void widgetSelected(SelectionEvent e) {
                List<String> selection = ((IStructuredSelection) filelist.getSelection()).toList();
                files.removeAll(selection);
                filelist.refresh();
                fileListUpdated();
                remove.setEnabled(false);
                edit.setEnabled(false);
            }
        });

        up = new Button(buttons, SWT.PUSH);
        up.setText("Up");
        up.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        up.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int i = getSelectedConfigurationIndex();
                String f = files.get(i);
                files.set(i, files.get(i - 1));
                files.set(i - 1, f);
                filelist.refresh();
                fileListUpdated();
                updateUpDownEnableButtons(true);
            }
        });

        down = new Button(buttons, SWT.PUSH);
        down.setText("Down");
        down.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        down.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int i = getSelectedConfigurationIndex();
                String f = files.get(i);
                files.set(i, files.get(i + 1));
                files.set(i + 1, f);
                filelist.refresh();
                fileListUpdated();
                updateUpDownEnableButtons(true);
            }
        });
    }

    private int getSelectedConfigurationIndex() {
        IStructuredSelection selection = (IStructuredSelection) filelist.getSelection();
        String file = (String) selection.getFirstElement();
        return getSelectedConfigurationIndex(file);
    }

    private int getSelectedConfigurationIndex(String file) {
        return files.indexOf(file);
    }

    private void updateUpDownEnableButtons(boolean enabled) {
        boolean selected = filelist.getList().getSelectionCount() != 0;
        int i = getSelectedConfigurationIndex();
        up.setEnabled(enabled && selected && i > 0);
        down.setEnabled(enabled && selected && i < files.size() - 1);
    }

    public void init(List<String> files) {
        this.files = files;
        filelist.setInput(files);
        remove.setEnabled(false);
        edit.setEnabled(false);
    }

    protected void fileListUpdated() {
        // to be override to listen to changes
    }

    public List<String> getFiles() {
        return files;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        filelist.getList().setEnabled(enabled);
        edit.setEnabled(enabled && !filelist.getSelection().isEmpty());
        remove.setEnabled(enabled && !filelist.getSelection().isEmpty());
        add.setEnabled(enabled);
        updateUpDownEnableButtons(enabled);
    }

}
