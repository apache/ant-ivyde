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
package org.apache.ivyde.eclipse.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.IvyMultiResolveJob;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

public class ProjectResolveAction extends Action {

    private final IProject[] projects;

    public ProjectResolveAction(IProject[] projects) {
        this.projects = projects;
        this.setText("Resolve");
    }

    public void run() {
        List allContainers = new ArrayList();
        for (int i = 0; i < projects.length; i++) {
            allContainers.addAll(IvyClasspathUtil.getIvyClasspathContainers(projects[i]));
        }

        Job multipleResolveJob = new IvyMultiResolveJob(allContainers);
        multipleResolveJob.setUser(true);
        multipleResolveJob.schedule();
    }
}
