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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

/**
 * Abstract class for helping with project delegation of actions <br>
 */
public abstract class IvyDEProjectAction implements IActionDelegate {
    protected abstract void selectionChanged(IAction action, IProject[] projects);
    
    /**
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public final void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Collection/*<IProject>*/ projects = new ArrayList/*<IProject>*/();

            for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }

                if (project != null) {
                    // TODO validate a project's "Ivy nature" here (has an ivy.xml or an ivy classpath container)
                    projects.add(project);
                }
            }
            
            if(projects.size() > 0) {
                action.setEnabled(true);
                selectionChanged(action, (IProject[]) projects.toArray(new IProject[projects.size()]));
            }
            else {
                action.setEnabled(false);
            }
        }
    }
}
