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
package org.apache.ivyde.eclipse.cpcontainer;

import java.util.Locale;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class FileExtFilter extends ViewerFilter {

    private String[] fTargetExtension;

    public FileExtFilter(String[] targetExtension) {
        fTargetExtension = targetExtension;
    }

    public boolean select(Viewer viewer, Object parent, Object element) {
        if (element instanceof IFile) {
            for (int i = 0; i < fTargetExtension.length; i++) {
                if (((IFile) element).getName().toLowerCase(Locale.ENGLISH).endsWith(
                    "." + fTargetExtension[i])) {
                    return true;
                }
                return false;
            }
        }

        if (element instanceof IContainer) { // i.e. IProject, IFolder
            try {
                IResource[] resources = ((IContainer) element).members();
                for (int i = 0; i < resources.length; i++) {
                    if (select(viewer, parent, resources[i])) {
                        return true;
                    }
                }
            } catch (CoreException e) {
                IvyPlugin.log(e);
            }
        }
        return false;
    }

}
