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
package org.apache.ivyde.eclipse;

import java.util.Collections;
import java.util.List;

import org.apache.ivy.util.MessageLogger;

public class NopMessageLogger implements MessageLogger {

    public static final NopMessageLogger INSTANCE = new NopMessageLogger();

    public void log(String msg, int level) {
        // no nothing
    }

    public void rawlog(String msg, int level) {
        // no nothing
    }

    public void debug(String msg) {
        // no nothing
    }

    public void verbose(String msg) {
        // no nothing
    }

    public void deprecated(String msg) {
        // no nothing
    }

    public void info(String msg) {
        // no nothing
    }

    public void rawinfo(String msg) {
        // no nothing
    }

    public void warn(String msg) {
        // no nothing
    }

    public void error(String msg) {
        // no nothing
    }

    public List<String> getProblems() {
        return Collections.emptyList();
    }

    public List<String> getWarns() {
        return Collections.emptyList();
    }

    public List<String> getErrors() {
        return Collections.emptyList();
    }

    public void clearProblems() {
        // no nothing
    }

    public void sumupProblems() {
        // no nothing
    }

    public void progress() {
        // no nothing
    }

    public void endProgress() {
        // no nothing
    }

    public void endProgress(String msg) {
        // no nothing
    }

    public boolean isShowProgress() {
        return false;
    }

    public void setShowProgress(boolean progress) {
        // no nothing
    }

}
