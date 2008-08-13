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

import org.apache.ivy.util.Message;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Exception that will be displayed somehow to the end user.
 * 
 * Once catched, depending of the context the error could be popuped or just logged
 * 
 */
public class IvyDEException extends Exception {

    private final String shortMsg;

    /**
     * Default constructor
     * 
     * @param shortMsg
     *            a short description of the error, whcih doesn't need context information
     * @param msg
     *            full message, with context information like the full path and the project name
     * @param cause
     *            the underlying cause
     */
    public IvyDEException(String shortMsg, String msg, Throwable cause) {
        super(msg, cause);
        this.shortMsg = shortMsg;
    }

    public String getShortMsg() {
        return shortMsg;
    }

    /**
     * Create a popup window to display the exception to the end user.
     * 
     * @param status
     *            the status of the exception (error, waring or info)
     * @param title
     *            the title of the window
     * @param preMsg
     *            the message to display before the actual exception message
     */
    public void show(final int status, final String title, String preMsg) {
        final String msg = (preMsg == null ? "" : preMsg + "\n\n")
                + getMessage()
                + (getCause() == null ? "" : "\n\nUnderlying error ("
                        + getCause().getClass().getCanonicalName() + "): "
                        + getCause().getMessage());
        IvyPlugin.getDefault().getWorkbench().getDisplay().syncExec(new Runnable() {
            public void run() {
                switch (status) {
                    case IStatus.ERROR:
                        MessageDialog.openError(IvyPlugin.getActiveWorkbenchShell(), title, msg);
                        return;
                    case IStatus.WARNING:
                        MessageDialog.openWarning(IvyPlugin.getActiveWorkbenchShell(), title, msg);
                        return;
                    case IStatus.INFO:
                        MessageDialog.openInformation(IvyPlugin.getActiveWorkbenchShell(), title,
                            msg);
                        return;
                }
            }
        });
    }

    /**
     * Log the exception in Eclipse log system.
     * 
     * @param status
     *            the status of the exception (error, waring or info)
     * @param preMsg
     *            the message to display before the actual error message
     */
    public void log(int status, String preMsg) {
        String msg = (preMsg == null ? "" : preMsg) + getMessage();
        IvyPlugin.log(status, msg, getCause());
    }

    /**
     * Show the exception in the Ivy console
     * 
     * @param status
     *            the status of the exception (error, waring or info)
     * @param preMsg
     *            the message to display before the actual error message
     */
    public void print(int status, String preMsg) {
        String msg = (preMsg == null ? "" : preMsg) + getMessage();
        switch (status) {
            case IStatus.ERROR:
                Message.error("IVYDE: " + msg);
                return;
            case IStatus.WARNING:
                Message.warn("IVYDE: " + msg);
                return;
            case IStatus.INFO:
                Message.info("IVYDE: " + msg);
                return;
        }
    }
}
