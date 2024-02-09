/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
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
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import io.openliberty.tools.common.plugins.config.ServerConfigDocument;
import io.openliberty.tools.langserver.CommonLogger;

//TODO: rename class to relate to ci.common
public class ServerConfigUtil {
    private final static Logger LOGGER = Logger.getLogger(ServerConfigUtil.class.getName());
    private static CommonLogger log = CommonLogger.getInstance(LOGGER);

    public static ServerConfigDocument parseXmlRequest(String uri) {
        // TODO: configDocument as an enhancement will need to be workspace aware
        ServerConfigDocument configDocument = new ServerConfigDocument(log);
        Document doc;
        try {
            doc = configDocument.parseDocument(new File(URI.create(uri)));
            configDocument.parseVariablesForBothValues(doc);
            return configDocument;
        } catch (XPathExpressionException | IOException e) {
            LOGGER.warning("Error trying to parse document: " + uri);
            e.printStackTrace();
        }
        return null;
    }

    public static Properties parseDocumentAndGetProperties(String uri) {
        ServerConfigDocument configDocument = parseXmlRequest(uri);
        if (configDocument == null) {
            return new Properties();
        }
        return configDocument.getProperties();
    }
}
