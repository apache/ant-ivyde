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
package org.apache.ivyde.internal.eclipse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;
import org.apache.ivyde.eclipse.NopMessageLogger;
import org.apache.ivyde.internal.eclipse.ui.console.IvyConsole;

public class IvyDEMessage {

    public static void debug(String msg) {
        getLogger().debug(msg);
    }

    private static MessageLogger getLogger() {
        IvyPlugin plugin = IvyPlugin.getDefault();
        if (plugin == null) {
            return NopMessageLogger.INSTANCE;
        }
        IvyConsole console = plugin.getConsole();
        if (console == null) {
            return NopMessageLogger.INSTANCE;
        }
        return console.getIvyDEMessageLogger();
    }

    public static void verbose(String msg) {
        getLogger().verbose(msg);
    }

    public static void info(String msg) {
        getLogger().info(msg);
    }

    public static void rawinfo(String msg) {
        getLogger().rawinfo(msg);
    }

    public static void deprecated(String msg) {
        getLogger().deprecated(msg);
    }

    public static void warn(String msg) {
        getLogger().warn(msg);
    }

    public static void error(String msg) {
        getLogger().error(msg);
    }

    public static void log(int logLevel, String msg) {
        switch (logLevel) {
            case Message.MSG_DEBUG:
                debug(msg);
                break;
            case Message.MSG_VERBOSE:
                verbose(msg);
                break;
            case Message.MSG_INFO:
                info(msg);
                break;
            case Message.MSG_WARN:
                warn(msg);
                break;
            case Message.MSG_ERR:
                error(msg);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level " + logLevel);
        }
    }

    public static List<String> getProblems() {
        return getLogger().getProblems();
    }

    public static void sumupProblems() {
        getLogger().sumupProblems();
    }

    public static void progress() {
        getLogger().progress();
    }

    public static void endProgress() {
        getLogger().endProgress();
    }

    public static void endProgress(String msg) {
        getLogger().endProgress(msg);
    }

    public static boolean isShowProgress() {
        return getLogger().isShowProgress();
    }

    public static void setShowProgress(boolean progress) {
        getLogger().setShowProgress(progress);
    }

    public static void debug(String message, Throwable t) {
        if (t == null) {
            debug(message);
        } else {
            debug(message + " (" + t.getClass().getName() + ": " + t.getMessage() + ")");
            debug(t);
        }
    }

    public static void verbose(String message, Throwable t) {
        if (t == null) {
            verbose(message);
        } else {
            verbose(message + " (" + t.getClass().getName() + ": " + t.getMessage() + ")");
            debug(t);
        }
    }

    public static void info(String message, Throwable t) {
        if (t == null) {
            info(message);
        } else {
            info(message + " (" + t.getClass().getName() + ": " + t.getMessage() + ")");
            debug(t);
        }
    }

    public static void warn(String message, Throwable t) {
        if (t == null) {
            warn(message);
        } else {
            warn(message + " (" + t.getClass().getName() + ": " + t.getMessage() + ")");
            debug(t);
        }
    }

    public static void error(String message, Throwable t) {
        if (t == null) {
            error(message);
        } else {
            error(message + " (" + t.getClass().getName() + ": " + t.getMessage() + ")");
            debug(t);
        }
    }

    public static void debug(Throwable t) {
        debug(getStackTrace(t));
    }

    private static String getStackTrace(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw, true);
        e.printStackTrace(printWriter);
        return sw.getBuffer().toString();
    }

}
