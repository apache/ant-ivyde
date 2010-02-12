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
package org.apache.ivyde.eclipse.ui.views;

import java.net.URL;

import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ReportView extends ViewPart implements ISelectionListener {
    private Browser browser;

    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);

        // add myself as a global selection listener
        getSite().getPage().addSelectionListener(this);

        // prime the selection
        selectionChanged(null, getSite().getPage().getSelection());

    }

    public void setFocus() {
        // nothing to do
    }

    public void selectionChanged(IWorkbenchPart part, ISelection sel) {
        if (sel instanceof IStructuredSelection) {
            IvyClasspathContainer ivycp = IvyClasspathUtil
                    .getIvyClasspathContainer((IStructuredSelection) sel);
            if (ivycp != null) {
                browser.setUrl("");
                URL report;
                try {
                    report = ivycp.getReportUrl();
                } catch (IvyDEException e) {
                    e.log(IStatus.WARNING, "Impossible show the report for " + ivycp.getConf());
                    e.show(IStatus.WARNING, "Show Ivy report failure",
                        "Impossible show the report for " + ivycp.getConf());
                    return;
                }
                if (!browser.setUrl(report.toExternalForm())) {
                    browser.setUrl("");
                    Message.warn("impossible to set report view url to " + report.toExternalForm());
                }
            }
        }
    }

}
