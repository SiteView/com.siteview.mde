/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.product;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.iproduct.IProduct;
import com.siteview.mde.internal.core.iproduct.IProductModel;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.ILauncherFormPageHelper;
import com.siteview.mde.internal.ui.editor.MDELauncherFormEditor;
import com.siteview.mde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class ProductLauncherFormPageHelper implements ILauncherFormPageHelper {
	MDELauncherFormEditor fEditor;

	public ProductLauncherFormPageHelper(MDELauncherFormEditor editor) {
		fEditor = editor;
	}

	public Object getLaunchObject() {
		Object file = fEditor.getEditorInput().getAdapter(IFile.class);
		if (file != null)
			return file;
		return ((IProductModel) fEditor.getAggregateModel()).getUnderlyingResource();
	}

	public boolean isOSGi() {
		return false;
	}

	public void preLaunch() {
		handleSynchronize(false);
	}

	public void handleSynchronize(boolean alert) {
		try {
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			IProject project = fEditor.getCommonProject();
			SynchronizationOperation op = new SynchronizationOperation(getProduct(), fEditor.getSite().getShell(), project);
			service.runInUI(service, op, MDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			if (alert)
				MessageDialog.openError(fEditor.getSite().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
		}
	}

	public IProduct getProduct() {
		IBaseModel model = fEditor.getAggregateModel();
		return ((IProductModel) model).getProduct();
	}
}
