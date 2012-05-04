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
package com.siteview.mde.internal.ui.nls;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.refactoring.PDERefactor;

public class ExternalizeStringsWizard extends RefactoringWizard {
	private ExternalizeStringsWizardPage page1;
	private ModelChangeTable fModelChangeTable;

	public ExternalizeStringsWizard(ModelChangeTable changeTable, PDERefactor refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setWindowTitle(MDEUIMessages.ExternalizeStringsWizard_title);
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_EXTSTR_WIZ);
		setNeedsProgressMonitor(true);
		fModelChangeTable = changeTable;
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		page1 = new ExternalizeStringsWizardPage(fModelChangeTable);
		addPage(page1);
	}
}
