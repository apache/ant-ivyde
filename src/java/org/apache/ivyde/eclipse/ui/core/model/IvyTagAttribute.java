/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.eclipse.ui.core.model;

public class IvyTagAttribute {
    private IvyTag _container;

    private String _name;

    private String _doc = "";

    private boolean _mandatory = false;

    private IValueProvider _valueProvider = null;

    /**
     * @param name
     */
    public IvyTagAttribute(String name) {
        super();
        _name = name;
    }

    public IvyTagAttribute(String name, IValueProvider vp) {
        super();
        _name = name;
        _valueProvider = vp;
    }

    public IvyTagAttribute(String name, String doc) {
        super();
        _name = name;
        _doc = doc;
    }

    public IvyTagAttribute(String name, String doc, boolean mandatory) {
        super();
        _name = name;
        _doc = doc;
        _mandatory = mandatory;
    }

    public IvyTagAttribute(String name, String doc, boolean mandatory, IValueProvider provider) {
        _name = name;
        _doc = doc;
        _mandatory = mandatory;
        _valueProvider = provider;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String toString() {
        return _name;
    }

    public String getDoc() {
        return _doc;
    }

    public void setDoc(String doc) {
        _doc = doc;
    }

    public IvyTag getContainer() {
        return _container;
    }

    public void setContainer(IvyTag container) {
        _container = container;
    }

    public boolean isMandatory() {
        return _mandatory;
    }

    public void setMandatory(boolean mandatory) {
        _mandatory = mandatory;
    }

    public IValueProvider getValueProvider() {
        return _valueProvider;
    }

    public void setValueProvider(IValueProvider valueProvider) {
        _valueProvider = valueProvider;
    }
}
