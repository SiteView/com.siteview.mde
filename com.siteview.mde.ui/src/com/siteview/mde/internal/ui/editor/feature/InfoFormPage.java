/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.feature;

import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.text.ColorManager;
import com.siteview.mde.internal.ui.editor.text.IColorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * 
 * 
 */
public class InfoFormPage extends MDEFormPage {
	public static final String PAGE_ID = "info"; //$NON-NLS-1$
	private IColorManager colorManager = ColorManager.getDefault();
	private InfoSection infoSection;

	/**
	 * 
	 * @param editor
	 * @param title
	 */
	public InfoFormPage(MDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_INFO;
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// Set form header image
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_DOC_SECTION_OBJ));

		infoSection = new InfoSection(this, form.getBody(), colorManager);
		managedForm.addPart(infoSection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_INFO);
		initialize();
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public void initialize() {
		getManagedForm().getForm().setText(MDEUIMessages.FeatureEditor_InfoPage_heading);
	}
}
