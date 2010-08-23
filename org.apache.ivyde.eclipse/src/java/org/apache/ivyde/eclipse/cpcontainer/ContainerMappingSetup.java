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

import java.util.List;

public class ContainerMappingSetup {

    private List/* <String> */acceptedTypes;

    private List/* <String> */sourceTypes;

    private List/* <String> */javadocTypes;

    private List/* <String> */sourceSuffixes;

    private List/* <String> */javadocSuffixes;

    private boolean mapIfOnlyOneSource = false;

    private boolean mapIfOnlyOneJavadoc = false;

    /**
     * Default constructor
     */
    public ContainerMappingSetup() {
        // default constructor
    }

    public void set(ContainerMappingSetup setup) {
        this.acceptedTypes = setup.acceptedTypes;
        this.sourceTypes = setup.sourceTypes;
        this.javadocTypes = setup.javadocTypes;
        this.sourceSuffixes = setup.sourceSuffixes;
        this.javadocSuffixes = setup.javadocSuffixes;
        this.mapIfOnlyOneSource = setup.mapIfOnlyOneSource;
        this.mapIfOnlyOneJavadoc = setup.mapIfOnlyOneJavadoc;
    }

    public List getAcceptedTypes() {
        return acceptedTypes;
    }

    public void setAcceptedTypes(List acceptedTypes) {
        this.acceptedTypes = acceptedTypes;
    }

    public List getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public List getJavadocTypes() {
        return javadocTypes;
    }

    public void setJavadocTypes(List javadocTypes) {
        this.javadocTypes = javadocTypes;
    }

    public List getSourceSuffixes() {
        return sourceSuffixes;
    }

    public void setSourceSuffixes(List sourceSuffixes) {
        this.sourceSuffixes = sourceSuffixes;
    }

    public List getJavadocSuffixes() {
        return javadocSuffixes;
    }

    public void setJavadocSuffixes(List javadocSuffixes) {
        this.javadocSuffixes = javadocSuffixes;
    }

    public boolean isMapIfOnlyOneSource() {
        return mapIfOnlyOneSource;
    }

    public void setMapIfOnlyOneSource(boolean autoMap) {
        mapIfOnlyOneSource = autoMap;
    }

    public boolean isMapIfOnlyOneJavadoc() {
        return mapIfOnlyOneJavadoc;
    }

    public void setMapIfOnlyOneJavadoc(boolean autoMap) {
        mapIfOnlyOneJavadoc = autoMap;
    }

}
