/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import java.util.Vector;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ILabelProvider;
import com.siteview.mde.internal.core.util.PDEJavaHelper;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class PackageSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public PackageSelectionDialog(Shell parent, ILabelProvider renderer, IJavaProject jProject, Vector existingPackages, boolean allowJava) {
		super(parent, renderer);
		//setElements(jProject, existingPackages, allowJava);
		setElements(PDEJavaHelper.getPackageFragments(jProject, existingPackages, allowJava));
		setMultipleSelection(true);
		setMessage(MDEUIMessages.PackageSelectionDialog_label);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ElementListSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(MDEUIMessages.PackageSelectionDialog_title);
		return control;
	}
}
