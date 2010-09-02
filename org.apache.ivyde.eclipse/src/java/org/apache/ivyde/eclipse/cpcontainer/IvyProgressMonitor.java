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

import org.apache.ivy.Ivy;
import org.eclipse.core.runtime.IProgressMonitor;

public class IvyProgressMonitor {

    private static final int WAIT_FOR_JOIN = 100;

    /**
     * Wait for the Ivy work to finish, or try to interrupt it if requested by the end user via the
     * progress monitor
     * 
     * @param ivy
     *            the ivy to monitor
     * @param monitor
     *            the monitor coming from the end user
     * @param resolverThread
     *            the thread doing the Ivy work
     * @return <code>true</code> if the work has been interrupted
     */
    public boolean wait(Ivy ivy, IProgressMonitor monitor, Thread resolverThread) {
        while (true) {
            try {
                resolverThread.join(WAIT_FOR_JOIN);
            } catch (InterruptedException e) {
                ivy.interrupt(resolverThread);
                return true;
            }
            if (!resolverThread.isAlive()) {
                return false;
            }
            if (monitor != null && monitor.isCanceled()) {
                ivy.interrupt(resolverThread);
                return true;
            }
        }
    }

}
