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
package org.apache.ivyde.internal.eclipse.resolve;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class RefreshFolderJob extends WorkspaceJob {

    private final IResource folder;

    public RefreshFolderJob(IResource folder) {
        super("IvyDE refresh " + folder);
        this.folder = folder;
    }

    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        if (monitor != null && monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        if (monitor != null && monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

}
