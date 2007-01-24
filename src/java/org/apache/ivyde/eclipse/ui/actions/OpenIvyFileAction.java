/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.actions;

import java.io.File;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.part.FileEditorInput;


public class OpenIvyFileAction  implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;
    /**
     * The constructor.
     */
    public OpenIvyFileAction() {
    }

    /**
     * The action has been activated. The argument of the
     * method represents the 'real' action sitting
     * in the workbench UI.
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
        ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
        if (sel instanceof IStructuredSelection) {
            IStructuredSelection s = (IStructuredSelection)sel;
            Object o = s.getFirstElement();
            if (o instanceof ClassPathContainer) {
                IPath path = ((ClassPathContainer)o).getClasspathEntry().getPath();
                IJavaProject project = ((ClassPathContainer)o).getJavaProject();
                try {
                    IClasspathContainer fContainer= JavaCore.getClasspathContainer(path, project);
                    if (fContainer instanceof IvyClasspathContainer) {
                        IvyClasspathContainer ivycp = (IvyClasspathContainer)fContainer;
                        
                        IFile file = ivycp.getIvyFile();
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        if (file != null) {
                            try {
                                String editorId = "org.apache.ivyde.editors.IvyEditor";
                                page.openEditor(new FileEditorInput(file), editorId, true);
                                // only remember the default editor if the open succeeds
                                IDE.setDefaultEditor(file, editorId);
                            } catch (PartInitException e) {
                                DialogUtil.openError(page.getWorkbenchWindow().getShell(),
                                        IDEWorkbenchMessages.OpenWithMenu_dialogTitle,
                                        e.getMessage(), e);
                            }
                        }
                    }
                } catch (Exception e) {  
                    // TODO : log exc
                    System.err.println(e.getMessage());
                }

            }
        }
    }

    /**
     * Selection in the workbench has been changed. We 
     * can change the state of the 'real' action here
     * if we want, but this can only happen after 
     * the delegate has been created.
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * We can use this method to dispose of any system
     * resources we previously allocated.
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to
     * be able to provide parent shell for the message dialog.
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }


}
