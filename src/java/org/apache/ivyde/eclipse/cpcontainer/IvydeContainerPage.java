/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.cpcontainer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.parser.ModuleDescriptorParserRegistry;

public class IvydeContainerPage extends NewElementWizardPage
    implements IClasspathContainerPage, IClasspathContainerPageExtension {

    private IJavaProject _project;
    private IClasspathEntry _entry;

    private Text _ivyFilePathText;
    private CheckboxTableViewer _confTableViewer;

    public IvydeContainerPage() {
        super("IvyDE Container");
    }
    public boolean isPageComplete() {
        return true;
    }
    public boolean finish() {
        if(_ivyFilePathText.getText().length()>0 && getConfigurationsText().length()>0 ) {
            return true;
        }
        MessageDialog.openWarning(_ivyFilePathText.getShell(), "Missing information", "Please select a valid file and choose at least one configuration");
        return false;
    }
    
    public IClasspathEntry getSelection() {
        return createEntry();
    }
    public void setSelection(IClasspathEntry entry) {
        _entry = entry != null ? entry : createDefaultEntry();
    }

    private IClasspathEntry createEntry() {
        IClasspathEntry entry = JavaCore.newContainerEntry(
                new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID)
                .append(_ivyFilePathText.getText())
                .append(getConfigurationsText()));
        return entry;
    }
    private String getConfigurationsText() {
        Object[]confs = _confTableViewer.getCheckedElements();
        int tot = _confTableViewer.getTable().getItemCount();
        if(confs != null && confs.length == tot) {
            return "*";
        }
        String text = "";
        for (int i = 0; i < confs.length; i++) {
            Configuration conf = (Configuration)confs[i];
            text+=conf.getName()+(i < confs.length?",":"");
        }
        return text;
    }
    private IClasspathEntry createDefaultEntry() {
        IClasspathEntry entry = JavaCore.newContainerEntry(
                new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID).append("ivy.xml/*"));
        return entry;
    }
    public void createControl(Composite parent) {
        setTitle("IvyDE Managed Libraries");
        setDescription("Choose ivy file and its configurations.");
        Composite control = new Composite(parent, 0);
        GridLayout layout = new GridLayout(2, false);
        control.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        control.setLayoutData(data);

        addMainSection(control);
        setControl(control);
    }

    private Composite createDefaultComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        composite.setLayout(layout);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);

        return composite;
    }

    private void addMainSection(Composite parent) {
        Composite composite = createDefaultComposite(parent);
       
        //Label for ivy file field
        Label pathLabel = new Label(composite, SWT.NONE);
        pathLabel.setText("Ivy File");

        _ivyFilePathText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _ivyFilePathText.setText(IvyClasspathContainer.getIvyFilePath(_entry.getPath()));
        _ivyFilePathText.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                refreshConfigurationTable();
            }
        });
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        _ivyFilePathText.setLayoutData(gridData);
        
        Button btn = new Button(composite, SWT.NONE);
        btn.setText("Browse");
        btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
                Class[] acceptedClasses= new Class[] { IFile.class };
                TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
                dialog.setValidator(validator);
                dialog.setTitle("choose ivy file"); 
                dialog.setMessage("choose the ivy file to use to resolve dependencies"); 
                dialog.setInput(_project.getProject());
                dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

                if (dialog.open() == Window.OK) {
                    Object[] elements= dialog.getResult();
                    if (elements.length > 0 && elements[0] instanceof IFile) {
                        IPath p = ((IFile)elements[0]).getProjectRelativePath();
                        _ivyFilePathText.setText(p.toString());
                        refreshConfigurationTable();
                    }
                }
            }
        });
        
        //Label for ivy configurations field
        Label confLabel = new Label(composite, SWT.NONE);
        confLabel.setText("Configurations");
        gridData = new GridData();
        gridData.verticalAlignment = GridData.BEGINNING;
        confLabel.setLayoutData(gridData);
        //table for configuration selection
        _confTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        _confTableViewer.getTable().setHeaderVisible (true);
        TableColumn col1 = new TableColumn(_confTableViewer.getTable(),SWT.NONE);
        col1.setText("Name");
        col1.setWidth(100);
        TableColumn col2 = new TableColumn(_confTableViewer.getTable(),SWT.NONE);
        col2.setText("Description");
        col2.setWidth(300);
        _confTableViewer.setColumnProperties(new String[]{"Name", "Description" });
        _confTableViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        _confTableViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                if(inputElement != null && !"".equals(inputElement)) {
                    return getConfigurations((String)inputElement);
                }
                return new Configuration[0];
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            public void dispose() {
            }
        });
        _confTableViewer.setLabelProvider(new ConfigurationLabelProvider());
        _confTableViewer.setInput(_ivyFilePathText.getText());
        initTableSelection(_ivyFilePathText.getText());
        
        //refresh
        Button refreshConf = new Button(composite, SWT.NONE);
        gridData = new GridData();
        gridData.verticalAlignment = GridData.BEGINNING;
        refreshConf.setLayoutData(gridData);
        refreshConf.setText("Refresh");
        refreshConf.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                _confTableViewer.setInput(_ivyFilePathText.getText());
            }
        });
        Composite spacer = new Composite(composite, SWT.NONE);
        Link select = new Link(composite, SWT.PUSH);
        gridData = new GridData();
        gridData.verticalAlignment = GridData.BEGINNING;
        select.setLayoutData(gridData);
        select.setText("<A>All</A>/<A>None</A>");
        select.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if(e.text.equals("All")) {;
                    _confTableViewer.setCheckedElements(getConfigurations(_ivyFilePathText.getText()));
                } else {
                    _confTableViewer.setCheckedElements(new Configuration[0]);
                }
            }
        });            
    }
    /**
     * @param ivyFile
     */
    private void initTableSelection(final String ivyFile) {
        String selectedConfsString = IvyClasspathContainer.getConfigurationsText(_entry.getPath());
        if("*".equals(selectedConfsString)) {
            _confTableViewer.setCheckedElements(getConfigurations(ivyFile));
        } else {
            String selectedConf[] = IvyClasspathContainer.getConfigurations(_entry.getPath());
            if(selectedConf != null) {
                ModuleDescriptor md = getModuleDescriptor(ivyFile);
                if(md != null) {
                    for (int i = 0; i < selectedConf.length; i++) {
                        String name = selectedConf[i];
                        Configuration configuration = md.getConfiguration(name);
                        if(configuration != null) {
                            _confTableViewer.setChecked(configuration, true);
                        }
                    }
                }
            }
        }
    }
    

    public void initialize(IJavaProject project, IClasspathEntry currentEntries[]) {
        _project = project;
    }
    /**
     * 
     */
    private void refreshConfigurationTable() {
        if(_confTableViewer.getInput() == null || !_confTableViewer.getInput().equals(_ivyFilePathText.getText())) {
            _confTableViewer.setInput(_ivyFilePathText.getText());
        }
    }
    /**
     * @param ivyfile
     * @return
     */
    private Configuration[] getConfigurations(String ivyfile) {
        try {
            ModuleDescriptor moduleDescriptor = getModuleDescriptor(ivyfile);
            if(moduleDescriptor != null) {
                return moduleDescriptor.getConfigurations();
            }
        } catch (Exception e) {}
        return new Configuration[0];
    }
    /**
     * @param ivyfile
     * @return
     * @throws MalformedURLException
     * @throws ParseException
     * @throws IOException
     */
    private ModuleDescriptor getModuleDescriptor(String ivyfile){
        try {
            IFile file = _project.getProject().getFile(ivyfile);
            URL url = new File( file.getLocation().toOSString()).toURL();
            return ModuleDescriptorParserRegistry.getInstance().parseDescriptor(IvyPlugin.getIvy(_project), url, false);
        } catch (Exception e) {}
        return null;
    }
    
    private static class ConfigurationLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if(columnIndex == 0) {
                return ((Configuration)element).getName();
            }
            return ((Configuration)element).getDescription();
        }
    }
}
