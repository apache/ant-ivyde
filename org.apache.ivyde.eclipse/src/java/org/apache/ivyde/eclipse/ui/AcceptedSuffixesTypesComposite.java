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

import org.apache.ivyde.eclipse.cpcontainer.ContainerMappingSetup;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
    
    public static final String TOOLTIP_MAP_IF_ONLY_ONE_SOURCE = "Will map the source artifact"
            + " to all jar artifact in modules with multiple jar artifacts and only one"
            + " source artifact";    

    public static final String TOOLTIP_MAP_IF_ONLY_ONE_JAVADOC = "Will map the javadoc artifact"
        + " to all jar artifact in modules with multiple jar artifacts and only one"
        + " javadoc artifact";    

    private Text acceptedTypesText;

    private Text sourceTypesText;

    private Text sourceSuffixesText;

    private Text javadocTypesText;

    private Text javadocSuffixesText;

    private Button mapIfOnlyOneSourceCheck;

    private Button mapIfOnlyOneJavadocCheck;

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

        sourceTypesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourceTypesText
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        sourceTypesText.setToolTipText(TOOLTIP_SOURCE_TYPES);

        label = new Label(this, SWT.NONE);
        label.setText("Sources suffixes:");

        sourceSuffixesText = new Text(this, SWT.SINGLE | SWT.BORDER);
        sourceSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        sourceSuffixesText.setToolTipText(TOOLTIP_SOURCE_SUFFIXES);

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

    public void init(ContainerMappingSetup setup) {
        acceptedTypesText.setText(IvyClasspathUtil.concat(setup.getAcceptedTypes()));
        sourceTypesText.setText(IvyClasspathUtil.concat(setup.getSourceTypes()));
        sourceSuffixesText.setText(IvyClasspathUtil.concat(setup.getSourceSuffixes()));
        javadocTypesText.setText(IvyClasspathUtil.concat(setup.getJavadocTypes()));
        javadocSuffixesText.setText(IvyClasspathUtil.concat(setup.getJavadocSuffixes()));
        mapIfOnlyOneSourceCheck.setSelection(setup.isMapIfOnlyOneSource());
        mapIfOnlyOneJavadocCheck.setSelection(setup.isMapIfOnlyOneJavadoc());
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        acceptedTypesText.setEnabled(enabled);
        sourceTypesText.setEnabled(enabled);
        sourceSuffixesText.setEnabled(enabled);
        javadocTypesText.setEnabled(enabled);
        javadocSuffixesText.setEnabled(enabled);
        mapIfOnlyOneSourceCheck.setEnabled(enabled);
        mapIfOnlyOneJavadocCheck.setEnabled(enabled);
    }

    public ContainerMappingSetup getContainerMappingSetup() {
        ContainerMappingSetup setup = new ContainerMappingSetup();
        setup.setAcceptedTypes(IvyClasspathUtil.split(acceptedTypesText.getText()));
        setup.setSourceTypes(IvyClasspathUtil.split(sourceTypesText.getText()));
        setup.setJavadocTypes(IvyClasspathUtil.split(javadocTypesText.getText()));
        setup.setSourceSuffixes(IvyClasspathUtil.split(sourceSuffixesText.getText()));
        setup.setJavadocSuffixes(IvyClasspathUtil.split(javadocSuffixesText.getText()));
        setup.setMapIfOnlyOneSource(mapIfOnlyOneSourceCheck.getSelection());
        setup.setMapIfOnlyOneJavadoc(mapIfOnlyOneJavadocCheck.getSelection());
        return setup;
    }
}
