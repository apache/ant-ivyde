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

import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ClasspathTypeComposite extends Composite {

    private static final int INDENT_BUTTONS = 0;

    private static final int INDENT_RETRIEVE = 60;

    private final Button selectRetrieve;

    private final RetrieveComposite retrieveComposite;

    private final Button selectCache;

    public ClasspathTypeComposite(Composite parent, int style, IProject project) {
        super(parent, style);
        setLayout(new GridLayout(1, false));

        Label label = new Label(this, SWT.NONE);
        label.setText("Build the classpath with:");

        Composite buttons = new Composite(this, SWT.NONE);
        buttons.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalIndent = INDENT_BUTTONS;
        buttons.setLayoutData(gridData);

        selectCache = new Button(buttons, SWT.RADIO);
        selectCache.setText("Ivy's cache");

        selectRetrieve = new Button(buttons, SWT.RADIO);
        selectRetrieve.setText("retrieved artifacts");

        retrieveComposite = new RetrieveComposite(this, SWT.NONE, false, project);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalIndent = INDENT_RETRIEVE;
        retrieveComposite.setLayoutData(gridData);

        selectRetrieve.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                retrieveComposite.setEnabled(selectRetrieve.getSelection());
            }
        });
    }

    public void init(boolean retrievedClasspath, RetrieveSetup retrieveSetup) {
        selectCache.setSelection(!retrievedClasspath);
        selectRetrieve.setSelection(retrievedClasspath);
        retrieveComposite.init(retrieveSetup);
        retrieveComposite.setEnabled(retrievedClasspath);
    }

    public boolean isRetrievedClasspath() {
        return selectRetrieve.getSelection();
    }

    public RetrieveSetup getRetrieveSetup() {
        return retrieveComposite.getRetrieveSetup();
    }

    public void setEnabled(boolean enabled) {
        selectCache.setEnabled(enabled);
        selectRetrieve.setEnabled(enabled);
        retrieveComposite.setEnabled(enabled);
        super.setEnabled(enabled);
    }
}
