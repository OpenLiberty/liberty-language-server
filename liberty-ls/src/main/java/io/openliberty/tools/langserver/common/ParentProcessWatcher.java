/*******************************************************************************
* Copyright (c) 2017 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
/**
 * This class is a copy/paste of eclipse/lsp4mp class file at location 
 * https://github.com/eclipse/lsp4mp/blob/master/microprofile.ls/org.eclipse.lsp4mp.ls/src/main/java/org/eclipse/lsp4mp/ls/commons/ParentProcessWatcher.java
 * with modifications made for the Liberty Config Language Server
 *
 */
package io.openliberty.tools.langserver.common;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageServer;

import com.google.common.io.Closeables;

/**
 * Watches the parent process PID and invokes exit if it is no longer available.
 * This implementation waits for periods of inactivity to start querying the
 * PIDs.
 */
public final class ParentProcessWatcher implements Runnable, Function<MessageConsumer, MessageConsumer> {

	private static final Logger LOGGER = Logger.getLogger(ParentProcessWatcher.class.getName());
	private static final boolean isJava1x = System.getProperty("java.version").startsWith("1.");
	private static final boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

	/**
	 * Exit code returned when XML Language Server is forced to exit.
	 */
	private static final int FORCED_EXIT_CODE = 1;

	private static final long INACTIVITY_DELAY_SECS = 30 * 1000;
	private static final int POLL_DELAY_SECS = 10;
	private volatile long lastActivityTime;
	private final ProcessLanguageServer server;
	private final Function<MessageConsumer, MessageConsumer> wrapper;
	private ScheduledFuture<?> task;
	private ScheduledExecutorService service;

	public interface ProcessLanguageServer extends LanguageServer {

		long getParentProcessId();

		void exit(int exitCode);
	}

	public ParentProcessWatcher(ProcessLanguageServer server, Function<MessageConsumer, MessageConsumer> wrapper) {
		this.server = server;
		this.wrapper = wrapper;
		service = Executors.newScheduledThreadPool(1);
		task = service.scheduleWithFixedDelay(this, POLL_DELAY_SECS, POLL_DELAY_SECS, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		if (!parentProcessStillRunning()) {
			LOGGER.warning("Parent process stopped running, forcing server exit");
			task.cancel(true);
			server.exit(FORCED_EXIT_CODE);
		}
	}

	/**
	 * Checks whether the parent process is still running. If not, then we assume it
	 * has crashed, and we have to terminate the Java Language Server.
	 *
	 * @return true if the parent process is still running
	 */
	private boolean parentProcessStillRunning() {
		// Wait until parent process id is available
		final long pid = server.getParentProcessId();
		if (pid == 0 || lastActivityTime > (System.currentTimeMillis() - INACTIVITY_DELAY_SECS)) {
			return true;
		}
		String command;
		if (isWindows) {
			command = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
		} else {
			command = "kill -0 " + pid;
		}
		Process process = null;
		boolean finished = false;
		try {
			process = Runtime.getRuntime().exec(command);
			finished = process.waitFor(POLL_DELAY_SECS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroy();
				finished = process.waitFor(POLL_DELAY_SECS, TimeUnit.SECONDS); // wait for the process to stop
			}
			if (isWindows && finished && process.exitValue() > 1) {
				// the tasklist command should return 0 (parent process exists) or 1 (parent
				// process doesn't exist)
				LOGGER.warning("The tasklist command: '" + command + "' returns " + process.exitValue());
				return true;
			}
			return !finished || process.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			return true;
		} finally {
			if (process != null) {
				if (!finished) {
					process.destroyForcibly();
				}
				// Terminating or destroying the Process doesn't close the process handle on
				// Windows.
				// It is only closed when the Process object is garbage collected (in its
				// finalize() method).
				// On Windows, when the Java LS is idle, we need to explicitly request a GC,
				// to prevent an accumulation of zombie processes, as finalize() will be called.
				if (isWindows) {
					// Java >= 9 doesn't close the handle when the process is garbage collected
					// We need to close the opened streams
					if (!isJava1x) {
						Closeables.closeQuietly(process.getInputStream());
						Closeables.closeQuietly(process.getErrorStream());
						try {
							Closeables.close(process.getOutputStream(), false);
						} catch (IOException e) {
						}
					}
					System.gc();
				}
			}
		}

	}

	@Override
	public MessageConsumer apply(final MessageConsumer consumer) {
		// inject our own consumer to refresh the timestamp
		return message -> {
			lastActivityTime = System.currentTimeMillis();
			wrapper.apply(consumer).consume(message);
		};
	}
}