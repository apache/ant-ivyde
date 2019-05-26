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

import org.apache.ivyde.common.ivysettings.IvySettingsModel;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.ui.core.IvyFileEditorInput;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.EclipseIvyModelSettings;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.IvyContentAssistProcessor;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.XMLEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
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

public class IvySettingsEditor extends FormEditor implements IResourceChangeListener {
    public static final String ID = "org.apache.ivyde.editors.IvySettingsEditor";

    private XMLEditor xmlEditor;

    /**
     * Creates a multi-page editor example.
     */
    public IvySettingsEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
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
                    return new IvySettingsModel(
                        new EclipseIvyModelSettings(getJavaProject()),
                        file.getFullPath().toFile());
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

    /**
     * Creates the pages of the multi-page editor.
     */
    protected void addPages() {
        // createPageOverView();
        createPageXML();
        // createPagePreview();
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
        IFile file = ((IvyFileEditorInput) getEditorInput()).getFile();
        for (IvyClasspathContainer container : IvyClasspathContainerHelper
                .getContainersFromIvySettings(file)) {
            container.launchResolve(false, null);
        }
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

    /*
     * (non-Javadoc) Method declared on IEditorPart
     */
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
                            IEditorPart editorPart = page
                                    .findEditor(xmlEditor.getEditorInput());
                            page.closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }
}
