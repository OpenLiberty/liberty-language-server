/*******************************************************************************
* Copyright (c) 2025 IBM Corporation and others.
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

public final class ResourceBundleMappingConstants {

    private ResourceBundleMappingConstants() {
    }

    public static final String ERR_FEATURE_ALREADY_INCLUDED = "liberty.lemminx.feature.already.included.error";
    public static final String ERR_FEATURE_MULTIPLE_VERSIONS = "liberty.lemminx.feature.multiple.versions.included.error";
    public static final String ERR_FEATURE_NAME_CHANGED = "liberty.lemminx.feature.name.changed.error";
    public static final String ERR_IMPLICIT_NOT_OPTIONAL_MESSAGE = "liberty.lemminx.resource.not.optional.add.attribute.error";
    public static final String ERR_NOT_OPTIONAL_MESSAGE = "liberty.lemminx.resource.not.optional.error";
    public static final String WARN_MISSING_FILE_MESSAGE = "liberty.lemminx.file.missing.warning";
    public static final String ERR_SPECIFIED_DIR_IS_FILE = "liberty.lemminx.resource.path.dir.error";
    public static final String ERR_SPECIFIED_FILE_IS_DIR = "liberty.lemminx.resource.path.file.error";
    public static final String ERR_MISSING_CONFIGURED_FEATURE_MESSAGE = "liberty.lemminx.config.element.not.related.error";
    public static final String ERR_PLATFORM_NOT_EXIST = "liberty.lemminx.platform.does.not.exist.error";
    public static final String ERR_PLATFORMS_IN_CONFLICT = "liberty.lemminx.platform.conflict.error";
    public static final String ERR_PLATFORM_ALREADY_INCLUDED = "liberty.lemminx.platform.already.included.error";
    public static final String ERR_PLATFORM_MULTIPLE_VERSIONS = "liberty.lemminx.platform.multiple.versions.included.error";
    public static final String WARN_VARIABLE_RESOLUTION_NOT_AVAILABLE = "liberty.lemminx.variable.resolution.not.available.warning";
    public static final String ERR_FEATURE_NOT_EXIST = "liberty.lemminx.feature.does.not.exist.error";
    public static final String ERR_VERSIONLESS_FEATURE_NO_PLATFORM_OR_FEATURE = "liberty.lemminx.versionless.feature.no.platform.or.feature.error";
    public static final String ERR_VERSIONLESS_FEATURE_NO_COMMON_PLATFORM = "liberty.lemminx.versionless.feature.no.common.platform.error";
    public static final String ERR_VERSIONLESS_FEATURE_MULTIPLE_COMMON_PLATFORM = "liberty.lemminx.versionless.feature.multiple.common.platform.error";
    public static final String ERR_VERSIONLESS_FEATURE_NO_CONFIGURED_PLATFORM = "liberty.lemminx.versionless.feature.no.platform.configured.error";
    public static final String ERR_VERSIONLESS_FEATURE_NO_SUPPORTED_PLATFORM = "liberty.lemminx.versionless.feature.no.supported.platform.error";
    public static final String ERR_VARIABLE_INVALID_NAME_ATTRIBUTE = "liberty.lemminx.variable.name.invalid.error";
    public static final String ERR_VARIABLE_INVALID_VALUE_ATTRIBUTE = "liberty.lemminx.variable.value.attribute.invalid.error";
    public static final String WARN_VARIABLE_INVALID_VALUE = "liberty.lemminx.variable.value.invalid.warning";
    public static final String ERR_VARIABLE_NOT_EXIST = "liberty.lemminx.variable.does.not.exist.error";

    public static final String TITLE_ADD_OPTIONAL_ATTRIBUTE = "liberty.lemminx.codeaction.add.optional.attribute.title";
    public static final String TITLE_SET_OPTIONAL_ATTRIBUTE = "liberty.lemminx.codeaction.set.optional.attribute.title";
    public static final String TITLE_ADD_FEATURE = "liberty.lemminx.codeaction.add.feature.title";
    public static final String TITLE_ADD_TRAILING_SLASH = "liberty.lemminx.codeaction.add.trailing.slash.title";
    public static final String TITLE_CREATE_SERVER_CONFIG_FILE = "liberty.lemminx.codeaction.create.server.config.file.title";
    public static final String TITLE_REMOVE_TRAILING_SLASH = "liberty.lemminx.codeaction.remove.trailing.slash.title";
    public static final String TITLE_REPLACE_FEATURE = "liberty.lemminx.codeaction.replace.feature.title";
    public static final String TITLE_REPLACE_PLATFORM = "liberty.lemminx.codeaction.replace.platform.title";
    public static final String TITLE_REPLACE_VARIABLE_VALUE = "liberty.lemminx.codeaction.replace.variable.value.title";
    public static final String TITLE_ADD_VARIABLE = "liberty.lemminx.codeaction.add.variable.title";

    public static final String TITLE_HOVER_DESCRIPTION = "liberty.lemminx.feature.hover.description.title";
    public static final String TITLE_HOVER_ENABLED_BY = "liberty.lemminx.feature.hover.enabled.by.title";
    public static final String TITLE_HOVER_ENABLES = "liberty.lemminx.feature.hover.enables.title";
    public static final String PLATFORM_DESCRIPTION = "liberty.lemminx.platform.hover.%s.description";
}
