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

import org.apache.ivyde.eclipse.cp.ClasspathSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.ui.preferences.ClasspathSetupPreferencePage;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class ClasspathSetupTab extends AbstractSetupTab {

    private ClasspathSetupEditor classpathSetupEditor;

    public ClasspathSetupTab(TabFolder tabs, IProject project) {
        super(tabs, "Classpath", ClasspathSetupPreferencePage.PREFERENCE_PAGE_ID, project);
    }

    protected Composite createSetupEditor(Composite configComposite, IProject project) {
        classpathSetupEditor = new ClasspathSetupEditor(configComposite, SWT.NONE, project);
        classpathSetupEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        return classpathSetupEditor;
    }

    public void init(boolean isProjectSpecific, ClasspathSetup setup) {
        init(isProjectSpecific);
        if (isProjectSpecific) {
            classpathSetupEditor.init(setup);
        } else {
            classpathSetupEditor.init(IvyPlugin.getPreferenceStoreHelper().getClasspathSetup());
        }
    }

    public ClasspathSetupEditor getClasspathSetupEditor() {
        return classpathSetupEditor;
    }
}
