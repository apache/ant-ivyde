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

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class IvyMenuSeparator extends ContributionItem {

    public void fill(Menu menu, int index) {
        if (index >= 0) {
            new MenuItem(menu, SWT.SEPARATOR, index);
        } else {
            new MenuItem(menu, SWT.SEPARATOR);
        }
    }

    public void fill(ToolBar toolbar, int index) {
        if (index >= 0) {
            new ToolItem(toolbar, SWT.SEPARATOR, index);
        } else {
            new ToolItem(toolbar, SWT.SEPARATOR);
        }
    }

    public boolean isSeparator() {
        return true;
    }

}
