/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.console;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;


public class IvyConsoleFactory implements IConsoleFactory {
    public void openConsole() {
        showConsole();
    }
    
    public static void showConsole() {
        IvyConsole console = IvyPlugin.getDefault().getConsole();
        if (console != null) {
            IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
            IConsole[] existing = manager.getConsoles();
            boolean exists = false;
            for (int i = 0; i < existing.length; i++) {
                if(console == existing[i])
                    exists = true;
            }
            if(! exists)
                manager.addConsoles(new IConsole[] {console});
            manager.showConsoleView(console);
        }
    }
    
    public static void closeConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        IvyConsole console = IvyPlugin.getDefault().getConsole();
        if (console != null) {
            manager.removeConsoles(new IConsole[] {console});
            ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new MyLifecycle());
        }
    }

}
