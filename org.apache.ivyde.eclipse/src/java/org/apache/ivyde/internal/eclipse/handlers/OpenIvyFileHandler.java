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
package org.apache.ivyde.internal.eclipse.handlers;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class OpenIvyFileHandler extends AbstractIvyDEHandler {

    public static final String COMMAND_ID = "org.apache.ivyde.commands.openivyfile";

    protected void handleContainer(IProject project, IvyClasspathContainer container) {
        open(container);
    }

    public static void open(IvyClasspathContainer container) {
        IvyClasspathContainerConfiguration conf = container.getConf();
        if (conf.getJavaProject() == null) {
            return;
        }
        IFile file = conf.getJavaProject().getProject().getFile(conf.getIvyXmlPath());
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (file != null) {
            try {
                String editorId = "org.apache.ivyde.editors.IvyEditor";
                page.openEditor(new FileEditorInput(file), editorId, true);
                // only remember the default editor if the open succeeds
                IDE.setDefaultEditor(file, editorId);
            } catch (PartInitException e) {
                Shell parent = page.getWorkbenchWindow().getShell();
                String title = "Problems Opening Editor";
                String message = e.getMessage();
                // Check for a nested CoreException
                CoreException nestedException = null;
                IStatus status = e.getStatus();
                if (status != null && status.getException() instanceof CoreException) {
                    nestedException = (CoreException) status.getException();
                }
                if (nestedException != null) {
                    // Open an error dialog and include the extra
                    // status information from the nested CoreException
                    ErrorDialog.openError(parent, title, message, nestedException.getStatus());
                } else {
                    // Open a regular error dialog since there is no
                    // extra information to display
                    MessageDialog.openError(parent, title, message);
                }
            }
        }
    }

}
