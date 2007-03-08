/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.console;

import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageImpl;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * This class is used to deal with ivy output, and is largely insprired of
 * CVSOutputConsole for its implementation
 * 
 */
public class IvyConsole extends MessageConsole implements MessageImpl {
    private MessageConsoleStream[] streams = new MessageConsoleStream[5];
    
    private ConsoleDocument document;
    private boolean initialized;
    private boolean visible;
    private boolean showOnMessage;
    private IConsoleManager consoleManager;
    
    public IvyConsole() {
        super("Ivy", IvyPlugin.getImageDescriptor("icons/logo16x16.gif")); //$NON-NLS-1$
        consoleManager = ConsolePlugin.getDefault().getConsoleManager();
        document = new ConsoleDocument();
        Message.init(this);
    }

    public void endProgress(String msg) {
    }

    public void progress() {
    }

    public void log(String msg, int level) {
        appendLine(level, msg);
    }        

    public void rawlog(String msg, int level) {
        appendLine(level, msg);
    }
    
    /**
     * Used to notify this console of lifecycle methods <code>init()</code>
     * and <code>dispose()</code>.
     */
    public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
        public void consolesAdded(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == IvyConsole.this) {
                    init();
                }
            }

        }
        public void consolesRemoved(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == IvyConsole.this) {
                    ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
                    dispose();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.AbstractConsole#init()
     */
    protected void init() {
        // Called when console is added to the console view
        super.init();   
        
        //  Ensure that initialization occurs in the ui thread
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                initializeStreams();
                dump();
            }
        });
    }

    /*
     * Initialize thre streams of the console. Must be 
     * called from the UI thread.
     */
    private void initializeStreams() {
        synchronized(document) {
            if (!initialized) {
                for (int i=0; i<5; i++) {
                    streams[i] = newMessageStream();
                }

                // install colors
                Color color;

                color = createColor(Display.getDefault(), IvyPlugin.PREF_CONSOLE_DEBUG_COLOR);
                streams[Message.MSG_DEBUG].setColor(color);
                color = createColor(Display.getDefault(), IvyPlugin.PREF_CONSOLE_VERBOSE_COLOR);
                streams[Message.MSG_VERBOSE].setColor(color);
                color = createColor(Display.getDefault(), IvyPlugin.PREF_CONSOLE_INFO_COLOR);
                streams[Message.MSG_INFO].setColor(color);
                color = createColor(Display.getDefault(), IvyPlugin.PREF_CONSOLE_WARN_COLOR);
                streams[Message.MSG_WARN].setColor(color);
                color = createColor(Display.getDefault(), IvyPlugin.PREF_CONSOLE_ERROR_COLOR);
                streams[Message.MSG_ERR].setColor(color);

                initialized = true;
            }
        }
    }

    private void dump() {
        synchronized(document) {
            visible = true;
            ConsoleDocument.ConsoleLine[] lines = document.getLines();
            for (int i = 0; i < lines.length; i++) {
                ConsoleDocument.ConsoleLine line = lines[i];
                appendLine(line.type, line.line);
            }
            document.clear();
        }
    }
    
    private void appendLine(int level, String line) {
        showConsole();
        synchronized(document) {
            if(visible) {
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
     */
    private Color createColor(Display display, String preference) {
        RGB rgb = PreferenceConverter.getColor(IvyPlugin.getDefault().getPreferenceStore(), preference);
        if (rgb == PreferenceConverter.COLOR_DEFAULT_DEFAULT) {
            if (IvyPlugin.PREF_CONSOLE_DEBUG_COLOR.equals(preference)) {
                rgb = new RGB(180,180,255);
            } else if (IvyPlugin.PREF_CONSOLE_VERBOSE_COLOR.equals(preference)) {
                rgb = new RGB(50,150,50);
            } else if (IvyPlugin.PREF_CONSOLE_WARN_COLOR.equals(preference)) {
                rgb = new RGB(255,80,20);
            } else if (IvyPlugin.PREF_CONSOLE_ERROR_COLOR.equals(preference)) {
                rgb = new RGB(255,0,0);
            }
        }
        return new Color(display, rgb);
    }

    /**
     * Show the console.
     * @param showNoMatterWhat ignore preferences if <code>true</code>
     */
    public void show(boolean showNoMatterWhat) {
        if(showNoMatterWhat || showOnMessage) {
            if(!visible)
                IvyConsoleFactory.showConsole();
            else
                consoleManager.showConsoleView(this);
        }
        
    }

}
