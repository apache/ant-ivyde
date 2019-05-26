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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class IvyTag {
    private static final ResourceBundle DOC_RESOURCE = ResourceBundle.getBundle(IvyTag.class
            .getPackage().getName() + ".tagsdoc");

    private IvyTag parent;

    private String name;

    private String doc;

    private final Map<String, IvyTagAttribute> attributes = new HashMap<>();

    private final List<IvyTag> childs = new ArrayList<>();

    private boolean allowNoChild = true;

    /**
     * @param name Ivy tag name
     */
    public IvyTag(String name) {
        super();
        this.name = name;
    }

    public IvyTag(String name, IvyTagAttribute[] atts) {
        super();
        this.name = name;
        for (IvyTagAttribute att : atts) {
            addAttribute(att);
        }
    }

    public IvyTag(String name, String doc) {
        this.name = name;
        this.doc = doc;
    }

    public IvyTag(String name, String doc, IvyTagAttribute[] atts) {
        this.name = name;
        this.doc = doc;
        for (IvyTagAttribute att : atts) {
            addAttribute(att);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAttribute(IvyTagAttribute attribute) {
        attribute.setContainer(this);
        attributes.put(attribute.getName(), attribute);
    }

    public IvyTag addChildIvyTag(IvyTag att) {
        att.setParent(this);
        childs.add(att);
        return this;
    }

    public boolean hasChild() {
        return childs.size() > 0;
    }

    public List<IvyTagAttribute> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    public List<IvyTag> getChilds() {
        return childs;
    }

    public String getEndTag() {
        if (getChilds().size() == 0) {
            return "/>";
        }

        return "</" + getName() + ">";
    }

    public String getStartTag() {
        return "<" + getName() + (getChilds().size() > 0 ? ">" : "");
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

    public String getId() {
        if (getParent() == null) {
            return getName();
        }

        return getParent().getId() + "." + getName();
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public IvyTag getParent() {
        return parent;
    }

    void setParent(IvyTag parent) {
        this.parent = parent;
    }

    public String toString() {
        return name;
    }

    public String[] getPossibleValuesForAttribute(String att, IvyFile ivyfile) {
        IvyTagAttribute ivyTagAttribute = attributes.get(att);
        if (ivyTagAttribute == null) {
            return null;
        }
        IValueProvider provider = ivyTagAttribute.getValueProvider();
        if (provider == null) {
            System.out.println("No provider set for:" + att);
        } else {
            String qualifier = ivyfile.getAttributeValueQualifier();
            String[] values = provider.getValuesfor(ivyTagAttribute, ivyfile);
            if (values != null) {
                Set<String> ret = new HashSet<>(values.length);
                for (String value : values) {
                    if (value.startsWith(qualifier)) {
                        ret.add(value);
                    }
                }
                return ret.toArray(new String[ret.size()]);
            }
        }
        return null;
    }

    public String getPossibleDocForValue(String value, IvyFile ivyfile) {
        IvyTagAttribute ivyTagAttribute = attributes.get(ivyfile.getAttributeName());
        if (ivyTagAttribute == null) {
            return null;
        }
        IValueProvider provider = ivyTagAttribute.getValueProvider();
        if (provider instanceof IDocumentedValueProvider) {
            return ((IDocumentedValueProvider) provider).getDocForValue(value, ivyfile);
        }
        return null;
    }

    public boolean isAllowNoChild() {
        return allowNoChild;
    }

    public void setAllowNoChild(boolean allowNoChild) {
        this.allowNoChild = allowNoChild;
    }

    public Proposal[] getProposals() {
        List<Proposal> ret = new ArrayList<>();
        // Yes -- compute whole proposal text
        String text = getStartTag() + getEndTag();
        // Derive cursor position
        int cursor = getStartTag().length();
        ret.add(new Proposal(text, cursor, getDoc()));

        if (allowNoChild && getChilds().size() > 0) {
            ret.add(new Proposal("<" + getName() + " />", cursor, getDoc()));
        }
        return ret.toArray(new Proposal[ret.size()]);
    }

}
