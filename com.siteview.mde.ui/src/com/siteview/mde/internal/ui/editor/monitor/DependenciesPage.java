/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import java.util.ArrayList;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;

public class DependenciesPage extends MDEFormPage {

	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$

	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.DependenciesPage_tabName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES;
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		boolean isBundle = isBundle();
		ScrolledForm form = managedForm.getForm();
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_REQ_PLUGINS_OBJ));
		form.setText(MDEUIMessages.DependenciesPage_title);
		Composite body = form.getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(isBundle, 2));
		Composite left, right;
		FormToolkit toolkit = managedForm.getToolkit();
		left = toolkit.createComposite(body, SWT.NONE);
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		left.setLayoutData(new GridData(GridData.FILL_BOTH));
		right = toolkit.createComposite(body, SWT.NONE);
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		right.setLayoutData(new GridData(GridData.FILL_BOTH));

		RequiresSection requiresSection = new RequiresSection(this, left, getRequiredSectionLabels());
		managedForm.addPart(requiresSection);

		DependencyAnalysisSection section;
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 150;
		if (isBundle) {
			ImportPackageSection importPackageSection = new ImportPackageSection(this, right);
			importPackageSection.getSection().descriptionVerticalSpacing = requiresSection.getSection().getTextClientHeightDifference();
			managedForm.addPart(importPackageSection);
			if (getModel().isEditable())
				managedForm.addPart(new DependencyManagementSection(this, left));
			else
				gd.horizontalSpan = 2;
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.COMPACT);
		} else {
			// No MANIFEST.MF (not a Bundle), 3.0 timeframe
			MatchSection matchSection = new MatchSection(this, right, true);
			matchSection.getSection().descriptionVerticalSpacing = requiresSection.getSection().getTextClientHeightDifference();
			managedForm.addPart(matchSection);
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.EXPANDED);
		}
		section.getSection().setLayoutData(gd);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES);
	}

	private boolean isBundle() {
		return getMDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	private String[] getRequiredSectionLabels() {
		ArrayList labels = new ArrayList();
		labels.add(MDEUIMessages.RequiresSection_add);
		labels.add(MDEUIMessages.RequiresSection_delete);
		labels.add(MDEUIMessages.RequiresSection_up);
		labels.add(MDEUIMessages.RequiresSection_down);
		if (isBundle())
			labels.add(MDEUIMessages.DependenciesPage_properties);
		return (String[]) labels.toArray(new String[labels.size()]);
	}

}
