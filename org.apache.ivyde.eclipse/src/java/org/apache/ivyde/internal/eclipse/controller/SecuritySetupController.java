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
package org.apache.ivyde.internal.eclipse.controller;

import org.apache.ivyde.eclipse.GUIfactoryHelper;
import org.apache.ivyde.eclipse.IvyDEsecurityHelper;
import org.apache.ivyde.eclipse.cp.SecuritySetup;
import org.apache.ivyde.internal.eclipse.ui.SecuritySetupEditor;
import org.apache.ivyde.internal.eclipse.ui.components.SecuritySetupDialog;
import org.apache.ivyde.internal.eclipse.validator.BaseValidator;
import org.apache.ivyde.internal.eclipse.validator.IValidationReaction;
import org.apache.ivyde.internal.eclipse.validator.impl.HostValidator;
import org.apache.ivyde.internal.eclipse.validator.impl.IdValidator;
import org.apache.ivyde.internal.eclipse.validator.impl.PasswordValidator;
import org.apache.ivyde.internal.eclipse.validator.impl.RealmValidator;
import org.apache.ivyde.internal.eclipse.validator.impl.UserNameValidator;
import org.apache.ivyde.internal.eclipse.validator.reaction.GeneralValidationReaction;
import org.apache.ivyde.internal.eclipse.validator.reaction.NopValidationReaction;
import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Widget;

public class SecuritySetupController {

    private final SecuritySetupEditor setupEditorGUI;

    private final SecuritySetupDialog addDialog;

    private MessageDialog confirmationDialog;

    private SecuritySetup currentSelection = new SecuritySetup();

    private SecuritySetup currentSelectionOldVal = new SecuritySetup();

    private String selectionHost;

    private String selectionRealm;

    private String selectionUserName;

    private boolean addOperation = true;

    private final DataBindingContext ctx = new DataBindingContext();

    /**
     * @param setupEditorGUI SecuritySetupEditor
     */
    public SecuritySetupController(SecuritySetupEditor setupEditorGUI) {
        this.setupEditorGUI = setupEditorGUI;
        addDialog = new SecuritySetupDialog(setupEditorGUI.getShell());
    }

    public void addHandlers() {
        setupEditorGUI.getAddBtn().addSelectionListener(this.createAddBtnSelectionAdapter());
        setupEditorGUI.getEditBtn().addSelectionListener(this.createEditBtnSelectionAdapter());
        setupEditorGUI.getDeleteBtn().addSelectionListener(this.createDelBtnSelectionAdapter());
        setupEditorGUI.getTableViewer()
                .addSelectionChangedListener(this.createSelectionChangedListener());
    }

    private SelectionListener createAddBtnSelectionAdapter() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addOperation = true;
                currentSelection = new SecuritySetup();
                addDialog.create();
                initDialog();
                if (addDialog.open() == Window.OK) {
                    IvyDEsecurityHelper.addCredentialsToSecureStorage(currentSelection);
                    IvyDEsecurityHelper.addCredentialsToIvyCredentialStorage(currentSelection);
                    // TODO: using init to reload directly from secure storage or use an
                    // intermediate-container?
                    setupEditorGUI.init(IvyDEsecurityHelper.getCredentialsFromSecureStore());
                } else {
                    // TODO: do something?
                }
                addDialog.close();
            }
        };
    }

    private SelectionListener createEditBtnSelectionAdapter() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addOperation = false;
                addDialog.create();
                initDialog();
                // initDialog(currentSelection);
                if (addDialog.open() == Window.OK) {
                    IvyDEsecurityHelper.removeCredentials(
                        new SecuritySetup(selectionHost, selectionRealm, selectionUserName, ""));
                    IvyDEsecurityHelper.addCredentialsToSecureStorage(currentSelection);
                    IvyDEsecurityHelper.addCredentialsToIvyCredentialStorage(currentSelection);
                    // TODO: using init to reload directly from secure storage or use an
                    // intermediate-container?
                    setupEditorGUI.init(IvyDEsecurityHelper.getCredentialsFromSecureStore());
                    setupEditorGUI.getEditBtn().setEnabled(false);
                    setupEditorGUI.getDeleteBtn().setEnabled(false);
                } else {
                    currentSelection.setAllValues(currentSelectionOldVal);
                }
                addDialog.close();
            }
        };
    }

    private SelectionListener createDelBtnSelectionAdapter() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                confirmationDialog = GUIfactoryHelper.buildConfirmationDialog(
                    setupEditorGUI.getShell(), "Confirmation",
                    "Remove selected credentials from secure storage?");
                if (confirmationDialog.open() == Window.OK) {
                    currentSelection.setHost(selectionHost);
                    currentSelection.setRealm(selectionRealm);
                    IvyDEsecurityHelper.removeCredentials(
                        new SecuritySetup(selectionHost, selectionRealm, selectionUserName, ""));
                    setupEditorGUI.init(IvyDEsecurityHelper.getCredentialsFromSecureStore());
                    setupEditorGUI.getEditBtn().setEnabled(false);
                    setupEditorGUI.getDeleteBtn().setEnabled(false);
                }
                confirmationDialog.close();
            }
        };
    }

    private ISelectionChangedListener createSelectionChangedListener() {
        return new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                currentSelection = (SecuritySetup) selection.getFirstElement();
                setupEditorGUI.getEditBtn().setEnabled(true);
                setupEditorGUI.getDeleteBtn().setEnabled(true);
                if (currentSelection != null) {
                    selectionHost = currentSelection.getHost();
                    selectionRealm = currentSelection.getRealm();
                    selectionUserName = currentSelection.getUserName();
                    currentSelectionOldVal = new SecuritySetup(selectionHost, selectionRealm,
                            selectionUserName, currentSelection.getPwd());
                } else {
                    currentSelection = new SecuritySetup();
                }
            }
        };
    }

    private void createHostDataBinder(String selectedHost, String selectedRealm,
            boolean isAddOperation) {
        IValidationReaction generalValidationReaction = new GeneralValidationReaction(
                this.addDialog.getOkButton(), this.addDialog.getErrorLabel(),
                this.addDialog.getErrorIcon());
        IValidationReaction nopValidationReaction = new NopValidationReaction();

        BaseValidator hostValidator = new HostValidator(generalValidationReaction);
        BaseValidator realmValidator = new RealmValidator(generalValidationReaction);
        BaseValidator idValidator = new IdValidator(generalValidationReaction, isAddOperation,
                selectedHost, selectedRealm);
        BaseValidator userNameValidator = new UserNameValidator(nopValidationReaction);
        BaseValidator passwordValidator = new PasswordValidator(nopValidationReaction);

        this.addDataBinder(this.addDialog.getIdText(), idValidator, SecuritySetup.class, "id",
            this.currentSelection, true);
        this.addDataBinder(this.addDialog.getHostText(), hostValidator, SecuritySetup.class, "host",
            this.currentSelection, true);
        this.addDataBinder(this.addDialog.getRealmText(), realmValidator, SecuritySetup.class,
            "realm", this.currentSelection, true);
        this.addDataBinder(this.addDialog.getUserNameText(), userNameValidator, SecuritySetup.class,
            "userName", this.currentSelection, true);
        this.addDataBinder(this.addDialog.getPwdText(), passwordValidator, SecuritySetup.class,
            "pwd", this.currentSelection, true);
    }

    private void addDataBinder(Widget toObserve, IValidator validator, Class<?> observableClass,
            String propertyName, Object observedProperty, boolean textDecorationEnabled) {
        IObservableValue textObservable = WidgetProperties.text(SWT.Modify).observe(toObserve);
        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator(validator);

        ValidationStatusProvider binding = this.ctx.bindValue(textObservable,
            PojoProperties.value(observableClass, propertyName).observe(observedProperty), strategy,
            null);
        if (textDecorationEnabled) {
            ControlDecorationSupport.create(binding, SWT.LEFT);
        }
        final IObservableValue errorObservable = WidgetProperties.text()
                .observe(this.addDialog.getErrorLabel());

        ctx.bindValue(errorObservable, new AggregateValidationStatus(ctx.getBindings(),
                AggregateValidationStatus.MAX_SEVERITY),
            null, null);

    }

    private void initDialog() {
        this.createHostDataBinder(this.selectionHost, this.selectionRealm, this.addOperation);

        addDialog.getHostText().addModifyListener(createModifyListener());
        addDialog.getRealmText().addModifyListener(createModifyListener());
    }

    private ModifyListener createModifyListener() {
        return new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                addDialog.getIdText().setText(
                    addDialog.getHostText().getText() + "@" + addDialog.getRealmText().getText());
            }
        };
    }
}
