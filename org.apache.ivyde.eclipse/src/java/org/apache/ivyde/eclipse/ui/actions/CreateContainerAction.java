/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.eclipse.ui.actions;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.NewIvyDEContainerWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class CreateContainerAction implements IWorkbenchWindowActionDelegate {

    public void run(IAction action) {
        ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
                .getSelection();
        if (sel instanceof IStructuredSelection) {
            IStructuredSelection s = (IStructuredSelection) sel;
            Object o = s.getFirstElement();
            if (o instanceof IFile) {
                IFile f = (IFile) o;
                IJavaProject javaProject = JavaCore.create(f.getProject());
                WizardDialog dialog = new WizardDialog(IvyPlugin.getActiveWorkbenchShell(),
                        new NewIvyDEContainerWizard(javaProject, f));
                dialog.open();
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // nothing to change
    }

    public void dispose() {
        // nothing to dispose
    }

    public void init(IWorkbenchWindow window) {
        // nothing to initialize
    }
}
