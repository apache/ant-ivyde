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
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

public class XMLScanner extends RuleBasedScanner {

    public XMLScanner(ColorManager manager) {
        IToken procInstr = new Token(new TextAttribute(manager
                .getColor(PreferenceConstants.EDITOR_COLOR_PROC_INSTR)));

        IRule[] rules = new IRule[2];
        // Add rule for processing instructions
        rules[0] = new SingleLineRule("<?", "?>", procInstr);
        // Add generic whitespace rule.
        rules[1] = new WhitespaceRule(new XMLWhitespaceDetector());

        setRules(rules);
    }
}
