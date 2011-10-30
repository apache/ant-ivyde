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
package org.apache.ivyde.eclipse.ui;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.AdvancedSetup;
import org.apache.ivyde.eclipse.ui.preferences.AdvancedSetupPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class AdvancedSetupTab extends AbstractSetupTab {

    private AdvancedSetupEditor advancedSetupEditor;

    public AdvancedSetupTab(TabFolder tabs) {
        super(tabs, "Advanced", AdvancedSetupPreferencePage.PEREFERENCE_PAGE_ID);
    }

    protected Composite createSetupEditor(Composite configComposite) {
        advancedSetupEditor = new AdvancedSetupEditor(configComposite, SWT.NONE);
        advancedSetupEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        return advancedSetupEditor;
    }

    public void init(boolean isProjectSpecific, AdvancedSetup setup, boolean forceResolveBeforeLaunch) {
        init(isProjectSpecific);
        if (isProjectSpecific) {
            advancedSetupEditor.init(setup, forceResolveBeforeLaunch);
        } else {
            advancedSetupEditor.init(IvyPlugin.getPreferenceStoreHelper().getAdvancedSetup(), forceResolveBeforeLaunch);
        }
    }

    public AdvancedSetupEditor getAdvancedSetupEditor() {
        return advancedSetupEditor;
    }

}