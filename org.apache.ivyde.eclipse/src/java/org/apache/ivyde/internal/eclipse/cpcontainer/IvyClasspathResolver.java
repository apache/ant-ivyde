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
package org.apache.ivyde.internal.eclipse.cpcontainer;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyClasspathResolver extends ClasspathEntriesResolver {

    private final IvyClasspathContainerImpl ivycp;

    public IvyClasspathResolver(IvyClasspathContainerImpl ivycp, boolean usePreviousResolveIfExist) {
        super(ivycp, usePreviousResolveIfExist);
        this.ivycp = ivycp;
    }

    /*
     * Actually set the classpath only after every resolve has been done. This will avoid having the
     * classpathsetter job blocked by this resolve job, and so have a popup blocking the end user of
     * doing anything.
     */
    public void postBatchResolve() {
        if (getClasspathEntries() != null) {
            ivycp.setResolveReport(getResolveReport());
            ivycp.updateClasspathEntries(getClasspathEntries());
        }
    }

}
