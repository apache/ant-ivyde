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
package org.apache.ivyde.internal.eclipse.ui.preferences;

import java.io.IOException;
import java.util.List;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.internal.eclipse.retrieve.StandaloneRetrieveSetup;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class IvyDEProjectPreferences extends PropertyPage implements IWorkbenchPropertyPage {

    private TableViewer table;

    private final RetrieveSetupManager retrieveSetupManager;

    public IvyDEProjectPreferences() {
        noDefaultAndApplyButton();
        retrieveSetupManager = IvyPlugin.getDefault().getRetrieveSetupManager();
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        final IProject project = IvyPlugin.adapt(getElement(), IProject.class);

        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 0));
        label.setText("Retrieve list:");

        table = new TableViewer(composite);
        table.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setContentProvider(ArrayContentProvider.getInstance());
        table.setLabelProvider(new RetrieveTableLabelProvider());
        table.getTable().setHeaderVisible(true);
        TableColumn col1 = new TableColumn(table.getTable(), SWT.NONE);
        col1.setText("Name");
        // CheckStyle:MagicNumber| OFF
        col1.setWidth(100);
        TableColumn col2 = new TableColumn(table.getTable(), SWT.NONE);
        col2.setText("Pattern");
        col2.setWidth(200);
        TableColumn col3 = new TableColumn(table.getTable(), SWT.NONE);
        col3.setText("Confs");
        col3.setWidth(50);
        TableColumn col4 = new TableColumn(table.getTable(), SWT.NONE);
        col4.setText("Types");
        col4.setWidth(50);
        // CheckStyle:MagicNumber| ON
        table.setColumnProperties(new String[] {"Name", "Pattern", "Confs", "Types"});

        Composite buttons = new Composite(composite, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        buttons.setLayout(new GridLayout(1, false));

        Button newButton = new Button(buttons, SWT.PUSH);
        newButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        newButton.setText("New...");
        newButton.addSelectionListener(new SelectionAdapter() {
            @SuppressWarnings("unchecked")
            public void widgetSelected(SelectionEvent e) {
                StandaloneRetrieveSetup setup = new StandaloneRetrieveSetup();
                EditStandaloneRetrieveDialog editDialog = new EditStandaloneRetrieveDialog(
                        getShell(), project, setup);
                if (editDialog.open() == Window.OK) {
                    List<Object> list = (List<Object>) table.getInput();
                    list.add(editDialog.getStandaloneRetrieveSetup());
                    table.refresh();
                }
            }
        });

        Button editButton = new Button(buttons, SWT.PUSH);
        editButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        editButton.setText("Edit...");
        editButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                openEdit((IStructuredSelection) table.getSelection(), project);
            }
        });

        final Button removeButton = new Button(buttons, SWT.PUSH);
        removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        removeButton.setText("Remove");
        removeButton.addSelectionListener(new SelectionAdapter() {
            @SuppressWarnings("unchecked")
            public void widgetSelected(SelectionEvent e) {
                List<Object> list = (List<Object>) table.getInput();
                list.removeAll(((IStructuredSelection) table.getSelection()).toList());
                table.refresh();
            }
        });
        removeButton.setEnabled(false);

        table.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                removeButton.setEnabled(!event.getSelection().isEmpty());
            }
        });

        table.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                openEdit((IStructuredSelection) event.getSelection(), project);
            }
        });

        List<StandaloneRetrieveSetup> retrieveSetups;
        try {
            retrieveSetups = retrieveSetupManager.getSetup(project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        table.setInput(retrieveSetups);

        return composite;
    }

    @SuppressWarnings("unchecked")
    private void openEdit(IStructuredSelection selection, IProject project) {
        StandaloneRetrieveSetup setup = (StandaloneRetrieveSetup) selection.getFirstElement();
        EditStandaloneRetrieveDialog editDialog = new EditStandaloneRetrieveDialog(getShell(),
                project, setup);
        if (editDialog.open() == Window.OK) {
            List<Object> list = (List<Object>) table.getInput();
            list.set(list.indexOf(setup), editDialog.getStandaloneRetrieveSetup());
            table.refresh();
        }
    }

    private class RetrieveTableLabelProvider extends BaseLabelProvider implements
            ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            StandaloneRetrieveSetup setup = (StandaloneRetrieveSetup) element;
            // CheckStyle:MagicNumber| OFF
            switch (columnIndex) {
                case 0:
                    return setup.getName();
                case 1:
                    return setup.getRetrieveSetup().getRetrievePattern();
                case 2:
                    return setup.getRetrieveSetup().getRetrieveConfs();
                case 3:
                    return setup.getRetrieveSetup().getRetrieveTypes();
            }
            // CheckStyle:MagicNumber| ON
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public boolean performOk() {
        final IProject project = IvyPlugin.adapt(getElement(), IProject.class);

        List<StandaloneRetrieveSetup> retrieveSetups = (List<StandaloneRetrieveSetup>) table.getInput();

        try {
            retrieveSetupManager.save(project, retrieveSetups);
        } catch (Exception e) {
            IvyPlugin.logError("Enable to write the retrieve setup into the project preference of "
                    + project.getName(), e);
            return false;
        }

        return true;
    }
}
