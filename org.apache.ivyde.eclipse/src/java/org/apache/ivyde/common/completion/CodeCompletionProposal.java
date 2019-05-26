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
package org.apache.ivyde.common.completion;

public class CodeCompletionProposal {

    /** The string to be displayed in the completion proposal popup. */
    private final String displayString;

    /** The replacement string. */
    private final String replacementString;

    /** The replacement offset. */
    private final int replacementOffset;

    /** The replacement length. */
    private final int replacementLength;

    /** The cursor position after this proposal has been applied. */
    private final int cursorPosition;

    /** The documentation of this proposal. */
    private final String doc;

    public CodeCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null);
    }

    public CodeCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, String displayString, String doc) {
        this.replacementString = replacementString;
        this.replacementOffset = replacementOffset;
        this.replacementLength = replacementLength;
        this.cursorPosition = cursorPosition;
        this.displayString = displayString;
        this.doc = doc;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getReplacementString() {
        return replacementString;
    }

    public int getReplacementOffset() {
        return replacementOffset;
    }

    public int getReplacementLength() {
        return replacementLength;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public String getDoc() {
        return doc;
    }
}
