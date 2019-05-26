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
package org.apache.ivyde.internal.eclipse.handlers;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ResolveAllHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        IJavaProject[] projects;
        try {
            projects = model.getJavaProjects();
        } catch (JavaModelException e) {
            // TODO deal with it properly
            return null;
        }

        for (IJavaProject project : projects) {
            for (IvyClasspathContainer container : IvyClasspathContainerHelper.getContainers(project)) {
                container.launchResolve(false, null);
            }
        }

        return null;
    }

}
