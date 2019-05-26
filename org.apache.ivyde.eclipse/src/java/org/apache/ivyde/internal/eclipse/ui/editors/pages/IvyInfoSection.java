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
package org.apache.ivyde.internal.eclipse.ui.editors.pages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class IvyInfoSection extends SectionPart implements PropertyChangeListener {
    @SuppressWarnings("unused")
    private final IFormPage page;

    private static final int NUM_COLUMNS = 2;

    public IvyInfoSection(IFormPage page, Composite parent, int style, boolean titleBar) {
        super(parent, page.getManagedForm().getToolkit(),
                titleBar ? (ExpandableComposite.TITLE_BAR | style) : style);
        this.page = page;
        createClient(getSection(), page.getEditor().getToolkit());
        getSection().setText("General Information");
        // ((IvyFileEditorInput)page.getEditorInput()).addPropertyChangeListener(this);
    }

    protected void createClient(Section section, FormToolkit toolkit) {
        section.setText("General Information"); //$NON-NLS-1$
        String desc = "This section describe the general information about your project";
        section.setDescription(desc);

        Composite client = toolkit.createComposite(section);
        TableWrapLayout layout = new TableWrapLayout();
        layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
        layout.leftMargin = layout.rightMargin;
        layout.numColumns = NUM_COLUMNS;
        client.setLayout(layout);
        // IvyFileEditorInput editorInput = (IvyFileEditorInput) page.getEditorInput();
        // PresentationModel moduleModel = editorInput.getPresentationModel();
        // PresentationModel revisionModel = new PresentationModel(moduleModel.getModel("resolvedModuleRevisionId"));
        // PresentationModel moduleIdModel = new PresentationModel(moduleModel.getModel("moduleId"));

        toolkit.createLabel(client, "Organisation");
        Text org = toolkit.createText(client, "");
        org.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

        // SWTBindings.bind(org, moduleIdModel.getModel("organisation"), true);

        toolkit.createLabel(client, "Module");
        Text mod = toolkit.createText(client, "");
        mod.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        // SWTBindings.bind(org, moduleIdModel.getModel("name"), true);

        toolkit.createLabel(client, "Status");
        Text status = toolkit.createText(client, "");
        status.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        // SWTBindings.bind(org, moduleModel.getModel("status"), true);

        toolkit.paintBordersFor(client);
        section.setClient(client);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        td.colspan = NUM_COLUMNS;
        section.setLayoutData(td);
    }

    @SuppressWarnings("unused")
    private void createOrganisationEntry(Composite parent, FormToolkit toolkit) {
    }

    public void refresh() {
        super.refresh();
    }

    public void propertyChange(PropertyChangeEvent evt) {

    }
}
