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

import org.apache.ivyde.eclipse.cp.ClasspathSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ClasspathSetupEditor extends Composite {

    private static final int INDENT_BUTTONS = 0;

    private static final int INDENT_RETRIEVE = 60;

    public static final String TOOLTIP_ACCEPTED_TYPES = "Comma separated list of artifact types to add to the classpath.\n"
            + "Example: jar, zip";

    private final Button resolveInWorkspaceCheck;

    private final Button transitiveResolveCheck;

    private final Label alphaOrderLabel;

    private final Combo alphaOrderCheck;

    private final Button selectCache;

    private final Button selectRetrieve;

    private final RetrieveComposite retrieveComposite;

    private final Label acceptedTypesLabel;

    private final Text acceptedTypesText;

    private final Button readOSGiMetadataCheck;

    private final boolean osgiAvailable;

    public ClasspathSetupEditor(Composite parent, int style, IProject project) {
        super(parent, style);
        setLayout(new GridLayout(2, false));

        osgiAvailable = IvyPlugin.getDefault().isOsgiAvailable();

        resolveInWorkspaceCheck = new Button(this, SWT.CHECK);
        resolveInWorkspaceCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 2, 1));
        resolveInWorkspaceCheck.setText("Resolve dependencies in workspace");
        resolveInWorkspaceCheck
                .setToolTipText("Will replace jars on the classpath with workspace projects");

        transitiveResolveCheck = new Button(this, SWT.CHECK);
        transitiveResolveCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 2, 1));
        transitiveResolveCheck.setText("Resolve dependencies transitively");
        transitiveResolveCheck
                .setToolTipText("If unchecked, will resolve dependency with transitivity disabled regardless of the settings in the ivy.xml file");

        readOSGiMetadataCheck = new Button(this, SWT.CHECK);
        readOSGiMetadataCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false,
                2, 1));
        readOSGiMetadataCheck.setText("Read and setup OSGi visibility metadata");
        readOSGiMetadataCheck
                .setToolTipText("Will setup access rules regarding OSGi metadata like Export-Package");
        readOSGiMetadataCheck.setEnabled(osgiAvailable);

        acceptedTypesLabel = new Label(this, SWT.NONE);
        acceptedTypesLabel.setText("Accepted types:");

        acceptedTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        acceptedTypesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        acceptedTypesText.setToolTipText(TOOLTIP_ACCEPTED_TYPES);

        alphaOrderLabel = new Label(this, SWT.NONE);
        alphaOrderLabel.setText("Order of the classpath entries:");

        alphaOrderCheck = new Combo(this, SWT.READ_ONLY);
        alphaOrderCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        alphaOrderCheck.setToolTipText("Order of the artifacts in the classpath container");
        alphaOrderCheck.add("From the ivy.xml");
        alphaOrderCheck.add("Lexical");

        Label label = new Label(this, SWT.NONE);
        label.setText("Build the classpath with:");
        label.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        Composite buttons = new Composite(this, SWT.NONE);
        buttons.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        gridData.horizontalIndent = INDENT_BUTTONS;
        buttons.setLayoutData(gridData);

        selectCache = new Button(buttons, SWT.RADIO);
        selectCache.setText("Ivy's cache");

        selectRetrieve = new Button(buttons, SWT.RADIO);
        selectRetrieve.setText("retrieved artifacts");

        retrieveComposite = new RetrieveComposite(this, SWT.NONE, false, project);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gridData.horizontalIndent = INDENT_RETRIEVE;
        retrieveComposite.setLayoutData(gridData);

        selectRetrieve.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                retrieveComposite.setEnabled(selectRetrieve.getSelection());
            }
        });
    }

    public void init(ClasspathSetup setup) {
        resolveInWorkspaceCheck.setSelection(setup.isResolveInWorkspace());
        transitiveResolveCheck.setSelection(setup.isTransitiveResolve());
        readOSGiMetadataCheck.setSelection(setup.isReadOSGiMetadata());
        acceptedTypesText.setText(IvyClasspathUtil.concat(setup.getAcceptedTypes()));
        alphaOrderCheck.select(setup.isAlphaOrder() ? 1 : 0);
        selectCache.setSelection(!setup.isRetrievedClasspath());
        selectRetrieve.setSelection(setup.isRetrievedClasspath());
        retrieveComposite.init(setup.getRetrieveSetup());
        retrieveComposite.setEnabled(setup.isRetrievedClasspath());
    }

    public ClasspathSetup getClasspathSetup() {
        ClasspathSetup setup = new ClasspathSetup();
        setup.setResolveInWorkspace(resolveInWorkspaceCheck.getSelection());
        setup.setTransitiveResolve(transitiveResolveCheck.getSelection());
        setup.setReadOSGiMetadata(readOSGiMetadataCheck.getSelection());
        setup.setAcceptedTypes(IvyClasspathUtil.split(acceptedTypesText.getText()));
        setup.setAlphaOrder(alphaOrderCheck.getSelectionIndex() == 1);
        setup.setRetrievedClasspath(selectRetrieve.getSelection());
        if (setup.isRetrievedClasspath()) {
            setup.setRetrieveSetup(retrieveComposite.getRetrieveSetup());
        }
        return setup;
    }

    public void setEnabled(boolean enabled) {
        resolveInWorkspaceCheck.setEnabled(enabled);
        transitiveResolveCheck.setEnabled(enabled);
        readOSGiMetadataCheck.setEnabled(osgiAvailable && enabled);
        acceptedTypesLabel.setEnabled(enabled);
        acceptedTypesText.setEnabled(enabled);
        alphaOrderLabel.setEnabled(enabled);
        alphaOrderCheck.setEnabled(enabled);
        selectCache.setEnabled(enabled);
        selectRetrieve.setEnabled(enabled);
        retrieveComposite.setEnabled(selectRetrieve.getSelection() && enabled);
        super.setEnabled(enabled);
    }
}
