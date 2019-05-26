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

import org.apache.ivyde.eclipse.cp.SecuritySetup;
import org.apache.ivyde.internal.eclipse.ui.components.CustomConfirmationDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public final class GUIfactoryHelper {

    private static final String SECRET = "******";

    private GUIfactoryHelper() {

    }

    public static ColumnLabelProvider buildHostLabelProvider() {
        return new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SecuritySetup) element).getHost();
            }
        };
    }

    public static ColumnLabelProvider buildRealmLabelProvider() {
        return new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SecuritySetup) element).getRealm();
            }
        };
    }

    public static ColumnLabelProvider buildUsernameLabelProvider() {
        return new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SecuritySetup) element).getUserName();
            }
        };
    }

    public static ColumnLabelProvider buildPwdLabelProvider() {
        return new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return SECRET;
            }
        };
    }

    public static TableViewerColumn buildTableColumn(TableViewer viewer, int width, String header,
            ColumnLabelProvider provider) {
        TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
        col.getColumn().setWidth(width);
        col.getColumn().setText(header);
        col.setLabelProvider(provider);
        return col;
    }

    public static CustomConfirmationDialog buildConfirmationDialog(Shell parentShell,
            String dialogTitle, String dialogMessage) {
        return new CustomConfirmationDialog(parentShell, dialogTitle, dialogMessage);
    }
}
