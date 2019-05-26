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
package org.apache.ivyde.test.launch.main;

import org.apache.ivyde.test.launch.dependency.HelloworldGetter;

public class Test {

    public static void main(String[] args) {
        HelloworldGetter helloworldGetter = new HelloworldGetter();
        System.out.println(helloworldGetter.get());
        try {
            Class.forName("junit.framework.Assert");
            System.out.println("junit.framework.Assert found : KO");
        } catch (ClassNotFoundException e) {
            System.out.println("junit.framework.Assert not found : OK");
        }
    }
}
