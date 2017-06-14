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
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.cp.SecuritySetup;
@Deprecated
//TODO: really needed?
public final class SecuritySetupContainer {

    public static final SecuritySetupContainer INSTANCE = new SecuritySetupContainer();

    private static List<SecuritySetup> credentials = new ArrayList<SecuritySetup>();

    private SecuritySetupContainer() {

    }
    
    //test-data
    static{
        credentials = new ArrayList<SecuritySetup>();
        credentials.add(new SecuritySetup("localhost","nexus","admin","secret"));
        credentials.add(new SecuritySetup("arctis","nexus3","adminArctis","secret"));
        credentials.add(new SecuritySetup("remote","nexus repo","adminRemote","secret"));        
    }
    
    public static void addEntry(SecuritySetup entry) {
        credentials.add(entry);
    }

    public static void removeEntry(SecuritySetup entry) {
        credentials.remove(entry);
    }

    public static List<SecuritySetup> getAllCredentials() {
        return credentials;
    }
}
