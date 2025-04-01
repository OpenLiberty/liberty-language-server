package io.openliberty.tools.test;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.SchemaAndFeatureListGeneratorUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class LibertyWorkspaceIT {
    private static final Logger LOGGER = Logger.getLogger(LibertyWorkspaceIT.class.getName());

    @AfterAll
    public static void tearDown() {
        LibertyProjectsManager.getInstance().cleanInstance();
        assert LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().isEmpty();
    }

    private Path schemaJarPath;
    private Path featureListJarPath;

    @BeforeEach
    void setUp() {
        Path projectRoot = getProjectRoot();

        this.schemaJarPath = projectRoot.resolve(Paths.get("target", "it", "all-locales-schema-and-features-gen-ol-it", "target", "liberty", "wlp", "bin", "tools", "ws-schemagen.jar"));
        this.featureListJarPath = projectRoot.resolve(Paths.get("target", "it", "all-locales-schema-and-features-gen-ol-it", "target", "liberty", "wlp", "bin", "tools", "ws-featurelist.jar"));

        Assertions.assertTrue(Files.exists(schemaJarPath),
                "Schema JAR file does not exist. Path: " + schemaJarPath.toAbsolutePath());
        Assertions.assertTrue(Files.exists(featureListJarPath),
                "Feature List JAR file does not exist. Path: " + featureListJarPath.toAbsolutePath());
    }

    @Test
    void generateSchemaFiles() throws Exception {
        Path projectRoot = getProjectRoot();
        Path resourcesSchemaDir = projectRoot.resolve("src/main/resources/schema/xsd/liberty");
        if (!resourcesSchemaDir.toFile().exists()) {
            if (!resourcesSchemaDir.toFile().mkdirs()) {
                LOGGER.warning("Could not create the schemas directory.");
                return;
            }
        }

        List<Locale> locales = new ArrayList<>(SettingsService.getInstance().getAvailableLocales());
        locales.add(new Locale("")); // This is to generate a common file (without locale)

        for (Locale locale : locales) {
            String localeName = locale.toString();
            File outputFile = new File(
                    resourcesSchemaDir.toFile(),
                    "server-cached-" + System.getProperty("liberty.version") + (StringUtils.isNotEmpty(localeName) ? "_" + localeName : "") + ".xsd"
            );
            SchemaAndFeatureListGeneratorUtil.generateFile(
                    SchemaAndFeatureListGeneratorUtil.ProcessType.SCHEMA,
                    resourcesSchemaDir,
                    schemaJarPath,
                    outputFile,
                    localeName
            );
        }
    }

    @Test
    void generateFeatureListFiles() throws Exception {
        Path projectRoot = getProjectRoot();
        Path resourcesFeatureListDir = projectRoot.resolve("src/main/resources/featurelist.cached");
        if (!resourcesFeatureListDir.toFile().exists()) {
            if (!resourcesFeatureListDir.toFile().mkdirs()) {
                LOGGER.warning("Could not create the featurelist directory.");
                return;
            }
        }
        Path testResourcesDir = projectRoot.resolve("src/test/resources");

        List<Locale> locales = new ArrayList<>(SettingsService.getInstance().getAvailableLocales());
        locales.add(new Locale("")); // This is to generate a common file (without locale)

        for (Locale locale : locales) {
            String localeName = locale.toString();
            File outputFile = new File(
                    resourcesFeatureListDir.toFile(),
                    "featurelist-cached-" + System.getProperty("liberty.version") + (StringUtils.isNotEmpty(localeName) ? "_" + localeName : "") + ".xml"
            );
            SchemaAndFeatureListGeneratorUtil.generateFile(
                    SchemaAndFeatureListGeneratorUtil.ProcessType.FEATURE_LIST,
                    resourcesFeatureListDir,
                    featureListJarPath,
                    outputFile,
                    localeName
            );
        }
        // copy default locale featurelist file to test/resources folder for unit tests
        Files.copy(new File(
                resourcesFeatureListDir.toFile(),
                "featurelist-cached-" + System.getProperty("liberty.version")+".xml").toPath(),
                new File(testResourcesDir.toFile(),"featurelist-ol-" + System.getProperty("liberty.version")+".xml").toPath());


    }

    private Path getProjectRoot() {
        String dir = System.getProperty("user.dir");
        String itPath = "/target/it/all-locales-schema-and-features-gen-ol-it";

        if (dir.endsWith(itPath) || dir.endsWith(itPath.replace("/", File.separator))) {
            return Paths.get(dir).getParent().getParent().getParent();
        }
        return Paths.get(dir);
    }
}
