/*******************************************************************************
* Copyright (c) 2023, 2024 IBM Corporation and others.
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

package io.openliberty.tools.langserver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import io.openliberty.tools.langserver.LibertyConfigFileManager;

public class XmlReader {
    private static final Logger LOGGER = Logger.getLogger(XmlReader.class.getName());

    /**
     * Returns the element values from the provided tag names with a given xml URI
     * @param fileUri - URI-formatted String
     * @param tagNames 
     * @return A map of found values to tag names
     */
    public static Map<String, String> readTagsFromXml(String fileUri, String... tagNames) {
        File xmlFile = null;
        fileUri = LibertyConfigFileManager.normalizeFilePath(fileUri);
        xmlFile = new File(fileUri);
        Set<String> tagSet = Set.of(tagNames);
        return getElementValues(xmlFile, tagSet);
    }

    public static Map<String, String> getElementValues(File file, Set<String> elementNames) {
        Map<String, String> returnValues = new HashMap<String, String> ();
        if (!file.exists() || file.length() == 0) {
            LOGGER.info("File could not be found or is empty: " + file.toString());
            return returnValues;
        }

        readElementValues(file, elementNames, returnValues);

        return returnValues;
    }

    private static void readElementValues(File file, Set<String> elementNames, Map<String, String> returnValues) {
        XMLInputFactory factory = getXmlInputFactory();
        XMLEventReader reader = null;
        try {
            readElementValues(file, elementNames, returnValues, reader, factory);
        } catch (Exception e) {
            LOGGER.severe("Unable to access XML file "+ file.getAbsolutePath());
        }
    }

    private static void readElementValues(File file, Set<String> elementNames, Map<String, String> returnValues, XMLEventReader reader, XMLInputFactory factory) {
        try {
            FileInputStream fis = new FileInputStream(file);
            reader = factory.createXMLEventReader(fis);
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (!nextEvent.isStartElement()) {
                    continue;
                }
                String elementName = getElementName(nextEvent);
                if (elementNames.contains(elementName) && reader.hasNext()) {
                    XMLEvent elementContent = reader.nextEvent();
                    if (elementContent.isCharacters()) {
                        Characters value = elementContent.asCharacters();
                        returnValues.put(elementName, value.getData());
                    }
                }
            }
        } catch (XMLStreamException | FileNotFoundException e) {
            LOGGER.severe("Error received trying to read XML file: " + file.getName() +
                      "\n\tError" + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static XMLInputFactory getXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception e) {
            LOGGER.warning("Could not set properties on XMLInputFactory.");
        }
        return factory;
    }

    protected static String getElementName(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

}
