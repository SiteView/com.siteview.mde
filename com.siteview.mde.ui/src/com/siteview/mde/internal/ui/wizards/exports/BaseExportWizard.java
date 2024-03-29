/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.exports;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.launching.ILaunchingPreferenceConstants;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.*;

public abstract class BaseExportWizard extends Wizard implements IExportWizard, ILaunchingPreferenceConstants {

	protected IStructuredSelection fSelection;

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		MDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings = MDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(MDEUIMessages.BaseExportWizard_wtitle);
	}

	public void dispose() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		String name = getSettingsSectionName();
		IDialogSettings settings = master.getSection(name);
		if (settings == null)
			settings = master.addNewSection(name);
		return settings;
	}

	protected abstract String getSettingsSectionName();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fSelection = selection;
	}

	public boolean performFinish() {
		saveSettings();
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return false;

		if (!performPreliminaryChecks())
			return false;

		if (!confirmDelete())
			return false;

		scheduleExportJob();
		return true;
	}

	protected void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		IWizardPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			((AbstractExportWizardPage) pages[i]).saveSettings(settings);
		}
	}

	protected abstract boolean performPreliminaryChecks();

	protected abstract boolean confirmDelete();

	protected abstract void scheduleExportJob();

}
