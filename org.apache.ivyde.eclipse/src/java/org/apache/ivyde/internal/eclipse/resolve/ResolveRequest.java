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

import org.apache.ivyde.internal.eclipse.CachedIvy;

public class ResolveRequest {

    private final IvyResolver resolver;

    private final CachedIvy cachedIvy;

    private boolean inWorkspace = false;

    private boolean transitive = true;

    private boolean forceFailOnError = false;

    private boolean resolveFailed = false;

    public ResolveRequest(IvyResolver resolver, CachedIvy cachedIvy) {
        this.resolver = resolver;
        this.cachedIvy = cachedIvy;
    }

    public void setForceFailOnError(boolean forceFailOnError) {
        this.forceFailOnError = forceFailOnError;
    }

    public boolean isForceFailOnError() {
        return forceFailOnError;
    }

    public IvyResolver getResolver() {
        return resolver;
    }

    public CachedIvy getCachedIvy() {
        return cachedIvy;
    }

    public void setInWorkspace(boolean inWorkspace) {
        this.inWorkspace = inWorkspace;
    }

    public boolean isInWorkspace() {
        return inWorkspace;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setResolveFailed(boolean resolveFailed) {
        this.resolveFailed = resolveFailed;
    }

    public boolean isResolveFailed() {
        return resolveFailed;
    }

    public String toString() {
        return resolver.toString();
    }
}
