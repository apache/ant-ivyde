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
package org.apache.ivyde.eclipse.cp;

import java.util.List;


public class ClasspathSetup {

    private boolean resolveInWorkspace;

    private List<String> acceptedTypes;

    private boolean alphaOrder;

    private boolean retrievedClasspath;

    private RetrieveSetup retrieveSetup = new RetrieveSetup();

    private boolean readOSGiMetadata;

    private boolean transitiveResolve = true;

    /**
     * Default constructor
     */
    public ClasspathSetup() {
        // default constructor
    }

    public void set(ClasspathSetup setup) {
        this.resolveInWorkspace = setup.resolveInWorkspace;
        this.acceptedTypes = setup.acceptedTypes;
        this.alphaOrder = setup.alphaOrder;
        this.retrievedClasspath = setup.retrievedClasspath;
        this.retrieveSetup.set(setup.retrieveSetup);
        this.readOSGiMetadata = setup.readOSGiMetadata;
        this.transitiveResolve = setup.transitiveResolve;
    }

    public boolean isResolveInWorkspace() {
        return resolveInWorkspace;
    }

    public void setResolveInWorkspace(boolean resolveInWorkspace) {
        this.resolveInWorkspace = resolveInWorkspace;
    }

    public List<String> getAcceptedTypes() {
        return acceptedTypes;
    }

    public void setAcceptedTypes(List<String> acceptedTypes) {
        this.acceptedTypes = acceptedTypes;
    }

    public boolean isAlphaOrder() {
        return alphaOrder;
    }

    public void setAlphaOrder(boolean alphaOrder) {
        this.alphaOrder = alphaOrder;
    }

    public boolean isRetrievedClasspath() {
        return retrievedClasspath;
    }

    public void setRetrievedClasspath(boolean retrievedClasspath) {
        this.retrievedClasspath = retrievedClasspath;
    }

    public RetrieveSetup getRetrieveSetup() {
        return retrieveSetup;
    }

    public void setRetrieveSetup(RetrieveSetup retrieveSetup) {
        this.retrieveSetup = retrieveSetup;
    }

    public boolean isReadOSGiMetadata() {
        return readOSGiMetadata;
    }

    public void setReadOSGiMetadata(boolean readOSGiMetadata) {
        this.readOSGiMetadata = readOSGiMetadata;
    }

    public boolean isTransitiveResolve() {
        return transitiveResolve;
    }

    public void setTransitiveResolve(boolean transitiveResolve) {
        this.transitiveResolve = transitiveResolve;
    }
}
