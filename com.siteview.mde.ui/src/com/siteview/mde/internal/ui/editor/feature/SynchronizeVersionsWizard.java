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
package com.siteview.mde.internal.ui.editor.feature;

import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.ui.*;

public class SynchronizeVersionsWizard extends Wizard {
	private FeatureEditor featureEditor;
	private SynchronizeVersionsWizardPage mainPage;

	public SynchronizeVersionsWizard(FeatureEditor featureEditor) {
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(MDEUIMessages.VersionSyncWizard_wtitle);
		this.featureEditor = featureEditor;
	}

	public void addPages() {
		mainPage = new SynchronizeVersionsWizardPage(featureEditor);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
