/*******************************************************************************
* Copyright (c) 2020, 2022, 2023 IBM Corporation and others.
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

public final class LibertyConstants {
    private LibertyConstants() {
    }

    public static final String SERVER_XML = "server.xml";

    public static final String SERVER_ELEMENT = "server";

    public static final String FEATURE_MANAGER_ELEMENT = "featureManager";
    public static final String FEATURE_ELEMENT = "feature";
    public static final String INCLUDE_ELEMENT = "include";

    public static final String PUBLIC_VISIBILITY = "PUBLIC";

    public static final String WLP_USER_CONFIG_DIR = "/usr/shared/config/";
    public static final String SERVER_CONFIG_DROPINS_DEFAULTS = "/configDropins/defaults/";
    public static final String SERVER_CONFIG_DROPINS_OVERRIDES = "/configDropins/overrides";
}
