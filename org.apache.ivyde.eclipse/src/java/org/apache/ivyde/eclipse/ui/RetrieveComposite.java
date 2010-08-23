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

import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RetrieveComposite extends Composite {

    public static final String TOOLTIP_RETRIEVE_PATTERN = "Exemple: lib/[conf]/[artifact].[ext]\n"
            + "To copy artifacts in folder named lib without revision by folder"
            + " named like configurations";

    public static final String TOOLTIP_RETRIEVE_CONFS = "Comma separated list of configuration to"
            + " retrieve\nExemple: '*' or 'compile,test'";

    public static final String TOOLTIP_RETRIEVE_TYPES = "Comma separated list of types to retrieve"
            + "\nExemple: '*' or 'jar,source'";

    private Text retrievePatternText;

    private Button retrieveSyncButton;

    private Text confsText;

    private Text typesText;

    public RetrieveComposite(Composite parent, int style, boolean withConf) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        Label label = new Label(this, SWT.NONE);
        label.setText("Retrieve pattern:");

        retrievePatternText = new Text(this, SWT.SINGLE | SWT.BORDER);
        retrievePatternText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        retrievePatternText.setToolTipText(TOOLTIP_RETRIEVE_PATTERN);

        retrieveSyncButton = new Button(this, SWT.CHECK);
        retrieveSyncButton.setText("Delete old retrieved artifacts");
        retrieveSyncButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false,
                2, 1));

        if (withConf) {
            label = new Label(this, SWT.NONE);
            label.setText("Configurations:");

            confsText = new Text(this, SWT.SINGLE | SWT.BORDER);
            confsText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
            confsText.setToolTipText(TOOLTIP_RETRIEVE_CONFS);
        }

        label = new Label(this, SWT.NONE);
        label.setText("Types:");

        typesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        typesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        typesText.setToolTipText(TOOLTIP_RETRIEVE_TYPES);

    }

    public RetrieveSetup getRetrieveSetup() {
        RetrieveSetup setup = new RetrieveSetup();
        setup.setRetrieveSync(retrieveSyncButton.getSelection());
        setup.setRetrievePattern(retrievePatternText.getText());
        if (confsText != null) {
            setup.setRetrieveConfs(confsText.getText());
        }
        setup.setRetrieveTypes(typesText.getText());
        return setup;
    }

    public void init(RetrieveSetup setup) {
        retrievePatternText.setText(setup.getRetrievePattern());
        retrieveSyncButton.setSelection(setup.isRetrieveSync());
        if (confsText != null) {
            confsText.setText(setup.getRetrieveConfs());
        }
        typesText.setText(setup.getRetrieveTypes());
        setEnabled(true);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        retrievePatternText.setEnabled(enabled);
        retrieveSyncButton.setEnabled(enabled);
        if (confsText != null) {
            confsText.setEnabled(enabled);
        }
        typesText.setEnabled(enabled);
    }
}
