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
package org.apache.ivyde.internal.eclipse.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;

public abstract class BaseValidator implements IValidator {

    private final IValidationReaction reaction;

    private IStatus validationStatus;

    public static final String EMPTY_ERROR = "The property '$entry' cannot be empty";

    public static final String EXISTING_ENTRY_ERROR = "An entry with that host and realm already exists";

    public static final String VALID_MESSAGE = "Valid Ivy credentials: Press 'OK' to save them";

    public abstract boolean doValidation(Object validatedObject);

    @Override
    public IStatus validate(Object value) {
        if (doValidation(value)) {
            this.reaction.ok();
        } else {
            this.reaction.error();
        }
        return this.validationStatus;
    }

    /**
     * @param reaction IValidationReaction
     */
    public BaseValidator(IValidationReaction reaction) {
        this.reaction = reaction;
    }

    public void setValidationStatus(IStatus validationStatus) {
        this.validationStatus = validationStatus;
    }
}
