/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.product;

import com.siteview.mde.internal.core.monitor.WorkspaceFragmentModel;
import com.siteview.mde.internal.core.monitor.WorkspaceMonitorModel;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.core.text.bundle.BundleSymbolicNameHeader;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Constants;

public abstract class BaseManifestOperation implements IRunnableWithProgress {

	private Shell fShell;
	protected String fPluginId;

	public BaseManifestOperation(Shell shell, String pluginId) {
		fShell = shell;
		fPluginId = pluginId;
	}

	protected Shell getShell() {
		return fShell;
	}

	protected IFile getFile() {
		IMonitorModelBase model = MonitorRegistry.findModel(fPluginId);
		IProject project = model.getUnderlyingResource().getProject();
		return model instanceof IFragmentModel ? PDEProject.getFragmentXml(project) : PDEProject.getPluginXml(project);
	}

	protected IMonitorModelBase getModel(IFile file) {
		if (ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR.equals(file.getName()))
			return new WorkspaceMonitorModel(file, false);
		return new WorkspaceFragmentModel(file, false);
	}

	protected void updateSingleton(IProgressMonitor monitor) throws CoreException {
		IMonitorModelBase plugin = MonitorRegistry.findModel(fPluginId);
		if (plugin instanceof IBundlePluginModel) {
			IFile file = (IFile) plugin.getUnderlyingResource();
			IStatus status = MDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
			if (status.getSeverity() != IStatus.OK)
				throw new CoreException(new Status(IStatus.ERROR, "com.siteview.mde.ui", IStatus.ERROR, NLS.bind(MDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId), null)); //$NON-NLS-1$

			ModelModification mod = new ModelModification(file) {
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (!(model instanceof IBundlePluginModelBase))
						return;
					IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
					IBundle bundle = modelBase.getBundleModel().getBundle();
					IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
					if (header instanceof BundleSymbolicNameHeader) {
						BundleSymbolicNameHeader symbolic = (BundleSymbolicNameHeader) header;
						if (!symbolic.isSingleton())
							symbolic.setSingleton(true);
					}
				}
			};
			PDEModelUtility.modifyModel(mod, null);
		}
	}
}
