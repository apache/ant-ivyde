package org.apache.ivyde.eclipse.ui.editors.pages;

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
    IFormPage page;

    private static int NUM_COLUMNS = 2;

    public IvyConfSection(IFormPage page, Composite parent, int style, boolean titleBar) {
        super(parent, page.getManagedForm().getToolkit(),
                titleBar ? (ExpandableComposite.TITLE_BAR | style) : style);
        this.page = page;
        createClient(getSection(), page.getEditor().getToolkit());
    }

    protected void createClient(Section section, FormToolkit toolkit) {
        section.setText("Configurations"); //$NON-NLS-1$
        section.setDescription("This section describe the configurations defined in your project"); //$NON-NLS-1$

        Composite client = toolkit.createComposite(section);
        TableWrapLayout layout = new TableWrapLayout();
        layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
        layout.numColumns = NUM_COLUMNS;
        client.setLayout(layout);

        toolkit.paintBordersFor(client);
        section.setClient(client);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        td.colspan = 2;
        section.setLayoutData(td);
    }

    private void createOrganisationEntry(Composite parent, FormToolkit toolkit) {

    }

    public void refresh() {
        super.refresh();
    }
}
