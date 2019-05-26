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
package org.apache.ivyde.internal.eclipse.validator.impl;

import org.apache.ivyde.internal.eclipse.validator.BaseValidator;
import org.apache.ivyde.internal.eclipse.validator.IValidationReaction;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

public class HostValidator extends BaseValidator {

    /**
     * @param reaction IValidationReaction
     */
    public HostValidator(IValidationReaction reaction) {
        super(reaction);
    }

    @Override
    public boolean doValidation(Object validatedObject) {
        String host = (String) validatedObject;
        boolean valid = !host.equals("");
        IStatus validationStatus = valid ? ValidationStatus.ok()
                : ValidationStatus.error(EMPTY_ERROR.replace("$entry", "Host"));
        super.setValidationStatus(validationStatus);
        return !host.equals("");
    }
}
