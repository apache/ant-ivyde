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
        layout.makeColumnsEqualWidth =true;
        layout.verticalSpacing = 30;
        layout.horizontalSpacing = 10;
        body.setLayout(layout);

        // sections
        managedForm.addPart(new IvyInfoSection(this, body, Section.TWISTIE, true));
        managedForm.addPart(new IvyConfSection(this, body, Section.TWISTIE, true));
    }
}
