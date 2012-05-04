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
package com.siteview.mde.internal.ui.wizards.tools;

import java.util.Vector;
import org.eclipse.core.resources.IProject;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.wizards.NewWizard;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;
	private Vector selected;
	private IProject[] fUnconverted;

	public ConvertedProjectWizard(IProject[] projects, Vector initialSelection) {
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(MDEUIMessages.ConvertedProjectWizard_title);
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		this.selected = initialSelection;
		this.fUnconverted = projects;
	}

	public void addPages() {
		mainPage = new ConvertedProjectsPage(fUnconverted, selected);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
