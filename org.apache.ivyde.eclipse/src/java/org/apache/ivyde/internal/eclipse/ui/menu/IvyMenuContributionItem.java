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
package org.apache.ivyde.internal.eclipse.ui.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivyde.eclipse.IvyNatureHelper;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.internal.eclipse.handlers.OpenIvyFileHandler;
import org.apache.ivyde.internal.eclipse.handlers.RefreshHandler;
import org.apache.ivyde.internal.eclipse.handlers.ReloadSettingsHandler;
import org.apache.ivyde.internal.eclipse.handlers.RemoveIvyNatureHandler;
import org.apache.ivyde.internal.eclipse.handlers.ResolveHandler;
import org.apache.ivyde.internal.eclipse.handlers.ViewReverseDependenciesHandler;
import org.apache.ivyde.internal.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.internal.eclipse.retrieve.StandaloneRetrieveSetup;
import org.apache.ivyde.internal.eclipse.ui.menu.CleanCacheAction.Cleanable;
import org.apache.ivyde.internal.eclipse.ui.menu.CleanCacheAction.RepositoryCacheCleanable;
import org.apache.ivyde.internal.eclipse.ui.menu.CleanCacheAction.ResolutionCacheCleanable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.action.ActionContributionItem;
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
        ISelectionService selectionService = (ISelectionService) serviceLocator.getService(ISelectionService.class);
        if (selectionService == null) {
            return new IContributionItem[0];
        }
        ISelection selection = selectionService.getSelection();
        if (!(selection instanceof IStructuredSelection)) {
            return new IContributionItem[0];
        }

        Map<IProject, Set<IvyClasspathContainer>> containers = new HashMap<>();

        Map<IProject, Set<StandaloneRetrieveSetup>> retrieveSetups = new HashMap<>();

        // this give info about if the selection is only based of classpath containers
        boolean onlyContainers = true;

        int totalSelected = 0;

        for (Object element : ((IStructuredSelection) selection).toList()) {
            totalSelected++;
            boolean projectCollected = collectProject(containers, retrieveSetups, element);
            if (projectCollected) {
                onlyContainers = false;
            } else {
                IWorkingSet workingSet = IvyPlugin.adapt(element, IWorkingSet.class);
                if (workingSet != null) {
                    onlyContainers = false;
                    for (IAdaptable elem : workingSet.getElements()) {
                        collectProject(containers, retrieveSetups, elem);
                    }
                } else if (element instanceof ClassPathContainer) {
                    collectContainer(containers, (ClassPathContainer) element);
                }
            }
        }

        List<IContributionItem> items;
        MenuManager menuManager;
        if (onlyContainers) {
            // we we have only containers, no need to have a root menu entry
            menuManager = null;
            items = new ArrayList<>();
        } else {
            menuManager = new MenuManager("Ivy", IvyPlugin
                    .getImageDescriptor("icons/logo16x16.gif"), "org.apache.ivyde.eclipse.menu");
            items = Collections.<IContributionItem>singletonList(menuManager);
        }

        // add resolve, refresh, reload settings
        if (!containers.isEmpty()) {
            addCommand(menuManager, items, ResolveHandler.COMMAND_ID);
            addCommand(menuManager, items, RefreshHandler.COMMAND_ID);
            addCommand(menuManager, items, ReloadSettingsHandler.COMMAND_ID);
            fillMenu(menuManager, items, new IvyMenuSeparator());
        }

        // add retrieve
        if (!retrieveSetups.isEmpty()) {
            boolean oneProject = retrieveSetups.size() == 1 && totalSelected == 1;
            for (Entry<IProject, Set<StandaloneRetrieveSetup>> entry : retrieveSetups.entrySet()) {
                IProject project = entry.getKey();
                for (StandaloneRetrieveSetup retrieveSetup : entry.getValue()) {
                    RetrieveAction action = new RetrieveAction(retrieveSetup);
                    action.setText("Retrieve '" + retrieveSetup.getName()
                            + (oneProject ? "'" : "' of " + project.getName()));
                    fillMenu(menuManager, items, new ActionContributionItem(action));
                }
            }
            fillMenu(menuManager, items, new IvyMenuSeparator());
        }

        // add open file
        if (!containers.isEmpty()) {
            addCommand(menuManager, items, OpenIvyFileHandler.COMMAND_ID);
            fillMenu(menuManager, items, new IvyMenuSeparator());
        }

        // add clean cache
        if (!containers.isEmpty()) {
            if (totalSelected == 1 && containers.size() == 1
                    && containers.values().iterator().next().size() == 1) {
                // only one container
                IvyClasspathContainerImpl ivycp = (IvyClasspathContainerImpl) containers.values()
                        .iterator().next().iterator().next();
                Ivy ivy = ivycp.getState().getCachedIvy();
                if (ivy != null) {
                    addCleanableForSingleContainer(menuManager, items, ivy);
                }
            } else {
                addCleanableForManyContainers(menuManager, items, containers.values());
            }
            fillMenu(menuManager, items, new IvyMenuSeparator());
        }

        // add reverse dependency explorer
        if (!containers.isEmpty()) {
            addCommand(menuManager, items, ViewReverseDependenciesHandler.COMMAND_ID);
            fillMenu(menuManager, items, new IvyMenuSeparator());
        }

        // add remove ivy nature
        addCommand(menuManager, items, RemoveIvyNatureHandler.COMMAND_ID);

        return items.toArray(new IContributionItem[items.size()]);
    }

    private void addCommand(MenuManager menuManager, List<IContributionItem> items,
            String commandId) {
        CommandContributionItemParameter parm = new CommandContributionItemParameter(
                serviceLocator, null, commandId, CommandContributionItem.STYLE_PUSH);
        fillMenu(menuManager, items, new CommandContributionItem(parm));
    }

    private void fillMenu(MenuManager menuManager, List<IContributionItem> items,
            IContributionItem commandContributionItem) {
        if (menuManager != null) {
            menuManager.add(commandContributionItem);
        } else {
            items.add(commandContributionItem);
        }
    }

    private boolean collectProject(Map<IProject, Set<IvyClasspathContainer>> containers,
            Map<IProject, Set<StandaloneRetrieveSetup>> retrieveSetups, Object element) {
        IProject project = IvyPlugin.adapt(element, IProject.class);
        if (project != null && project.isOpen() && IvyNatureHelper.hasNature(project)) {
            doCollectProject(containers, retrieveSetups, project);
            return true;
        }
        return false;
    }

    private void doCollectProject(Map<IProject, Set<IvyClasspathContainer>> containers,
            Map<IProject, Set<StandaloneRetrieveSetup>> retrieveSetups, IProject project) {
        List<IvyClasspathContainer> containerList = IvyClasspathContainerHelper.getContainers(project);
        if (!containerList.isEmpty()) {
            containers.put(project, new HashSet<>(containerList));
        }
        RetrieveSetupManager manager = IvyPlugin.getDefault().getRetrieveSetupManager();
        List<StandaloneRetrieveSetup> setupList;
        try {
            setupList = manager.getSetup(project);
        } catch (IOException e) {
            IvyPlugin.logWarn("Unable to get the retrieve setup for project " + project.getName(),
                e);
            return;
        }
        if (!setupList.isEmpty()) {
            retrieveSetups.put(project, new HashSet<>(setupList));
        }
    }

    private boolean collectContainer(Map<IProject, Set<IvyClasspathContainer>> containers,
            ClassPathContainer element) {
        IvyClasspathContainerImpl ivycp = IvyClasspathUtil.jdt2IvyCPC(element);
        if (ivycp == null) {
            return false;
        }
        doCollectContainer(containers, ivycp);
        return true;
    }

    private void doCollectContainer(Map<IProject, Set<IvyClasspathContainer>> containers,
            IvyClasspathContainerImpl ivycp) {
        IJavaProject javaProject = ivycp.getConf().getJavaProject();
        if (javaProject == null) {
            return;
        }
        Set<IvyClasspathContainer> containerSet = containers.get(javaProject.getProject());
        if (containerSet == null) {
            containerSet = new HashSet<>();
            containers.put(javaProject.getProject(), containerSet);
        }
        containerSet.add(ivycp);
    }

    private void addCleanableForSingleContainer(MenuManager menuManager,
            List<IContributionItem> items, Ivy ivy) {
        List<Cleanable> allCleanables = new ArrayList<>();
        List<Cleanable> repositoryCleanables = new ArrayList<>();
        List<Cleanable> resolutionCleanables = new ArrayList<>();

        addResolutionCleanable(allCleanables, ivy);
        addResolutionCleanable(resolutionCleanables, ivy);

        addRepositoryCleanable(allCleanables, ivy);
        addRepositoryCleanable(repositoryCleanables, ivy);

        addCleanable(menuManager, items, "Clean all caches", allCleanables);
        addCleanable(menuManager, items, "Clean the resolution cache", resolutionCleanables);
        addCleanable(menuManager, items, "Clean every repository cache", repositoryCleanables);
        for (Cleanable cleanable : resolutionCleanables) {
            addCleanable(menuManager, items, "Clean the cache '" + cleanable.getName() + "'",
                    Collections.singletonList(cleanable));
        }
    }

    private void addCleanableForManyContainers(MenuManager menuManager,
            List<IContributionItem> items, Collection<Set<IvyClasspathContainer>> containerSets) {
        List<Cleanable> allCleanables = new ArrayList<>();
        List<Cleanable> repositoryCleanables = new ArrayList<>();
        List<Cleanable> resolutionCleanables = new ArrayList<>();

        for (Set<IvyClasspathContainer> containerSet : containerSets) {
            for (IvyClasspathContainer container : containerSet) {
                Ivy ivy = ((IvyClasspathContainerImpl) container).getState().getCachedIvy();
                if (ivy != null) {
                    addResolutionCleanable(allCleanables, ivy);
                    addResolutionCleanable(resolutionCleanables, ivy);

                    addRepositoryCleanable(allCleanables, ivy);
                    addRepositoryCleanable(repositoryCleanables, ivy);
                }
            }
        }
        addCleanable(menuManager, items, "Clean all caches", allCleanables);
        addCleanable(menuManager, items, "Clean every resolution cache", resolutionCleanables);
        addCleanable(menuManager, items, "Clean every repository cache", repositoryCleanables);
    }

    private void addResolutionCleanable(List<Cleanable> cleanables, Ivy ivy) {
        ResolutionCacheManager manager = ivy.getSettings().getResolutionCacheManager();
        cleanables.add(new ResolutionCacheCleanable(manager));
    }

    private void addRepositoryCleanable(List<Cleanable> cleanables, Ivy ivy) {
        for (RepositoryCacheManager manager : ivy.getSettings().getRepositoryCacheManagers()) {
            cleanables.add(new RepositoryCacheCleanable(manager));
        }
    }

    private void addCleanable(MenuManager menuManager, List<IContributionItem> items,
            String name, List<Cleanable> cleanables) {
        CleanCacheAction action = new CleanCacheAction(name, cleanables);
        action.setText(name);
        fillMenu(menuManager, items, new ActionContributionItem(action));
    }

}
