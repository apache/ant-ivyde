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
package org.apache.ivyde.eclipse.ui.editors;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivyde.eclipse.XMLHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IvySettingsContentDescriber extends XMLContentDescriber implements
        IExecutableExtension {

    public void
            setInitializationData(IConfigurationElement config, String propertyName, Object data)
                    throws CoreException {
        // nothing to do
    }

    public int describe(InputStream input, IContentDescription description) throws IOException {
        // call the basic XML describer to do basic recognition
        if (super.describe(input, description) == INVALID) {
            return INVALID;
        }
        // super.describe will have consumed some chars, need to rewind
        input.reset();
        // Check to see if we matched our criteria.
        return checkCriteria(new InputSource(input));
    }

    public int describe(Reader input, IContentDescription description) throws IOException {
        // call the basic XML describer to do basic recognition
        if (super.describe(input, description) == INVALID) {
            return INVALID;
        }
        // super.describe will have consumed some chars, need to rewind
        input.reset();
        // Check to see if we matched our criteria.
        return checkCriteria(new InputSource(input));
    }

    private int checkCriteria(InputSource contents) throws IOException {
        IvySettingsHandler ivySettingsHandler = new IvySettingsHandler();
        try {
            XMLHelper.parse(contents, null, ivySettingsHandler, null, false);
        } catch (SAXException e) {
            // we may be handed any kind of contents... it is normal we fail to parse
            return INDETERMINATE;
        } catch (ParserConfigurationException e) {
            // some bad thing happened - force this describer to be disabled
            String message = "Internal Error: XML parser configuration error during content description for Ivy files";
            throw new RuntimeException(message);
        }
        if (ivySettingsHandler.isIvySettings) {
            return VALID;
        }
        return INDETERMINATE;
    }

    /**
     * Stupid handler to check that the file starts with 'ivysettings'
     */
    private static final class IvySettingsHandler extends DefaultHandler {
        boolean root = true;

        boolean isIvySettings;

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (!root) {
                return;
            }

            if ("ivysettings".equals(localName)) {
                isIvySettings = true;
            }

            root = false;
        }
    }
}
