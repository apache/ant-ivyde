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
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.Cleanable;
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.RepositoryCacheCleanable;
import org.apache.ivyde.eclipse.ui.actions.CleanCacheAction.ResolutionCacheCleanable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class CleanCacheMenuAction extends IvyDEContainerMenuAction {

    protected void fill(Menu menu, IvyClasspathContainer ivycp) {
        try {
            fill(menu, ivycp.getConf().getIvy());
        } catch (IvyDEException e) {
            e.log(IStatus.WARNING,
                "Cache delection actions in the context menu could not be populated. ");
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

}
