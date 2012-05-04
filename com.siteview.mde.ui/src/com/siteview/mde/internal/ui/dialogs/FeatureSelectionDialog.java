/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.siteview.mde.internal.ui.dialogs;

import com.siteview.mde.internal.core.ifeature.IFeatureModel;
import com.siteview.mde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class FeatureSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public FeatureSelectionDialog(Shell parent, IFeatureModel[] models, boolean multiSelect) {
		super(parent, MDEPlugin.getDefault().getLabelProvider());
		setTitle(MDEUIMessages.FeatureSelectionDialog_title);
		setMessage(MDEUIMessages.FeatureSelectionDialog_message);
		setElements(models);
		setMultipleSelection(multiSelect);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.FEATURE_SELECTION);
	}

	public boolean close() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

}
