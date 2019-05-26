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
package org.apache.ivyde.eclipse.resolvevisualizer;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class ClasspathContainerSelectionDialog extends ElementListSelectionDialog {
    public ClasspathContainerSelectionDialog(Shell parentShell) {
        super(parentShell, new LabelProvider() {
            public String getText(Object element) {
                IvyClasspathContainer container = (IvyClasspathContainer) element;
                return container.getConf().getJavaProject().getProject().getName() + " -> "
                        + container.getDescription();
            }
        });
        setTitle("Ivy Classpath Containers");
        setMessage("Select a container to view in the resolve visualizer.");

        List<IvyClasspathContainer> classpathContainers = new ArrayList<>();
        for (IProject ivyProject : IvyClasspathContainerHelper.getIvyProjectsInWorkspace()) {
            classpathContainers.addAll(IvyClasspathContainerHelper.getContainers(ivyProject));
        }

        setElements(classpathContainers.toArray());
        setMultipleSelection(false);
    }
}
