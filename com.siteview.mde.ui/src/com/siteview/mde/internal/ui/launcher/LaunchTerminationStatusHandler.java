/*******************************************************************************
 * Copyright (c) 2009, 2010 eXXcellent solutions gmbh, EclipseSource Corporation
 * and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *     EclipseSource - initial API and implementation, ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.launcher;

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import com.siteview.mde.internal.launching.MDEMessages;
import com.siteview.mde.internal.launching.launcher.LaunchListener;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.views.log.LogView;

public class LaunchTerminationStatusHandler implements IStatusHandler {
	// different ways to open the error log
	public static final int OPEN_IN_ERROR_LOG_VIEW = 0;
	public static final int OPEN_IN_SYSTEM_EDITOR = 1;

	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (status.getCode() == 13)
			handleOtherReasonsFoundInLog((ILaunch) source);
		else if (status.getCode() == 15)
			handleWorkspaceInUse();
		return null;
	}

	private void handleWorkspaceInUse() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.Launcher_error_title, MDEMessages.Launcher_error_code15);
			}
		});
	}

	private void handleOtherReasonsFoundInLog(final ILaunch launch) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					File log = LaunchListener.getMostRecentLogFile(launch.getLaunchConfiguration());
					if (log != null && log.exists()) {
						MessageDialog dialog = new MessageDialog(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.Launcher_error_title, null, // accept the default window icon
								MDEUIMessages.Launcher_error_code13, MessageDialog.ERROR, new String[] {MDEUIMessages.Launcher_error_displayInLogView, MDEUIMessages.Launcher_error_displayInSystemEditor, IDialogConstants.NO_LABEL}, OPEN_IN_ERROR_LOG_VIEW);
						int dialog_value = dialog.open();
						if (dialog_value == OPEN_IN_ERROR_LOG_VIEW) {
							IWorkbenchPage page = MDEPlugin.getActivePage();
							if (page != null) {
								LogView errlog = (LogView) page.showView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
								errlog.handleImportPath(log.getAbsolutePath());
								errlog.sortByDateDescending();
							}
						} else if (dialog_value == OPEN_IN_SYSTEM_EDITOR) {
							openInEditor(log);
						}
					}
				} catch (CoreException e) {
				}
			}
		});
	}

	private void openInEditor(File log) {
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(log.getAbsolutePath()));
		if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
			IWorkbenchWindow ww = MDEPlugin.getActiveWorkbenchWindow();
			IWorkbenchPage page = ww.getActivePage();
			try {
				IDE.openEditorOnFileStore(page, fileStore);
			} catch (PartInitException e) {
			}
		}
	}
}
