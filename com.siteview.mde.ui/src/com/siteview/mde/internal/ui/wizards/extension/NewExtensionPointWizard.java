/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.extension;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.core.monitor.IPluginExtensionPoint;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.IProject;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.wizards.NewWizard;

public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private IMonitorModelBase model;
	private IProject project;
	private IPluginExtensionPoint point;
	private ManifestEditor editor;

	public NewExtensionPointWizard(IProject project, IMonitorModelBase model, ManifestEditor editor) {
		this(project, model, (IPluginExtensionPoint) null);
		this.editor = editor;
	}

	public NewExtensionPointWizard(IProject project, IMonitorModelBase model, IPluginExtensionPoint point) {
		initialize();
		this.project = project;
		this.model = model;
		this.point = point;
	}

	public void initialize() {
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWEXP_WIZ);
		setWindowTitle(MDEUIMessages.NewExtensionPointWizard_wtitle);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		mainPage = new NewExtensionPointMainPage(project, model, point);
		addPage(mainPage);
	}

	public boolean performFinish() {
		if (editor != null)
			editor.ensurePluginContextPresence();
		return mainPage.finish();
	}
}
