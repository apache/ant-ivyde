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
package org.apache.ivyde.internal.eclipse.ui.console;

import java.util.Collections;
import java.util.List;

import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;
import org.apache.ivyde.internal.eclipse.IvyPlugin;

public class IvyDEMessageLogger implements MessageLogger {

    private final IvyConsole console;

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

    public void debug(String msg) {
        log(msg, Message.MSG_DEBUG);
    }

    public void verbose(String msg) {
        log(msg, Message.MSG_VERBOSE);
    }

    public void deprecated(String msg) {
        log("DEPRECATED: " + msg, Message.MSG_WARN);
    }

    public void info(String msg) {
        log(msg, Message.MSG_INFO);
    }

    public void rawinfo(String msg) {
        rawlog(msg, Message.MSG_INFO);
    }

    public void warn(String msg) {
        log(msg, Message.MSG_WARN);
    }

    public void error(String msg) {
        log(msg, Message.MSG_ERR);
    }

    public void sumupProblems() {
        // we don't buffer anything, nothing to sumup
    }

    public void clearProblems() {
        // do nothing
    }

    public List<String> getProblems() {
        return Collections.emptyList();
    }

    public List<String> getErrors() {
        return Collections.emptyList();
    }

    public List<String> getWarns() {
        return Collections.emptyList();
    }

    public void progress() {
        // do nothing
    }

    public void endProgress() {
        // do nothing
    }

    public void endProgress(String msg) {
        // do nothing
    }

    public boolean isShowProgress() {
        return false;
    }

    public void setShowProgress(boolean progress) {
        // do nothing
    }

}
