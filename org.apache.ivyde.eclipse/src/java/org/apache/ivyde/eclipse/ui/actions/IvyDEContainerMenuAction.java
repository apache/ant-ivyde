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

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public abstract class IvyDEContainerMenuAction extends IvyDEContainerAction implements IMenuCreator {

    boolean selectionChanged;

    private IAction proxyAction;

    IvyClasspathContainer cp;

    private MenuListener menuListener = new MenuAdapter() {
        public void menuShown(MenuEvent e) {
            if (selectionChanged) {
                Menu m = (Menu) e.widget;
                MenuItem[] items = m.getItems();
                for (int i = 0; i < items.length; i++) {
                    items[i].dispose();
                }
                fill(m, cp);
                selectionChanged = false;
            }
        }
    };

    public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);
        fill(menu, cp);
        menu.addMenuListener(menuListener);
        return menu;
    }

    public Menu getMenu(Menu parent) {
        Menu menu = new Menu(parent);
        fill(menu, cp);
        menu.addMenuListener(menuListener);
        return menu;
    }

    abstract protected void fill(Menu menu, IvyClasspathContainer ivycp);

    public void run(IAction action) {
        // nothing to run
    }

    protected void selectionChanged(IAction a, IvyClasspathContainer ivycp) {
        this.cp = ivycp;
        selectionChanged = true;
        if (proxyAction != a) {
            proxyAction = a;
            proxyAction.setMenuCreator(this);
        }
    }

    public void dispose() {
        // nothing to dispose
    }

}
