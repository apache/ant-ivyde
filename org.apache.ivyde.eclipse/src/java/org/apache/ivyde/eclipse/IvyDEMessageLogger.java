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
package org.apache.ivyde.eclipse;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivyde.eclipse.ui.console.IvyConsole;

public class IvyDEMessageLogger extends AbstractMessageLogger {

    private IvyConsole console;

    private int logLevel;

    public IvyDEMessageLogger(IvyConsole console) {
        this.console = console;
        logLevel = IvyPlugin.getPreferenceStoreHelper().getIvyConsoleIvyDELogLevel();
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        IvyPlugin.getPreferenceStoreHelper().setIvyConsoleIvyDELogLevel(logLevel);
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void log(String msg, int level) {
        if (level <= logLevel) {
            console.doAppendLine(level, "[IvyDE] " + msg);
        }
    }

    public void rawlog(String msg, int level) {
        log(msg, level);
    }

    public void doProgress() {
        // nothing
    }

    public void doEndProgress(String msg) {
        // nothing
    }

}
