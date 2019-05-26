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

import org.apache.ivyde.eclipse.IvyDEsecurityHelper;
import org.apache.ivyde.internal.eclipse.validator.BaseValidator;
import org.apache.ivyde.internal.eclipse.validator.IValidationReaction;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

public class IdValidator extends BaseValidator {

    private final boolean isAddOperation;

    private final String prevHostVal;

    private final String prevRealmVal;

    /**
     * @param reaction IValidationReaction
     * @param isAddOperation boolean
     * @param prevHostVal String
     * @param prevRealmVal String
     */
    public IdValidator(IValidationReaction reaction, boolean isAddOperation, String prevHostVal,
            String prevRealmVal) {
        super(reaction);
        this.isAddOperation = isAddOperation;
        this.prevHostVal = prevHostVal;
        this.prevRealmVal = prevRealmVal;
    }

    @Override
    public boolean doValidation(Object validatedObject) {
        String id = (String) validatedObject;
        String[] hostRealm = id.split("@");
        boolean valid = true;
        String message = VALID_MESSAGE;
        IStatus validationStatus;
        if (id.equals("") || id.equals("@")) {
            message = "Properties 'Host' and 'Realm' cannot be empty";
            valid = false;
        } else if (id.indexOf("@") == 0) {
            message = EMPTY_ERROR.replace("$entry", "Host");
            valid = false;
        } else if (id.indexOf("@") == id.length() - 1) {
            message = EMPTY_ERROR.replace("$entry", "Realm");
            valid = false;
        } else if (!isAddOperation && prevHostVal.equals(hostRealm[0])
                && prevRealmVal.equals(hostRealm[1])) {
            valid = true;
        } else if (IvyDEsecurityHelper.hostExistsInSecureStorage(hostRealm[0], hostRealm[1])) {
            message = EXISTING_ENTRY_ERROR;
            valid = false;
        }

        if (valid) {
            validationStatus = ValidationStatus.info(message);
        } else {
            validationStatus = ValidationStatus.error(message);
        }
        super.setValidationStatus(validationStatus);
        return valid;
    }
}
