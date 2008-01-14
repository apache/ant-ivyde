package org.apache.ivyde.eclipse.ui.editors.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivyde.eclipse.ui.core.model.IvyFile;
import org.apache.ivyde.eclipse.ui.core.model.IvyModel;
import org.apache.ivyde.eclipse.ui.core.model.IvyTag;
import org.apache.ivyde.eclipse.ui.core.model.IvyTagAttribute;
import org.apache.ivyde.eclipse.ui.core.model.Proposal;
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

public class IvyContentAssistProcessor implements IContentAssistProcessor {
    private IContextInformationValidator fValidator = new ContextInformationValidator(this);

    private String errorMessage = null;

    private IFile file;

    private IvyModel _model;

    /**
     * Call by viewer to retreive a list of ICompletionProposal
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
        // Retrieve current document
        IDocument doc = viewer.getDocument();
        // Retrieve current selection range
        Point selectedRange = viewer.getSelectedRange();
        List propList = new ArrayList();
        String ivyFileString;
        try {
            ivyFileString = doc.get(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
        IProject project = getProject();
        IvyFile ivyfile = new IvyFile(project != null ? project.getName() : "", ivyFileString,
                documentOffset);
        if (ivyfile.inTag()) {
            String tagName = ivyfile.getTagName();
            if (ivyfile.readyForValue()) {
                computeValueProposals(tagName, ivyfile, propList, selectedRange);
            } else {
                // found a value to put in tag
                computeTagAttributeProposals(tagName, ivyfile, propList, selectedRange);
            }
        } else { // not in an xml tag
            computeStructureProposals(ivyfile, propList, selectedRange);
        }
        // Create completion proposal array
        ICompletionProposal[] proposals = new ICompletionProposal[propList.size()];

        // and fill with list elements
        propList.toArray(proposals);

        // Return the proposals
        return proposals;
    }

    /**
     * Compute a list of possible attribute for the tag given in arguement.<br/> If attribute are
     * already used in tag they are discard of the list
     * 
     * @param tagName
     * @param doc
     * @param documentOffset
     * @param propList
     * @param selectedRange
     */
    private void computeTagAttributeProposals(String tagName, IvyFile ivyfile, List propList,
            Point selectedRange) {
        String qualifier = ivyfile.getQualifier();
        int qlen = qualifier.length();
        if (qualifier.indexOf('/') > -1) {
            String text = "/>";
            CompletionProposal proposal = new CompletionProposal(text, ivyfile.getOffset() - qlen,
                    qlen + selectedRange.y, text.length());
            propList.add(proposal);
        } else {
            String parent = ivyfile.getParentTagName();
            IvyTag tag = _model.getIvyTag(tagName, parent);
            if (tag == null) {
                errorMessage = "tag :" + tagName + " not found in model:";
                return;
            }
            errorMessage = null;
            List atts = tag.getAttributes();
            Map existingAtts = ivyfile.getAllAttsValues();
            // Loop through all proposals
            for (Iterator iter = atts.iterator(); iter.hasNext();) {
                IvyTagAttribute att = (IvyTagAttribute) iter.next();
                if (att.getName().startsWith(qualifier) && !existingAtts.containsKey(att.getName())) {
                    // Yes -- compute whole proposal text
                    String text = att.getName() + "=\"\"";
                    // Construct proposal
                    CompletionProposal proposal = new CompletionProposal(text, ivyfile.getOffset()
                            - qlen, qlen + selectedRange.y, text.length() - 1, null, att.getName(),
                            null, att.getDoc());
                    // and add to result list
                    propList.add(proposal);
                }
            }
        }
    }

    /**
     * Compute a list of possible values for the current attribute of the given tag.<br>
     * The list is retrieve by calling <code> IvyTag.getPossibleValuesForAttribute</code>
     * 
     * @see IvyTag#getPossibleValuesForAttribute(String, Map, String)
     * @param tagName
     * @param doc
     * @param documentOffset
     * @param propList
     * @param selection
     */
    private void computeValueProposals(String tagName, IvyFile ivyfile, List propList,
            Point selection) {
        String parent = null;
        String tag = ivyfile.getTagName();
        if (tag != null) {
            parent = ivyfile.getParentTagName(ivyfile.getStringIndexBackward("<" + tag));
        }
        IvyTag ivyTag = _model.getIvyTag(tag, parent);
        if (ivyTag != null) {
            String[] values = ivyTag.getPossibleValuesForAttribute(ivyfile.getAttributeName(),
                ivyfile);
            if (values != null) {
                String qualifier = ivyfile.getAttributeValueQualifier();
                int qlen = qualifier == null ? 0 : qualifier.length();
                Arrays.sort(values);
                for (int i = 0; i < values.length; i++) {
                    String val = values[i];
                    CompletionProposal proposal = new CompletionProposal(val, ivyfile.getOffset()
                            - qlen, qlen + selection.y, val.length());
                    propList.add(proposal);
                }
            }
        }
    }

    /**
     * Compute xml structural proposition
     */
    private void computeStructureProposals(IvyFile ivyfile, List propList, Point selectedRange) {
        String parent = ivyfile.getParentTagName();
        String qualifier = ivyfile.getQualifier();
        int qlen = qualifier.length();
        if (parent != null
                && ivyfile.getOffset() >= 2 + qualifier.length()
                && ivyfile.getString(ivyfile.getOffset() - 2 - qualifier.length(),
                    ivyfile.getOffset()).startsWith("</")) {
            // closing tag (already started)
            String text = "</" + parent + ">";
            CompletionProposal proposal = new CompletionProposal(text, ivyfile.getOffset() - qlen
                    - 2, qlen + 2 + selectedRange.y, text.length());
            propList.add(proposal);
        } else {
            if (parent != null && qualifier.length() == 0) {
                String text = "</" + parent + ">";
                int closingIndex = ivyfile.getStringIndexForward(text);
                int openingIndex = ivyfile.getStringIndexForward("<" + parent);
                if (closingIndex == -1 || (openingIndex != -1 && closingIndex > openingIndex)) {
                    // suggest closing tag if tag not yet closed
                    CompletionProposal proposal = new CompletionProposal(text, ivyfile.getOffset(),
                            selectedRange.y, text.length());
                    propList.add(proposal);
                }
            }

            List childs = null;

            if (parent != null) {
                String parentParent = ivyfile.getParentTagName(ivyfile.getStringIndexBackward("<"
                        + parent));
                IvyTag root = _model.getIvyTag(parent, parentParent);
                if (root == null) {
                    errorMessage = "parent tag :" + parent + " not found in model:";
                    return;
                } else {
                    childs = root.getChilds();
                }
            } else {
                childs = Collections.singletonList(_model.getRootIvyTag());
            }
            errorMessage = null;
            for (Iterator iter = childs.iterator(); iter.hasNext();) {
                IvyTag child = (IvyTag) iter.next();

                // Check if proposal matches qualifier
                if (child.getStartTag().startsWith(qualifier)) {
                    Proposal[] props = child.getProposals();
                    for (int i = 0; i < props.length; i++) {
                        // Construct proposal and add to result list
                        propList.add(new CompletionProposal(props[i].getProposal(), ivyfile
                                .getOffset()
                                - qlen, qlen + selectedRange.y, props[i].getCursor(), null, null,
                                null, props[i].getDoc()));
                    }
                }
            }
        }
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
        _model = new IvyModel(getJavaProject());
    }

}
