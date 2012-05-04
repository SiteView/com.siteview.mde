/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.launcher;

import com.siteview.mde.core.monitor.TargetPlatform;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import com.siteview.mde.internal.ui.IHelpContextIds;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class ApplicationSelectionDialog extends TrayDialog {

	private String fMode;
	private Combo applicationCombo;
	private String[] fApplicationNames;
	private String fSelectedApplication;

	public ApplicationSelectionDialog(Shell parentShell, String[] applicationNames, String mode) {
		super(parentShell);
		fMode = mode;
		fApplicationNames = applicationNames;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.LAUNCHER_APPLICATION_SELECTION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 9;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NONE);
		if (fMode.equals(ILaunchManager.DEBUG_MODE))
			label.setText(MDEUIMessages.ApplicationSelectionDialog_debug);
		else
			label.setText(MDEUIMessages.ApplicationSelectionDialog_run);

		applicationCombo = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		applicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		applicationCombo.setItems(fApplicationNames);

		String defaultApp = TargetPlatform.getDefaultApplication();
		if (applicationCombo.indexOf(defaultApp) == -1)
			applicationCombo.add(defaultApp);

		applicationCombo.setText(applicationCombo.getItem(0));

		getShell().setText(fMode.equals(ILaunchManager.DEBUG_MODE) ? MDEUIMessages.ApplicationSelectionDialog_dtitle : MDEUIMessages.ApplicationSelectionDialog_rtitle); // 
		Dialog.applyDialogFont(container);
		return container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		fSelectedApplication = applicationCombo.getText();
		super.okPressed();
	}

	public String getSelectedApplication() {
		if (fSelectedApplication.equals(TargetPlatform.getDefaultApplication()))
			return null;
		return fSelectedApplication;
	}

}
