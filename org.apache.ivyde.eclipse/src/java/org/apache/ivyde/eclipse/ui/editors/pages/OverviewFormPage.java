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
package org.apache.ivyde.eclipse.ui.editors.pages;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class OverviewFormPage extends FormPage {

    /**
     * @param editor
     * @param id
     * @param title
     */
    public OverviewFormPage(FormEditor editor) {
        super(editor, "overview", "Overview");
    }

    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        form.setText("Overview");
        fillBody(managedForm, toolkit);
        managedForm.refresh();
    }

    private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
        Composite body = managedForm.getForm().getBody();
        TableWrapLayout layout = new TableWrapLayout();
        layout.bottomMargin = 10;
        layout.topMargin = 5;
        layout.leftMargin = 10;
        layout.rightMargin = 10;
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = true;
        layout.verticalSpacing = 30;
        layout.horizontalSpacing = 10;
        body.setLayout(layout);

        // sections
        managedForm.addPart(new IvyInfoSection(this, body, Section.TWISTIE, true));
        managedForm.addPart(new IvyConfSection(this, body, Section.TWISTIE, true));
    }
}
