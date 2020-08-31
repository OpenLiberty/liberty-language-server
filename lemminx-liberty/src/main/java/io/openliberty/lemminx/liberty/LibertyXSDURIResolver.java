package io.openliberty.lemminx.liberty;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import io.openliberty.lemminx.liberty.util.LibertyUtils;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;

public class LibertyXSDURIResolver implements URIResolverExtension {

  private static final Logger LOGGER = Logger.getLogger(LibertyXSDURIResolver.class.getName());

  private static final String XSD_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/src/main/resources/schema/server.xsd";
  private static final String XSD_CLASSPATH_LOCATION = "/schema/xsd/liberty/server.xsd";

  /**
   * SERVER_XSD_RESOURCE is the server.xsd that is located at `/schema/server.xsd`
   * that should be deployed (copied) to the .lemminx cache. The resourceURI is
   * used by lemmix determine the path to store the file in the cache. So for
   * server.xsd it takes the resource located at `/schema/server.xsd` and deploys
   * it to:
   * ~/.lemminx/cache/https/github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/src/main/resources/schema/server.xsd
   */
  private static final ResourceToDeploy SERVER_XSD_RESOURCE = new ResourceToDeploy(XSD_RESOURCE_URL,
      XSD_CLASSPATH_LOCATION);

  @Override
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
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) {
    String publicId = resourceIdentifier.getNamespace();
    String baseLocation = resourceIdentifier.getBaseSystemId();
    if (LibertyUtils.isServerXMLFile(baseLocation)) {
      String xslFilePath = resolve(baseLocation, publicId, null);
      if (xslFilePath != null) {
        return new XMLInputSource(publicId, xslFilePath, xslFilePath);
      }
    }

    return null;
  }
}
