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
package com.siteview.mde.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.ui.IBasePluginWizard;
import org.eclipse.swt.graphics.Point;

public abstract class WizardNode implements IWizardNode {
	private IWizard wizard;
	private WizardSelectionPage parentWizardPage;
	protected WizardElement wizardElement;

	public WizardNode(WizardSelectionPage parentPage, WizardElement element) {
		parentWizardPage = parentPage;
		wizardElement = element;
	}

	protected abstract IBasePluginWizard createWizard() throws CoreException;

	public void dispose() {
		if (wizard != null) {
			wizard.dispose();
			wizard = null;
		}
	}

	public WizardElement getElement() {
		return wizardElement;
	}

	public Point getExtent() {
		return new Point(-1, -1);
	}

	public IWizard getWizard() {
		if (wizard != null)
			return wizard; // we've already created it

		IBasePluginWizard pluginWizard;
		try {
			pluginWizard = createWizard(); // create instance of target wizard
		} catch (CoreException e) {
			if (parentWizardPage instanceof BaseWizardSelectionPage)
				((BaseWizardSelectionPage) parentWizardPage).setDescriptionText(""); //$NON-NLS-1$
			MDEPlugin.logException(e);
			parentWizardPage.setErrorMessage(MDEUIMessages.Errors_CreationError_NoWizard);
			MessageDialog.openError(parentWizardPage.getWizard().getContainer().getShell(), MDEUIMessages.Errors_CreationError, MDEUIMessages.Errors_CreationError_NoWizard);
			return null;
		}
		wizard = pluginWizard;
		//wizard.setUseContainerState(false);
		return wizard;
	}

	public boolean isContentCreated() {
		return wizard != null;
	}
}
