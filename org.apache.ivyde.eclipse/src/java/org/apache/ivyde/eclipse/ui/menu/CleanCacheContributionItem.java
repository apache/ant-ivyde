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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.handlers.AbstractIvyDEHandler;
import org.apache.ivyde.eclipse.ui.menu.CleanCacheAction.Cleanable;
import org.apache.ivyde.eclipse.ui.menu.CleanCacheAction.RepositoryCacheCleanable;
import org.apache.ivyde.eclipse.ui.menu.CleanCacheAction.ResolutionCacheCleanable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class CleanCacheContributionItem extends CompoundContributionItem implements
        IWorkbenchContribution {

    private IServiceLocator serviceLocator;

    public CleanCacheContributionItem() {
        // nothing to do
    }

    public CleanCacheContributionItem(String id) {
        super(id);
    }

    public void initialize(IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
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

        Map/* <IProject, Set<IvyClasspathContainer>> */projects = AbstractIvyDEHandler
                .getProjectAndContainers((IStructuredSelection) selection);

        List/* <IContributionItem> */items = new ArrayList();

        if (projects.size() == 1 && ((Set) projects.values().iterator().next()).size() == 1) {
            // only one container
            IvyClasspathContainer ivycp = (IvyClasspathContainer) ((Set) projects.values()
                    .iterator().next()).iterator().next();
            Ivy ivy = getSafelyIvy(ivycp);
            if (ivy == null) {
                return new IContributionItem[0];
            }
            fillForSingleContainer(items, ivy);
        } else {
            fillForManyContainers(items, projects.values());
        }

        return (IContributionItem[]) items.toArray(new IContributionItem[items.size()]);
    }

    private Ivy getSafelyIvy(IvyClasspathContainer ivycp) {
        try {
            return ivycp.getState().getCachedIvy();
        } catch (IvyDEException e) {
            e.log(IStatus.WARNING, "Cache deletion actions could not be populated for "
                    + ivycp.getConf().toString());
            return null;
        }
    }

    private void fillForSingleContainer(List/* <IContributionItem> */items, Ivy ivy) {
        List/* <Cleanable> */allCleanables = new ArrayList();
        List/* <Cleanable> */repositoryCleanables = new ArrayList();
        List/* <Cleanable> */resolutionCleanables = new ArrayList();

        addResolutionCleanable(allCleanables, ivy);
        addResolutionCleanable(resolutionCleanables, ivy);

        addRepositoryCleanable(allCleanables, ivy);
        addRepositoryCleanable(repositoryCleanables, ivy);

        add(items, "Clean all caches", allCleanables);
        add(items, "Clean the resolution cache", resolutionCleanables);
        add(items, "Clean every repository cache", repositoryCleanables);
        Iterator itCleanble = resolutionCleanables.iterator();
        while (itCleanble.hasNext()) {
            Cleanable cleanable = (Cleanable) itCleanble.next();
            add(items, "Clean the cache '" + cleanable.getName() + "'", Collections.singletonList(cleanable));
        }
    }

    private void fillForManyContainers(List/* <IContributionItem> */items,
            Collection/*
                       * <Set<IvyClasspathContainer >>
                       */containerSets) {
        List/* <Cleanable> */allCleanables = new ArrayList();
        List/* <Cleanable> */repositoryCleanables = new ArrayList();
        List/* <Cleanable> */resolutionCleanables = new ArrayList();

        Iterator itSet = containerSets.iterator();
        while (itSet.hasNext()) {
            Set set = (Set) itSet.next();
            Iterator itContainer = set.iterator();
            while (itContainer.hasNext()) {
                IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainer.next();
                Ivy ivy = getSafelyIvy(ivycp);
                if (ivy != null) {
                    addResolutionCleanable(allCleanables, ivy);
                    addResolutionCleanable(resolutionCleanables, ivy);

                    addRepositoryCleanable(allCleanables, ivy);
                    addRepositoryCleanable(repositoryCleanables, ivy);
                }
            }
        }
        add(items, "Clean all caches", allCleanables);
        add(items, "Clean every resolution cache", resolutionCleanables);
        add(items, "Clean every repository cache", repositoryCleanables);
    }

    private void addResolutionCleanable(List/* <Cleanable> */cleanables, Ivy ivy) {
        ResolutionCacheManager manager = ivy.getSettings().getResolutionCacheManager();
        cleanables.add(new ResolutionCacheCleanable(manager));
    }

    private void addRepositoryCleanable(List/* <Cleanable> */cleanables, Ivy ivy) {
        RepositoryCacheManager[] managers = ivy.getSettings().getRepositoryCacheManagers();
        for (int i = 0; i < managers.length; i++) {
            cleanables.add(new RepositoryCacheCleanable(managers[i]));
        }
    }

    public void add(List/* <IContributionItem> */items, String name,
            List/* <Cleanable> */cleanables) {
        CleanCacheAction action = new CleanCacheAction(cleanables);
        action.setText(name);
        items.add(new ActionContributionItem(action));
    }

}
