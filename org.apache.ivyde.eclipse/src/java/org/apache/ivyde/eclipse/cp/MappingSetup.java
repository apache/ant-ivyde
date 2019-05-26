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

public class MappingSetup {

    private List<String> sourceTypes;

    private List<String> javadocTypes;

    private List<String> sourceSuffixes;

    private List<String> javadocSuffixes;

    private boolean mapIfOnlyOneSource = false;

    private boolean mapIfOnlyOneJavadoc = false;

    /**
     * Default constructor
     */
    public MappingSetup() {
        // default constructor
    }

    public void set(MappingSetup setup) {
        this.sourceTypes = setup.sourceTypes;
        this.javadocTypes = setup.javadocTypes;
        this.sourceSuffixes = setup.sourceSuffixes;
        this.javadocSuffixes = setup.javadocSuffixes;
        this.mapIfOnlyOneSource = setup.mapIfOnlyOneSource;
        this.mapIfOnlyOneJavadoc = setup.mapIfOnlyOneJavadoc;
    }

    public List<String> getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public List<String> getJavadocTypes() {
        return javadocTypes;
    }

    public void setJavadocTypes(List<String> javadocTypes) {
        this.javadocTypes = javadocTypes;
    }

    public List<String> getSourceSuffixes() {
        return sourceSuffixes;
    }

    public void setSourceSuffixes(List<String> sourceSuffixes) {
        this.sourceSuffixes = sourceSuffixes;
    }

    public List<String> getJavadocSuffixes() {
        return javadocSuffixes;
    }

    public void setJavadocSuffixes(List<String> javadocSuffixes) {
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
