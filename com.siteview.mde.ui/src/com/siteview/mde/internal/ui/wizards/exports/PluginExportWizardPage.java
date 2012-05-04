/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.exports;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.siteview.mde.core.IModel;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.ui.IHelpContextIds;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.PersistablePluginObject;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

public class PluginExportWizardPage extends BaseExportWizardPage {

	public PluginExportWizardPage(IStructuredSelection selection) {
		super(selection, "pluginExport", //$NON-NLS-1$
				MDEUIMessages.ExportWizard_Plugin_pageBlock);
		setTitle(MDEUIMessages.ExportWizard_Plugin_pageTitle);
	}

	protected Object getInput() {
		return MDECore.getDefault().getModelManager();
	}

	public Object[] getListElements() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList result = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			if (!WorkspaceModelManager.isBinaryProject(projects[i]) && WorkspaceModelManager.isPluginProject(projects[i])) {
				IModel model = MonitorRegistry.findModel(projects[i]);
				if (model != null && isValidModel(model) && hasBuildProperties((IMonitorModelBase) model)) {
					result.add(model);
				}
			}
		}
		return result.toArray();
	}

	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}

	private boolean hasBuildProperties(IMonitorModelBase model) {
		File file = new File(model.getInstallLocation(), ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
		return file.exists();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#isValidModel(org.eclipse.pde.core.IModel)
	 */
	protected boolean isValidModel(IModel model) {
		return model != null && model instanceof IMonitorModelBase;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#findModelFor(org.eclipse.core.runtime.IAdaptable)
	 */
	protected IModel findModelFor(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject) object).getProject();
		if (object instanceof IProject)
			return MonitorRegistry.findModel((IProject) object);
		if (object instanceof PersistablePluginObject) {
			IMonitorModelBase model = MonitorRegistry.findModel(((PersistablePluginObject) object).getPluginID());
			if (model != null && model.getUnderlyingResource() != null) {
				return model;
			}
		}
		return null;
	}

	protected boolean isEnableJarButton() {
		return getSelectedItems().length <= 1;
	}

	protected void adjustAdvancedTabsVisibility() {
		adjustJARSigningTabVisibility();
		pageChanged();
	}
}
