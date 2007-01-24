package org.apache.ivyde.eclipse.ui.editors.pages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.ivyde.eclipse.ui.core.IvyFileEditorInput;
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
    IFormPage page;
    private static int NUM_COLUMNS = 2;
    
    public IvyInfoSection(IFormPage page, Composite parent, int style, boolean titleBar) {
        super(parent, page.getManagedForm().getToolkit(), titleBar?(ExpandableComposite.TITLE_BAR | style): style);
        this.page = page;
        createClient(getSection(), page.getEditor().getToolkit());
        getSection().setText("General Information");
//        ((IvyFileEditorInput)page.getEditorInput()).addPropertyChangeListener(this);
    }

    protected void createClient(Section section, FormToolkit toolkit) {
        section.setText("General Information"); //$NON-NLS-1$
        section.setDescription("This section describe the general information about your project"); //$NON-NLS-1$

        Composite client = toolkit.createComposite(section);
        TableWrapLayout layout = new TableWrapLayout();
        layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
        layout.numColumns = 2;
        client.setLayout(layout);
        IvyFileEditorInput editorInput = (IvyFileEditorInput) page.getEditorInput();
//        PresentationModel moduleModel = editorInput.getPresentationModel();
//        PresentationModel revisionModel = new PresentationModel(moduleModel.getModel("resolvedModuleRevisionId"));
//        PresentationModel moduleIdModel = new PresentationModel(moduleModel.getModel("moduleId"));
        
        toolkit.createLabel(client, "Organisation");
        Text org = toolkit.createText(client, "");
        org.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
       
        
//        SWTBindings.bind(org, moduleIdModel.getModel("organisation"), true);
        
        toolkit.createLabel(client, "Module");
        Text mod = toolkit.createText(client, "");
        mod.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
//        SWTBindings.bind(org, moduleIdModel.getModel("name"), true);
        
        toolkit.createLabel(client, "Status");
        Text status = toolkit.createText(client, "");
        status.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
//        SWTBindings.bind(org, moduleModel.getModel("status"), true);
        
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
    
    public void propertyChange(PropertyChangeEvent evt) {
        
    }
}
