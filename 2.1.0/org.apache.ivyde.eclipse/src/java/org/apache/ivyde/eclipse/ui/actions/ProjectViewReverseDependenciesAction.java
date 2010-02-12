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
import org.apache.ivyde.eclipse.ui.views.ReverseDependencyExplorerView;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

public class ProjectViewReverseDependenciesAction extends Action {

    private IProject[] projects;

    private IWorkbenchPage page;

    public ProjectViewReverseDependenciesAction(IProject[] projects, IWorkbenchPage workbenchPage) {
        this.projects = projects;
        this.page = workbenchPage;
        this.setText("Show in Reverse Dependency Explorer");
    }

    public void run() {
        try {
            ReverseDependencyExplorerView.setSelectedProjects(projects);
            page.showView("org.apache.ivyde.eclipse.ui.views.ReverseDependencyExplorer");
            ReverseDependencyExplorerView.refresh(true);
        } catch (PartInitException e) {
            IvyPlugin.log(IStatus.ERROR, "Error creating Reverse Dependency Explorer", e);
        }
    }
}
