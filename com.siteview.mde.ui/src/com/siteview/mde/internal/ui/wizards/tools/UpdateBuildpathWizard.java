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

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.ui.*;
import org.eclipse.ui.PlatformUI;

public class UpdateBuildpathWizard extends Wizard {
	private UpdateBuildpathWizardPage page1;
	private IMonitorModelBase[] fSelected;
	private IMonitorModelBase[] fUnupdated;
	private static final String STORE_SECTION = "UpdateBuildpathWizard"; //$NON-NLS-1$

	public UpdateBuildpathWizard(IMonitorModelBase[] models, IMonitorModelBase[] selected) {
		IDialogSettings masterSettings = MDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(MDEUIMessages.UpdateBuildpathWizard_wtitle);
		setNeedsProgressMonitor(true);
		this.fSelected = selected;
		this.fUnupdated = models;
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	public boolean performFinish() {
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return false;

		Object[] finalSelected = page1.getSelected();
		page1.storeSettings();
		IMonitorModelBase[] modelArray = new IMonitorModelBase[finalSelected.length];
		System.arraycopy(finalSelected, 0, modelArray, 0, finalSelected.length);
		Job j = new UpdateClasspathJob(modelArray);
		j.setUser(true);
		j.schedule();
		return true;
	}

	public void addPages() {
		page1 = new UpdateBuildpathWizardPage(fUnupdated, fSelected);
		addPage(page1);
	}
}
