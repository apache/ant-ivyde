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
package org.apache.ivyde.internal.eclipse.ui.editors.xml;

import org.apache.ivyde.internal.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;

public class XMLConfiguration extends SourceViewerConfiguration {
    private XMLDoubleClickStrategy doubleClickStrategy;

    private XMLTagScanner tagScanner;

    private XMLScanner scanner;

    private final ColorManager colorManager;

    private final IvyContentAssistProcessor processor;

    private IFile file;

    public XMLConfiguration(ColorManager colorManager, IvyContentAssistProcessor processor) {
        this.colorManager = colorManager;
        this.processor = processor;
    }

    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT,
                XMLPartitionScanner.XML_TAG};
    }

    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
            String contentType) {
        if (doubleClickStrategy == null) {
            doubleClickStrategy = new XMLDoubleClickStrategy();
        }
        return doubleClickStrategy;
    }

    protected XMLScanner getXMLScanner() {
        if (scanner == null) {
            scanner = new XMLScanner(colorManager);
            scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
                    .getColor(PreferenceConstants.EDITOR_COLOR_DEFAULT))));
        }
        return scanner;
    }

    protected XMLTagScanner getXMLTagScanner() {
        if (tagScanner == null) {
            tagScanner = new XMLTagScanner(colorManager);
            tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
                    .getColor(PreferenceConstants.EDITOR_COLOR_TAG))));
        }
        return tagScanner;
    }

    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
        reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
        reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

        dr = new DefaultDamagerRepairer(getXMLScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(
                colorManager.getColor(PreferenceConstants.EDITOR_COLOR_XML_COMMENT)));
        reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
        reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

        return reconciler;
    }

    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        // Create content assistant
        ContentAssistant assistant = new ContentAssistant();

        // required to display additional info
        assistant.setInformationControlCreator(new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, true);
            }
        });

        processor.setFile(file);
        // Set this processor for each supported content type
        assistant.setContentAssistProcessor(processor, XMLPartitionScanner.XML_TAG);
        assistant.setContentAssistProcessor(processor, XMLPartitionScanner.XML_DEFAULT);
        assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);

        // Return the content assistant
        return assistant;
    }

    public void setFile(IFile file) {
        if (processor != null) {
            processor.setFile(file);
        }
        this.file = file;
    }
}
