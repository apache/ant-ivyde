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
package org.apache.ivyde.eclipse.revdepexplorer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.id.ModuleRules;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.version.VersionMatcher;

/**
 * This is a editable implementation of a module descriptor; this was created so a module descriptor
 * could be modified. This could go away if ivy ever exposes a descriptor where dependencies can be
 * added/removed. <br>
 */
public class EditableModuleDescriptor implements ModuleDescriptor {
    private ModuleDescriptor descriptor;

    private Set/* <DependencyDescriptor> */dependencies;

    private DependencyDescriptorDelta delta = new DependencyDescriptorDelta();

    private static final Comparator/* <DependencyDescriptor> */DEFAULT_DEPENDENCY_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            int result = 0;

            DependencyDescriptor desc1 = (DependencyDescriptor) o1;
            DependencyDescriptor desc2 = (DependencyDescriptor) o2;

            result = desc1.getDependencyId().getOrganisation().compareTo(
                desc2.getDependencyId().getOrganisation());

            if (result == 0) {
                result = desc1.getDependencyId().getName().compareTo(
                    desc2.getDependencyId().getName());
            }

            return result;
        }
    };

    public EditableModuleDescriptor(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
        dependencies = new TreeSet/* <DependencyDescriptor> */(DEFAULT_DEPENDENCY_COMPARATOR);
        dependencies.addAll(Arrays.asList(descriptor.getDependencies()));
    }

    public void removeDependency(DependencyDescriptor remove) {
        DependencyDescriptor ddToRemove;
        if ((ddToRemove = findDependencyDescriptor(dependencies, remove)) != null) {
            delta.remove(ddToRemove);
            dependencies.remove(ddToRemove);
        }
    }

    public void addDependency(DependencyDescriptor add) {
        add.getDependencyConfigurations(add.getModuleConfigurations());

        if (findDependencyDescriptor(dependencies, add) == null) {
            delta.add(add);
            dependencies.add(add);
        }
    }

    public void removeDependencies(DependencyDescriptor[] removes) {
        for (int i = 0; i < removes.length; i++) {
            removeDependency(removes[i]);
        }
    }

    public void removeDependencies(Collection/* <DependencyDescriptor> */removes) {
        removeDependencies((DependencyDescriptor[]) removes
                .toArray(new DependencyDescriptor[removes.size()]));
    }

    public void addDependencies(DependencyDescriptor[] additions) {
        for (int i = 0; i < additions.length; i++)
            addDependency(additions[i]);
    }

    public void addDependencies(Collection/* <DependencyDescriptor> */additions) {
        addDependencies((DependencyDescriptor[]) additions
                .toArray(new DependencyDescriptor[additions.size()]));
    }

    public DependencyDescriptor[] getDependencies() {
        return (DependencyDescriptor[]) dependencies.toArray(new DependencyDescriptor[dependencies
                .size()]);
    }

    public boolean isEdited() {
        return !delta.isEmpty();
    }

    public boolean dependsOn(VersionMatcher matcher, ModuleDescriptor md) {
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
            DependencyDescriptor dd = (DependencyDescriptor) iter.next();
            if (dd.getDependencyId().equals(md.getModuleRevisionId().getModuleId())) {
                if (md.getResolvedModuleRevisionId().getRevision() == null) {
                    return true;
                }
                if (matcher.accept(dd.getDependencyRevisionId(), md)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Careful - This method bypasses the module descriptor Parser and Resource and delegates
     * directly to XmlModuleDescriptorWriter.
     * 
     * @see org.apache.ivy.core.module.descriptor.ModuleDescriptor#toIvyFile(java.io.File)
     */
    public void toIvyFile(File destFile) throws ParseException, IOException {
        XmlModuleDescriptorWriter.write(this, destFile);
    }

    private DependencyDescriptor findDependencyDescriptor(
            Collection/* <DependencyDescriptor> */findIn, DependencyDescriptor find) {
        Iterator iter = findIn.iterator();
        while (iter.hasNext()) {
            DependencyDescriptor dependency = (DependencyDescriptor) iter.next();
            if (dependency.getDependencyRevisionId().equals(find.getDependencyRevisionId())) {
                return dependency;
            }
        }

        return null;
    }

    // Delegate all of the interface methods ---------------------------------

    public Artifact[] getAllArtifacts() {
        return descriptor.getAllArtifacts();
    }

    public Artifact[] getArtifacts(String s) {
        return descriptor.getArtifacts(s);
    }

    public String getAttribute(String s) {
        return descriptor.getAttribute(s);
    }

    public Map getAttributes() {
        return descriptor.getAttributes();
    }

    public Configuration getConfiguration(String s) {
        return descriptor.getConfiguration(s);
    }

    public Configuration[] getConfigurations() {
        return descriptor.getConfigurations();
    }

    public String[] getConfigurationsNames() {
        return descriptor.getConfigurationsNames();
    }

    public ConflictManager getConflictManager(ModuleId moduleid) {
        return descriptor.getConflictManager(moduleid);
    }

    public String getExtraAttribute(String s) {
        return descriptor.getExtraAttribute(s);
    }

    public Map getExtraAttributes() {
        return descriptor.getExtraAttributes();
    }

    public String getHomePage() {
        return descriptor.getHomePage();
    }

    public long getLastModified() {
        return descriptor.getLastModified();
    }

    public License[] getLicenses() {
        return descriptor.getLicenses();
    }

    public ModuleRevisionId getModuleRevisionId() {
        return descriptor.getModuleRevisionId();
    }

    public ModuleDescriptorParser getParser() {
        return descriptor.getParser();
    }

    public Date getPublicationDate() {
        return descriptor.getPublicationDate();
    }

    public String[] getPublicConfigurationsNames() {
        return descriptor.getPublicConfigurationsNames();
    }

    public ModuleRevisionId getResolvedModuleRevisionId() {
        return ModuleRevisionId.newInstance(descriptor.getResolvedModuleRevisionId()
                .getOrganisation(), descriptor.getResolvedModuleRevisionId().getName(), "revision");
    }

    public Date getResolvedPublicationDate() {
        return descriptor.getResolvedPublicationDate();
    }

    public Resource getResource() {
        return descriptor.getResource();
    }

    public String getStatus() {
        return descriptor.getStatus();
    }

    public boolean isDefault() {
        return descriptor.isDefault();
    }

    public boolean canExclude() {
        return descriptor.canExclude();
    }

    public boolean doesExclude(String[] as, ArtifactId artifactid) {
        return descriptor.doesExclude(as, artifactid);
    }

    public ExcludeRule[] getAllExcludeRules() {
        return descriptor.getAllExcludeRules();
    }

    public void setResolvedModuleRevisionId(ModuleRevisionId modulerevisionid) {
        descriptor.setResolvedModuleRevisionId(modulerevisionid);
    }

    public void setResolvedPublicationDate(Date date) {
        descriptor.setResolvedPublicationDate(date);
    }

    protected class DependencyDescriptorDelta {
        private Set/* <DependencyDescriptor> */added = new HashSet();

        private Set/* <DependencyDescriptor> */deleted = new HashSet();

        public void add(DependencyDescriptor dependency) {
            DependencyDescriptor ddToRemove;
            if ((ddToRemove = findDependencyDescriptor(deleted, dependency)) != null) {
                deleted.remove(ddToRemove);
            } else {
                added.add(dependency);
            }
        }

        public void remove(DependencyDescriptor dependency) {
            DependencyDescriptor ddToRemove;
            if ((ddToRemove = findDependencyDescriptor(added, dependency)) != null) {
                added.remove(ddToRemove);
            } else {
                deleted.add(dependency);
            }
        }

        public void clear() {
            added = new HashSet/* <DependencyDescriptor> */();
            deleted = new HashSet/* <DependencyDescriptor> */();
        }

        public boolean isEmpty() {
            return added.size() == 0 && deleted.size() == 0;
        }

        public Collection/* <DependencyDescriptor> */getDeletedDeltas() {
            return deleted;
        }

        public Collection/* <DependencyDescriptor> */getAddedDeltas() {
            return added;
        }
    }

    public Map getExtraAttributesNamespaces() {
        return descriptor.getExtraAttributesNamespaces();
    }

    public Map getExtraInfo() {
        return descriptor.getExtraInfo();
    }

    public Artifact getMetadataArtifact() {
        return descriptor.getMetadataArtifact();
    }

    public Map getQualifiedExtraAttributes() {
        return descriptor.getQualifiedExtraAttributes();
    }

    public String getRevision() {
        return descriptor.getRevision();
    }

    public ModuleRules getAllDependencyDescriptorMediators() {
        return descriptor.getAllDependencyDescriptorMediators();
    }
    public String getDescription() {
        return descriptor.getDescription();
    }

    public DependencyDescriptor mediate(DependencyDescriptor arg0) {
        return descriptor.mediate(arg0);
    }
}
