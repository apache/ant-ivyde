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

import java.util.Map;

import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.IncludeRule;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.namespace.Namespace;

/**
 * Delegate dependency descriptor that makes the descriptor mutable. <br>
 */
public class EditableDependencyDescriptor implements DependencyDescriptor {

    private DependencyDescriptor descriptor;

    private ModuleRevisionId revisionId;

    public EditableDependencyDescriptor(DependencyDescriptor dd) {
        descriptor = dd;
        revisionId = dd.getDependencyRevisionId();
    }

    public void setRevision(String revision) {
        revisionId = new ModuleRevisionId(getDependencyId(), revision);
    }

    public ModuleRevisionId getDynamicConstraintDependencyRevisionId() {
        return revisionId;
    }

    public ModuleRevisionId getDependencyRevisionId() {
        return revisionId;
    }

    public DependencyDescriptor asSystem() {
        return descriptor.asSystem();
    }

    public boolean canExclude() {
        return descriptor.canExclude();
    }

    public DependencyDescriptor clone(ModuleRevisionId revision) {
        return descriptor.clone(revision);
    }

    public boolean doesExclude(String[] moduleConfigurations, ArtifactId artifactId) {
        return descriptor.doesExclude(moduleConfigurations, artifactId);
    }

    public DependencyArtifactDescriptor[] getAllDependencyArtifacts() {
        return descriptor.getAllDependencyArtifacts();
    }

    public ExcludeRule[] getAllExcludeRules() {
        return descriptor.getAllExcludeRules();
    }

    public IncludeRule[] getAllIncludeRules() {
        return descriptor.getAllIncludeRules();
    }

    public String getAttribute(String attName) {
        return descriptor.getAttribute(attName);
    }

    public Map getAttributes() {
        return descriptor.getAttributes();
    }

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String moduleConfigurations) {
        return descriptor.getDependencyArtifacts(moduleConfigurations);
    }

    public DependencyArtifactDescriptor[] getDependencyArtifacts(String[] moduleConfigurations) {
        return descriptor.getDependencyArtifacts(moduleConfigurations);
    }

    public String[] getDependencyConfigurations(String moduleConfiguration,
            String requestedConfiguration) {
        return descriptor.getDependencyConfigurations(moduleConfiguration, requestedConfiguration);
    }

    public String[] getDependencyConfigurations(String moduleConfiguration) {
        return descriptor.getDependencyConfigurations(moduleConfiguration);
    }

    public String[] getDependencyConfigurations(String[] moduleConfigurations) {
        return descriptor.getDependencyConfigurations(moduleConfigurations);
    }

    public ModuleId getDependencyId() {
        return descriptor.getDependencyId();
    }

    public ExcludeRule[] getExcludeRules(String moduleConfigurations) {
        return descriptor.getExcludeRules(moduleConfigurations);
    }

    public ExcludeRule[] getExcludeRules(String[] moduleConfigurations) {
        return descriptor.getExcludeRules(moduleConfigurations);
    }

    public String getExtraAttribute(String attName) {
        return descriptor.getExtraAttribute(attName);
    }

    public Map getExtraAttributes() {
        return descriptor.getExtraAttributes();
    }

    public IncludeRule[] getIncludeRules(String moduleConfigurations) {
        return descriptor.getIncludeRules(moduleConfigurations);
    }

    public IncludeRule[] getIncludeRules(String[] moduleConfigurations) {
        return descriptor.getIncludeRules(moduleConfigurations);
    }

    public String[] getModuleConfigurations() {
        return descriptor.getModuleConfigurations();
    }

    public Namespace getNamespace() {
        return descriptor.getNamespace();
    }

    public ModuleRevisionId getParentRevisionId() {
        return descriptor.getParentRevisionId();
    }

    public Map getQualifiedExtraAttributes() {
        return descriptor.getQualifiedExtraAttributes();
    }

    public boolean isChanging() {
        return descriptor.isChanging();
    }

    public boolean isForce() {
        return descriptor.isForce();
    }

    public boolean isTransitive() {
        return descriptor.isTransitive();
    }
}
