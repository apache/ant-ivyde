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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.StandaloneRetrieveSetup;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class IvyDEProjectPreferences extends PropertyPage implements IWorkbenchPropertyPage {

    private static final int NUM_COLUMNS = 3;

    private ListViewer listViewer;

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        final IProject project = (IProject) IvyPlugin.adapt(getElement(), IProject.class);

        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        label.setText("Retrieve list:");

        listViewer = new ListViewer(composite);
        listViewer.getList().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        listViewer.setContentProvider(ArrayContentProvider.getInstance());
        listViewer.setLabelProvider(new LabelProvider());

        Composite buttons = new Composite(composite, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        buttons.setLayout(new GridLayout(NUM_COLUMNS, false));

        Composite empty = new Composite(buttons, SWT.NONE);
        empty.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        Button addButton = new Button(buttons, SWT.PUSH);
        addButton.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, false));
        addButton.setText("Add...");
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                StandaloneRetrieveSetup setup = new StandaloneRetrieveSetup();
                EditStandaloneRetrieveDialog editDialog = new EditStandaloneRetrieveDialog(
                        getShell(), project, setup);
                if (editDialog.open() == Window.OK) {
                    List list = ((List) listViewer.getInput());
                    list.add(editDialog.getStandaloneRetrieveSetup());
                    listViewer.refresh();
                }
            }
        });

        final Button removeButton = new Button(buttons, SWT.PUSH);
        removeButton.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, false));
        removeButton.setText("Remove...");
        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean confirmed = MessageDialog.openConfirm(getShell(), "Delete retrieve setup",
                    "Do you really want to delete the selected retrieve configuration ?");
                if (confirmed) {
                    List list = ((List) listViewer.getInput());
                    Iterator it = ((IStructuredSelection) listViewer.getSelection()).iterator();
                    while (it.hasNext()) {
                        list.remove(it.next());
                    }
                    listViewer.refresh();
                }
            }
        });
        removeButton.setEnabled(false);

        listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                removeButton.setEnabled(!event.getSelection().isEmpty());
            }
        });

        listViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                StandaloneRetrieveSetup setup = (StandaloneRetrieveSetup) selection
                        .getFirstElement();
                EditStandaloneRetrieveDialog editDialog = new EditStandaloneRetrieveDialog(
                        getShell(), project, setup);
                if (editDialog.open() == Window.OK) {
                    List list = ((List) listViewer.getInput());
                    list.set(list.indexOf(setup), editDialog.getStandaloneRetrieveSetup());
                    listViewer.refresh();
                }
            }
        });

        List/* <StandaloneRetrieveSetup> */retrieveSetups;

        IScopeContext projectScope = new ProjectScope(project);
        IEclipsePreferences projectNode = projectScope.getNode(IvyPlugin.ID);
        String retrieveSetup = projectNode.get("StandaloneRetrieveSetup", null);
        if (retrieveSetup != null) {
            StandaloneRetrieveSerializer serializer = new StandaloneRetrieveSerializer();
            ByteArrayInputStream in = new ByteArrayInputStream(retrieveSetup.getBytes());
            try {
                try {
                    retrieveSetups = serializer.read(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // we don't care
                }
            }
        } else {
            retrieveSetups = new ArrayList();
        }

        listViewer.setInput(retrieveSetups);

        return composite;
    }

    public boolean performOk() {
        final IProject project = (IProject) IvyPlugin.adapt(getElement(), IProject.class);

        List/* <StandaloneRetrieveSetup> */retrieveSetups = (List) listViewer.getInput();

        StandaloneRetrieveSerializer serializer = new StandaloneRetrieveSerializer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            serializer.write(out, retrieveSetups);
        } catch (IOException e) {
            IvyPlugin.log(IStatus.ERROR, "Enable to write the retrieve setup", e);
            return false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // we don't care
            }
        }

        String retrieveSetup = new String(out.toByteArray());

        IScopeContext projectScope = new ProjectScope(project);
        IEclipsePreferences projectNode = projectScope.getNode(IvyPlugin.ID);
        projectNode.put("StandaloneRetrieveSetup", retrieveSetup);

        return true;
    }
}
