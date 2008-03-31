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
package org.apache.ivyde.eclipse.ui.core.model;

public class Proposal {
    private int _cursor;

    private String _proposal;

    private String _doc;

    public Proposal(String proposal, int cursor, String doc) {
        _cursor = cursor;
        _proposal = proposal;
        _doc = doc;
    }

    public int getCursor() {
        return _cursor;
    }

    public String getProposal() {
        return _proposal;
    }

    public String getDoc() {
        return _doc;
    }

}
