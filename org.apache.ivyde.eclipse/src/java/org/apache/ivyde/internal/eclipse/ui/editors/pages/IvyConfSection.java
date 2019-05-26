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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class IvyConfSection extends SectionPart {
    @SuppressWarnings("unused")
    private final IFormPage page;

    private static final int NUM_COLUMNS = 2;

    public IvyConfSection(IFormPage page, Composite parent, int style, boolean titleBar) {
        super(parent, page.getManagedForm().getToolkit(),
                titleBar ? (ExpandableComposite.TITLE_BAR | style) : style);
        this.page = page;
        createClient(getSection(), page.getEditor().getToolkit());
    }

    protected void createClient(Section section, FormToolkit toolkit) {
        section.setText("Configurations"); //$NON-NLS-1$
        String desc = "This section describe the configurations defined in your project";
        section.setDescription(desc);

        Composite client = toolkit.createComposite(section);
        TableWrapLayout layout = new TableWrapLayout();
        layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
        layout.leftMargin = layout.rightMargin;
        layout.numColumns = NUM_COLUMNS;
        client.setLayout(layout);

        toolkit.paintBordersFor(client);
        section.setClient(client);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        td.colspan = 2;
        section.setLayoutData(td);
    }

    @SuppressWarnings("unused")
    private void createOrganisationEntry(Composite parent, FormToolkit toolkit) {
    }

    public void refresh() {
        super.refresh();
    }
}
