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
package org.apache.ivyde.internal.eclipse.conditions;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

public class IvyDEConditionTester extends PropertyTester {

    public static final String PROPERTY_IVYPROJECT = "ivyproject"; //$NON-NLS-1$

    public static final String PROPERTY_IVYCP = "ivycp"; //$NON-NLS-1$

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (property.equals(PROPERTY_IVYPROJECT)) {
            IProject project = (IProject) receiver;
            return !IvyClasspathContainerHelper.getContainers(project).isEmpty();
        } else if (property.equals(PROPERTY_IVYCP)) {
            ClassPathContainer cp = (ClassPathContainer) receiver;
            return IvyClasspathContainerHelper.isIvyClasspathContainer(cp.getClasspathEntry().getPath());
        }
        return false;
    }

}
