/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public abstract class OrientableBlock extends MDEMasterDetailsBlock {

	public OrientableBlock(MDEFormPage page) {
		super(page);
	}

	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();

		Action haction = new Action("hor", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText(MDEUIMessages.DetailsBlock_horizontal);
		haction.setImageDescriptor(MDEPluginImages.DESC_HORIZONTAL);
		haction.setDisabledImageDescriptor(MDEPluginImages.DESC_HORIZONTAL_DISABLED);

		Action vaction = new Action("ver", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText(MDEUIMessages.DetailsBlock_vertical);
		vaction.setImageDescriptor(MDEPluginImages.DESC_VERTICAL);
		vaction.setDisabledImageDescriptor(MDEPluginImages.DESC_VERTICAL_DISABLED);
		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}
}