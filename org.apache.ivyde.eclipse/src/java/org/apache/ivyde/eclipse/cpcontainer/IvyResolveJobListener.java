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

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public class IvyResolveJobListener implements TransferListener, IvyListener {

    private static final int DOWNLOAD_MONITOR_LENGTH = 100;

    private static final int KILO_BITS_UNIT = 1024;

    private static final int MONITOR_LENGTH = 1000;

    private static final int WORK_PER_ARTIFACT = 100;

    private long expectedTotalLength = 1;

    private int workPerArtifact = WORK_PER_ARTIFACT;

    private long currentLength = 0;

    private final IProgressMonitor monitor;

    private IProgressMonitor dlmonitor;

    public IvyResolveJobListener(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void transferProgress(TransferEvent evt) {
        switch (evt.getEventType()) {
            case TransferEvent.TRANSFER_INITIATED:
                monitor.setTaskName("downloading " + evt.getResource());
                break;
            case TransferEvent.TRANSFER_STARTED:
                currentLength = 0;
                if (evt.isTotalLengthSet()) {
                    expectedTotalLength = evt.getTotalLength();
                    dlmonitor
                            .beginTask("downloading " + evt.getResource(), DOWNLOAD_MONITOR_LENGTH);
                }
                break;
            case TransferEvent.TRANSFER_PROGRESS:
                if (expectedTotalLength > 1) {
                    currentLength += evt.getLength();
                    int progress = (int) (currentLength * DOWNLOAD_MONITOR_LENGTH
                            / expectedTotalLength);
                    dlmonitor.worked(progress);
                    monitor.subTask((currentLength / KILO_BITS_UNIT) + " / "
                            + (expectedTotalLength / KILO_BITS_UNIT) + "kB");
                }
                break;
            default:
        }
    }

    public void progress(IvyEvent event) {
        if (event instanceof TransferEvent) {
            if (dlmonitor != null) {
                transferProgress((TransferEvent) event);
            }
        } else if (event instanceof PrepareDownloadEvent) {
            PrepareDownloadEvent pde = (PrepareDownloadEvent) event;
            Artifact[] artifacts = pde.getArtifacts();
            if (artifacts.length > 0) {
                workPerArtifact = MONITOR_LENGTH / artifacts.length;
            }
        } else if (event instanceof StartArtifactDownloadEvent) {
            StartArtifactDownloadEvent evt = (StartArtifactDownloadEvent) event;
            monitor.setTaskName("downloading " + evt.getArtifact());
            if (dlmonitor != null) {
                dlmonitor.done();
            }
            dlmonitor = new SubProgressMonitor(monitor, workPerArtifact);
        } else if (event instanceof EndArtifactDownloadEvent) {
            if (dlmonitor != null) {
                dlmonitor.done();
            }
            monitor.subTask(" ");
            dlmonitor = null;
        } else if (event instanceof StartResolveDependencyEvent) {
            StartResolveDependencyEvent ev = (StartResolveDependencyEvent) event;
            monitor.subTask("resolving " + ev.getDependencyDescriptor().getDependencyRevisionId());
        } else if (event instanceof EndResolveDependencyEvent) {
            monitor.subTask(" ");
        }
    }

}
