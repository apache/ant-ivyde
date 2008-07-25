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
package org.apache.ivyde.eclipse.ui.editors.xml;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;

public class XMLEditor extends TextEditor {

    private ColorManager colorManager;

    public XMLEditor(IvyContentAssistProcessor processor) {
        super();
        colorManager = new ColorManager();
        _configuration = new XMLConfiguration(colorManager, processor);
        setSourceViewerConfiguration(_configuration);
        setDocumentProvider(new XMLDocumentProvider());

    }

    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }

    private static final String CONTENTASSIST_PROPOSAL_ID = "org.apache.ivyde.ContentAssistProposal";

    private XMLConfiguration _configuration;

    protected void createActions() {
        super.createActions();

        // This action will fire a CONTENTASSIST_PROPOSALS operation
        // when executed
        IAction action = new TextOperationAction(IvyPlugin.getDefault().getResourceBundle(),
                "ContentAssistProposal", this, SourceViewer.CONTENTASSIST_PROPOSALS);
        action.setActionDefinitionId(CONTENTASSIST_PROPOSAL_ID);

        // Tell the editor about this new action
        setAction(CONTENTASSIST_PROPOSAL_ID, action);

        // Tell the editor to execute this action
        // when Ctrl+Spacebar is pressed
        setActionActivationCode(CONTENTASSIST_PROPOSAL_ID, ' ', -1, SWT.CTRL);
    }

    public void setFile(IFile file) {
        _configuration.setFile(file);
    }
}
