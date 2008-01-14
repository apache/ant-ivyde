/*
 * This file is given under the licence found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.core.model;

public class Proposal {
    private int _cursor;

    private String _proposal;

    private String _doc;

    public Proposal(String proposal, int cursor, String doc) {
        _cursor = cursor;
        _proposal = proposal;
        _doc = doc;
    }

    public int getCursor() {
        return _cursor;
    }

    public String getProposal() {
        return _proposal;
    }

    public String getDoc() {
        return _doc;
    }

}
