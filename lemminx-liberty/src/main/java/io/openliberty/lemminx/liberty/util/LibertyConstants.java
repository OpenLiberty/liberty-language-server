package io.openliberty.lemminx.liberty.util;

public final class LibertyConstants {
    private LibertyConstants() {
    }

    public static final String SERVER_XML = "server.xml";

    public static final String SERVER_ELEMENT = "server";

    public static final String FEATURE_MANAGER_ELEMENT = "featureManager";
    public static final String FEATURE_ELEMENT = "feature";

    public static final String PUBLIC_VISIBILITY = "PUBLIC";

    public static final String DEFAULT_SERVER_VERSION = "20.0.0.9";

    // used in the feature cache to store the installed feature list
    public static final String INSTALLED_FEATURE_KEY = "installedFeatures";
}
