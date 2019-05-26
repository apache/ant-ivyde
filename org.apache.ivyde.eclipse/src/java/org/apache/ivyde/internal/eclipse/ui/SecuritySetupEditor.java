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

import java.util.List;

import org.apache.ivyde.eclipse.GUIfactoryHelper;
import org.apache.ivyde.eclipse.cp.SecuritySetup;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;

public class SecuritySetupEditor extends Composite {

    private final TableViewer tableViewer;
    private final Group credentialsGroup;
    private final Button addBtn;
    private final Button editBtn;
    private final Button deleteBtn;
    private final Table table;
    public SecuritySetupEditor(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout());

        credentialsGroup = new Group(this, style);
        credentialsGroup.setText("Credentials");
        credentialsGroup.setLayout(new GridLayout(2, false));
        credentialsGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

        tableViewer = new TableViewer(credentialsGroup,
                SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

        GUIfactoryHelper.buildTableColumn(tableViewer, 100, "Host", GUIfactoryHelper.buildHostLabelProvider());
        GUIfactoryHelper.buildTableColumn(tableViewer, 175, "Realm", GUIfactoryHelper.buildRealmLabelProvider());
        GUIfactoryHelper.buildTableColumn(tableViewer, 100, "Username", GUIfactoryHelper.buildUsernameLabelProvider());
        GUIfactoryHelper.buildTableColumn(tableViewer, 100, "Pwd", GUIfactoryHelper.buildPwdLabelProvider());

        // make lines and header visible
        table = tableViewer.getTable();
        table.setParent(credentialsGroup);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        GridData tableGD = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 3);
        tableGD.heightHint = 200;
        table.setLayoutData(tableGD);

        addBtn = new Button(credentialsGroup, SWT.PUSH);
        addBtn.setText("Add...");
        addBtn.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));

        editBtn = new Button(credentialsGroup, SWT.PUSH);
        editBtn.setText("Edit...");
        editBtn.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));
        editBtn.setEnabled(false);

        deleteBtn = new Button(credentialsGroup, SWT.PUSH);
        deleteBtn.setText("Remove");
        deleteBtn.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));
        deleteBtn.setEnabled(false);
    }

    public void init(List<SecuritySetup> setup) {
       this.tableViewer.setContentProvider(ArrayContentProvider.getInstance());
       this.tableViewer.setInput(setup);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        credentialsGroup.setEnabled(enabled);
        addBtn.setEnabled(enabled);
        editBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
        table.setEnabled(enabled);
    }

    /**
     * @return the addBtn
     */
    public Button getAddBtn() {
        return addBtn;
    }

    /**
     * @return the editBtn
     */
    public Button getEditBtn() {
        return editBtn;
    }

    /**
     * @return the deleteBtn
     */
    public Button getDeleteBtn() {
        return deleteBtn;
    }

    /**
     * @return the tableViewer
     */
    public TableViewer getTableViewer() {
        return tableViewer;
    }

}
