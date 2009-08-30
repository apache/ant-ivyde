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
package org.apache.ivyde.eclipse.ui;

import java.util.Collection;
import java.util.List;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AcceptedSuffixesTypesComposite extends Composite {

    public static final String TOOLTIP_ACCEPTED_TYPES = "Comma separated list of artifact types"
            + " to use in IvyDE Managed Dependencies Library.\n" + "Example: jar, zip";

    public static final String TOOLTIP_SOURCE_TYPES = "Comma separated list of artifact types to"
            + " be used as sources.\nExample: source, src";

    public static final String TOOLTIP_JAVADOC_TYPES = "Comma separated list of artifact types to"
            + " be used as javadoc.\nExample: javadoc.";

    public static final String TOOLTIP_SOURCE_SUFFIXES = "Comma separated list of suffixes to match"
            + " sources to artifacts.\nExample: -source, -src";

    public static final String TOOLTIP_JAVADOC_SUFFIXES = "Comma separated list of suffixes to"
            + " match javadocs to artifacts.\nExample: -javadoc, -doc";

    private Text acceptedTypesText;

    private Text sourcesTypesText;

    private Text sourcesSuffixesText;

    private Text javadocTypesText;

    private Text javadocSuffixesText;

    public AcceptedSuffixesTypesComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        Label label = new Label(this, SWT.NONE);
        label.setText("Accepted types:");

        acceptedTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        acceptedTypesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        acceptedTypesText.setToolTipText(TOOLTIP_ACCEPTED_TYPES);

        label = new Label(this, SWT.NONE);
        label.setText("Sources types:");

        sourcesTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourcesTypesText
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        sourcesTypesText.setToolTipText(TOOLTIP_SOURCE_TYPES);

        label = new Label(this, SWT.NONE);
        label.setText("Sources suffixes:");

        sourcesSuffixesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourcesSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        sourcesSuffixesText.setToolTipText(TOOLTIP_SOURCE_SUFFIXES);

        label = new Label(this, SWT.NONE);
        label.setText("Javadoc types:");

        javadocTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        javadocTypesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        javadocTypesText.setToolTipText(TOOLTIP_JAVADOC_TYPES);

        label = new Label(this, SWT.NONE);
        label.setText("Javadoc suffixes:");

        javadocSuffixesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        javadocSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        javadocSuffixesText.setToolTipText(TOOLTIP_JAVADOC_SUFFIXES);
    }

    public void init(Collection acceptedTypes, Collection sourceTypes, Collection sourceSuffixes,
            Collection javadocTypes, Collection javadocSuffixes) {
        init(IvyClasspathUtil.concat(acceptedTypes), IvyClasspathUtil.concat(sourceTypes),
            IvyClasspathUtil.concat(sourceSuffixes), IvyClasspathUtil.concat(javadocTypes),
            IvyClasspathUtil.concat(javadocSuffixes));
    }

    public void init(String acceptedTypes, String sourceTypes, String sourceSuffixes,
            String javadocTypes, String javadocSuffixes) {
        acceptedTypesText.setText(acceptedTypes);
        sourcesTypesText.setText(sourceTypes);
        sourcesSuffixesText.setText(sourceSuffixes);
        javadocTypesText.setText(javadocTypes);
        javadocSuffixesText.setText(javadocSuffixes);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        acceptedTypesText.setEnabled(enabled);
        sourcesTypesText.setEnabled(enabled);
        sourcesSuffixesText.setEnabled(enabled);
        javadocTypesText.setEnabled(enabled);
        javadocSuffixesText.setEnabled(enabled);
    }

    public List getAcceptedTypes() {
        return IvyClasspathUtil.split(acceptedTypesText.getText());
    }

    public List getSourcesTypes() {
        return IvyClasspathUtil.split(sourcesTypesText.getText());
    }

    public List getJavadocTypes() {
        return IvyClasspathUtil.split(javadocTypesText.getText());
    }

    public List getSourceSuffixes() {
        return IvyClasspathUtil.split(sourcesSuffixesText.getText());
    }

    public List getJavadocSuffixes() {
        return IvyClasspathUtil.split(javadocSuffixesText.getText());
    }
}
