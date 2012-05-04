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
package com.siteview.mde.internal.ui.editor.product;

import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class BrandingPage extends MDEFormPage {

	public static final String PAGE_ID = "branding"; //$NON-NLS-1$

	public BrandingPage(FormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.BrandingPage_title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.BRANDING_PAGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_PRODUCT_BRANDING));
		form.setText(MDEUIMessages.BrandingPage_title);
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.BRANDING_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// Sections
		managedForm.addPart(new WindowImagesSection(this, body));
		managedForm.addPart(new AboutSection(this, body));
		managedForm.addPart(new IntroSection(this, body));
	}

}
