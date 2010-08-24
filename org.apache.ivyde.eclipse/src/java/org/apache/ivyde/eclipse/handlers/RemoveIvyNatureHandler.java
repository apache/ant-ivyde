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
package org.apache.ivyde.eclipse.handlers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.ivyde.eclipse.IvyMarkerManager;
import org.apache.ivyde.eclipse.IvyNature;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class RemoveIvyNatureHandler extends AbstractHandler {

    public static final String COMMAND_ID = "org.apache.ivyde.commands.removeivynature";

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        List/* <IProject> */projects = new LinkedList();
        IStructuredSelection newSelection = (IStructuredSelection) selection;
        Iterator iter = newSelection.iterator();
        while (iter.hasNext()) {
            Object object = iter.next();
            if (object instanceof IAdaptable) {
                IProject project = (IProject) ((IAdaptable) object).getAdapter(IProject.class);
                if (project != null) {
                    if (IvyNature.hasNature(project)) {
                        projects.add(project);
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        if (projects.size() > 0) {
            Shell shell = IvyPlugin.getActiveWorkbenchShell();
            boolean doRemove = MessageDialog.openQuestion(shell,
                "Remove Ivy dependency management",
                "Do you want to remove the Ivy dependency management ?\n\n"
                        + "The configuration of the classpath containers will be lost.\n"
                        + "This operation cannot be undone.");
            if (doRemove) {
                Iterator itProject = projects.iterator();
                while (itProject.hasNext()) {
                    IProject project = (IProject) itProject.next();
                    IvyNature.removeNature(project);
                }
            }
        }

        return null;
    }
}
