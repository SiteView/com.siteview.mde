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
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class RuntimePage extends MDEFormPage {
	public static final String PAGE_ID = "runtime"; //$NON-NLS-1$

	public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.RuntimePage_tabName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		if (((IMonitorModelBase) getMDEEditor().getAggregateModel()).isFragmentModel())
			return IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME;
		return IHelpContextIds.MANIFEST_PLUGIN_RUNTIME;
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_JAVA_LIB_OBJ));
		form.setText(MDEUIMessages.ManifestEditor_RuntimeForm_title);
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 2));

		if (isBundle()) {
			mform.addPart(new ExportPackageSection(this, form.getBody()));
			if (((ManifestEditor) getEditor()).isEquinox())
				mform.addPart(new ExportPackageVisibilitySection(this, form.getBody()));
			mform.addPart(new LibrarySection(this, form.getBody()));
		} else {
			// No MANIFEST.MF (not a Bundle)
			// Create a new plug-in project targeted for 3.0 using the hello 
			// world template to see this section (no MANIFEST.MF is created)			
			mform.addPart(new LibrarySection(this, form.getBody()));
			mform.addPart(new LibraryVisibilitySection(this, form.getBody()));
		}

		if (((IMonitorModelBase) getMDEEditor().getAggregateModel()).isFragmentModel())
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}

	private boolean isBundle() {
		return getMDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

}
