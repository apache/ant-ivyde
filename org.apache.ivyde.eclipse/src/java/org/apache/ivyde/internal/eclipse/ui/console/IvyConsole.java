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

import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;
import org.apache.ivy.util.MessageLoggerHelper;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * This class is used to deal with Ivy output, and is largely inspired of CVSOutputConsole for its
 * implementation
 */
public class IvyConsole extends MessageConsole implements MessageLogger {

    public static final String PREF_CONSOLE_DEBUG_COLOR = IvyPlugin.ID + ".console.color.debug";

    public static final String PREF_CONSOLE_VERBOSE_COLOR = IvyPlugin.ID + ".console.color.verbose";

    public static final String PREF_CONSOLE_INFO_COLOR = IvyPlugin.ID + ".console.color.info";

    public static final String PREF_CONSOLE_WARN_COLOR = IvyPlugin.ID + ".console.color.warn";

    public static final String PREF_CONSOLE_ERROR_COLOR = IvyPlugin.ID + ".console.color.error";

    // CheckStyle:MagicNumber| OFF
    private final MessageConsoleStream[] streams = new MessageConsoleStream[5];

    // CheckStyle:MagicNumber| ON

    private final ConsoleDocument document;

    private boolean initialized = false;

    private boolean visible = false;

    private boolean showOnMessage;

    private final IConsoleManager consoleManager;

    private int logLevel;

    private final IvyDEMessageLogger ivyDEMessageLogger;

    public IvyConsole() {
        this("Ivy", IvyPlugin.getImageDescriptor("icons/logo16x16.gif")); //$NON-NLS-1$
    }

    public IvyConsole(String name, ImageDescriptor imageDescriptor) {
        super(name, imageDescriptor);
        consoleManager = ConsolePlugin.getDefault().getConsoleManager();
        document = new ConsoleDocument();
        Message.setDefaultLogger(this);
        logLevel = IvyPlugin.getPreferenceStoreHelper().getIvyConsoleLogLevel();
        ivyDEMessageLogger = new IvyDEMessageLogger(this);
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        IvyPlugin.getPreferenceStoreHelper().setIvyConsoleLogLevel(logLevel);
    }

    public int getLogLevel() {
        return logLevel;
    }

    public IvyDEMessageLogger getIvyDEMessageLogger() {
        return ivyDEMessageLogger;
    }

    public void endProgress(String msg) {
        // nothing to log
    }

    public void progress() {
        // nothing to step
    }

    public void log(String msg, int level) {
        appendLine(level, msg);
    }

    public void rawlog(String msg, int level) {
        appendLine(level, msg);
    }

    /**
     * Used to notify this console of lifecycle methods <code>init()</code> and
     * <code>dispose()</code>.
     */
    public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
        public void consolesAdded(IConsole[] consoles) {
            for (IConsole console : consoles) {
                if (console == IvyConsole.this) {
                    init();
                }
            }

        }

        public void consolesRemoved(IConsole[] consoles) {
            for (IConsole console : consoles) {
                if (console == IvyConsole.this) {
                    ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
                    dispose();
                }
            }
        }
    }

    protected void init() {
        // Called when console is added to the console view
        super.init();

        // Ensure that initialization occurs in the ui thread
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                initializeStreams();
                dump();
            }
        });
    }

    /*
     * Initialize three streams of the console. Must be called from the UI thread.
     */
    private void initializeStreams() {
        synchronized (document) {
            if (!initialized) {
                for (int i = 0; i < streams.length; i++) {
                    streams[i] = newMessageStream();
                }

                // install colors
                Color color;

                color = createColor(Display.getDefault(), PREF_CONSOLE_DEBUG_COLOR);
                streams[Message.MSG_DEBUG].setColor(color);
                color = createColor(Display.getDefault(), PREF_CONSOLE_VERBOSE_COLOR);
                streams[Message.MSG_VERBOSE].setColor(color);
                color = createColor(Display.getDefault(), PREF_CONSOLE_INFO_COLOR);
                streams[Message.MSG_INFO].setColor(color);
                color = createColor(Display.getDefault(), PREF_CONSOLE_WARN_COLOR);
                streams[Message.MSG_WARN].setColor(color);
                color = createColor(Display.getDefault(), PREF_CONSOLE_ERROR_COLOR);
                streams[Message.MSG_ERR].setColor(color);

                initialized = true;
            }
        }
    }

    private void dump() {
        synchronized (document) {
            visible = true;
            for (ConsoleDocument.ConsoleLine line : document.getLines()) {
                appendLine(line.getType(), line.getLine());
            }
            document.clear();
        }
    }

    private void appendLine(int level, String line) {
        if (level > logLevel) {
            // log message filtered by its level
            return;
        }
        doAppendLine(level, line);
    }

    public void doAppendLine(int level, String line) {
        showConsole();
        synchronized (document) {
            if (visible) {
                streams[level].println(line);
            } else {
                document.appendConsoleLine(level, line);
            }
        }
    }

    private void showConsole() {
        show(false);
    }

    /**
     * Returns a color instance based on data from a preference field.
     *
     * @param display Display
     * @param preference String
     * @return Color
     */
    private Color createColor(Display display, String preference) {
        RGB rgb = PreferenceConverter.getColor(IvyPlugin.getDefault().getPreferenceStore(),
            preference);
        if (rgb == PreferenceConverter.COLOR_DEFAULT_DEFAULT) {
            // CheckStyle:MagicNumber| OFF
            switch (preference) {
                case PREF_CONSOLE_DEBUG_COLOR:
                    rgb = new RGB(180, 180, 255);
                    break;
                case PREF_CONSOLE_VERBOSE_COLOR:
                    rgb = new RGB(50, 150, 50);
                    break;
                case PREF_CONSOLE_WARN_COLOR:
                    rgb = new RGB(255, 80, 20);
                    break;
                case PREF_CONSOLE_ERROR_COLOR:
                    rgb = new RGB(255, 0, 0);
                    break;
            }
            // CheckStyle:MagicNumber| ON
        }
        return new Color(display, rgb);
    }

    protected void dispose() {
        // Here we can't call super.dispose() because we actually want the partitioner to remain
        // connected, but we won't show lines until the console is added to the console manager
        // again.

        // Called when console is removed from the console view
        synchronized (document) {
            visible = false;
        }
    }

    /**
     * Show the console.
     *
     * @param showNoMatterWhat
     *            ignore preferences if <code>true</code>
     */
    public void show(boolean showNoMatterWhat) {
        if (showNoMatterWhat || showOnMessage) {
            if (!visible) {
                IvyConsoleFactory.showConsole();
            } else {
                consoleManager.showConsoleView(this);
            }
        }

    }

    // MessageLogger implementation
    private final List<String> problems = new ArrayList<>();

    private final List<String> warns = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    private boolean showProgress = true;

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
        log("WARN: " + msg, Message.MSG_VERBOSE);
        problems.add("WARN:  " + msg);
        getWarns().add(msg);
    }

    public void error(String msg) {
        // log in verbose mode because message is appended as a problem, and will be
        // logged at the end at error level
        log("ERROR: " + msg, Message.MSG_VERBOSE);
        problems.add("\tERROR: " + msg);
        getErrors().add(msg);
    }

    public List<String> getProblems() {
        return problems;
    }

    public void sumupProblems() {
        MessageLoggerHelper.sumupProblems(this);
        clearProblems();
    }

    public void clearProblems() {
        problems.clear();
        warns.clear();
        errors.clear();
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarns() {
        return warns;
    }

    public void endProgress() {
        endProgress("");
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean progress) {
        showProgress = progress;
    }

}
