/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.targetdefinition;

import com.siteview.mde.internal.core.target.provisional.ITargetDefinition;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Second page in the target definition editor.  Allows for editing of the included bundles in
 * the target
 * @see TargetEditor
 * @see ContentSection
 */
public class ContentPage extends FormPage {

	public static final String PAGE_ID = "content"; //$NON-NLS-1$

	public ContentPage(TargetEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.ContentPage_0);
	}

	/**
	 * @return The target model backing this editor
	 */
	public ITargetDefinition getTarget() {
		return ((TargetEditor) getEditor()).getTarget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(MDEUIMessages.ContentPage_1);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_TARGET_DEFINITION));
		toolkit.decorateFormHeading(form.getForm());
		fillBody(managedForm, toolkit);
		((TargetEditor) getEditor()).contributeToToolbar(managedForm.getForm(), IHelpContextIds.TARGET_EDITOR_CONTENT_PAGE);
		((TargetEditor) getEditor()).addForm(managedForm);
		form.updateToolBar();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.TARGET_EDITOR_CONTENT_PAGE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form, IHelpContextIds.TARGET_EDITOR_CONTENT_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#dispose()
	 */
	public void dispose() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 1));
		managedForm.addPart(new ContentSection(this, body));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#canLeaveThePage()
	 */
	public boolean canLeaveThePage() {
		((TargetEditor) getEditor()).setDirty(isDirty());
		return true;
	}
}
