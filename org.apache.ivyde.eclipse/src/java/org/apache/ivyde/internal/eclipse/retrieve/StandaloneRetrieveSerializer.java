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
package org.apache.ivyde.internal.eclipse.retrieve;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class StandaloneRetrieveSerializer {

    private static final String ROOT = "setuplist";

    private static final String SETUP = "setup";

    private static final String SETUP_NAME = "name";

    private static final String IVYSETTINGS = "ivysettings";

    private static final String IVYSETTING_PATH = "path";

    private static final String IVYSETTING_LOADONDEMAND = "loadondemand";

    private static final String IVY_USER_DIR = "ivyUserDir";

    private static final String PROPERTYFILE = "propertyfile";

    private static final String PROPERTYFILE_PATH = "path";

    private static final String IVYXML = "ivyxml";

    private static final String IVYXML_PATH = "path";

    private static final String RETRIEVE = "retrieve";

    private static final String RETRIEVE_SYNC = "sync";

    private static final String RETRIEVE_TYPES = "types";

    private static final String RETRIEVE_CONFS = "confs";

    private static final String RETRIEVE_PATTERN = "pattern";

    private static final String RESOLVE_IN_WORKSPACE = "resolveInWorkspace";

    public void write(OutputStream out, List<StandaloneRetrieveSetup> setuplist)
            throws IOException {
        try {
            StreamResult result = new StreamResult(out);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Node root = document.createElement(ROOT);
            document.appendChild(root);

            for (StandaloneRetrieveSetup setup : setuplist) {

                Node node = document.createElement(SETUP);
                root.appendChild(node);
                NamedNodeMap attributes = node.getAttributes();
                Attr attr = document.createAttribute(SETUP_NAME);
                attr.setValue(setup.getName());
                attributes.setNamedItem(attr);

                attr = document.createAttribute(RESOLVE_IN_WORKSPACE);
                attr.setValue(Boolean.toString(setup.isResolveInWorkspace()));
                attributes.setNamedItem(attr);

                if (setup.isSettingProjectSpecific()) {
                    Node settingsNode = document.createElement(IVYSETTINGS);
                    node.appendChild(settingsNode);
                    writeSettingsSetup(document, settingsNode, setup.getSettingsSetup());
                }

                Node ivyxmlNode = document.createElement(IVYXML);
                node.appendChild(ivyxmlNode);
                writeIvyXmlPath(document, ivyxmlNode, setup.getIvyXmlPath());

                Node retrieveNode = document.createElement(RETRIEVE);
                node.appendChild(retrieveNode);
                writeRetrieveSetup(document, retrieveNode, setup.getRetrieveSetup());
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource source = new DOMSource(document);

            transformer.transform(source, result);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (TransformerException e) {
            if (e.getException() instanceof IOException) {
                throw (IOException) e.getException();
            }
            throw new IOException(e.getMessage(), e);
        }
    }

    private void writeSettingsSetup(Document document, Node settingsNode,
            SettingsSetup settingsSetup) {
        NamedNodeMap attributes = settingsNode.getAttributes();

        Attr attr = document.createAttribute(IVYSETTING_PATH);
        attr.setValue(settingsSetup.getRawIvySettingsPath());
        attributes.setNamedItem(attr);

        attr = document.createAttribute(IVYSETTING_LOADONDEMAND);
        attr.setValue(Boolean.toString(settingsSetup.isLoadSettingsOnDemand()));
        attributes.setNamedItem(attr);

        attr = document.createAttribute(IVY_USER_DIR);
        attr.setValue(settingsSetup.getRawIvyUserDir());
        attributes.setNamedItem(attr);

        for (String file : settingsSetup.getRawPropertyFiles()) {
            Node pathNode = document.createElement(PROPERTYFILE);
            settingsNode.appendChild(pathNode);
            attributes = pathNode.getAttributes();
            attr = document.createAttribute(PROPERTYFILE_PATH);
            attr.setValue(file);
            attributes.setNamedItem(attr);
        }
    }

    private void writeIvyXmlPath(Document document, Node ivyxmlNode, String ivyXmlPath) {
        NamedNodeMap attributes = ivyxmlNode.getAttributes();
        Attr attr = document.createAttribute(IVYXML_PATH);
        attr.setValue(ivyXmlPath);
        attributes.setNamedItem(attr);
    }

    private void writeRetrieveSetup(Document document, Node retrieveNode,
            RetrieveSetup retrieveSetup) {
        NamedNodeMap attributes = retrieveNode.getAttributes();

        Attr attr = document.createAttribute(RETRIEVE_PATTERN);
        attr.setValue(retrieveSetup.getRetrievePattern());
        attributes.setNamedItem(attr);

        attr = document.createAttribute(RETRIEVE_CONFS);
        attr.setValue(retrieveSetup.getRetrieveConfs());
        attributes.setNamedItem(attr);

        attr = document.createAttribute(RETRIEVE_TYPES);
        attr.setValue(retrieveSetup.getRetrieveTypes());
        attributes.setNamedItem(attr);

        attr = document.createAttribute(RETRIEVE_SYNC);
        attr.setValue(Boolean.toString(retrieveSetup.isRetrieveSync()));
        attributes.setNamedItem(attr);
    }

    public List<StandaloneRetrieveSetup> read(InputStream in, IProject project)
            throws IOException {
        try {
            InputSource source = new InputSource(in);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(source);

            NodeList elements = document.getElementsByTagName(SETUP);

            List<StandaloneRetrieveSetup> setupList = new ArrayList<>();

            int count = elements.getLength();
            for (int i = 0; i != count; i++) {
                Node node = elements.item(i);

                StandaloneRetrieveSetup setup = new StandaloneRetrieveSetup();
                setup.setProject(project);

                NamedNodeMap attributes = node.getAttributes();
                setup.setName(getAttribute(attributes, SETUP_NAME));

                Node attr = attributes.getNamedItem(RESOLVE_IN_WORKSPACE);
                if (attr != null) {
                    setup.setResolveInWorkspace(Boolean.valueOf(attr.getNodeValue()));
                }

                NodeList children = node.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node item = children.item(j);
                    switch (item.getNodeName()) {
                        case IVYSETTINGS:
                            SettingsSetup settingsSetup = readSettingsSetup(item);
                            setup.setSettingsSetup(settingsSetup);
                            setup.setSettingsProjectSpecific(true);
                            break;
                        case IVYXML:
                            String ivyXmlPath = readIvyXmlPath(item);
                            setup.setIvyXmlPath(ivyXmlPath);
                            break;
                        case RETRIEVE:
                            RetrieveSetup retrieveSetup = readRetrieveSetup(item);
                            setup.setRetrieveSetup(retrieveSetup);
                            break;
                    }
                }

                setupList.add(setup);
            }
            return setupList;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (SAXException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t == null) {
                t = e;
            }
            throw new IOException(t.getMessage(), t);
        }

    }

    private String getAttribute(NamedNodeMap attributes, String name) throws SAXException {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            throw new SAXException("Attribute '" + name + "' not found");
        }
        return node.getNodeValue();
    }

    private SettingsSetup readSettingsSetup(Node node) throws SAXException {
        NamedNodeMap attributes = node.getAttributes();

        SettingsSetup settingsSetup = new SettingsSetup();

        String path = getAttribute(attributes, IVYSETTING_PATH);
        settingsSetup.setIvySettingsPath(path);

        String loadOnDemand = getAttribute(attributes, IVYSETTING_LOADONDEMAND);
        settingsSetup.setLoadSettingsOnDemand(Boolean.valueOf(loadOnDemand));

        String ivyUserDir = getAttribute(attributes, IVY_USER_DIR);
        settingsSetup.setIvyUserDir(ivyUserDir);

        List<String> propertyFiles = new ArrayList<>();

        NodeList children = node.getChildNodes();
        for (int j = 0; j != children.getLength(); j++) {
            Node item = children.item(j);
            if (item.getNodeName().equals(PROPERTYFILE)) {
                attributes = item.getAttributes();

                path = getAttribute(attributes, PROPERTYFILE_PATH);
                propertyFiles.add(path);
            }
        }

        settingsSetup.setPropertyFiles(propertyFiles);

        return settingsSetup;
    }

    private String readIvyXmlPath(Node node) throws SAXException {
        NamedNodeMap attributes = node.getAttributes();
        return getAttribute(attributes, IVYXML_PATH);
    }

    private RetrieveSetup readRetrieveSetup(Node node) throws SAXException {
        NamedNodeMap attributes = node.getAttributes();

        RetrieveSetup retrieveSetup = new RetrieveSetup();

        String pattern = getAttribute(attributes, RETRIEVE_PATTERN);
        retrieveSetup.setRetrievePattern(pattern);

        String confs = getAttribute(attributes, RETRIEVE_CONFS);
        retrieveSetup.setRetrieveConfs(confs);

        String types = getAttribute(attributes, RETRIEVE_TYPES);
        retrieveSetup.setRetrieveTypes(types);

        String sync = getAttribute(attributes, RETRIEVE_SYNC);
        retrieveSetup.setRetrieveSync(Boolean.valueOf(sync));

        return retrieveSetup;
    }

}
