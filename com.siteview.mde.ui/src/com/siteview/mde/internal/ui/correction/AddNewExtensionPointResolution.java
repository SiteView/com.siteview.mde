/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.SWTUtil;
import com.siteview.mde.internal.ui.wizards.extension.NewExtensionPointWizard;
import org.eclipse.ui.IEditorPart;

public class AddNewExtensionPointResolution extends AbstractPDEMarkerResolution {

	public AddNewExtensionPointResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return MDEUIMessages.AddNewExtensionPointResolution_description;
	}

	protected void createChange(IBaseModel model) {
		IEditorPart part = MDEPlugin.getActivePage().getActiveEditor();
		if (part instanceof ManifestEditor) {
			ManifestEditor editor = (ManifestEditor) part;
			IBaseModel base = editor.getAggregateModel();
			if (base instanceof IBundlePluginModelBase) {
				IBundlePluginModelBase pluginModel = (IBundlePluginModelBase) base;
				NewExtensionPointWizard wizard = new NewExtensionPointWizard(pluginModel.getUnderlyingResource().getProject(), pluginModel, editor) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				dialog.open();
			}
		}
	}
}