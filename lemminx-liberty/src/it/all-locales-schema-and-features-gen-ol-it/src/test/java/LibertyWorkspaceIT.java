import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.util.DocumentUtil;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LibertyWorkspaceIT {
    private static final Logger LOGGER = Logger.getLogger(LibertyWorkspaceIT.class.getName());

    @AfterAll
    public static void tearDown() {
        LibertyProjectsManager.getInstance().cleanInstance();
        assert(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().isEmpty());
    }

    @Test
    public void testWorkspace() throws BadLocationException, IOException, URISyntaxException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");
        String serverXMLURI = serverXmlFile.toURI().toString();

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLURI);
        libertyWorkspace.setLibertyInstallationDir(testFolder + "/target/liberty/wlp/bin/tools/ws-schemagen.jar");
        libertyWorkspace.setLibertyRuntime("ol");
        libertyWorkspace.setLibertyVersion("ol-25.0.0.2");
        libertyWorkspace.setLibertyInstalled(true);

        Path schemaGenJarPath = LibertyUtils.findLibertyFileForWorkspace(libertyWorkspace, Paths.get("bin", "tools", "ws-schemagen.jar"));
        if (schemaGenJarPath != null) {
            //Generate schema file
            generateAllLocaleSchemas(libertyWorkspace, schemaGenJarPath);
        }

        libertyWorkspace.setLibertyInstallationDir(System.getProperty("user.dir") + "/target/liberty/wlp/bin/tools/ws-featurelist.jar");
        Path featureListJAR = LibertyUtils.findLibertyFileForWorkspace(libertyWorkspace, Paths.get("bin", "tools", "ws-featurelist.jar"));
        if (featureListJAR != null && featureListJAR.toFile().exists()) {
            //Generate featurelist file
            generateAllLocaleFeatureLists(libertyWorkspace, featureListJAR);
        }
    }

    public void generateAllLocaleSchemas(LibertyWorkspace libertyWorkspace, Path schemaGenJarPath) {
        Locale[] localesList = new Locale[]{Locale.ENGLISH, Locale.FRENCH};
        for (Locale locale : localesList) {
            generateServerSchemaXsd(libertyWorkspace, schemaGenJarPath, locale);
        }
    }

    private void generateServerSchemaXsd(LibertyWorkspace libertyWorkspace, Path schemaGenJarPath, Locale locale) {
        String projectBaseDir = System.getProperty("user.dir");
        projectBaseDir = projectBaseDir.replace("/target/it/all-locales-schema-and-features-gen-ol-it", "");
        Path resourcesSchemaDir = Paths.get(projectBaseDir, "src", "main", "resources", "schema", "xsd", "liberty");

        File schemasDirFile = resourcesSchemaDir.toFile();

        if (!schemasDirFile.exists()) {
            if (!schemasDirFile.mkdirs()) {
                LOGGER.warning("Could not create the schemas directory.");
                return;
            }
        }

        String localeTag = locale.toLanguageTag();
        String baseFilename = "schema";
        if (libertyWorkspace.isLibertyRuntimeAndVersionSet()) {
            baseFilename += "-" + libertyWorkspace.getLibertyVersion();
        }

        File xsdDestFile = new File(schemasDirFile, baseFilename + "-" + localeTag + ".xsd");

        if (!xsdDestFile.exists()) {
            try {
                LOGGER.info("Generating schema file from: " + schemaGenJarPath.toString() + " for locale: " + localeTag);
                String xsdDestPath = xsdDestFile.getCanonicalPath();

                LOGGER.info("Generating schema file at: " + xsdDestPath);

                ProcessBuilder pb = new ProcessBuilder("java", "-jar", schemaGenJarPath.toAbsolutePath().toString(), "--schemaVersion=1.1",
                        "--outputVersion=2", xsdDestPath, "--locale=" + localeTag); // Added locale parameter
                pb.directory(schemasDirFile);
                File logFile = new File(schemasDirFile, "schemagen.log");
                pb.redirectErrorStream(true);
                pb.redirectOutput(logFile);

                Process proc = pb.start();
                if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                    proc.destroy();
                    LOGGER.warning("Exceeded 30 second timeout during schema file generation for locale: " + localeTag);
                    return;
                }

                if (xsdDestFile.exists()) {
                    DocumentUtil.removeExtraneousAnyAttributeElements(xsdDestFile);
                }

                LOGGER.info("Caching schema file with URI: " + xsdDestFile.toURI().toString());
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                LOGGER.warning("Due to an exception during schema file generation for locale: " + localeTag + ", a cached schema file will be used.");
                return;
            }
        }

        LOGGER.info("Using schema file at: " + xsdDestFile.toURI().toString());
    }

    public void generateAllLocaleFeatureLists(LibertyWorkspace libertyWorkspace, Path featurelistJarPath) {
        Locale[] localesList = new Locale[]{Locale.ENGLISH, Locale.FRENCH};
        for (Locale locale : localesList) {
            generateFeatureListXml(libertyWorkspace, featurelistJarPath, locale);
        }
    }

    private void generateFeatureListXml(LibertyWorkspace libertyWorkspace, Path featurelistJarPath, Locale locale) {
        String projectBaseDir = System.getProperty("user.dir");
        projectBaseDir = projectBaseDir.replace("/target/it/all-locales-schema-and-features-gen-ol-it", "");
        Path featureListDir = Paths.get(projectBaseDir, "src", "main", "resources");

        File featuresDirFile = featureListDir.toFile();

        if (!featuresDirFile.exists()) {
            if (!featuresDirFile.mkdirs()) {
                LOGGER.warning("Could not create the feature list directory.");
                return;
            }
        }

        String localeTag = locale.toLanguageTag();
        String baseFilename = "featurelist";
        if (libertyWorkspace.isLibertyRuntimeAndVersionSet()) {
            baseFilename += "-" + libertyWorkspace.getLibertyVersion();
        }

        File featureListFile = new File(featuresDirFile, baseFilename + "-" + localeTag + ".xml");

        try {
            LOGGER.info("Generating feature list file from: " + featurelistJarPath.toString() + " for locale: " + localeTag);
            String xmlDestPath = featureListFile.getCanonicalPath();

            LOGGER.info("Generating feature list file at: " + xmlDestPath);
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", featurelistJarPath.toAbsolutePath().toString(), xmlDestPath, "--locale=" + localeTag);

            pb.directory(featuresDirFile);
            File logFile = new File(featuresDirFile, "featurelist.log");
            pb.redirectErrorStream(true);

            pb.redirectOutput(logFile);
            Process proc = pb.start();
            if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                proc.destroy();
                LOGGER.warning("Exceeded 30 second timeout during feature list generation for locale: " + localeTag + ". Using cached features json file.");
                return;
            }

        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            LOGGER.warning("Due to an exception during feature list file generation for locale: " + localeTag + ", a cached features json file will be used.");
            return;
        }

        LOGGER.info("Using feature list file at: " + featureListFile.toURI().toString());
    }
}
