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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class PathEditorDialog extends Dialog {

    private final String label;

    private final IProject project;

    private final String defaultExtension;

    private PathEditor editor;

    private String file;

    private String path;

    protected PathEditorDialog(Shell parentShell, String label, IProject project,
            String defaultExtension) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        this.label = label;
        this.project = project;
        this.defaultExtension = defaultExtension;
    }

    protected Control createDialogArea(Composite parent) {
        Control dialogArea = super.createDialogArea(parent);
        editor = new PathEditor((Composite) dialogArea, SWT.NONE, label, project, defaultExtension);
        if (path != null) {
            editor.getText().setText(path);
        }
        GridData layoutData = new GridData(GridData.FILL, GridData.FILL, true, true);
        layoutData.widthHint = 500;
        editor.setLayoutData(layoutData);
        return dialogArea;
    }

    protected void okPressed() {
        file = editor.getText().getText();
        super.okPressed();
    }

    public void init(String path) {
        this.path = path;
    }

    public String getFile() {
        return file;
    }
}
