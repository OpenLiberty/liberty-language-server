/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;

import io.openliberty.tools.langserver.lemminx.util.XmlReader.RootElement;

/**
 * Finds the bundled schema for IBM deployment descriptors such as ibm-web-bnd.xml.
 * The schemas are the ones shipped in a Liberty install under dev/api/ibm/schema.
 *
 * A file is a deployment descriptor when its root element is one of the known descriptor roots in the IBM
 * namespace. It can be in any folder. If the root element cannot be read (new or empty file), the
 * file name is used instead.
 */
public final class DeploymentDescriptorSchemaUtil {

    private static final Logger LOGGER = Logger.getLogger(DeploymentDescriptorSchemaUtil.class.getName());

    private static final String IBM_NAMESPACE = "http://websphere.ibm.com/xml/ns/javaee";

    private static final String CLASSPATH_DIR = "/schema/xsd/ibm/";
    // Bump the version segment when the bundled schemas change, so the .lemminx cache gets refreshed.
    private static final String CACHE_URL_DIR = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/ibm/v1/";

    // Application client descriptors (ibm-application-client-bnd/ext) are not included: 
    // Liberty does not ship their schemas.

    // root element -> latest schema
    private static final Map<String, String> ROOT_TO_LATEST_SCHEMA = Map.of(
            "web-ext", "ibm-web-ext_1_1.xsd",
            "web-bnd", "ibm-web-bnd_1_2.xsd",
            "application-ext", "ibm-application-ext_1_1.xsd",
            "application-bnd", "ibm-application-bnd_1_2.xsd",
            "ejb-jar-ext", "ibm-ejb-jar-ext_1_1.xsd",
            "ejb-jar-bnd", "ibm-ejb-jar-bnd_1_2.xsd",
            "managed-bean-bnd", "ibm-managed-bean-bnd_1_1.xsd",
            "webservices-bnd", "ibm-ws-bnd_1_0.xsd",
            "cache", "cachespec.xsd");

    // file name -> latest schema, only used when the root element cannot be read
    private static final Map<String, String> FILE_NAME_TO_LATEST_SCHEMA = Map.of(
            "ibm-web-ext.xml", "ibm-web-ext_1_1.xsd",
            "ibm-web-bnd.xml", "ibm-web-bnd_1_2.xsd",
            "ibm-application-ext.xml", "ibm-application-ext_1_1.xsd",
            "ibm-application-bnd.xml", "ibm-application-bnd_1_2.xsd",
            "ibm-ejb-jar-ext.xml", "ibm-ejb-jar-ext_1_1.xsd",
            "ibm-ejb-jar-bnd.xml", "ibm-ejb-jar-bnd_1_2.xsd",
            "ibm-managed-bean-bnd.xml", "ibm-managed-bean-bnd_1_1.xsd",
            "ibm-ws-bnd.xml", "ibm-ws-bnd_1_0.xsd",
            "cachespec.xml", "cachespec.xsd");

    // Every bundled file. The ibm-common-* files are included by the others and must sit next to them.
    private static final List<String> BUNDLED_FILES = List.of(
            "cachespec.xsd",
            "ibm-application-bnd_1_0.xsd", "ibm-application-bnd_1_1.xsd", "ibm-application-bnd_1_2.xsd",
            "ibm-application-ext_1_0.xsd", "ibm-application-ext_1_1.xsd",
            "ibm-common-bnd_1_0.xsd", "ibm-common-bnd_1_1.xsd", "ibm-common-bnd_1_2.xsd",
            "ibm-common-ext_1_0.xsd", "ibm-common-ext_1_1.xsd",
            "ibm-ejb-jar-bnd_1_0.xsd", "ibm-ejb-jar-bnd_1_1.xsd", "ibm-ejb-jar-bnd_1_2.xsd",
            "ibm-ejb-jar-ext_1_0.xsd", "ibm-ejb-jar-ext_1_1.xsd",
            "ibm-managed-bean-bnd_1_0.xsd", "ibm-managed-bean-bnd_1_1.xsd",
            "ibm-web-bnd_1_0.xsd", "ibm-web-bnd_1_1.xsd", "ibm-web-bnd_1_2.xsd",
            "ibm-web-ext_1_0.xsd", "ibm-web-ext_1_1.xsd",
            "ibm-ws-bnd_1_0.xsd");

    private DeploymentDescriptorSchemaUtil() {
    }

    /**
     * @return the file name of the latest schema for this descriptor, or null if the file is not a deployment descriptor
     */
    private static String getLatestSchemaName(String fileUri) {
        if (fileUri == null || !fileUri.endsWith(".xml")) {
            return null;
        }
        RootElement root = XmlReader.getRootElement(fileUri);
        if (root != null) {
            return IBM_NAMESPACE.equals(root.namespace()) ? ROOT_TO_LATEST_SCHEMA.get(root.localName()) : null;
        }
        // Root element not readable: the file may be new, empty, or not on disk. Use the file name.
        String fileName = fileUri.substring(fileUri.lastIndexOf('/') + 1);
        return FILE_NAME_TO_LATEST_SCHEMA.get(fileName);
    }

    /**
     * @param fileUri URI of the file, may be null
     * @return true if the file is an IBM deployment descriptor such as ibm-web-bnd.xml
     */
    public static boolean isDeploymentDescriptor(String fileUri) {
        return getLatestSchemaName(fileUri) != null;
    }

    /**
     * Get the file name of the schema to use for a deployment descriptor, for example ibm-web-bnd_1_0.xsd.
     *
     * @param fileUri  URI of the descriptor file
     * @param systemId schema location from xsi:schemaLocation, may be null. If it names a bundled version
     *                 of the schema of this descriptor, that version is used. Otherwise the latest version is used.
     * @return schema file name, or null if the file is not a deployment descriptor
     */
    private static String getSchemaName(String fileUri, String systemId) {
        String schemaName = getLatestSchemaName(fileUri);
        if (schemaName == null) {
            return null;
        }
        if (systemId != null) {
            String hinted = systemId.substring(systemId.lastIndexOf('/') + 1);
            // only accept another version of the same schema
            if (BUNDLED_FILES.contains(hinted) && hinted.startsWith(schemaBaseName(schemaName))) {
                return hinted;
            }
        }
        return schemaName;
    }

    /**
     * Get the schema URL from the xsi:schemaLocation of a deployment descriptor (the last value of the
     * "namespace url" pair), so that the schema version named in the file can be used.
     *
     * @param fileUri URI of the descriptor file
     * @return the schema URL, or null if the file is not a deployment descriptor or has no xsi:schemaLocation
     */
    public static String getSchemaLocationHint(String fileUri) {
        if (!isDeploymentDescriptor(fileUri)) {
            return null;
        }
        RootElement root = XmlReader.getRootElement(fileUri);
        if (root == null || root.schemaLocation() == null || root.schemaLocation().isBlank()) {
            return null;
        }
        String[] tokens = root.schemaLocation().trim().split("\\s+");
        return tokens[tokens.length - 1];
    }

    /**
     * Get the URI of the local schema for a deployment descriptor.
     *
     * @param fileUri  URI of the descriptor file
     * @param systemId schema location from xsi:schemaLocation, may be null. If it names a bundled version
     *                 of the schema of this descriptor, that version is used. Otherwise the latest version is used.
     * @return schema URI as a String, or null if the file is not a deployment descriptor
     */
    public static String getSchemaUri(String fileUri, String systemId) {
        String schemaName = getSchemaName(fileUri, systemId);
        if (schemaName == null) {
            return null;
        }
        try {
            Path schemaPath = deploy(schemaName);
            if (schemaPath == null) {
                LOGGER.warning("The schema " + schemaName + " for " + fileUri + " is not one of the bundled schemas");
                return null;
            }
            return schemaPath.toUri().toString();
        } catch (IOException e) {
            LOGGER.warning("Unable to deploy the schema " + schemaName + " for " + fileUri + ": " + e.getMessage());
            return null;
        }
    }

    // ibm-web-bnd_1_2.xsd -> ibm-web-bnd_ ; cachespec.xsd -> cachespec
    private static String schemaBaseName(String schemaName) {
        int underscore = schemaName.lastIndexOf('_');
        return underscore >= 0 ? schemaName.substring(0, underscore + 1) : schemaName;
    }

    /**
     * Copies all bundled schemas to the .lemminx cache and returns the path of the requested one.
     * The files are deployed together because the schemas include each other by relative path.
     */
    private static Path deploy(String schemaName) throws IOException {
        Path requested = null;
        for (String file : BUNDLED_FILES) {
            Path deployed = CacheResourcesManager
                    .getResourceCachePath(new ResourceToDeploy(CACHE_URL_DIR + file, CLASSPATH_DIR + file));
            if (file.equals(schemaName)) {
                requested = deployed;
            }
        }
        return requested;
    }
}
