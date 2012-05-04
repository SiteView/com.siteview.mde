/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - additional enhancements
 *     Bartosz Michalik (bartosz.michalik@gmail.com)
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.product;

import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Configuration page of the product editor
 * 
 * @see ProductEditor
 * @see ConfigurationSection
 * @see PluginConfigurationSection
 * @see PropertiesSection
 */
public class ConfigurationPage extends MDEFormPage {
	public static final String PLUGIN_ID = "plugin-configuration"; //$NON-NLS-1$

	/**
	 * @param productEditor
	 * @param useFeatures
	 */
	public ConfigurationPage(ProductEditor editor, boolean useFeatures) {
		super(editor, PLUGIN_ID, MDEUIMessages.ConfigurationPageMock_pageTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_FEATURE_OBJ));
		form.setText(MDEUIMessages.ConfigurationPageMock_pageTitle);
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CONFIGURATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 2));
		managedForm.addPart(new ConfigurationSection(this, body));
		if (TargetPlatformHelper.getTargetVersion() > 3.4) {
			managedForm.addPart(new PluginConfigurationSection(this, body));
		}
		managedForm.addPart(new PropertiesSection(this, body));
	}
}
