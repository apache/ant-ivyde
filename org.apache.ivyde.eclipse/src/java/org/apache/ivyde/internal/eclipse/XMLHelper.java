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
package org.apache.ivyde.internal.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Duplicate of {@link org.apache.ivy.util.XMLHelper} put patched with IVYDE-329. Once Ivy 2.4 is
 * the minimum required version of IvyDE, we can drop this class.
 */
public class XMLHelper {

    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    static final String XML_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    static final String XERCES_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static boolean canUseSchemaValidation = true;

    private static Boolean canDisableExternalDtds = null;

    public static void parse(InputSource xmlStream, URL schema, DefaultHandler handler,
            LexicalHandler lHandler, boolean loadExternalDtds) throws SAXException, IOException,
            ParserConfigurationException {
        InputStream schemaStream = null;
        try {
            if (schema != null) {
                schemaStream = URLHandlerRegistry.getDefault().openStream(schema);
            }
            SAXParser parser = newSAXParser(schema, schemaStream, loadExternalDtds);

            if (lHandler != null) {
                try {
                    parser.setProperty("http://xml.org/sax/properties/lexical-handler", lHandler);
                } catch (SAXException ex) {
                    Message.warn("problem while setting the lexical handler property on SAXParser ("
                            + ex.getClass().getName() + ": " + ex.getMessage() + ")");
                    // continue without the lexical handler
                }
            }

            parser.parse(xmlStream, handler);
        } finally {
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (IOException ex) {
                    // ignored
                }
            }
        }
    }

    private static SAXParser newSAXParser(URL schema, InputStream schemaStream,
            boolean loadExternalDtds) throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setValidating(canUseSchemaValidation && (schema != null));
        if (!loadExternalDtds && canDisableExternalDtds(parserFactory)) {
            parserFactory.setFeature(XERCES_LOAD_EXTERNAL_DTD, false);
        }
        SAXParser parser = parserFactory.newSAXParser();

        if (canUseSchemaValidation && (schema != null)) {
            try {
                parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                parser.setProperty(JAXP_SCHEMA_SOURCE, schemaStream);
            } catch (SAXNotRecognizedException ex) {
                Message.warn("problem while setting JAXP validating property on SAXParser... "
                        + "XML validation will not be done (" + ex.getClass().getName() + ": "
                        + ex.getMessage() + ")");
                canUseSchemaValidation = false;
                parserFactory.setValidating(false);
                parser = parserFactory.newSAXParser();
            }
        }

        parser.getXMLReader().setFeature(XML_NAMESPACE_PREFIXES, true);
        return parser;
    }

    private static boolean canDisableExternalDtds(SAXParserFactory parserFactory) {
        if (canDisableExternalDtds == null) {
            try {
                parserFactory.getFeature(XERCES_LOAD_EXTERNAL_DTD);
                canDisableExternalDtds = Boolean.TRUE;
            } catch (Exception ex) {
                canDisableExternalDtds = Boolean.FALSE;
            }
        }
        return canDisableExternalDtds;
    }

}
