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

package io.openliberty.tools.langserver.lemminx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

public class XmlReader {
    private static final Logger LOGGER = Logger.getLogger(XmlReader.class.getName());

    public static boolean hasServerRoot(String filePath) {
        File file = null;
        
        try {
            file = new File(new URI(filePath).getPath());
            return hasServerRoot(file);
        } catch (URISyntaxException e) {
            LOGGER.severe("Error received converting file path to URI for path " + filePath);
        }
        return false;
    }

    public static boolean hasServerRoot(Path filePath) {
        return hasServerRoot(filePath.toFile());
    }

    public static boolean hasServerRoot(File xmlFile) {
        if (!xmlFile.exists() || xmlFile.length() == 0) {
            return false;
        }
        
        try {
            XMLInputFactory factory = getXmlInputFactory();

            XMLEventReader reader = null;

            try (FileInputStream fis = new FileInputStream(xmlFile)) {
                reader = factory.createXMLEventReader(fis);
                while (reader.hasNext()) {
                    XMLEvent nextEvent = reader.nextEvent();
                    if (nextEvent.isStartElement()) {
                        return isServerElement(nextEvent);
                    }
                }
            } catch (XMLStreamException | FileNotFoundException e) {
                LOGGER.severe("Error received trying to read XML file: " + xmlFile.getAbsolutePath()); 
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {   
                    }
                }
            }            
        } catch (Exception e) {
            LOGGER.severe("Unable to access XML file "+ xmlFile.getAbsolutePath());
        }

        return false;
    }

    private static XMLInputFactory getXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        } catch (Exception e) {
            LOGGER.warning("Could not set properties on XMLInputFactory.");
        }
        return factory;
    }

    public static String getElementValue(Path file, String elementName) {
        Set<String> names = new HashSet<String> ();
        names.add(elementName);
        Map<String, String> values = getElementValues(file, names);
        if (values != null && values.containsKey(elementName)) {
            return values.get(elementName);
        }
        return null;
    }

    public static Map<String, String> getElementValues(Path file, Set<String> elementNames) {
        if (!file.toFile().exists()) {
            return null;
        }
        Map<String, String> returnValues = new HashMap<String, String> ();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        try {
            reader = factory.createXMLEventReader(new FileInputStream(file.toFile()));
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
            LOGGER.severe("Unable to access file "+ file.toFile().getName());
        } catch (XMLStreamException e) {
            LOGGER.severe("Error received trying to read XML file " + file.toFile().getName() + " : "+e.getMessage());
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

    protected static boolean isServerElement(XMLEvent event) {
        return getElementName(event).equals("server");
    }
}
