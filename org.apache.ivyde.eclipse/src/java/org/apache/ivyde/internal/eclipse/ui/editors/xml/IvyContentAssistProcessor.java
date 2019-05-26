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

import org.apache.ivyde.common.completion.CodeCompletionProposal;
import org.apache.ivyde.common.completion.IvyCodeCompletionProcessor;
import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;

public abstract class IvyContentAssistProcessor implements IContentAssistProcessor {
    private final IContextInformationValidator fValidator = new ContextInformationValidator(this);

    private String errorMessage = null;

    private IFile file;

    private IvyCodeCompletionProcessor completionProcessor;

    /**
     * Call by viewer to retrieve a list of ICompletionProposal
     *
     * @param viewer ITextViewer
     * @param documentOffset int
     * @return ICompletionProposal[]
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int documentOffset) {
        // Retrieve current document
        IDocument doc = viewer.getDocument();
        // Retrieve current selection range
        Point selectedRange = viewer.getSelectedRange();
        String ivyFileString;
        try {
            ivyFileString = doc.get(0, doc.getLength());
        } catch (BadLocationException e) {
            // Unless there is a bug in JFace, this should never never happen
            IvyPlugin.logError("Getting the content of the document " + doc.toString() + " failed",
                e);
            return null;
        }
        IProject project = getProject();
        IvyFile ivyfile = completionProcessor.getModel().newIvyFile(
            project != null ? project.getName() : "", ivyFileString, documentOffset);
        CodeCompletionProposal[] proposals = completionProcessor.computeCompletionProposals(
            ivyfile, selectedRange.y);

        // convert code completion proposal into eclipse ICompletionProposal
        ICompletionProposal[] ret = new ICompletionProposal[proposals.length];
        for (int i = 0; i < proposals.length; i++) {
            CodeCompletionProposal prop = proposals[i];
            ret[i] = new CompletionProposal(prop.getReplacementString(), prop
                    .getReplacementOffset(), prop.getReplacementLength(), prop.getCursorPosition(),
                    null, prop.getDisplayString(), null, prop.getDoc());
        }

        return ret;
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {'<', '"'};
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public IContextInformationValidator getContextInformationValidator() {
        return fValidator;
    }

    public IJavaProject getJavaProject() {
        IProject p = getProject();
        return JavaCore.create(p);
    }

    public IProject getProject() {
        return file == null ? null : file.getProject();
    }

    public void setFile(IFile file) {
        this.file = file;
        completionProcessor = new IvyCodeCompletionProcessor(newCompletionModel(file));
    }

    protected abstract IvyModel newCompletionModel(IFile file);

}
