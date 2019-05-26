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
package org.apache.ivyde.internal.eclipse.validator.reaction;

import org.apache.ivyde.internal.eclipse.validator.IValidationReaction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

public class GeneralValidationReaction implements IValidationReaction {

    private final Button okButton;

    @SuppressWarnings("unused")
    private final Label errorLabel;

    private final Label errorIcon;

    @Override
    public void ok() {
        this.okButton.setEnabled(true);
        // this.errorIcon.setVisible(false);
        // this.errorLabel.setVisible(false);
        this.errorIcon.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
    }

    /**
     * @param okButton Button
     * @param errorLabel Label
     * @param errorIcon Label
     */
    public GeneralValidationReaction(Button okButton, Label errorLabel, Label errorIcon) {
        this.okButton = okButton;
        this.errorLabel = errorLabel;
        this.errorIcon = errorIcon;
    }

    @Override
    public void error() {
        this.okButton.setEnabled(false);
        this.errorIcon.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));
        // this.errorLabel.setText("Insert credentials");
    }

}
