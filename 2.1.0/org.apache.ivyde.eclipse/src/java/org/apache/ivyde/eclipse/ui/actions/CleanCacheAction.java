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

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

public class CleanCacheAction extends Action {

    private final Cleanable[] cleanables;

    public abstract static class Cleanable {
        public void launchClean() {
            Job cleanJob = new Job("Cleaning cache " + getName()) {
                protected IStatus run(IProgressMonitor monitor) {
                    clean();
                    return Status.OK_STATUS;
                }
            };

            cleanJob.setUser(true);
            cleanJob.schedule();
        }

        protected abstract void clean();

        public abstract String getName();
    }

    public static class ResolutionCacheCleanable extends Cleanable {
        private final ResolutionCacheManager manager;

        public ResolutionCacheCleanable(ResolutionCacheManager manager) {
            this.manager = manager;
        }

        protected void clean() {
            manager.clean();
        }

        public String getName() {
            return "resolution";
        }
    }

    public static class RepositoryCacheCleanable extends Cleanable {
        private final RepositoryCacheManager manager;

        public RepositoryCacheCleanable(RepositoryCacheManager manager) {
            this.manager = manager;
        }

        protected void clean() {
            manager.clean();
        }

        public String getName() {
            return manager.getName();
        }
    }

    public CleanCacheAction(Cleanable cleanable) {
        this.cleanables = new Cleanable[] {cleanable};
    }

    public CleanCacheAction(Cleanable[] cleanables) {
        this.cleanables = cleanables;
    }

    public void run() {
        StringBuffer builder = new StringBuffer("Ivy cache cleaned: ");
        for (int i = 0; i < cleanables.length; i++) {
            cleanables[i].launchClean();
            builder.append(cleanables[i].getName());
            if (i < cleanables.length - 1) {
                builder.append(", ");
            }
        }
        IvyPlugin.log(IStatus.INFO, builder.toString(), null);
    }
}
