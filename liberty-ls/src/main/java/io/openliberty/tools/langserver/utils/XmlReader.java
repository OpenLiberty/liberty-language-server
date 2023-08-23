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
import java.nio.file.Path;
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

    /**
     * Reads a liberty-plugin-cfg.xml, and returns found values for:
     * serverEnvFile, bootstrapPropertiesFile
     * @param file
     * @return
     */
    public static Map<String, String> readLibertyPluginCfgXml(String fileUri) {
        File xmlFile = new File(fileUri);

        if (!xmlFile.toPath().endsWith("liberty-plugin-config.xml")) {
            return null;
        }

        Set<String> elementNames = new HashSet<String>();
        elementNames.add("serverEnvFile");
        elementNames.add("bootstrapPropertiesFile");
        // elementNames.add("serverXmlFile"); // not needed
        return getElementValues(xmlFile, elementNames);
    }

    public static Map<String, String> getElementValues(File file, Set<String> elementNames) {
        if (!file.exists() || file.length() == 0) {
            return null;
        }

        Map<String, String> returnValues = new HashMap<String, String> ();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        try {
            reader = factory.createXMLEventReader(new FileInputStream(file));
            while (reader.hasNext()) {
                XMLEvent event = reader.nextTag();
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
            LOGGER.severe("Error received trying to read XML file: " + file.getName());
            e.printStackTrace();
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
