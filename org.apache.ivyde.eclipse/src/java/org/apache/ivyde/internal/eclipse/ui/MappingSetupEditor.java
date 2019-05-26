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
package org.apache.ivyde.internal.eclipse.ui;

import org.apache.ivyde.eclipse.cp.MappingSetup;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MappingSetupEditor extends Composite {

    public static final String TOOLTIP_SOURCE_TYPES = "Comma separated list of artifact types to"
            + " be used as sources.\nExample: source, src";

    public static final String TOOLTIP_JAVADOC_TYPES = "Comma separated list of artifact types to"
            + " be used as javadoc.\nExample: javadoc.";

    public static final String TOOLTIP_SOURCE_SUFFIXES = "Comma separated list of suffixes to match"
            + " sources to jars in the classpath.\nExample: -source, -src";

    public static final String TOOLTIP_JAVADOC_SUFFIXES = "Comma separated list of suffixes to"
            + " match javadocs to jars in the classpath.\nExample: -javadoc, -doc";

    public static final String TOOLTIP_MAP_IF_ONLY_ONE_SOURCE = "Will map the source artifact"
            + " to all jar artifact in modules with multiple jar artifacts and only one"
            + " source artifact";

    public static final String TOOLTIP_MAP_IF_ONLY_ONE_JAVADOC = "Will map the javadoc artifact"
            + " to all jar artifact in modules with multiple jar artifacts and only one"
            + " javadoc artifact";

    private final Text sourceTypesText;

    private final Text sourceSuffixesText;

    private final Text javadocTypesText;

    private final Text javadocSuffixesText;

    private final Button mapIfOnlyOneSourceCheck;

    private final Button mapIfOnlyOneJavadocCheck;

    private final Label sourceTypesLabel;

    private final Label sourceSuffixesLabel;

    private final Label javadocTypesLabel;

    private final Label javadocSuffixesLabel;

    public MappingSetupEditor(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));

        sourceTypesLabel = new Label(this, SWT.NONE);
        sourceTypesLabel.setText("Sources types:");

        sourceTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourceTypesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        sourceTypesText.setToolTipText(TOOLTIP_SOURCE_TYPES);

        sourceSuffixesLabel = new Label(this, SWT.NONE);
        sourceSuffixesLabel.setText("Sources suffixes:");

        sourceSuffixesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourceSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        sourceSuffixesText.setToolTipText(TOOLTIP_SOURCE_SUFFIXES);

        javadocTypesLabel = new Label(this, SWT.NONE);
        javadocTypesLabel.setText("Javadoc types:");

        javadocTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        javadocTypesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        javadocTypesText.setToolTipText(TOOLTIP_JAVADOC_TYPES);

        javadocSuffixesLabel = new Label(this, SWT.NONE);
        javadocSuffixesLabel.setText("Javadoc suffixes:");

        javadocSuffixesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        javadocSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        javadocSuffixesText.setToolTipText(TOOLTIP_JAVADOC_SUFFIXES);

        mapIfOnlyOneSourceCheck = new Button(this, SWT.CHECK);
        mapIfOnlyOneSourceCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 2, 1));
        mapIfOnlyOneSourceCheck.setText("Auto map jar artifacts with unique source artifact");
        mapIfOnlyOneSourceCheck.setToolTipText(TOOLTIP_MAP_IF_ONLY_ONE_SOURCE);

        mapIfOnlyOneJavadocCheck = new Button(this, SWT.CHECK);
        mapIfOnlyOneJavadocCheck.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
                false, 2, 1));
        mapIfOnlyOneJavadocCheck.setText("Auto map jar artifacts with unique javadoc artifact");
        mapIfOnlyOneJavadocCheck.setToolTipText(TOOLTIP_MAP_IF_ONLY_ONE_JAVADOC);

    }

    public void init(MappingSetup setup) {
        sourceTypesText.setText(IvyClasspathUtil.concat(setup.getSourceTypes()));
        sourceSuffixesText.setText(IvyClasspathUtil.concat(setup.getSourceSuffixes()));
        javadocTypesText.setText(IvyClasspathUtil.concat(setup.getJavadocTypes()));
        javadocSuffixesText.setText(IvyClasspathUtil.concat(setup.getJavadocSuffixes()));
        mapIfOnlyOneSourceCheck.setSelection(setup.isMapIfOnlyOneSource());
        mapIfOnlyOneJavadocCheck.setSelection(setup.isMapIfOnlyOneJavadoc());
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        sourceTypesLabel.setEnabled(enabled);
        sourceTypesText.setEnabled(enabled);
        sourceSuffixesLabel.setEnabled(enabled);
        sourceSuffixesText.setEnabled(enabled);
        javadocTypesLabel.setEnabled(enabled);
        javadocTypesText.setEnabled(enabled);
        javadocSuffixesLabel.setEnabled(enabled);
        javadocSuffixesText.setEnabled(enabled);
        mapIfOnlyOneSourceCheck.setEnabled(enabled);
        mapIfOnlyOneJavadocCheck.setEnabled(enabled);
    }

    public MappingSetup getMappingSetup() {
        MappingSetup setup = new MappingSetup();
        setup.setSourceTypes(IvyClasspathUtil.split(sourceTypesText.getText()));
        setup.setJavadocTypes(IvyClasspathUtil.split(javadocTypesText.getText()));
        setup.setSourceSuffixes(IvyClasspathUtil.split(sourceSuffixesText.getText()));
        setup.setJavadocSuffixes(IvyClasspathUtil.split(javadocSuffixesText.getText()));
        setup.setMapIfOnlyOneSource(mapIfOnlyOneSourceCheck.getSelection());
        setup.setMapIfOnlyOneJavadoc(mapIfOnlyOneJavadocCheck.getSelection());
        return setup;
    }
}
