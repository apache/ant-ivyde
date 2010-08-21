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
package org.apache.ivyde.eclipse.ui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivyde.eclipse.IvyNature;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.handlers.OpenIvyFileHandler;
import org.apache.ivyde.eclipse.handlers.RefreshHandler;
import org.apache.ivyde.eclipse.handlers.ReloadSettingsHandler;
import org.apache.ivyde.eclipse.handlers.RemoveIvyNatureHandler;
import org.apache.ivyde.eclipse.handlers.ResolveHandler;
import org.apache.ivyde.eclipse.handlers.ViewReverseDependenciesHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class IvyMenuContributionItem extends CompoundContributionItem implements
        IWorkbenchContribution {

    private IServiceLocator serviceLocator;

    public void initialize(IServiceLocator locator) {
        this.serviceLocator = locator;
    }

    protected IContributionItem[] getContributionItems() {
        ISelectionService selectionService = (ISelectionService) serviceLocator
                .getService(ISelectionService.class);
        if (selectionService == null) {
            return new IContributionItem[0];
        }
        ISelection selection = selectionService.getSelection();
        if (selection == null || !(selection instanceof IStructuredSelection)) {
            return new IContributionItem[0];
        }

        Map/* <IProject, Set<IvyClasspathContainer>> */projects = new HashMap();

        // this give info about if the selection is only based of classpath containers
        boolean onlyContainers = true;

        Iterator it = ((IStructuredSelection) selection).iterator();
        while (it.hasNext()) {
            Object element = it.next();
            boolean projectCollected = collectProject(projects, element);
            if (projectCollected) {
                onlyContainers = false;
            } else {
                IWorkingSet workingSet = (IWorkingSet) IvyPlugin.adapt(element, IWorkingSet.class);
                if (workingSet != null) {
                    onlyContainers = false;
                    IAdaptable[] elements = workingSet.getElements();
                    for (int i = 0; i < elements.length; i++) {
                        collectProject(projects, elements[i]);
                    }
                } else if (element instanceof ClassPathContainer) {
                    collectContainer(projects, (ClassPathContainer) element);
                }
            }
        }

        List/* <IContributionItem> */items;
        MenuManager menuManager;
        if (onlyContainers) {
            // we we have only containers, no need to have a root menu entry
            menuManager = null;
            items = new ArrayList();
        } else {
            menuManager = new MenuManager("Ivy", IvyPlugin
                    .getImageDescriptor("icons/logo16x16.gif"), "org.apache.ivyde.eclipse.menu");
            items = Collections.singletonList(menuManager);
        }

        addCommand(menuManager, items, ResolveHandler.COMMAND_ID);
        addCommand(menuManager, items, RefreshHandler.COMMAND_ID);
        addCommand(menuManager, items, ReloadSettingsHandler.COMMAND_ID);
        fillMenu(menuManager, items, new IvyMenuSeparator());
        addCommand(menuManager, items, OpenIvyFileHandler.COMMAND_ID);
        fillMenu(menuManager, items, new IvyMenuSeparator());
        CleanCacheContributionItem cleanCacheContributionItem = new CleanCacheContributionItem();
        cleanCacheContributionItem.initialize(serviceLocator);
        fillMenu(menuManager, items, cleanCacheContributionItem);
        fillMenu(menuManager, items, new IvyMenuSeparator());
        addCommand(menuManager, items, ViewReverseDependenciesHandler.COMMAND_ID);
        fillMenu(menuManager, items, new IvyMenuSeparator());
        addCommand(menuManager, items, RemoveIvyNatureHandler.COMMAND_ID);

        return (IContributionItem[]) items.toArray(new IContributionItem[items.size()]);
    }

    private void addCommand(MenuManager menuManager, List/* <IContributionItem> */items,
            String commandId) {
        CommandContributionItemParameter parm = new CommandContributionItemParameter(
                serviceLocator, null, commandId, CommandContributionItem.STYLE_PUSH);
        fillMenu(menuManager, items, new CommandContributionItem(parm));
    }

    private void fillMenu(MenuManager menuManager, List/* <IContributionItem> */items,
            IContributionItem item) {
        if (menuManager != null) {
            menuManager.add(item);
        } else {
            items.add(item);
        }
    }

    private boolean collectProject(Map/* <IProject, Set<IvyClasspathContainer>> */projects,
            Object element) {
        IProject project = (IProject) IvyPlugin.adapt(element, IProject.class);
        if (project != null && project.isOpen() && IvyNature.hasNature(project)) {
            doCollectProject(projects, project);
            return true;
        }
        return false;
    }

    private void doCollectProject(Map/* <IProject, Set<IvyClasspathContainer>> */projects,
            IProject project) {
        List containers = IvyClasspathUtil.getIvyClasspathContainers(project);
        if (!containers.isEmpty()) {
            projects.put(project, new HashSet(containers));
        }
    }

    private boolean collectContainer(Map/* <IProject, Set<IvyClasspathContainer>> */projects,
            ClassPathContainer element) {
        IvyClasspathContainer ivycp = IvyClasspathUtil.jdt2IvyCPC(element);
        if (ivycp == null) {
            return false;
        }
        doCollectContainer(projects, ivycp);
        return true;
    }

    private void doCollectContainer(Map/* <IProject, Set<IvyClasspathContainer>> */projects,
            IvyClasspathContainer ivycp) {
        IJavaProject javaProject = ivycp.getConf().getJavaProject();
        Set/* <IvyClasspathContainer> */cplist = (Set) projects.get(javaProject.getProject());
        if (cplist == null) {
            cplist = new HashSet();
            projects.put(javaProject.getProject(), cplist);
        }
        cplist.add(ivycp);
    }

}
