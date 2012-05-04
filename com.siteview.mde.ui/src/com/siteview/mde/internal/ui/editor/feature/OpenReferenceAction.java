/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.feature;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.siteview.mde.internal.core.feature.FeatureChild;
import com.siteview.mde.internal.core.feature.FeaturePlugin;
import com.siteview.mde.internal.core.ifeature.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;

public class OpenReferenceAction extends SelectionProviderAction {
	public OpenReferenceAction(ISelectionProvider provider) {
		super(provider, MDEUIMessages.Actions_open_label);
	}

	public void run() {
		IStructuredSelection sel = (IStructuredSelection) getSelection();
		Object obj = sel.getFirstElement();

		if (obj instanceof FeaturePlugin) {
			IMonitorBase base = ((FeaturePlugin) obj).getPluginBase();
			if (base != null)
				ManifestEditor.openPluginEditor((IMonitorModelBase) base.getModel());
		} else if (obj instanceof IFeatureData) {
			IFeatureData data = (IFeatureData) obj;
			String id = data.getId();
			IResource resource = data.getModel().getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				IFile file = project.getFile(id);
				if (file != null && file.exists()) {
					IWorkbenchPage page = MDEPlugin.getActivePage();
					try {
						IDE.openEditor(page, file, true);
					} catch (PartInitException e) {
					}
				}
			}
		} else if (obj instanceof IFeatureChild) {
			IFeatureChild included = (IFeatureChild) obj;
			IFeature feature = ((FeatureChild) included).getReferencedFeature();
			FeatureEditor.openFeatureEditor(feature);
		}
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(!selection.isEmpty());
	}
}
