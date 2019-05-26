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
package org.apache.ivyde.internal.eclipse.ui.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class IvyFileEditorInput implements IPathEditorInput, IStorageEditorInput, IFileEditorInput {
    private final IFile ivyFile;

    public IvyFileEditorInput(IFile input) {
        super();
        ivyFile = input;
    }

    public boolean exists() {
        return ivyFile.exists();
    }

    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    public String getName() {
        return ivyFile.getName();
    }

    public String getToolTipText() {
        return ivyFile.getFullPath().makeRelative().toString();
    }

    public IPath getPath() {
        return ivyFile.getLocation();
    }

    public String toString() {
        return getClass().getName() + "(" + ivyFile.getFullPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        return null;
    }

    public IPersistableElement getPersistable() {
        return null;
    }

    public IStorage getStorage() throws CoreException {
        return ivyFile;
    }

    public IFile getFile() {
        return ivyFile;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IFileEditorInput)) {
            return false;
        }
        IFileEditorInput o = (IFileEditorInput) obj;
        return getFile().equals(o.getFile());
    }

    public int hashCode() {
        return getFile().hashCode();
    }
}
