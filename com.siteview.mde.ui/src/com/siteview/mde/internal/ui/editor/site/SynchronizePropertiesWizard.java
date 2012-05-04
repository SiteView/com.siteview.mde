/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 181878
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.site;

import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.core.isite.ISiteFeature;
import com.siteview.mde.internal.core.isite.ISiteModel;
import com.siteview.mde.internal.ui.*;

public class SynchronizePropertiesWizard extends Wizard {
	private SynchronizePropertiesWizardPage fMainPage;

	private ISiteModel fModel;

	private ISiteFeature[] fSiteFeatures;

	public SynchronizePropertiesWizard(ISiteFeature[] siteFeatures, ISiteModel model) {
		super();
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(MDEUIMessages.SynchronizePropertiesWizard_wtitle);
		fSiteFeatures = siteFeatures;
		fModel = model;
	}

	public void addPages() {
		fMainPage = new SynchronizePropertiesWizardPage(fSiteFeatures, fModel);
		addPage(fMainPage);
	}

	public boolean performFinish() {
		return fMainPage.finish();
	}
}
