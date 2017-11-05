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
package org.apache.ivyde.internal.eclipse.ui.console;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class IvyConsoleFilterAction extends Action implements IMenuCreator {

    private static final String[] LOG_LEVEL_MESSAGES = {"error", "warning", "info", "verbose",
            "debug"};

    private Menu fMenu;

    private final IvyConsole console;

    public IvyConsoleFilterAction(IvyConsole console) {
        this.console = console;
        setText("Log filter");
        setToolTipText("Filter the log level of the Ivy console");
        setImageDescriptor(IvyPlugin.getImageDescriptor("/icons/log_level.png"));
        setMenuCreator(this);
    }

    public void dispose() {
        if (fMenu != null) {
            fMenu.dispose();
        }
    }

    public Menu getMenu(Menu parent) {
        return null;
    }

    public Menu getMenu(Control parent) {
        if (fMenu != null) {
            fMenu.dispose();
        }

        fMenu = new Menu(parent);
        for (int i = 0; i < LOG_LEVEL_MESSAGES.length; i++) {
            final int logLevel = i;
            Action action = new Action(LOG_LEVEL_MESSAGES[i]) {
                public void run() {
                    console.setLogLevel(logLevel);
                }
            };
            action.setChecked(console.getLogLevel() == i);
            addActionToMenu(fMenu, action);
        }
        new Separator().fill(fMenu, -1);
        for (int i = 0; i < LOG_LEVEL_MESSAGES.length; i++) {
            final int logLevel = i;
            Action action = new Action("IvyDE " + LOG_LEVEL_MESSAGES[i]) {
                public void run() {
                    console.getIvyDEMessageLogger().setLogLevel(logLevel);
                }
            };
            action.setChecked(console.getIvyDEMessageLogger().getLogLevel() == i);
            addActionToMenu(fMenu, action);
        }
        return fMenu;
    }

    private void addActionToMenu(Menu parent, Action action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(parent, -1);
    }

    public void run() {
        // nothing to do
    }

}
