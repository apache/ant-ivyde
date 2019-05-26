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
package org.apache.ivyde.internal.eclipse.ui.preferences;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class WorkspaceResolverPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private static final int HORIZ_INDENT = 40;

    public WorkspaceResolverPreferencePage() {
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

        BooleanFieldEditor autoResolveOnClose = new BooleanFieldEditor(
                PreferenceConstants.AUTO_RESOLVE_ON_CLOSE,
                "On project closing trigger resolve on dependent project", fieldParent) {
            protected Label getLabelControl() {
                Label label = super.getLabelControl();
                label.setToolTipText("Will automatically resolve projects in the "
                        + "workspace after dependent project is closed");
                return label;
            }
        };
        addField(autoResolveOnClose);

        BooleanFieldEditor autoResolveOnOpen = new BooleanFieldEditor(
                PreferenceConstants.AUTO_RESOLVE_ON_OPEN,
                "On project opening trigger resolve on every other project", fieldParent) {
            protected Label getLabelControl() {
                Label label = super.getLabelControl();
                label.setToolTipText("Will automatically resolve projects in the "
                        + "workspace and link open project where necessary");
                return label;
            }
        };
        addField(autoResolveOnOpen);

        BooleanFieldEditor ignoreBranchOnWorkspaceProjects = new BooleanFieldEditor(
                PreferenceConstants.IGNORE_BRANCH_ON_WORKSPACE_PROJECTS,
                "Ignore branch when resolving workspace projects", fieldParent) {
            protected Label getLabelControl() {
                Label label = super.getLabelControl();
                label.setToolTipText("Will ignore the artifact branch when resolving "
                        + "against workspace projects");
                return label;
            }
        };

        addField(ignoreBranchOnWorkspaceProjects);

        BooleanFieldEditor ignoreVersionOnWorkspaceProjects = new BooleanFieldEditor(
                PreferenceConstants.IGNORE_VERSION_ON_WORKSPACE_PROJECTS,
                "Ignore version when resolving workspace projects", fieldParent) {
            protected Label getLabelControl() {
                Label label = super.getLabelControl();
                label.setToolTipText("Will ignore the artifact version when resolving "
                        + "against workspace projects");
                return label;
            }
        };

        addField(ignoreVersionOnWorkspaceProjects);

        Label warning = new Label(fieldParent, SWT.NONE);
        warning.setText("WARNING: you should ignore branch and/or revision only\n"
                    + "if you are sure that all the project revisions that are\n"
                    + "open in your workspace can actually work together\n"
                    + "(be careful with branch management for instance)");
        GridData gridData = new GridData();
        gridData.horizontalIndent = HORIZ_INDENT;
        warning.setLayoutData(gridData);
    }

    public void init(IWorkbench workbench) {
        // nothing to do
    }

}
