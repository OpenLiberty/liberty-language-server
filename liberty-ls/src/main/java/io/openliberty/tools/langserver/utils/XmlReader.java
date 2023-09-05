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

package io.openliberty.tools.langserver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        try {
            reader = factory.createXMLEventReader(new FileInputStream(file));
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (!event.isStartElement()) {
                    continue;
                }
                String elementName = getElementName(event);
                if (elementNames.contains(elementName) && reader.hasNext()) {
                    XMLEvent elementContent = reader.nextEvent();
                    if (elementContent.isCharacters()) {
                        Characters value = elementContent.asCharacters();
                        returnValues.put(elementName, value.getData());
                    }
                }
            } 
        } catch (FileNotFoundException e) {
            LOGGER.severe("Unable to access file "+ file.getName());
        } catch (XMLStreamException e) {
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

        return returnValues;
    }

    protected static String getElementName(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

}
