/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.target;

import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.core.target.provisional.ITargetDefinition;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;

/**
 * Target definition wizard used to create a new target definition from
 * the new target platform preference page. 
 */
public class NewTargetDefinitionWizard2 extends Wizard {

	TargetCreationPage fPage;
	ITargetDefinition fDefinition;

	public NewTargetDefinitionWizard2() {
		super();
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_TARGET_WIZ);
		setWindowTitle(MDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		fPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		addPage(fPage);
		addPage(new TargetDefinitionContentPage(null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		return true;
	}

	/**
	 * Returns the target definition created by this wizard.
	 * 
	 * @return target definition or <code>null</code> if none
	 */
	public ITargetDefinition getTargetDefinition() {
		return fDefinition;
	}

	/**
	 * Sets the target being edited.
	 * 
	 * @param definition target
	 */
	public void setTargetDefinition(ITargetDefinition definition) {
		fDefinition = definition;
	}
}
