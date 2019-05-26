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
package org.apache.ivyde.internal.eclipse.ui.console;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

public class IvyConsolePageParticipant implements IConsolePageParticipant {

    private CloseConsoleAction closeAction;

    private IvyConsoleFilterAction filterLogAction;

    public void init(IPageBookViewPage page, IConsole console) {
        IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();

        closeAction = new IvyConsoleRemoveAction(console);
        manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);

        filterLogAction = new IvyConsoleFilterAction((IvyConsole) console);
        manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, filterLogAction);
    }

    public void dispose() {
        closeAction = null;
        filterLogAction = null;
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        return null;
    }

    public void activated() {
        // nothing to do
    }

    public void deactivated() {
        // nothing to do
    }

}
