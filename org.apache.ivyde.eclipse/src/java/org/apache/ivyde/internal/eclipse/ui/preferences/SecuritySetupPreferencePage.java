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
package org.apache.ivyde.internal.eclipse.ui.preferences;

import org.apache.ivyde.eclipse.IvyDEsecurityHelper;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.controller.SecuritySetupController;
import org.apache.ivyde.internal.eclipse.ui.SecuritySetupEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SecuritySetupPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /** the ID of the preference page. */
    public static final String PREFERENCE_PAGE_ID = "org.apache.ivyde.eclipse.ui.preferences.SecuritySetupPreferencePage";

    private SecuritySetupEditor securitySetupComposite;

    private SecuritySetupController buttonController;

    public SecuritySetupPreferencePage() {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        securitySetupComposite = new SecuritySetupEditor(parent, SWT.NONE);
        securitySetupComposite
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        buttonController = new SecuritySetupController(securitySetupComposite);
        buttonController.addHandlers();

        securitySetupComposite.init(IvyDEsecurityHelper.getCredentialsFromSecureStore());

        return securitySetupComposite;
    }

    /*
     * NOTE: The table containing the credentials is directly coupled with the eclipse
     * secure-storage:
     * <ul>
     * <li>all operations are performed immediately on the secure-storage</li>
     * <li>performOk(), performApply() and performDefaults() won't have any additional effects: They just
     * redo performed operations (for the sake of completeness)</li>
     * </ul>
     */

    @Override
    public boolean performOk() {
        // TODO: Do what? => directly coupled with secure-storage
        IvyDEsecurityHelper.cpyCredentialsFromSecureToIvyStorage();
        return true;
    }

    @Override
    protected void performApply() {
        // TODO: Do what? => directly coupled with secure-storage
        IvyDEsecurityHelper.cpyCredentialsFromSecureToIvyStorage();
        super.performApply();
    }

    @Override
    protected void performDefaults() {
        // TODO: Do nothing? => directly coupled with secure-storage...
        securitySetupComposite.init(IvyDEsecurityHelper.getCredentialsFromSecureStore());
        super.performDefaults();
    }
}
