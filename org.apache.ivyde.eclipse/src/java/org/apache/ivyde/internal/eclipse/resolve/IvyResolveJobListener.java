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

import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.download.EndArtifactDownloadEvent;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.download.StartArtifactDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveDependencyEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.event.resolve.StartResolveDependencyEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.eclipse.core.runtime.IProgressMonitor;

public class IvyResolveJobListener implements TransferListener, IvyListener {

    private static final int KILO_BITS_UNIT = 1024;

    private static final int RESOLVE_PERCENT = 2;

    private long totalLength = 1;

    private int workPerArtifact = 0;

    private long currentLength = 0;

    private int currentProgress = 0;

    private final IProgressMonitor monitor;

    private final int downloadStep;

    private final int resolveStep;

    public IvyResolveJobListener(final IProgressMonitor monitor, int step) {
        this.monitor = monitor;
        this.resolveStep = step / RESOLVE_PERCENT;
        this.downloadStep = step - resolveStep;
    }

    public void transferProgress(TransferEvent evt) {
        switch (evt.getEventType()) {
            case TransferEvent.TRANSFER_INITIATED:
                monitor.subTask("downloading " + evt.getResource());
                break;
            case TransferEvent.TRANSFER_STARTED:
                currentLength = 0;
                currentProgress = 0;
                if (evt.isTotalLengthSet()) {
                    totalLength = evt.getTotalLength();
                    monitor.subTask("downloading " + evt.getResource() + ": 0 / "
                            + (totalLength / KILO_BITS_UNIT) + "kB");
                }
                break;
            case TransferEvent.TRANSFER_PROGRESS:
                if (totalLength > 1) {
                    currentLength += evt.getLength();
                    int progress = (int) ((currentLength * workPerArtifact) / totalLength);
                    // log an accumulated diff
                    monitor.worked(progress - currentProgress);
                    currentProgress = progress;
                    monitor.subTask("downloading " + evt.getResource() + ": "
                            + (currentLength / KILO_BITS_UNIT) + " / "
                            + (totalLength / KILO_BITS_UNIT) + "kB");
                }
                break;
            case TransferEvent.TRANSFER_COMPLETED:
                monitor.worked(workPerArtifact - currentProgress);
                break;
            default:
        }
    }

    public void progress(IvyEvent event) {
        if (event instanceof TransferEvent) {
            transferProgress((TransferEvent) event);
        } else if (event instanceof PrepareDownloadEvent) {
            PrepareDownloadEvent pde = (PrepareDownloadEvent) event;
            Artifact[] artifacts = pde.getArtifacts();
            if (artifacts.length > 0) {
                workPerArtifact = downloadStep / artifacts.length;
            } else {
                monitor.worked(downloadStep);
            }
        } else if (event instanceof StartArtifactDownloadEvent) {
            StartArtifactDownloadEvent evt = (StartArtifactDownloadEvent) event;
            monitor.subTask("downloading " + evt.getArtifact());
        } else if (event instanceof EndArtifactDownloadEvent) {
            monitor.worked(workPerArtifact - currentProgress);
            currentProgress = 0;
            monitor.subTask(" ");
        } else if (event instanceof StartResolveDependencyEvent) {
            StartResolveDependencyEvent ev = (StartResolveDependencyEvent) event;
            ModuleRevisionId mrid = ev.getDependencyDescriptor().getDependencyRevisionId();
            monitor.subTask("looking for " + mrid);
        } else if (event instanceof EndResolveDependencyEvent) {
            monitor.subTask(" ");
        } else if (event instanceof EndResolveEvent) {
            monitor.worked(resolveStep);
        }
    }

}
