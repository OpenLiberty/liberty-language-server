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
package io.openliberty.tools.langserver.lemminx.services;

import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class FileWatchService {

    private final Set<FileAlterationObserver> fileObservers = new HashSet<>();
    private final Set<FileAlterationMonitor> monitors = new HashSet<>();

    private static final FileWatchService instance = new FileWatchService();

    public static FileWatchService getInstance() {
        return instance;
    }

    private static final Logger LOGGER = Logger.getLogger(FileWatchService.class.getName());

    private FileWatchService() {
    }

    /**
     * add observer for file changes
     *
     * @param workspace  workspace
     * @param watchLocations parent locations
     */
    public void addFileAlterationObserver(LibertyWorkspace workspace, List<String> watchLocations)
            throws Exception {
        for (String location:watchLocations) {
            FileAlterationObserver observer = getFileAlterationObserver(location, workspace);
            observer.initialize();
            fileObservers.add(observer);
            FileAlterationMonitor monitor = new FileAlterationMonitor();
            monitor.addObserver(observer);
            monitor.start();
            monitors.add(monitor);
        }
    }

    private FileAlterationObserver getFileAlterationObserver(final String parentPath, LibertyWorkspace workspace) {
        IOFileFilter notFileFilter = FileFilterUtils.notFileFilter(
                new SuffixFileFilter(Arrays.asList(".class", ".lst", ".txt",".log",".manager",".libertyls",".sLock"),
                        IOCase.INSENSITIVE)
                        .or(new NameFileFilter("workarea"))
                        .or(new NameFileFilter("plugin-cfg.xml")));
        FileAlterationObserver observer = new FileAlterationObserver(parentPath, notFileFilter);
        addFileAlterationListener(observer, workspace);
        return observer;
    }

    private void addFileAlterationListener(FileAlterationObserver observer, LibertyWorkspace workspace) {
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onDirectoryCreate(File file) {
            }

            @Override
            public void onDirectoryDelete(File file) {
            }

            @Override
            public void onDirectoryChange(File file) {
            }

            @Override
            public void onFileCreate(File file) {
                onAlteration(file, workspace);
            }

            @Override
            public void onFileDelete(File file) {
                onAlteration(file, workspace);
            }

            @Override
            public void onFileChange(File file) {
                onAlteration(file, workspace);
            }

            /**
             * update variables on file alteration, if modified file is a config
             *
             * @param file changed file
             * @param workspace current workspace
             */
            private void onAlteration(File file, LibertyWorkspace workspace) {
                boolean watchedFileChanged = LibertyConstants.filesToWatch.stream().anyMatch(fileName -> file.getName().contains(fileName));
                boolean isConfigXmlFile = false;
                try {
                    isConfigXmlFile = LibertyUtils.isConfigXMLFile(file.getCanonicalPath());
                } catch (IOException e) {
                    LOGGER.warning("Liberty XML variables cannot be updated for file path %s with error %s"
                            .formatted(file.getPath(), e.getMessage()));
                }
                if (watchedFileChanged || isConfigXmlFile) {
                    SettingsService.getInstance().populateVariablesForWorkspace(workspace);
                    LOGGER.info("Liberty XML variables updated for workspace URI " + workspace.getWorkspaceString());
                }
            }
        });
    }

    /**
     * clean all monitors for all workspaces
     */
    public void cleanFileMonitors() {
        fileObservers.clear();
        try {
            for (FileAlterationMonitor monitor : monitors) {
                monitor.stop();
            }
        } catch (Exception e) {
            LOGGER.warning("Issue while removing file monitors with error %s"
                    .formatted(e.getMessage()));
        }
    }
}
