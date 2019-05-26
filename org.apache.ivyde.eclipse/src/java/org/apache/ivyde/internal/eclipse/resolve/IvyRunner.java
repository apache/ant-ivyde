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

import org.apache.ivy.Ivy;
import org.eclipse.core.runtime.IProgressMonitor;

public class IvyRunner {

    private static final int WAIT_FOR_JOIN = 100;

    /**
     * Launch some ivy related work and cancel it if the end user has requested it.
     *
     * @param runnable
     *            the ivy work to do
     * @param ivy
     *            the ivy instance which handle the work, used to cancel the work if requested
     * @param monitor
     *            the monitor from the end user
     * @return <code>true</code> if the work has been canceled
     */
    public boolean launchIvyThread(Runnable runnable, Ivy ivy, IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            return true;
        }

        Thread runnerThread = new Thread(runnable);
        runnerThread.setName("IvyDE resolver thread");
        runnerThread.start();

        while (true) {
            try {
                runnerThread.join(WAIT_FOR_JOIN);
            } catch (InterruptedException e) {
                ivy.interrupt(runnerThread);
                return true;
            }
            if (!runnerThread.isAlive()) {
                return false;
            }
            if (monitor != null && monitor.isCanceled()) {
                ivy.interrupt(runnerThread);
                return true;
            }
        }
    }

}
