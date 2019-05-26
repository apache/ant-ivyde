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
import org.apache.ivyde.internal.eclipse.ui.editors.xml.ColorManager;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class XMLEditorPreferencesPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public XMLEditorPreferencesPage() {
        super(GRID);
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
        setDescription("");
    }

    public void createFieldEditors() {
        final Composite fieldParent = getFieldEditorParent();

        addField(new ColorFieldEditor(PreferenceConstants.EDITOR_COLOR_XML_COMMENT, "XML comment",
                fieldParent));

        addField(new ColorFieldEditor(PreferenceConstants.EDITOR_COLOR_PROC_INSTR,
                "Processing instruction", fieldParent));

        addField(new ColorFieldEditor(PreferenceConstants.EDITOR_COLOR_STRING, "String",
                fieldParent));

        addField(new ColorFieldEditor(PreferenceConstants.EDITOR_COLOR_DEFAULT, "Default",
                fieldParent));

        addField(new ColorFieldEditor(PreferenceConstants.EDITOR_COLOR_TAG, "XML tag",
                fieldParent));
    }

    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    public boolean performOk() {
        boolean success = super.performOk();
        if (success) {
            ColorManager colorManager = IvyPlugin.getDefault().getColorManager();
            colorManager.refreshFromStore(getPreferenceStore());
        }
        return success;
    }
}
