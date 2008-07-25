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
package org.apache.ivyde.common.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class IvyTag {
    private final static ResourceBundle doc = ResourceBundle.getBundle(
        IvyTag.class.getPackage().getName() + ".tagsdoc");
    
    private IvyTag _parent;

    private String _name;

    private String _doc;

    private Map _attributes = new HashMap();

    private List _childs = new ArrayList();

    private boolean _allowNoChild = true;

    /**
     * @param name
     */
    public IvyTag(String name) {
        super();
        _name = name;
    }

    public IvyTag(String name, IvyTagAttribute[] atts) {
        super();
        _name = name;
        for (int i = 0; i < atts.length; i++) {
            addAttribute(atts[i]);
        }
    }

    public IvyTag(String name, String doc) {
        _name = name;
        _doc = doc;
    }

    public IvyTag(String name, String doc, IvyTagAttribute[] atts) {
        _name = name;
        _doc = doc;
        for (int i = 0; i < atts.length; i++) {
            addAttribute(atts[i]);
        }
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void addAttribute(IvyTagAttribute attribute) {
        attribute.setContainer(this);
        _attributes.put(attribute.getName(), attribute);
    }

    public IvyTag addChildIvyTag(IvyTag att) {
        att.setParent(this);
        _childs.add(att);
        return this;
    }

    public boolean hasChild() {
        return _childs.size() > 0;
    }

    public List getAttributes() {
        return new ArrayList(_attributes.values());
    }

    public List getChilds() {
        return _childs;
    }

    public String getEndTag() {
        if (getChilds().size() > 0) {
            return "</" + getName() + ">";
        } else {
            return "/>";
        }
    }

    public String getStartTag() {
        return "<" + getName() + (getChilds().size() > 0 ? ">" : "");
    }

    public String getDoc() {
        if (_doc == null) {
            try {
                _doc = doc.getString(getId());
            } catch (MissingResourceException ex) {
                _doc = "";
            }
        }
        return _doc;
    }

    public String getId() {
        if (getParent() != null) {
            return getParent().getId() + "." + getName();
        }
        return getName();
    }

    public void setDoc(String doc) {
        _doc = doc;
    }

    public IvyTag getParent() {
        return _parent;
    }

    void setParent(IvyTag parent) {
        _parent = parent;
    }

    public String toString() {
        return _name;
    }

    public String[] getPossibleValuesForAttribute(String att, IvyFile ivyfile) {
        IvyTagAttribute ivyTagAttribute = (IvyTagAttribute) _attributes.get(att);
        if (ivyTagAttribute == null) {
            return null;
        }
        IValueProvider provider = ivyTagAttribute.getValueProvider();
        if (provider != null) {
            String qualifier = ivyfile.getAttributeValueQualifier();
            String[] values = provider.getValuesfor(ivyTagAttribute, ivyfile);
            if (values != null) {
                Set ret = new HashSet(values.length);
                for (int i = 0; i < values.length; i++) {
                    if (values[i].startsWith(qualifier)) {
                        ret.add(values[i]);
                    }
                }
                return (String[]) ret.toArray(new String[ret.size()]);
            } else {
                return null;
            }
        } else {
            System.out.println("No provider set for:" + att);
        }
        return null;
    }

    public boolean isAllowNoChild() {
        return _allowNoChild;
    }

    public void setAllowNoChild(boolean allowNoChild) {
        _allowNoChild = allowNoChild;
    }

    public Proposal[] getProposals() {
        List ret = new ArrayList();
        // Yes -- compute whole proposal text
        String text = getStartTag() + getEndTag();
        // Derive cursor position
        int cursor = getStartTag().length();
        ret.add(new Proposal(text, cursor, getDoc()));

        if (_allowNoChild && getChilds().size() > 0) {
            ret.add(new Proposal("<" + getName() + " />", cursor, getDoc()));
        }
        return (Proposal[]) ret.toArray(new Proposal[ret.size()]);
    }

}
