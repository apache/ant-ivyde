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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.Cleanable;
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.RepositoryCacheCleanable;
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.ResolutionCacheCleanable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

public class CleanCacheMenuAction implements IObjectActionDelegate, IMenuCreator,
        IWorkbenchWindowPulldownDelegate2 {

    IStructuredSelection selection;

    public void init(IWorkbenchWindow window) {
    }

    public void dispose() {
    }

    public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);
        fill(menu);
        return menu;
    }

    public Menu getMenu(Menu parent) {
        Menu menu = new Menu(parent);
        fill(menu);
        return menu;
    }

    private void fill(Menu menu) {
        IvyClasspathContainer cp = IvyClasspathUtil.getIvyClasspathContainer(selection);
        if (cp != null) {
            try {
                fill(menu, cp.getConf().getIvy());
            } catch (IvyDEException e) {
                e.log(IStatus.WARNING,
                    "Cache delection actions in the context menu could not be populated. ");
            }
            return;
        }
    }

    private void fill(Menu menu, Ivy ivy) {
        ResolutionCacheManager resolutionCacheManager = ivy.getSettings()
                .getResolutionCacheManager();
        ResolutionCacheCleanable resolutionCacheCleanable = new ResolutionCacheCleanable(
                resolutionCacheManager);

        RepositoryCacheManager[] repositoryCacheManagers = ivy.getSettings()
                .getRepositoryCacheManagers();
        RepositoryCacheCleanable[] repositoryCacheCleanables = new RepositoryCacheCleanable[repositoryCacheManagers.length];
        for (int i = 0; i < repositoryCacheManagers.length; i++) {
            repositoryCacheCleanables[i] = new RepositoryCacheCleanable(repositoryCacheManagers[i]);
        }

        Cleanable[] all = new Cleanable[repositoryCacheManagers.length + 1];
        all[0] = resolutionCacheCleanable;
        System.arraycopy(repositoryCacheCleanables, 0, all, 1, repositoryCacheManagers.length);

        add(menu, "All", all);
        add(menu, "Resolution cache", resolutionCacheCleanable);
        add(menu, "Every repository cache", repositoryCacheCleanables);
        for (int i = 0; i < repositoryCacheManagers.length; i++) {
            add(menu, "Cache '" + repositoryCacheManagers[i].getName() + "'",
                repositoryCacheCleanables[i]);
        }
    }

    public void add(Menu menu, String name, Cleanable[] cleanables) {
        add(menu, name, new CleanCacheAction(cleanables));
    }

    public void add(Menu menu, String name, Cleanable cleanable) {
        add(menu, name, new CleanCacheAction(cleanable));
    }

    public void add(Menu menu, String name, CleanCacheAction action) {
        action.setText(name);
        new ActionContributionItem(action).fill(menu, -1);
    }

    public void run(IAction action) {
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            this.selection = (IStructuredSelection) selection;
            action.setMenuCreator(this);
            action.setEnabled(!selection.isEmpty());
        }
    }

}
