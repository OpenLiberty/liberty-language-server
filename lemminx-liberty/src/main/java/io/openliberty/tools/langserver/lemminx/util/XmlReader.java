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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class XmlReader {
    private static final Logger LOGGER = Logger.getLogger(XmlReader.class.getName());

    public static boolean hasServerRoot(File file) throws XMLStreamException, IOException {
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(file));
        if (reader.hasNext()) {
            XMLEvent firstTag = reader.nextTag(); // first start/end element
            reader.close();
            return isServerElement(firstTag);
        }
        reader.close();
        return false;
    }

    public static void readLibertPluginConfigXml(File file) throws XMLStreamException, IOException {
        if (!file.exists() || file.length() == 0) {
            return;
        }
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(file));
        while (reader.hasNext()) {
            XMLEvent event = reader.nextTag();
            if (!event.isStartElement()) {
                continue;
            }
            switch (getElementName(event)) {
                case "serverEnvFile":
                    // store
                    break;
                case "bootstrapPropertiesFile":
                    // store
                    break;
                default:
                    break;
            }
        } 
        reader.close();
    }

    protected static String getElementName(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

    protected static boolean isServerElement(XMLEvent event) {
        return getElementName(event).equals("server");
    }

    /* ===== Same methods written with try/catch blocks ===== */

    // public static boolean hasServerRoot(File file) {
    //     if (!file.exists() || file.length() == 0) {
    //         return false;
    //     }

    //     XMLInputFactory factory = XMLInputFactory.newInstance();
    //     try {
    //         XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(file));
    //         try  {
    //             if (reader.hasNext()) {
    //                 XMLEvent firstTag = reader.nextTag(); // first start/end element
    //                 reader.close();
    //                 return isServerElement(firstTag);
    //             }
    //         } catch (XMLStreamException e) {
    //             LOGGER.severe("XmlReader failed to grab the first starting element for file: " + file);
    //             e.printStackTrace();
    //         } finally {
    //             try {
    //                 reader.close();
    //             } catch (Exception ignore) {
    //             }
    //         }
    //     } catch (Exception e) {
    //         LOGGER.severe("Unable to create an XMLEventReader for file: " + file);
    //     }

    //     return false;
    // }

    // public static void readLibertPluginConfigXml(File file) {
    //     if (!file.exists() || file.length() == 0) {
    //         return;
    //     }

    //     XMLInputFactory factory = XMLInputFactory.newInstance();
    //     try {
    //         XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(file));
    //         try {
    //             while (reader.hasNext()) {
    //                 XMLEvent event = reader.nextTag();
    //                 if (!event.isStartElement()) {
    //                     continue;
    //                 }
    //                 switch (getElementName(event)) {
    //                     case "serverEnvFile":
    //                         // store
    //                         break;
    //                     case "bootstrapPropertiesFile":
    //                         // store
    //                         break;
    //                     default:
    //                         break;
    //                 }
    //             } 
    //         } catch (XMLStreamException e) {
    //             LOGGER.severe("XmlReader encountered an error trying to read XML file: " + file);
    //             e.printStackTrace();
    //         } finally {
    //             try {
    //                 reader.close();
    //             } catch (Exception ignore) {  
    //             }
    //         }
    //     } catch (Exception e) {
    //         LOGGER.severe("Unable to create an XMLEventReader for file: " + file);
    //     } 
    // }
}
