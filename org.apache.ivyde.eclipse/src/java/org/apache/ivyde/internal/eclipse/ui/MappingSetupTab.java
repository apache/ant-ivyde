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
package org.apache.ivyde.internal.eclipse.ui;

import org.apache.ivyde.eclipse.cp.MappingSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.preferences.MappingSetupPreferencePage;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class MappingSetupTab extends AbstractSetupTab {

    private MappingSetupEditor mappingSetupEditor;

    public MappingSetupTab(TabFolder tabs, IProject project) {
        super(tabs, "Source/Javadoc", MappingSetupPreferencePage.PREFERENCE_PAGE_ID, project);
    }

    protected Composite createSetupEditor(Composite configComposite, IProject project) {
        mappingSetupEditor = new MappingSetupEditor(configComposite, SWT.NONE);
        mappingSetupEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        return mappingSetupEditor;
    }

    public void init(boolean isProjectSpecific, MappingSetup setup) {
        init(isProjectSpecific);
        if (isProjectSpecific) {
            mappingSetupEditor.init(setup);
        } else {
            mappingSetupEditor.init(IvyPlugin.getPreferenceStoreHelper().getMappingSetup());
        }
    }

    public MappingSetupEditor getMappingSetupEditor() {
        return mappingSetupEditor;
    }

}
