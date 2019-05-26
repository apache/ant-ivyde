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

import java.util.Map;
import java.util.Set;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.views.ReverseDependencyExplorerView;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

public class ViewReverseDependenciesHandler extends AbstractIvyDEHandler {

    public static final String COMMAND_ID = "org.apache.ivyde.commands.revdepexplorer";

    protected void handleProjects(Map<IProject, Set<IvyClasspathContainer>> projects) {
        try {
            ReverseDependencyExplorerView.setSelectedProjects(projects.keySet().toArray(new IProject[0]));
            IWorkbenchPage page = IvyPlugin.getActivePage();
            page.showView("org.apache.ivyde.eclipse.ui.views.ReverseDependencyExplorer");
            ReverseDependencyExplorerView.refresh(true);
        } catch (PartInitException e) {
            IvyPlugin.logError("Error creating Reverse Dependency Explorer", e);
        }
    }
}
