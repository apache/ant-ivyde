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

import org.apache.ivy.Ivy;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathInitializer;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Constants;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace
 * that allows us to create a page that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that
 * belongs to the main plug-in class. That way, preferences can be accessed directly via the
 * preference store.
 */

public class IvyPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /** the ID of the preference page. */
    public static final String PREFERENCE_PAGE_ID
            = "org.apache.ivyde.eclipse.ui.preferences.IvyPreferencePage";

    private Text organizationText;

    private Text organizationUrlText;

    private Button refreshOnStartupButton;

    private Button resolveOnStartupButton;

    private Button doNothingOnStartupButton;

    private Button openIvyConsoleOnStartupButton;

    private Button resolveOnChangeButton;

    private Button doNothingOnChangeButton;

    private Button offlineButton;

    private Button errorPopupButton;

    public IvyPreferencePage() {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
        Object ivydeVersion = IvyPlugin.getDefault().getBundle().getHeaders().get(
            Constants.BUNDLE_VERSION);
        setDescription("Ivy " + Ivy.getIvyVersion() + " (" + Ivy.getIvyDate() + ")  --  IvyDE "
                + ivydeVersion);
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(IvyPlugin.getDefault().getPreferenceStore());
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        composite.setLayout(new GridLayout());

        // CheckStyle:MagicNumber| OFF

        Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        offlineButton = new Button(composite, SWT.CHECK);
        offlineButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        offlineButton.setText("Offline (use only the caches)");

        openIvyConsoleOnStartupButton = new Button(composite, SWT.CHECK);
        openIvyConsoleOnStartupButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false));
        openIvyConsoleOnStartupButton.setText("Open the Ivy console on startup");

        Group startupGroup = new Group(composite, SWT.NONE);
        startupGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        startupGroup.setLayout(new GridLayout());
        startupGroup.setText("On Eclipse startup");

        doNothingOnStartupButton = new Button(startupGroup, SWT.RADIO);
        doNothingOnStartupButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false));
        doNothingOnStartupButton.setText("Do nothing");

        refreshOnStartupButton = new Button(startupGroup, SWT.RADIO);
        refreshOnStartupButton
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        refreshOnStartupButton.setText("Trigger refresh");

        resolveOnStartupButton = new Button(startupGroup, SWT.RADIO);
        resolveOnStartupButton
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        resolveOnStartupButton.setText("Trigger resolve");

        Group fileChangeGroup = new Group(composite, SWT.NONE);
        fileChangeGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        fileChangeGroup.setLayout(new GridLayout());
        fileChangeGroup.setText("On Ivy File Change");

        doNothingOnChangeButton = new Button(fileChangeGroup, SWT.RADIO);
        doNothingOnChangeButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false));
        doNothingOnChangeButton.setText("Do nothing");

        resolveOnChangeButton = new Button(fileChangeGroup, SWT.RADIO);
        resolveOnChangeButton
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        resolveOnChangeButton.setText("Trigger resolve");

        Group editorGroup = new Group(composite, SWT.NONE);
        editorGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        editorGroup.setLayout(new GridLayout(2, false));
        editorGroup.setText("Editor information");

        Label label = new Label(editorGroup, SWT.NONE);
        label.setText("Organisation:");
        organizationText = new Text(editorGroup, SWT.SINGLE | SWT.BORDER);
        organizationText
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        label = new Label(editorGroup, SWT.NONE);
        label.setText("Organisation URL:");
        organizationUrlText = new Text(editorGroup, SWT.SINGLE | SWT.BORDER);
        organizationUrlText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false));

        errorPopupButton = new Button(composite, SWT.CHECK);
        errorPopupButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        errorPopupButton.setText("Open a popup on each failed resolve");

        // CheckStyle:MagicNumber| ON

        initPreferences();

        return composite;
    }

    private void initPreferences() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();
        switch (helper.getResolveOnStartup()) {
            case IvyClasspathInitializer.ON_STARTUP_NOTHING:
                doNothingOnStartupButton.setSelection(true);
                break;
            case IvyClasspathInitializer.ON_STARTUP_REFRESH:
                refreshOnStartupButton.setSelection(true);
                break;
            case IvyClasspathInitializer.ON_STARTUP_RESOLVE:
                resolveOnStartupButton.setSelection(true);
                break;
            default:
                IvyPlugin.logWarn("Unknown resolve-on-startup mode: "
                        + helper.getResolveOnStartup());
        }

        if (helper.getAutoResolveOnChange()) {
            resolveOnChangeButton.setSelection(true);
        } else {
            doNothingOnChangeButton.setSelection(true);
        }

        openIvyConsoleOnStartupButton.setSelection(helper.isOpenIvyConsoleOnStartup());

        organizationText.setText(helper.getIvyOrg());
        organizationUrlText.setText(helper.getIvyOrgUrl());

        offlineButton.setSelection(helper.isOffline());
        errorPopupButton.setSelection(helper.isErrorPopup());
    }

    public boolean performOk() {
        IvyDEPreferenceStoreHelper helper = IvyPlugin.getPreferenceStoreHelper();

        if (doNothingOnStartupButton.getSelection()) {
            helper.setResolveOnStartup(IvyClasspathInitializer.ON_STARTUP_NOTHING);
        } else if (refreshOnStartupButton.getSelection()) {
            helper.setResolveOnStartup(IvyClasspathInitializer.ON_STARTUP_REFRESH);
        } else {
            helper.setResolveOnStartup(IvyClasspathInitializer.ON_STARTUP_RESOLVE);
        }

        if (resolveOnChangeButton.getSelection()) {
            helper.setAutoResolveOnChange(true);
        } else {
            helper.setAutoResolveOnChange(false);
        }

        helper.setOpenIvyConsoleOnStartup(openIvyConsoleOnStartupButton.getSelection());

        helper.setOrganization(organizationText.getText());
        helper.setOrganizationUrl(organizationUrlText.getText());

        helper.setOffline(offlineButton.getSelection());
        helper.setErrorPopup(errorPopupButton.getSelection());
        return true;
    }

    protected void performDefaults() {
        switch (PreferenceInitializer.DEFAULT_RESOLVE_ON_STARTUP) {
            case IvyClasspathInitializer.ON_STARTUP_NOTHING:
                doNothingOnStartupButton.setSelection(true);
                break;
            case IvyClasspathInitializer.ON_STARTUP_REFRESH:
                refreshOnStartupButton.setSelection(true);
                break;
            case IvyClasspathInitializer.ON_STARTUP_RESOLVE:
                resolveOnStartupButton.setSelection(true);
                break;
            default:
                IvyPlugin.logWarn("Unknown resolve-on-startup mode: "
                        + PreferenceInitializer.DEFAULT_RESOLVE_ON_STARTUP);
        }

        if (PreferenceInitializer.DEFAULT_AUTO_RESOLVE_ON_CHANGE) {
            resolveOnChangeButton.setSelection(true);
        } else {
            doNothingOnChangeButton.setSelection(true);
        }

        openIvyConsoleOnStartupButton
                .setSelection(PreferenceInitializer.DEFAULT_OPEN_IVY_CONSOLE_ON_STARTUP);

        organizationText.setText(PreferenceInitializer.DEFAULT_ORGANISATION);
        organizationUrlText.setText(PreferenceInitializer.DEFAULT_ORGANISATION_URL);

        offlineButton.setSelection(PreferenceInitializer.DEFAULT_OFFLINE);
        errorPopupButton.setSelection(PreferenceInitializer.DEFAULT_ERROR_POPUP);
    }
}
