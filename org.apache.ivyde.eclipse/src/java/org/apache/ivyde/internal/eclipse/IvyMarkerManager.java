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
package org.apache.ivyde.internal.eclipse;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class IvyMarkerManager {

    private IResource findResource(IProject project, String ivyXmlFile) {
        if (project == null) {
            return null;
        }
        IResource r = project.getFile(ivyXmlFile);
        if (!r.exists()) {
            r = project;
        }
        return r;
    }

    public void removeMarkers(IProject project, String ivyXmlFile) {
        IResource r = findResource(project, ivyXmlFile);
        if (r == null) {
            return;
        }
        removeMarkers(r);
    }

    public void removeMarkers(IResource r) {
        try {
            r.deleteMarkers(IvyPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            IvyPlugin.log(e);
        }
    }

    public void setResolveStatus(IStatus status, IProject project, String ivyXmlFile) {
        try {
            IResource r = findResource(project, ivyXmlFile);
            if (r == null) {
                return;
            }
            removeMarkers(r);
            if (status == Status.OK_STATUS) {
                return;
            }
            if (status.isMultiStatus()) {
                for (IStatus childStatus : status.getChildren()) {
                    addMarker(r, childStatus);
                }
            } else {
                addMarker(r, status);
            }
        } catch (CoreException e) {
            IvyPlugin.log(e);
        }
    }

    private void addMarker(IResource r, IStatus status) throws CoreException {
        IMarker marker = r.createMarker(IvyPlugin.MARKER_ID);
        marker.setAttribute(IMarker.MESSAGE, status.getMessage());
        switch (status.getSeverity()) {
            case IStatus.ERROR:
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                break;
            case IStatus.WARNING:
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                break;
            case IStatus.INFO:
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                break;
            default:
                IvyPlugin.logWarn("Unsupported resolve status: " + status.getSeverity());
        }
    }

}
