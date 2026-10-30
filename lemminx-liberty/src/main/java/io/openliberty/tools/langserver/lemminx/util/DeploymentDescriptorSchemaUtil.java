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
import java.util.HashMap;
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
    // Only used by LemMinX to pick the folder in the .lemminx cache. The files are copied from the classpath, not downloaded.
    private static final String CACHE_URL_DIR = "https://github.com/OpenLiberty/liberty-language-server/blob/main/lemminx-liberty/src/main/resources/schema/xsd/ibm/";

    // Application client descriptors (ibm-application-client-bnd/ext) are not included:
    // Liberty does not ship their schemas.

    // root element -> schema versions of that descriptor, oldest first. The last one is the latest.
    private static final Map<String, List<String>> ROOT_TO_SCHEMAS = Map.of(
            "web-ext", List.of("ibm-web-ext_1_0.xsd", "ibm-web-ext_1_1.xsd"),
            "web-bnd", List.of("ibm-web-bnd_1_0.xsd", "ibm-web-bnd_1_1.xsd", "ibm-web-bnd_1_2.xsd"),
            "application-ext", List.of("ibm-application-ext_1_0.xsd", "ibm-application-ext_1_1.xsd"),
            "application-bnd", List.of("ibm-application-bnd_1_0.xsd", "ibm-application-bnd_1_1.xsd", "ibm-application-bnd_1_2.xsd"),
            "ejb-jar-ext", List.of("ibm-ejb-jar-ext_1_0.xsd", "ibm-ejb-jar-ext_1_1.xsd"),
            "ejb-jar-bnd", List.of("ibm-ejb-jar-bnd_1_0.xsd", "ibm-ejb-jar-bnd_1_1.xsd", "ibm-ejb-jar-bnd_1_2.xsd"),
            "managed-bean-bnd", List.of("ibm-managed-bean-bnd_1_0.xsd", "ibm-managed-bean-bnd_1_1.xsd"),
            "webservices-bnd", List.of("ibm-ws-bnd_1_0.xsd"),
            "cache", List.of("cachespec.xsd"));

    // file name -> root element, only used when the root element cannot be read
    private static final Map<String, String> FILE_NAME_TO_ROOT = Map.of(
            "ibm-web-ext.xml", "web-ext",
            "ibm-web-bnd.xml", "web-bnd",
            "ibm-application-ext.xml", "application-ext",
            "ibm-application-bnd.xml", "application-bnd",
            "ibm-ejb-jar-ext.xml", "ejb-jar-ext",
            "ibm-ejb-jar-bnd.xml", "ejb-jar-bnd",
            "ibm-managed-bean-bnd.xml", "managed-bean-bnd",
            "ibm-ws-bnd.xml", "webservices-bnd",
            "cachespec.xml", "cache");

    // schema file name -> path in the .lemminx cache. Null until the schemas have been deployed.
    private static volatile Map<String, Path> deployedSchemas;

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
     * Read only the root element of an xml file.
     *
     * @return the root element, or null if the file is not an xml file or cannot be read
     */
    private static RootElement readRoot(String fileUri) {
        if (fileUri == null || !fileUri.endsWith(".xml")) {
            return null;
        }
        return XmlReader.getRootElement(fileUri);
    }

    /**
     * @param root the root element of the file, or null if it could not be read
     * @return the schema versions of this descriptor, oldest first, or null if the file is not a deployment descriptor
     */
    private static List<String> getSchemas(String fileUri, RootElement root) {
        if (fileUri == null) {
            return null;
        }
        if (root != null) {
            return IBM_NAMESPACE.equals(root.namespace()) ? ROOT_TO_SCHEMAS.get(root.localName()) : null;
        }
        // Root element not readable: the file may be new, empty, or not on disk. Use the file name.
        String fileName = fileUri.substring(fileUri.lastIndexOf('/') + 1);
        String rootName = FILE_NAME_TO_ROOT.get(fileName);
        return rootName == null ? null : ROOT_TO_SCHEMAS.get(rootName);
    }

    /**
     * @param fileUri URI of the file, may be null
     * @return true if the file is an IBM deployment descriptor such as ibm-web-bnd.xml
     */
    public static boolean isDeploymentDescriptor(String fileUri) {
        return getSchemas(fileUri, readRoot(fileUri)) != null;
    }

    /**
     * Get the file name of the schema to use for a deployment descriptor, for example ibm-web-bnd_1_0.xsd.
     *
     * @param schemas  the schema versions of this descriptor, oldest first
     * @param systemId schema location from xsi:schemaLocation, may be null. If it names one of the schemas of this
     *                 descriptor, that version is used. Otherwise the latest version is used.
     */
    private static String getSchemaName(List<String> schemas, String systemId) {
        if (systemId != null) {
            String hinted = systemId.substring(systemId.lastIndexOf('/') + 1);
            if (schemas.contains(hinted)) {
                return hinted;
            }
        }
        return schemas.get(schemas.size() - 1);
    }

    /**
     * @return the last value of the "namespace url" pairs of the xsi:schemaLocation, or null if there is none
     */
    private static String getSchemaLocationHint(RootElement root) {
        if (root == null || root.schemaLocation() == null || root.schemaLocation().isBlank()) {
            return null;
        }
        String[] tokens = root.schemaLocation().trim().split("\\s+");
        return tokens[tokens.length - 1];
    }

    /**
     * Get the URI of the local schema for a deployment descriptor, using the schema version named in the
     * xsi:schemaLocation of the file. The file is read once.
     *
     * @param fileUri URI of the descriptor file
     * @return schema URI as a String, or null if the file is not a deployment descriptor
     */
    public static String getSchemaUri(String fileUri) {
        RootElement root = readRoot(fileUri);
        List<String> schemas = getSchemas(fileUri, root);
        if (schemas == null) {
            return null;
        }
        return deploySchema(fileUri, getSchemaName(schemas, getSchemaLocationHint(root)));
    }

    /**
     * Get the URI of the local schema for a deployment descriptor.
     *
     * @param fileUri  URI of the descriptor file
     * @param systemId schema location from xsi:schemaLocation, may be null. If it names a version of the schema
     *                 of this descriptor, that version is used. Otherwise the latest version is used.
     * @return schema URI as a String, or null if the file is not a deployment descriptor
     */
    public static String getSchemaUri(String fileUri, String systemId) {
        List<String> schemas = getSchemas(fileUri, readRoot(fileUri));
        if (schemas == null) {
            return null;
        }
        return deploySchema(fileUri, getSchemaName(schemas, systemId));
    }

    private static String deploySchema(String fileUri, String schemaName) {
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

    /**
     * Copies all bundled schemas to the .lemminx cache the first time it is called, and returns the path of the
     * requested one. The files are deployed together because the schemas include each other by relative path.
     * If copying fails, nothing is remembered, so the next call tries again.
     */
    private static Path deploy(String schemaName) throws IOException {
        Map<String, Path> deployed = deployedSchemas;
        if (deployed == null) {
            Map<String, Path> paths = new HashMap<>();
            for (String file : BUNDLED_FILES) {
                paths.put(file, CacheResourcesManager
                        .getResourceCachePath(new ResourceToDeploy(CACHE_URL_DIR + file, CLASSPATH_DIR + file)));
            }
            deployed = Map.copyOf(paths);
            deployedSchemas = deployed;
        }
        return deployed.get(schemaName);
    }
}
