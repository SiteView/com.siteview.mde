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

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.ElementList;

public class ExtensionEditorWizard extends Wizard {
	public static final String PLUGIN_POINT = "newExtension"; //$NON-NLS-1$
	private ExtensionEditorSelectionPage pointPage;
	private IMonitorModelBase model;
	private IProject project;
	private IStructuredSelection selection;
	private ElementList wizards;

	public ExtensionEditorWizard(IProject project, IMonitorModelBase model, IStructuredSelection selection) {
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWEX_WIZ);
		this.model = model;
		this.project = project;
		this.selection = selection;
		setForcePreviousAndNextButtons(true);
		setWindowTitle(MDEUIMessages.ExtensionEditorWizard_wtitle);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
		loadWizardCollection();
	}

	public void addPages() {
		pointPage = new ExtensionEditorSelectionPage(wizards);
		pointPage.init(project, model.getMonitorBase(), selection);
		addPage(pointPage);
	}

	private void loadWizardCollection() {
		NewExtensionRegistryReader reader = new NewExtensionRegistryReader(true);
		wizards = reader.readRegistry(MDEPlugin.getPluginId(), PLUGIN_POINT, true);
	}

	public boolean performFinish() {
		return true;
	}

	public void dispose() {
		super.dispose();
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}
