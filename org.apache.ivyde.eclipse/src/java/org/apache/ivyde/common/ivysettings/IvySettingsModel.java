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
package org.apache.ivyde.common.ivysettings;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.Ivy;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.latest.LatestStrategy;
import org.apache.ivy.plugins.lock.LockStrategy;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.report.ReportOutputter;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.StringUtils;
import org.apache.ivyde.common.model.IvyBooleanTagAttribute;
import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.common.model.IvyReferenceTag;
import org.apache.ivyde.common.model.IvyTag;
import org.apache.ivyde.common.model.IvyTagAttribute;

public class IvySettingsModel extends IvyModel {
    private String loaded = null;
    private final File file;
    private ClassLoader cl;
    private Map<String, Class<?>> typedefClasses;

    public IvySettingsModel(IvyModelSettings settings, File file) {
        super(settings);
        this.file = file;
        this.cl = Ivy.class.getClassLoader();
        this.typedefClasses = getTypedefClasses(this.cl, IvySettingsFile.getDefaultTypedefs());
        doLoadModel();
    }

    public void refreshIfNeeded(IvyFile file) {
        String toLoad = getLoad(file);
        if (!toLoad.equals(loaded)) {
            doRefresh(file, toLoad);
        }
    }

    private String getLoad(IvyFile file) {
        IvySettingsFile sfile = (IvySettingsFile) file;
        return Arrays.asList(sfile.getClasspathUrls()) + "|" + sfile.getTypedefs();
    }

    private void doRefresh(IvyFile file, String toLoad) {
        IvySettingsFile sfile = (IvySettingsFile) file;
        clearModel();
        cl = getClassLoader(sfile);
        typedefClasses = getTypedefClasses(sfile, cl);

        doLoadModel();
        loaded = toLoad;
    }

    private void doLoadModel() {
        IvyTag ivyTag = new IvyTag("ivysettings", "Root tag of Ivy settings file");

        ivyTag.addChildIvyTag(new IvyTag("property",
            new IvyTagAttribute[] {
                new IvyTagAttribute("name", true),
                new IvyTagAttribute("value", true),
                new IvyBooleanTagAttribute("override"),
        }));
        ivyTag.addChildIvyTag(new IvyTag("properties",
            new IvyTagAttribute[] {
                new IvyTagAttribute("file", true),
                new IvyTagAttribute("environment"),
                new IvyBooleanTagAttribute("override"),
        }));
        ivyTag.addChildIvyTag(new IvyTag("settings",
            new IvyTagAttribute[] {
                new IvyTagAttribute("defaultResolver"),
                new IvyTagAttribute("defaultLatestStrategy"),
                new IvyTagAttribute("defaultConflictManager"),
                new IvyTagAttribute("defaultBranch"),
                new IvyTagAttribute("defaultResolveMode"),
                new IvyTagAttribute("circularDependencyStrategy"),
                new IvyBooleanTagAttribute("validate"),
                new IvyBooleanTagAttribute("useRemoteConfig"),
        }));
        ivyTag.addChildIvyTag(new IvyTag("include",
            new IvyTagAttribute[] {
                new IvyTagAttribute("url"),
                new IvyTagAttribute("file"),
        }));
        ivyTag.addChildIvyTag(new IvyTag("classpath",
            new IvyTagAttribute[] {
                new IvyTagAttribute("url"),
                new IvyTagAttribute("file"),
        }));
        ivyTag.addChildIvyTag(new IvyTag("typedef",
            new IvyTagAttribute[] {
                new IvyTagAttribute("name"),
                new IvyTagAttribute("classname"),
        }));

        IvyTag tag = new IvyTag("locking-strategies");
        addTypedefChildren(tag, getChildClasses(typedefClasses, LockStrategy.class));
        ivyTag.addChildIvyTag(tag);

        ivyTag.addChildIvyTag(new IvyTag("caches",
            new IvyTagAttribute[] {
                new IvyTagAttribute("default"),
                new IvyTagAttribute("defaultCacheDir"),
                new IvyTagAttribute("resolutionCacheDir"),
                new IvyTagAttribute("repositoryCacheDir"),
                new IvyTagAttribute("ivyPattern"),
                new IvyTagAttribute("artifactPattern"),
                new IvyBooleanTagAttribute("checkUpToDate"),
                new IvyBooleanTagAttribute("useOrigin"),
                new IvyTagAttribute("lockStrategy"),
            }).addChildIvyTag(new IvyTag("cache",
                new IvyTagAttribute[] {
                    new IvyTagAttribute("name"),
                    new IvyTagAttribute("basedir"),
                    new IvyTagAttribute("ivyPattern"),
                    new IvyTagAttribute("artifactPattern"),
                    new IvyBooleanTagAttribute("useOrigin"),
                    new IvyTagAttribute("lockStrategy"),
                    new IvyTagAttribute("defaultTTL"),
                }).addChildIvyTag(new IvyTag("ttl",
                    new IvyTagAttribute[] {
                        new IvyTagAttribute("organisation"),
                        new IvyTagAttribute("module"),
                        new IvyTagAttribute("revision"),
                        new IvyTagAttribute("matcher"),
                        new IvyTagAttribute("duration", true),
                }))));

        tag = new IvyTag("latest-strategies");
        addTypedefChildren(tag, getChildClasses(typedefClasses, LatestStrategy.class));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("parsers");
        addTypedefChildren(tag, getChildClasses(typedefClasses, ModuleDescriptorParser.class));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("namespaces");
        addTypedefChildren(tag, getChildClasses(typedefClasses, Namespace.class));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("macrodef", new IvyTagAttribute[] {
                new IvyTagAttribute("name"),
            }).addChildIvyTag(new IvyTag("attribute", new IvyTagAttribute[] {
                    new IvyTagAttribute("name"),
                    new IvyTagAttribute("default"),
            }));
        addTypedefChildren(tag, getChildClasses(typedefClasses, DependencyResolver.class));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("resolvers");
        addTypedefChildren(tag, getChildClasses(typedefClasses, DependencyResolver.class));
        tag.addChildIvyTag(new IvyReferenceTag("resolver"));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("conflict-managers");
        addTypedefChildren(tag, getChildClasses(typedefClasses, ConflictManager.class));
        ivyTag.addChildIvyTag(tag);

        ivyTag.addChildIvyTag(new IvyTag("modules")
            .addChildIvyTag(new IvyTag("module",
                new IvyTagAttribute[] {
                    new IvyTagAttribute("organisation"),
                    new IvyTagAttribute("name"),
                    new IvyTagAttribute("revision"),
                    new IvyTagAttribute("matcher"),
                    new IvyTagAttribute("resolver"),
                    new IvyTagAttribute("conflict-manager"),
                    new IvyTagAttribute("branch"),
                    new IvyTagAttribute("resolveMode"),
                })));

        tag = new IvyTag("outputters");
        addTypedefChildren(tag, getChildClasses(typedefClasses, ReportOutputter.class));
        ivyTag.addChildIvyTag(tag);

        ivyTag.addChildIvyTag(new IvyTag("statuses",
            new IvyTagAttribute[] {
                    new IvyTagAttribute("default"),
                })
            .addChildIvyTag(new IvyTag("status",
                new IvyTagAttribute[] {
                    new IvyTagAttribute("name"),
                    new IvyTagAttribute("integration"),
                })));

        tag = new IvyTag("triggers");
        addTypedefChildren(tag, getChildClasses(typedefClasses, Trigger.class));
        ivyTag.addChildIvyTag(tag);

        tag = new IvyTag("version-matchers");
        addTypedefChildren(tag, getChildClasses(typedefClasses, VersionMatcher.class));
        ivyTag.addChildIvyTag(tag);

        addTag(ivyTag);
    }

    private void addTypedefChildren(IvyTag tag, Map<String, Class<?>> children) {
        for (Entry<String, Class<?>> entry : children.entrySet()) {
            tag.addChildIvyTag(typedefedTag(entry.getKey(), entry.getValue()));
        }
    }

    private IvyTag typedefedTag(String tagName, final Class<?> clazz) {
        return new IvyTag(tagName) {
            // we lazy load children, since we may have a loop in children (chain can contain chain)
            // causing a stack overflow if we try to recursively add typedefed children
            private boolean init = false;
            public List<IvyTagAttribute> getAttributes() {
                init();
                return super.getAttributes();
            }

            public List<IvyTag> getChilds() {
                init();
                return super.getChilds();
            }

            public boolean hasChild() {
                init();
                return super.hasChild();
            }

            private void init() {
                if (!init) {
                    try {
                        for (Method m : clazz.getMethods()) {
                            if (m.getName().startsWith("create")
                                    && m.getParameterTypes().length == 0
                                    && isSupportedChildType(m.getReturnType())) {
                                String name = StringUtils
                                    .uncapitalize(m.getName().substring("create".length()));
                                if (name.length() == 0) {
                                    continue;
                                }
                                addChildIvyTag(typedefedTag(name, m.getReturnType()));
                            } else if (m.getName().startsWith("add")
                                    && m.getParameterTypes().length == 1
                                    && isSupportedChildType(m.getParameterTypes()[0])
                                    && Void.TYPE.equals(m.getReturnType())) {
                                String name = StringUtils.uncapitalize(m.getName().substring(
                                    m.getName().startsWith("addConfigured") ? "addConfigured"
                                            .length() : "add".length()));
                                if (name.length() == 0) {
                                    addTypedefChildren(this, getChildClasses(typedefClasses, m
                                            .getParameterTypes()[0]));
                                } else {
                                    addChildIvyTag(typedefedTag(name, m.getParameterTypes()[0]));
                                }
                            } else if (m.getName().startsWith("set")
                                    && Void.TYPE.equals(m.getReturnType())
                                    && m.getParameterTypes().length == 1
                                    && isSupportedAttributeType(m.getParameterTypes()[0])) {
                                IvyTagAttribute att = new IvyTagAttribute(StringUtils
                                        .uncapitalize(m.getName().substring("set".length())));
                                if (m.getParameterTypes()[0] == boolean.class) {
                                    att.setValueProvider(IvyBooleanTagAttribute.VALUE_PROVIDER);
                                }
                                addAttribute(att);
                            }
                        }
                    } catch (NoClassDefFoundError e) {
                        // we catch this because listing methods may raise a NoClassDefFoundError
                        // if the class relies on a dependency which is not available in classpath
                        getSettings().logError(
                            "impossible to init tag for " + clazz + ": " + e,
                            null);
                    } catch (Exception e) {
                        getSettings().logError(
                            "error occurred while initializing tag for " + clazz + ": " + e,
                            e);
                    }
                    init = true;
                }
            }
        };
    }

    protected boolean isSupportedChildType(Class<?> type) {
        return !Void.TYPE.equals(type)
                && !String.class.equals(type)
                && !Character.class.equals(type) && !char.class.equals(type)
                && !Boolean.class.equals(type) && !boolean.class.equals(type)
                && !Integer.class.equals(type) && !int.class.equals(type)
                && !Short.class.equals(type) && !short.class.equals(type)
                && !Long.class.equals(type) && !long.class.equals(type)
                && !Class.class.equals(type);
    }

    private boolean isSupportedAttributeType(Class<?> type) {
        if (String.class.isAssignableFrom(type)
                || Character.class.equals(type) || char.class.equals(type)
                || Boolean.class.equals(type) || boolean.class.equals(type)
                || Short.class.equals(type) || short.class.equals(type)
                || Integer.class.equals(type) || int.class.equals(type)
                || Long.class.equals(type) || long.class.equals(type)
                || Class.class.equals(type)
                ) {
            return true;
        }
        try {
            type.getConstructor(String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ClassLoader getClassLoader(IvySettingsFile sfile) {
        if (sfile.getClasspathUrls().length > 0) {
            return new URLClassLoader(sfile.getClasspathUrls(), Ivy.class.getClassLoader());
        } else {
            return Ivy.class.getClassLoader();
        }
    }

    private Map<String, Class<?>> getTypedefClasses(IvySettingsFile file, ClassLoader cl) {
        Map<Object, Object> typedefs = file.getTypedefs();
        return getTypedefClasses(cl, typedefs);
    }

    private Map<String, Class<?>> getTypedefClasses(ClassLoader cl, Map<Object, Object> typedefs) {
        Map<String, Class<?>> classes = new LinkedHashMap<>();
        for (Entry<Object, Object> entry : typedefs.entrySet()) {
            try {
                classes.put((String) entry.getKey(), cl.loadClass((String) entry.getValue()));
            } catch (ClassNotFoundException e) {
                // ignored
            }
        }
        return classes;
    }

    private Map<String, Class<?>> getChildClasses(Map<String, Class<?>> classes, Class<?> type) {
        Map<String, Class<?>> childClasses = new LinkedHashMap<>();
        for (Entry<String, Class<?>> entry : classes.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                childClasses.put(entry.getKey(), entry.getValue());
            }
        }
        return childClasses;
    }

    protected String getRootIvyTagName() {
        return "ivysettings";
    }

    public IvyFile newIvyFile(String name, String content, int documentOffset) {
        return new IvySettingsFile(getSettings(), file, name, content, documentOffset);
    }

}
