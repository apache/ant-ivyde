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
package org.apache.ivyde.common.model;

public class ListValueProvider implements IValueProvider {

    private String[] values;

    public ListValueProvider(String commaSeparatedValuesList) {
        if (commaSeparatedValuesList != null) {
            init(commaSeparatedValuesList.split(","), true);
        }
    }

    public ListValueProvider(String[] values) {
        if (values != null) {
            init(values, false);
        }
    }

    private void init(String[] v, boolean trim) {
        values = new String[v.length];
        if (trim) {
            for (int i = 0; i < v.length; i++) {
                values[i] = v[i].trim();
            }
        } else {
            System.arraycopy(v, 0, values, 0, v.length);
        }
    }

    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
        return values;
    }

}
