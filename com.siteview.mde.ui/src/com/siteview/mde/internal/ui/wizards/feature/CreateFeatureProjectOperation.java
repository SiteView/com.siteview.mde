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
package com.siteview.mde.internal.ui.wizards.feature;

import com.siteview.mde.core.monitor.IMonitorBase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import com.siteview.mde.internal.core.feature.FeaturePlugin;
import com.siteview.mde.internal.core.feature.WorkspaceFeatureModel;
import com.siteview.mde.internal.core.ifeature.IFeature;
import com.siteview.mde.internal.core.ifeature.IFeaturePlugin;
import com.siteview.mde.internal.core.util.CoreUtility;
import org.eclipse.swt.widgets.Shell;

public class CreateFeatureProjectOperation extends AbstractCreateFeatureOperation {

	protected IMonitorBase[] fPlugins;

	public CreateFeatureProjectOperation(IProject project, IPath location, FeatureData featureData, IMonitorBase[] plugins, Shell shell) {
		super(project, location, featureData, shell);
		fPlugins = plugins != null ? plugins : new IMonitorBase[0];
	}

	protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
		IFeaturePlugin[] added = new IFeaturePlugin[fPlugins.length];
		for (int i = 0; i < fPlugins.length; i++) {
			IMonitorBase plugin = fPlugins[i];
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(plugin);
			fplugin.setVersion("0.0.0"); //$NON-NLS-1$
			fplugin.setUnpack(CoreUtility.guessUnpack(plugin.getMonitorModel().getBundleDescription()));
			added[i] = fplugin;
		}
		feature.addPlugins(added);
	}

}
