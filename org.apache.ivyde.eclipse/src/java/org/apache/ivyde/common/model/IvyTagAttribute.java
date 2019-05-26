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
package org.apache.ivyde.common.model;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class IvyTagAttribute {
    private static final ResourceBundle DOC_RESOURCE = ResourceBundle
            .getBundle(IvyTagAttribute.class.getPackage().getName() + ".tagsdoc");

    private IvyTag container;

    private String name;

    private String doc;

    private boolean mandatory = false;

    private IValueProvider valueProvider = null;

    /**
     * @param name Ivy tag attribute name
     */
    public IvyTagAttribute(String name) {
        super();
        this.name = name;
    }

    public IvyTagAttribute(String name, IValueProvider vp) {
        super();
        this.name = name;
        this.valueProvider = vp;
    }

    public IvyTagAttribute(String name, String doc) {
        super();
        this.name = name;
        this.doc = doc;
    }

    public IvyTagAttribute(String name, boolean mandatory) {
        this.name = name;
        this.mandatory = mandatory;
    }

    public IvyTagAttribute(String name, String doc, boolean mandatory) {
        super();
        this.name = name;
        this.doc = doc;
        this.mandatory = mandatory;
    }

    public IvyTagAttribute(String name, String doc, boolean mandatory, IValueProvider provider) {
        this.name = name;
        this.doc = doc;
        this.mandatory = mandatory;
        this.valueProvider = provider;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public String getDoc() {
        if (doc == null) {
            try {
                doc = DOC_RESOURCE.getString(getId());
            } catch (MissingResourceException ex) {
                doc = "";
            }
        }
        return doc;
    }

    private String getId() {
        if (getContainer() != null) {
            return getContainer().getId() + ".@" + getName();
        }
        return "@" + getName();
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public IvyTag getContainer() {
        return container;
    }

    public void setContainer(IvyTag container) {
        this.container = container;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public IValueProvider getValueProvider() {
        return valueProvider;
    }

    public void setValueProvider(IValueProvider valueProvider) {
        this.valueProvider = valueProvider;
    }
}
