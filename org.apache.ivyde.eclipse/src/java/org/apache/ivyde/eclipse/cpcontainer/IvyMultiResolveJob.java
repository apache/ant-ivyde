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

import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class IvyMultiResolveJob extends Job {

    private final List containers;

    public IvyMultiResolveJob(List/* <IvyClasspathContainer> */containers) {
        super("Multiple Ivy resolve");
        this.containers = containers;
    }

    protected IStatus run(IProgressMonitor monitor) {
        MultiStatus errorsStatus = new MultiStatus(IvyPlugin.ID, IStatus.ERROR,
                "Some projects fail to be resolved", null);

        monitor.beginTask("Resolving dependencies", containers.size());

        Iterator containerIterator = containers.iterator();
        while (containerIterator.hasNext()) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            IvyClasspathContainer container = (IvyClasspathContainer) containerIterator.next();

            SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
            String projectName = container.getConf().getJavaProject().getProject().getName();
            subMonitor.beginTask("Resolving dependencies of " + projectName + " ("
                    + container.getDescription() + ")", 1);

            IStatus jobStatus = container.launchResolve(false, true, subMonitor);
            switch (jobStatus.getCode()) {
                case IStatus.CANCEL:
                    return Status.CANCEL_STATUS;
                case IStatus.OK:
                case IStatus.INFO:
                    break;
                case IStatus.ERROR:
                    errorsStatus.add(jobStatus);
                    break;
            }
        }

        if (errorsStatus.getChildren().length != 0) {
            return errorsStatus;
        }

        return Status.OK_STATUS;
    }

}
