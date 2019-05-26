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

import java.util.LinkedList;
import java.util.List;

import org.apache.ivyde.eclipse.IvyNatureHelper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddIvyNatureHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        List<IProject> projects = new LinkedList<>();
        for (Object object : ((IStructuredSelection) selection).toList()) {
            if (!(object instanceof IAdaptable)) {
                return null;
            }
            IProject project = (IProject) ((IAdaptable) object).getAdapter(IProject.class);
            if (project == null) {
                return null;
            }
            projects.add(project);
        }

        if (projects.size() > 0) {
            for (IProject project : projects) {
                IvyNatureHelper.addNature(project);
            }
        }

        return null;
    }

}
