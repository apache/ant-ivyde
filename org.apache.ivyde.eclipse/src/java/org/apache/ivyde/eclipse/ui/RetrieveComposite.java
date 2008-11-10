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
package org.apache.ivyde.eclipse.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RetrieveComposite extends Composite {

    private Button doRetrieveButton;

    private Text retrievePatternText;

    private Button retrieveSyncButton;

    public RetrieveComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        doRetrieveButton = new Button(this, SWT.CHECK);
        doRetrieveButton.setText("Do retrieve after resolve");
        doRetrieveButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2,
                1));

        Label label = new Label(this, SWT.NONE);
        label.setText("Retrieve pattern:");

        retrievePatternText = new Text(this, SWT.SINGLE | SWT.BORDER);
        retrievePatternText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        retrievePatternText.setEnabled(doRetrieveButton.getSelection());
        retrievePatternText.setToolTipText("Example: lib/[conf]/[artifact].[ext]\n"
                + "To copy artifacts in folder named lib without revision by folder"
                + " named like configurations");

        retrieveSyncButton = new Button(this, SWT.CHECK);
        retrieveSyncButton.setText("Delete old retrieved artifacts");
        retrieveSyncButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false,
                2, 1));
        retrieveSyncButton.setEnabled(doRetrieveButton.getSelection());

        doRetrieveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                retrievePatternText.setEnabled(doRetrieveButton.getSelection());
                retrieveSyncButton.setEnabled(doRetrieveButton.getSelection());
            }
        });
    }

    public boolean isRetrieveEnabled() {
        return doRetrieveButton.getSelection();
    }

    public boolean isSyncEnabled() {
        return retrieveSyncButton.getSelection();
    }

    public String getRetrievePattern() {
        return retrievePatternText.getText();
    }

    public void init(boolean doRetrieve, String retrievePattern, boolean retrieveSync) {
        doRetrieveButton.setSelection(doRetrieve);
        retrievePatternText.setText(retrievePattern);
        retrieveSyncButton.setSelection(retrieveSync);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        doRetrieveButton.setEnabled(enabled);
        retrievePatternText.setEnabled(enabled);
        retrieveSyncButton.setEnabled(enabled);
    }
}
