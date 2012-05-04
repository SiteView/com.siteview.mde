/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.product;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.core.IModel;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.wizards.ResizableWizardDialog;
import com.siteview.mde.internal.ui.wizards.exports.ProductExportWizard;
import org.eclipse.ui.PlatformUI;

public class ProductExportAction extends Action {

	private IProject fProject;

	private IStructuredSelection fSelection;

	public ProductExportAction(MDEFormEditor editor) {
		IResource resource = null;
		if (editor != null)
			resource = ((IModel) editor.getAggregateModel()).getUnderlyingResource();
		fSelection = resource != null ? new StructuredSelection(resource) : new StructuredSelection();
		fProject = editor.getCommonProject();
	}

	public ProductExportAction(IStructuredSelection selection) {
		fSelection = selection;
		fProject = null;
	}

	public void run() {
		ProductExportWizard wizard = new ProductExportWizard(fProject);
		wizard.init(PlatformUI.getWorkbench(), fSelection);
		WizardDialog wd = new ResizableWizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		notifyResult(wd.open() == Window.OK);
	}

}
