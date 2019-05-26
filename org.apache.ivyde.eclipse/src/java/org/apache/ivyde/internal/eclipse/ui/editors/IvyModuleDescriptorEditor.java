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
package org.apache.ivyde.internal.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.common.ivyfile.IvyModuleDescriptorModel;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.eclipse.extension.IvyEditorPage;
import org.apache.ivyde.eclipse.extension.ModuleDescriptorExtension;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.core.IvyFileEditorInput;
import org.apache.ivyde.internal.eclipse.ui.editors.pages.OverviewFormPage;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.EclipseIvyModelSettings;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.IvyContentAssistProcessor;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.XMLEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class IvyModuleDescriptorEditor extends FormEditor implements IResourceChangeListener {
    public static final String ID = "org.apache.ivyde.editors.IvyEditor";

    private XMLEditor xmlEditor;

    private Browser browser;

    private final List<IvyEditorPageDescriptor> ivyEditorPageDescriptors = new ArrayList<>();

    private final List<ModuleDescriptorExtensionDescriptor> moduleDescriptorExtensionDescriptors = new ArrayList<>();

    /**
     * Creates a multi-page editor example.
     */
    public IvyModuleDescriptorEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        parseModuleDescriptorExtensionMetadata();
        parseEditorPageExtensionMetadata();
    }

    private void parseModuleDescriptorExtensionMetadata() {
        final IExtension[] extensions = Platform.getExtensionRegistry()
                .getExtensionPoint(ModuleDescriptorExtension.EXTENSION_POINT).getExtensions();
        for (IExtension extension : extensions) {
            for (IConfigurationElement configElement : extension.getConfigurationElements()) {
                final ModuleDescriptorExtensionDescriptor descriptor = new ModuleDescriptorExtensionDescriptor(
                        configElement);
                moduleDescriptorExtensionDescriptors.add(descriptor);
            }
        }
    }

    private void parseEditorPageExtensionMetadata() {
        final IExtension[] extensions = Platform.getExtensionRegistry()
                .getExtensionPoint(IvyEditorPage.EXTENSION_POINT).getExtensions();
        for (IExtension extension : extensions) {
            for (IConfigurationElement configElement : extension.getConfigurationElements()) {
                final IvyEditorPageDescriptor descriptor = new IvyEditorPageDescriptor(
                        configElement);
                ivyEditorPageDescriptors.add(descriptor);
            }
        }
    }

    protected void setInput(IEditorInput input) {
        IvyFileEditorInput ivyFileEditorInput = null;
        if (input instanceof FileEditorInput) {
            FileEditorInput fei = (FileEditorInput) input;
            IFile file = fei.getFile();
            ivyFileEditorInput = new IvyFileEditorInput(file);
        } else if (input instanceof IvyFileEditorInput) {
            ivyFileEditorInput = (IvyFileEditorInput) input;
        }
        super.setInput(ivyFileEditorInput);
        if (ivyFileEditorInput.getFile() != null) {
            if (xmlEditor != null) {
                xmlEditor.setFile(ivyFileEditorInput.getFile());
            }
        }
        setPartName(ivyFileEditorInput.getFile().getName());
    }

    void createPageXML() {
        try {
            xmlEditor = new XMLEditor(new IvyContentAssistProcessor() {
                protected IvyModel newCompletionModel(IFile file) {
                    return getIvyCompletionModel(new EclipseIvyModelSettings(file));
                }
            });
            xmlEditor.setFile(((IvyFileEditorInput) getEditorInput()).getFile());
            int index = addPage(xmlEditor, getEditorInput());
            setPageText(index, xmlEditor.getTitle());
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null,
                e.getStatus());
        }
    }

    private IvyModuleDescriptorModel getIvyCompletionModel(IvyModelSettings ivyModelSettings) {
        IvyModuleDescriptorModel ivyModuleDescriptorModel = new IvyModuleDescriptorModel(
                ivyModelSettings);
        for (ModuleDescriptorExtensionDescriptor descriptor : moduleDescriptorExtensionDescriptors) {
            ModuleDescriptorExtension moduleDescriptorExtension = descriptor
                    .createModuleDescriptorExtension();
            if (moduleDescriptorExtension != null) {
                ivyModuleDescriptorModel = moduleDescriptorExtension
                        .contributeModel(ivyModuleDescriptorModel);
            }
        }
        return ivyModuleDescriptorModel;
    }

    void createPageOverView() {
        try {
            int index = addPage(new OverviewFormPage(this));
            setPageText(index, "Information");
        } catch (PartInitException e) {
            // Should not happen
            IvyPlugin.logError("The overview page could not be created", e);
        }

    }

    void createPagePreview() {
        try {
            browser = new Browser(getContainer(), SWT.NONE);
            browser.setUrl(((IvyFileEditorInput) getEditorInput()).getPath().toOSString());
            int index = addPage(browser);
            setPageText(index, "Preview");
        } catch (SWTError e) {
            // IVYDE-10: under Linux if MOZILLA_FIVE_HOME is not set, it fails badly
            MessageDialog.openError(IvyPlugin.getActiveWorkbenchShell(),
                "Fail to create the preview",
                "The page preview could not be created :" + e.getMessage());
            IvyPlugin.logError("The preview page in the ivy.xml editor could not be created", e);
        }
    }

    /**
     * Creates the pages of the multi-page editor.
     */
    protected void addPages() {
        // createPageOverView();
        createPageXML();
        // createPagePreview();
        addIvyEditorPageExtensions();
    }

    private void addIvyEditorPageExtensions() {
        for (IvyEditorPageDescriptor ivyEditorPageDescriptor : ivyEditorPageDescriptors) {
            IvyEditorPage page = ivyEditorPageDescriptor.createPage();
            try {
                page.initialize(this);
                int pageIndex = addPage(page);
                setPageText(pageIndex, page.getPageName());
            } catch (PartInitException e) {
                IvyPlugin.log(IStatus.ERROR, "Cannot add Ivy editor extension", e);
            }
        }

    }

    /**
     * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code>
     * method disposes all nested editors. Subclasses may extend.
     */
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    /**
     * Saves the multi-page editor's document.
     *
     * @param monitor IProgressMonitor
     */
    public void doSave(IProgressMonitor monitor) {
        xmlEditor.doSave(monitor);
    }

    /**
     * Saves the multi-page editor's document as another file. Also updates the text for page 0's
     * tab, and updates this multi-page editor's input to correspond to the nested editor's.
     */
    public void doSaveAs() {
        xmlEditor.doSaveAs();
        setPageText(0, xmlEditor.getTitle());
        setInput(xmlEditor.getEditorInput());
    }

    public void gotoMarker(IMarker marker) {
        setActivePage(0);
        IDE.gotoMarker(getEditor(0), marker);
    }

    /**
     * The <code>MultiPageEditorExample</code> implementation of this method checks that the input
     * is an instance of <code>IFileEditorInput</code>.
     *
     * @param site IEditorSite
     * @param editorInput IEditorInput
     * @throws PartInitException if editorInput is not IFileEditorInput
     */
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput)) {
            throw new PartInitException("Invalid Input: Must be IFileEditorInput");
        }
        super.init(site, editorInput);
    }

    public boolean isSaveAsAllowed() {
        return xmlEditor.isSaveAsAllowed();
    }

    /**
     * Calculates the contents of page 2 when the it is activated.
     *
     * @param newPageIndex int
     */
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (newPageIndex == 1 && browser != null) {
            browser.refresh();
        }
    }

    /**
     * Closes all project files on project close.
     *
     * @param event IResourceChangeEvent
     */
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            final IResource res = event.getResource();
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    for (IWorkbenchPage page : getSite().getWorkbenchWindow().getPages()) {
                        if (((IFileEditorInput) xmlEditor.getEditorInput()).getFile().getProject()
                                .equals(res)) {
                            IEditorPart editorPart = page.findEditor(xmlEditor.getEditorInput());
                            page.closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }

}
