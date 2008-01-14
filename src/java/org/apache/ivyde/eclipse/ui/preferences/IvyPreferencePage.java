package org.apache.ivyde.eclipse.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.ivy.Ivy;
import org.apache.ivyde.eclipse.IvyPlugin;
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

    private StringFieldEditor _pattern;

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
        addField(new FileFieldEditor(PreferenceConstants.IVYCONF_PATH, "&IvyConf URL:", fieldParent) {
            /* Opens the file chooser dialog and returns the selected file as an url. */
            protected String changePressed() {
                String f = super.changePressed();
                if (f == null) {
                    return null;
                } else {
                    File d = new File(f);
                    try {
                        return d.toURL().toExternalForm();
                    } catch (MalformedURLException e) {
                        return null;
                    }
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
        explanation
                .setText("The url where your ivyconf file can be found. \nUse default to reference the default ivy configuration.");
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
                    }
                });
            }
        };
        _pattern = new StringFieldEditor(PreferenceConstants.RETRIEVE_PATTERN, "Pattern",
                fieldParent);
        _pattern.setEnabled(getPreferenceStore().getBoolean(PreferenceConstants.DO_RETRIEVE),
            fieldParent);
        addField(doR);
        addField(_pattern);

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Pattern example: lib/[conf]/[artifact].[ext]\nTo copy artifacts in folder named lib without revision by folder named like configurations");
        new Label(fieldParent, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 3, 1)); // space

        addField(new StringFieldEditor(PreferenceConstants.ACCEPTED_TYPES, "Accepted types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Comma separated list of artifact types to use in IvyDE Managed Dependencies Library\nExample: jar, zip");

        addField(new StringFieldEditor(PreferenceConstants.SOURCES_TYPES, "Sources types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Comma separated list of artifact types to be used as sources. \nExample: source, src");

        addField(new StringFieldEditor(PreferenceConstants.JAVADOC_TYPES, "Javadoc types",
                fieldParent));

        new Label(fieldParent, SWT.NONE); // space
        explanation = new Label(fieldParent, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2,
                1));
        explanation
                .setText("Comma separated list of artifact types to be used as javadoc. \nExample: javadoc");

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
