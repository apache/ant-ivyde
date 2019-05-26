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

import org.apache.ivyde.internal.eclipse.IvyDEMessage;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Exception that will be displayed somehow to the end user. Once caught, depending of the context
 * the error could be popped up or just logged
 */
@SuppressWarnings("serial")
public class IvyDEException extends Exception {

    private final String shortMsg;

    private String msg;

    /**
     * Default constructor
     *
     * @param shortMsg
     *            a short description of the error, which doesn't need context information
     * @param msg
     *            full message, with context information like the full path and the project name
     * @param cause
     *            the underlying cause
     */
    public IvyDEException(String shortMsg, String msg, Throwable cause) {
        super(cause);
        this.shortMsg = shortMsg;
        this.msg = msg;
    }

    public String getShortMsg() {
        return shortMsg;
    }

    public String getMessage() {
        return msg;
    }

    public void contextualizeMessage(String context) {
        this.msg = context + ":\n  " + msg;
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
                        + getCause().getClass().getName() + "): " + getCause().getMessage());
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
                    default:
                        IvyPlugin.logWarn("Unsupported IvyDE error status: " + status);
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
     * Convert the exception into an Eclipse status
     *
     * @param status
     *            the status of the exception (error, waring or info)
     * @param preMsg
     *            the message to display before the actual error message
     * @return the eclipse status, never <code>null</code>
     */
    public IStatus asStatus(int status, String preMsg) {
        String msg = (preMsg == null ? "" : preMsg) + getMessage();
        return new Status(status, IvyPlugin.ID, 0, msg, this);
    }

    /**
     * Show the exception in the Ivy console.
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
                IvyDEMessage.error(msg);
                return;
            case IStatus.CANCEL:
            case IStatus.WARNING:
                IvyDEMessage.warn(msg);
                return;
            case IStatus.OK:
            case IStatus.INFO:
                IvyDEMessage.info(msg);
                return;
            default:
                IvyPlugin.logWarn("Unsupported IvyDE error status: " + status);
        }
    }
}
