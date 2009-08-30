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
package org.apache.ivyde.common.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.common.model.IvyTag;
import org.apache.ivyde.common.model.IvyTagAttribute;
import org.apache.ivyde.common.model.Proposal;

public class IvyCodeCompletionProcessor {
    private String errorMessage = null;
    private final IvyModel model;
    
    public IvyCodeCompletionProcessor(IvyModel model) {
        this.model = model;
    }

    /**
     * Call by viewer to retrieve a list of ICompletionProposal
     */
    public CodeCompletionProposal[] computeCompletionProposals(IvyFile ivyfile, int caretOffset) {
        model.refreshIfNeeded(ivyfile);
        List propList = new ArrayList();
        if (ivyfile.inTag()) {
            String tagName = ivyfile.getTagName();
            if (ivyfile.readyForValue()) {
                computeValueProposals(tagName, ivyfile, propList, caretOffset);
            } else {
                // found a value to put in tag
                computeTagAttributeProposals(tagName, ivyfile, propList, caretOffset);
            }
        } else { // not in an xml tag
            computeStructureProposals(ivyfile, propList, caretOffset);
        }

        return (CodeCompletionProposal[]) propList.toArray(
                            new CodeCompletionProposal[propList.size()]);
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
            int caretOffset) {
        String qualifier = ivyfile.getQualifier();
        int qlen = qualifier.length();
        if (qualifier.indexOf('/') > -1) {
            String text = "/>";
            CodeCompletionProposal proposal = new CodeCompletionProposal(
                text, ivyfile.getOffset() - qlen, qlen + caretOffset, text.length());
            propList.add(proposal);
        } else {
            String parent = ivyfile.getParentTagName();
            IvyTag tag = model.getIvyTag(tagName, parent);
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
                if (att.getName().startsWith(qualifier)
                        && !existingAtts.containsKey(att.getName())) {
                    // Yes -- compute whole proposal text
                    String text = att.getName() + "=\"\"";
                    // Construct proposal
                    CodeCompletionProposal proposal = new CodeCompletionProposal(
                            text, ivyfile.getOffset() - qlen, qlen + caretOffset, 
                            text.length() - 1, att.getName(), att.getDoc());
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
            int caretOffset) {
        String parent = null;
        String tag = ivyfile.getTagName();
        if (tag != null) {
            parent = ivyfile.getParentTagName(ivyfile.getStringIndexBackward("<" + tag));
        }
        IvyTag ivyTag = model.getIvyTag(tag, parent);
        if (ivyTag != null) {
            String[] values = ivyTag.getPossibleValuesForAttribute(ivyfile.getAttributeName(),
                ivyfile);
            if (values != null) {
                String qualifier = ivyfile.getAttributeValueQualifier();
                int qlen = qualifier == null ? 0 : qualifier.length();
                Arrays.sort(values);
                for (int i = 0; i < values.length; i++) {
                    String val = values[i];
                    CodeCompletionProposal proposal = new CodeCompletionProposal(
                        val, ivyfile.getOffset() - qlen, qlen + caretOffset, val.length());
                    propList.add(proposal);
                }
            }
        }
    }

    /**
     * Compute xml structural proposition
     */
    private void computeStructureProposals(IvyFile ivyfile, List propList, int caretOffset) {
        String parent = ivyfile.getParentTagName();
        String qualifier = ivyfile.getQualifier();
        int qlen = qualifier.length();
        if (parent != null
                && ivyfile.getOffset() >= 2 + qualifier.length()
                && ivyfile.getString(ivyfile.getOffset() - 2 - qualifier.length(),
                    ivyfile.getOffset()).startsWith("</")) {
            // closing tag (already started)
            String text = "</" + parent + ">";
            CodeCompletionProposal proposal = new CodeCompletionProposal(
                text, ivyfile.getOffset() - qlen - 2, qlen + 2 + caretOffset, text.length());
            propList.add(proposal);
        } else {
            if (parent != null && qualifier.length() == 0) {
                String text = "</" + parent + ">";
                int closingIndex = ivyfile.getStringIndexForward(text);
                int openingIndex = ivyfile.getStringIndexForward("<" + parent);
                if (closingIndex == -1 || (openingIndex != -1 && closingIndex > openingIndex)) {
                    // suggest closing tag if tag not yet closed
                    CodeCompletionProposal proposal = new CodeCompletionProposal(
                        text, ivyfile.getOffset(), caretOffset, text.length());
                    propList.add(proposal);
                }
            }

            List childs = null;

            if (parent != null) {
                String parentParent = ivyfile.getParentTagName(ivyfile.getStringIndexBackward("<"
                        + parent));
                IvyTag root = model.getIvyTag(parent, parentParent);
                if (root == null) {
                    errorMessage = "parent tag :" + parent + " not found in model:";
                    return;
                } else {
                    childs = root.getChilds();
                }
            } else {
                childs = Collections.singletonList(model.getRootIvyTag());
            }
            errorMessage = null;
            for (Iterator iter = childs.iterator(); iter.hasNext();) {
                IvyTag child = (IvyTag) iter.next();

                // Check if proposal matches qualifier
                if (child.getStartTag().startsWith(qualifier)) {
                    Proposal[] props = child.getProposals();
                    for (int i = 0; i < props.length; i++) {
                        // Construct proposal and add to result list
                        propList.add(new CodeCompletionProposal(
                            props[i].getProposal(), ivyfile.getOffset() - qlen, qlen + caretOffset, 
                            props[i].getCursor(), null, props[i].getDoc()));
                    }
                }
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public IvyModel getModel() {
        return model;
    }
}
