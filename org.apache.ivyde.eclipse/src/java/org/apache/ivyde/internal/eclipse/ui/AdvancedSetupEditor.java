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

import org.apache.ivyde.eclipse.cp.AdvancedSetup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class AdvancedSetupEditor extends Composite {

    private final Button resolveBeforeLaunchCheck;

    private final Button useExtendedResolveIdCheck;

    public AdvancedSetupEditor(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout());

        resolveBeforeLaunchCheck = new Button(this, SWT.CHECK);
        resolveBeforeLaunchCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false));
        resolveBeforeLaunchCheck.setText("Resolve before launch");
        resolveBeforeLaunchCheck
                .setToolTipText("Trigger a resolve before each run of any kind of java launch configuration");

        useExtendedResolveIdCheck = new Button(this, SWT.CHECK);
        useExtendedResolveIdCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false));
        useExtendedResolveIdCheck.setText("Use extended resolve id");
        useExtendedResolveIdCheck
                .setToolTipText("Will append status, branch and revision info to the default resolve id");
    }

    public void init(AdvancedSetup setup, boolean forceResolveBeforeLaunch) {
        resolveBeforeLaunchCheck.setSelection(setup.isResolveBeforeLaunch());
        useExtendedResolveIdCheck.setSelection(setup.isUseExtendedResolveId());

        if (forceResolveBeforeLaunch) {
            // container in a launch config: always resolve before launch
            resolveBeforeLaunchCheck.setEnabled(false);
            resolveBeforeLaunchCheck.setSelection(true);
        }
    }

    public AdvancedSetup getAdvancedSetup() {
        AdvancedSetup setup = new AdvancedSetup();
        setup.setResolveBeforeLaunch(resolveBeforeLaunchCheck.getSelection());
        setup.setUseExtendedResolveId(useExtendedResolveIdCheck.getSelection());
        return setup;
    }

    public void setEnabled(boolean enabled) {
        resolveBeforeLaunchCheck.setEnabled(enabled);
        useExtendedResolveIdCheck.setEnabled(enabled);
        super.setEnabled(enabled);
    }
}
