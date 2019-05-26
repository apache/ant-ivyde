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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;

public abstract class IvyModel {
    private final Map<String, List<IvyTag>> model = new HashMap<>();

    private Properties defaults;

    private final IvyModelSettings settings;

    public IvyModel(IvyModelSettings settings) {
        loadDefaults();
        this.settings = settings;
    }

    public IvyTag getIvyTag(String tagName, String parentName) {
        for (IvyTag tag : model.get(tagName)) {
            if (tag.getParent() != null && tag.getParent().getName().equals(parentName)) {
                return tag;
            }
        }
        return null;
    }

    private void loadDefaults() {
        defaults = new Properties();
        try {
            defaults.load(IvyModel.class.getResourceAsStream("defaults.properties"));
        } catch (IOException e) {
            // should never never happen
            settings.logError("The default properties could not be loaded", e);
        }
    }

    public IvyTag getRootIvyTag() {
        return (IvyTag) model.get(getRootIvyTagName());
    }

    protected abstract String getRootIvyTagName();

    protected Ivy getIvy() {
        return settings.getIvyInstance();
    }

    public IvyModelSettings getSettings() {
        return settings;
    }

    protected void addTag(String name, List<IvyTag> list) {
        model.put(name, list);
    }

    public void addTag(IvyTag ivyTag) {
        if (!model.containsKey(ivyTag.getName())) {
            List<IvyTag> list = new ArrayList<>();
            list.add(ivyTag);
            model.put(ivyTag.getName(), list);
            for (IvyTag child : ivyTag.getChilds()) {
                addTag(child);
            }
        } else {
            // the model already contains a tag for this name... maybe we should add it to a list,
            // but we still have problem of tags with infinite children hierarchy (like chain of
            // chain) where we need to stop adding children somewhere.
        }
    }

    protected String getDefault(String name) {
        return defaults.getProperty(name);
    }

    public abstract IvyFile newIvyFile(String name, String content, int documentOffset);

    public void refreshIfNeeded(IvyFile file) {
    }

    protected void clearModel() {
        model.clear();
    }
}
