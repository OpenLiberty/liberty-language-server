package io.openliberty.tools.langserver.lemminx.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class XmlReader {
    private static final Logger LOGGER = Logger.getLogger(XmlReader.class.getName());
    private static XmlReader instance;

    public static XmlReader getInstance() {
        if (instance == null) {
            instance = new XmlReader();
        }
        return instance;
    }

    public boolean hasServerRoot(File file) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(file));
            if (reader.hasNext()) {
                XMLEvent firstTag = reader.nextTag(); // first start/end element
                reader.close();
                return isServerElement(firstTag);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            LOGGER.severe("Failed to grab the first starting element for file: " + file);
            e.printStackTrace();
        }

        return false;
    }

    public void readLibertPluginConfigXml(File file) {
        if (!file.exists() || file.length() == 0) {
            return;
        }

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            LOGGER.severe("Encountered an error reading XML file " + file);
            e.printStackTrace();
        }
    }

    protected String getElementName(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

    protected boolean isServerElement(XMLEvent event) {
        return getElementName(event).equals("server");
    }

}
