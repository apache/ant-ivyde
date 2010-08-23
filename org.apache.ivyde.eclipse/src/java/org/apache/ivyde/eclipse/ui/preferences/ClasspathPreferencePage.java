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

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.AcceptedSuffixesTypesComposite;
import org.apache.ivyde.eclipse.ui.ClasspathTypeComposite;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ClasspathPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /** the ID of the preference page */
    public static final String PEREFERENCE_PAGE_ID = "org.apache.ivyde.eclipse.ui.preferences.ClasspathPreferencePage";

    private Button resolveInWorkspaceCheck;

    private Button resolveBeforeLaunchCheck;

    private Combo alphaOrderCheck;

    private AcceptedSuffixesTypesComposite acceptedSuffixesTypesComposite;

    private ClasspathTypeComposite classpathTypeComposite;

    public ClasspathPreferencePage() {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        // CheckStyle:MagicNumber| OFF
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        resolveInWorkspaceCheck = new Button(composite, SWT.CHECK);
        resolveInWorkspaceCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 3, 1));
        resolveInWorkspaceCheck.setText("Resolve dependencies in workspace");
        resolveInWorkspaceCheck
                .setToolTipText("Will replace jars on the classpath with workspace projects");

        resolveBeforeLaunchCheck = new Button(composite, SWT.CHECK);
        resolveBeforeLaunchCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 3, 1));
        resolveBeforeLaunchCheck.setText("Resolve before launch");
        resolveBeforeLaunchCheck
                .setToolTipText("Trigger a resolve before each run of any kind of java launch configuration");

        Label label = new Label(composite, SWT.NONE);
        label.setText("Order of the classpath entries:");

        alphaOrderCheck = new Combo(composite, SWT.READ_ONLY);
        alphaOrderCheck
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        alphaOrderCheck.setToolTipText("Order of the artifacts in the classpath container");
        alphaOrderCheck.add("From the ivy.xml");
        alphaOrderCheck.add("Lexical");

        acceptedSuffixesTypesComposite = new AcceptedSuffixesTypesComposite(composite, SWT.NONE);
        acceptedSuffixesTypesComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
                true, false, 3, 1));

        classpathTypeComposite = new ClasspathTypeComposite(composite, SWT.NONE);
        classpathTypeComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 3, 1));

        // CheckStyle:MagicNumber| ON

        initPreferences();

        return composite;
    }

    private void initPreferences() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
        resolveInWorkspaceCheck.setSelection(helper.isResolveInWorkspace());
        resolveBeforeLaunchCheck.setSelection(helper.isResolveBeforeLaunch());
        alphaOrderCheck.select(helper.isAlphOrder() ? 1 : 0);
        acceptedSuffixesTypesComposite.init(helper.getContainerMappingSetup());
        classpathTypeComposite.init(helper.isRetrievedClasspath(), helper
                .getRetrievedClasspathSetup());
    }

    public boolean performOk() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
        helper.setResolveInWorkspace(resolveInWorkspaceCheck.getSelection());
        helper.setResolveBeforeLaunch(resolveBeforeLaunchCheck.getSelection());
        helper.setAlphOrder(alphaOrderCheck.getSelectionIndex() == 1);
        helper.setContainerMappingSetup(acceptedSuffixesTypesComposite.getContainerMappingSetup());
        helper.setRetrievedClasspath(classpathTypeComposite.isRetrievedClasspath());
        helper.setRetrievedClasspathSetup(classpathTypeComposite.getRetrieveSetup());
        return true;
    }

    protected void performDefaults() {
        resolveInWorkspaceCheck.setSelection(PreferenceInitializer.DEFAULT_RESOLVE_IN_WORKSPACE);
        resolveBeforeLaunchCheck.setSelection(PreferenceInitializer.DEFAULT_RESOLVE_BEFORE_LAUNCH);
        alphaOrderCheck.select(PreferenceInitializer.DEFAULT_ALPHABETICAL_ORDER ? 1 : 0);
        acceptedSuffixesTypesComposite.init(PreferenceInitializer.DEFAULT_CONTAINER_MAPPING_SETUP);
        classpathTypeComposite.init(PreferenceInitializer.DEFAULT_RETRIEVED_CLASSPATH,
            PreferenceInitializer.DEFAULT_RETRIEVED_CLASSPATH_SETUP);
    }
}
