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

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;

public class IvyConsoleFactory implements IConsoleFactory {
    public void openConsole() {
        showConsole();
    }

    public static void showConsole() {
        IvyConsole console = IvyPlugin.getDefault().getConsole();
        if (console != null) {
            IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
            boolean exists = false;
            for (IConsole existing : manager.getConsoles()) {
                if (console == existing) {
                    exists = true;
                }
            }
            if (!exists) {
                manager.addConsoles(new IConsole[] {console});
            }
            manager.showConsoleView(console);
        }
    }

    public static void closeConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        IvyConsole console = IvyPlugin.getDefault().getConsole();
        if (console != null) {
            manager.removeConsoles(new IConsole[] {console});
            ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(
                console.new MyLifecycle());
        }
    }

}
