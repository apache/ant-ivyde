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

import java.io.File;
import java.net.MalformedURLException;

import org.apache.ivy.Ivy;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we can use the field support built into
 * JFace that allows us to create a page that is small and knows how to save, restore and apply
 * itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that
 * belongs to the main plug-in class. That way, preferences can be accessed directly via the
 * preference store.
 */

public class IvyPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    /** the ID of the preference page */
    public static final String PEREFERENCE_PAGE_ID = "org.apache.ivyde.eclipse.ui.preferences.IvyPreferencePage";

    private StringFieldEditor _pattern;

    private BooleanFieldEditor _retreiveSync;

    public IvyPreferencePage() {
        super(GRID);
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
        setDescription("");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
     * manipulate various types of preferences. Each field editor knows how to save and restore
     * itself.
     */
    public void createFieldEditors() {
        final Composite fieldParent = getFieldEditorParent();

        Label info = new Label(fieldParent, SWT.NONE);
        info.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3, 1));
        info.setText("Apache Ivy version " + Ivy.getIvyVersion() + " - " + Ivy.getIvyDate());
        new Label(fieldParent, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 3, 1)); // space

        Label spacer = new Label(fieldParent, SWT.NONE);
        GridData spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacer.setLayoutData(spacerData);
        spacer.setText("Runtime option");
        spacer = new Label(fieldParent, SWT.SEPARATOR | SWT.HORIZONTAL);
        spacer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));
        addField(new FileFieldEditor(PreferenceConstants.IVYSETTINGS_PATH, "&Ivy settings URL:",
                fieldParent) {
            /* Opens the file chooser dialog and returns the selected file as an url. */
            protected String changePressed() {
                String f = super.changePressed();
                if (f == null) {
                    return null;
                }
                File d = new File(f);
                try {
                    return d.toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    // should never happen
                    IvyPlugin.log(IStatus.ERROR, "A file from the file chooser is not an URL", e);
                    return null;
                }
            }

            protected boolean checkState() {
                return true;
            }
        });

        new Label(fieldParent, SWT.NONE); // space
        Label explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation.setText("The url where your ivyconf file can be found. \n"
                + "Leave empty to reference the default ivy configuration.");
        new Label(fieldParent, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 3, 1)); // space

        BooleanFieldEditor doR = new BooleanFieldEditor(PreferenceConstants.DO_RETRIEVE,
                "Do a retrieve after resolve", fieldParent) {
            protected void createControl(final Composite parent) {
                super.createControl(parent);
                final Button b = getChangeControl(parent);
                b.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        _pattern.setEnabled(b.getSelection(), parent);
                        _retreiveSync.setEnabled(b.getSelection(), parent);
                    }
                });
            }
        };
        _pattern = new StringFieldEditor(PreferenceConstants.RETRIEVE_PATTERN, "Pattern",
                fieldParent);
        _pattern.setEnabled(getPreferenceStore().getBoolean(PreferenceConstants.DO_RETRIEVE),
            fieldParent);
        _retreiveSync = new BooleanFieldEditor(PreferenceConstants.RETRIEVE_SYNC,
                "Delete old retrieved artifacts", fieldParent);
        addField(doR);
        addField(_pattern);
        addField(_retreiveSync);

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Pattern example: lib/[conf]/[artifact].[ext]\n"
                        + "To copy artifacts in folder named lib without revision by folder named like configurations");
        new Label(fieldParent, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 3, 1)); // space

        addField(new StringFieldEditor(PreferenceConstants.ACCEPTED_TYPES, "Accepted types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Comma separated list of artifact types to use in IvyDE Managed Dependencies Library\n"
                        + "Example: jar, zip");

        addField(new StringFieldEditor(PreferenceConstants.SOURCES_TYPES, "Sources types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation.setText("Comma separated list of artifact types to be used as sources. \n"
                + "Example: source, src");

        addField(new StringFieldEditor(PreferenceConstants.SOURCES_SUFFIXES, "Sources suffixes",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation.setText("Comma separated list of suffixes to match sources and artifacts. \n"
                + "Example: -source, -src");

        addField(new StringFieldEditor(PreferenceConstants.JAVADOC_TYPES, "Javadoc types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation.setText("Comma separated list of artifact types to be used as javadoc. \n"
                + "Example: javadoc");

        addField(new StringFieldEditor(PreferenceConstants.JAVADOC_SUFFIXES, "Javadoc suffixes",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation.setText("Comma separated list of suffixes to match javadocs and artifacts. \n"
                + "Example: -javadoc, -doc");

        spacer = new Label(fieldParent, SWT.NONE);
        spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacer.setLayoutData(spacerData);

        BooleanFieldEditor alphaOrder = new BooleanFieldEditor(
                PreferenceConstants.ALPHABETICAL_ORDER,
                "Order alphabetically the artifacts in the classpath container", fieldParent);
        addField(alphaOrder);

        spacer = new Label(fieldParent, SWT.NONE);
        spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacer.setLayoutData(spacerData);

        BooleanFieldEditor resolveInWorkspace = new BooleanFieldEditor(
                PreferenceConstants.RESOLVE_IN_WORKSPACE,
                "Resolve dependencies to workspace projects",
                fieldParent);
        addField(resolveInWorkspace);

        spacer = new Label(fieldParent, SWT.NONE);
        spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacer.setLayoutData(spacerData);

        spacer = new Label(fieldParent, SWT.NONE);
        spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacer.setLayoutData(spacerData);
        spacer.setText("Editor information");
        spacer = new Label(fieldParent, SWT.SEPARATOR | SWT.HORIZONTAL);
        spacer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));
        addField(new StringFieldEditor(PreferenceConstants.ORGANISATION, "&Organisation:",
                fieldParent));
        addField(new StringFieldEditor(PreferenceConstants.ORGANISATION_URL, "Organisation &URL:",
                fieldParent));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}
