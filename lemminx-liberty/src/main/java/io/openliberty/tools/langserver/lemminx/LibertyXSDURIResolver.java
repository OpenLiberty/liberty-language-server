/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.IExternalGrammarLocationProvider;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;

import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyXSDURIResolver implements URIResolverExtension, IExternalGrammarLocationProvider {
  private static final Logger LOGGER = Logger.getLogger(LibertyXSDURIResolver.class.getName());

  private static final String XSD_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/liberty/server.xsd";
  private static final String XSD_CLASSPATH_LOCATION = "/schema/xsd/liberty/server.xsd";

  /**
   * SERVER_XSD_RESOURCE is the server.xsd that is located at `/schema/server.xsd`
   * that should be deployed (copied) to the .lemminx cache. The resourceURI is
   * used by lemmix determine the path to store the file in the cache. So for
   * server.xsd it takes the resource located at `/schema/server.xsd` and deploys
   * it to:
   * ~/.lemminx/cache/https/github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/src/main/resources/schema/server.xsd
   * 
   * Declared public to be used by tests
   */
  public static final ResourceToDeploy SERVER_XSD_RESOURCE = new ResourceToDeploy(XSD_RESOURCE_URL,
      XSD_CLASSPATH_LOCATION);

  public String resolve(String baseLocation, String publicId, String systemId) {
    if (LibertyUtils.isServerXMLFile(baseLocation)) {
      try {
        Path serverXSDCacheFile = CacheResourcesManager.getResourceCachePath(SERVER_XSD_RESOURCE);
        return serverXSDCacheFile.toFile().toURI().toString();
      } catch (Exception e) {
        LOGGER.severe("Error: Unable to deploy server.xsd to lemminx cache.");
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public Map<String, String> getExternalGrammarLocation(URI fileURI) {
    String xsdFile = resolve(fileURI.toString(), null, null);
    if (xsdFile == null) {
      return null;
    }

    Map<String, String> externalGrammar = new HashMap<>();
    externalGrammar.put(IExternalGrammarLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, xsdFile);
    return externalGrammar;
  }

}