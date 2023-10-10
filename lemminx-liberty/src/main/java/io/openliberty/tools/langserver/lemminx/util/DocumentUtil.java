/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package io.openliberty.tools.langserver.lemminx.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class DocumentUtil {
    private static final Logger LOGGER = Logger.getLogger(DocumentUtil.class.getName());

    public static Document getDocument(File inputFile) throws Exception {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.parse(inputFile);
        } catch (Exception e) {
            LOGGER.warning("Error creating document from xml file: " + inputFile.getAbsolutePath() +" exception: "+e.getMessage());
            throw e;
        }
    }

    public static void writeDocToXmlFile(Document doc, File inputFile) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // Need to use this xsl to prevent extra lines in the updated xsd file. It is a known issue in Java 9 and up that is not going to be fixed.
        // It was a design decision. Ref link: https://bugs.openjdk.org/browse/JDK-8262285?attachmentViewMode=list
        InputStream is = DocumentUtil.class.getClassLoader().getResourceAsStream("formatxsd.xsl"); 
        Transformer transformer = transformerFactory.newTransformer(new StreamSource(is));
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");

        doc.setXmlStandalone(true);
        DOMSource source = new DOMSource(doc);
        StreamResult file = new StreamResult(new OutputStreamWriter(new FileOutputStream(inputFile), "UTF-8"));
        transformer.transform(source, file);
    }

    public static void removeExtraneousAnyAttributeElements(File schemaFile) {
        try {
            Document doc = getDocument(schemaFile);
            boolean updated = false;

            List<Element> anyAttrElements = getElementsByName(doc,"anyAttribute");
            for (Element anyAttr : anyAttrElements) {
                Node anyAttrParent = anyAttr.getParentNode(); 
                // if the parent node does not contain a <annotation><appinfo><extraProperties> element, remove the anyAttribute element
                if (anyAttrParent != null && anyAttrParent.getNodeType() == Node.ELEMENT_NODE) {
                    Element anyAttrParentElement = (Element) anyAttrParent;
                    NodeList extraPropertiesList = anyAttrParentElement.getElementsByTagNameNS("*", "extraProperties");
                    if (extraPropertiesList == null || extraPropertiesList.getLength() == 0) {
                        if (anyAttrParent.removeChild(anyAttr) != null) {
                            updated = true;
                        }
                    }
                }
            }   
            
            if (updated) {
                writeDocToXmlFile(doc, schemaFile);
                LOGGER.info("Finished post processing of schema file: "+schemaFile.getCanonicalPath());
            }
        } catch (Exception e) {
            LOGGER.warning("Received exception during post processing of schema file "+schemaFile.getAbsolutePath()+" : "+e.getMessage());
        }
    }

    public static List<Element> getElementsByName(Document doc, String name) {
        List<Element> elements = new ArrayList<Element>();
        try {
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagNameNS("*", name);
            if (nList != null) {
                for (int temp = 0; temp < nList.getLength(); temp++) {

                    Node nNode = nList.item(temp);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                        Element elem = (Element) nNode;
                        elements.add(elem);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Exception received while parsing "+ doc.getDocumentURI() +": "+e.getMessage());
        }
        return elements;
    }
}
