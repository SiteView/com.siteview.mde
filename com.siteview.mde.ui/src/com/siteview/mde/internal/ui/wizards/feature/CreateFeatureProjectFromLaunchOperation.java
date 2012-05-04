/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/

package com.siteview.mde.internal.ui.wizards.feature;

import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import com.siteview.mde.launching.IPDELauncherConstants;

import com.siteview.mde.internal.launching.launcher.BundleLauncherHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import com.siteview.mde.internal.core.feature.WorkspaceFeatureModel;
import com.siteview.mde.internal.core.ifeature.IFeature;
import com.siteview.mde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.widgets.Shell;

public class CreateFeatureProjectFromLaunchOperation extends CreateFeatureProjectOperation {

	private ILaunchConfiguration fLaunchConfig;

	public CreateFeatureProjectFromLaunchOperation(IProject project, IPath location, FeatureData featureData, ILaunchConfiguration launchConfig, Shell shell) {
		super(project, location, featureData, null, shell);
		fLaunchConfig = launchConfig;
	}

	protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
		fPlugins = getPlugins();
		super.configureFeature(feature, model);
	}

	private IMonitorBase[] getPlugins() {
		IMonitorModelBase[] models = null;
		try {
			ILaunchConfigurationType type = fLaunchConfig.getType();
			String id = type.getIdentifier();
			// if it is an Eclipse launch
			if (id.equals(EclipseLaunchShortcut.CONFIGURATION_TYPE))
				models = BundleLauncherHelper.getMergedBundles(fLaunchConfig, false);
			// else if it is an OSGi launch
			else if (id.equals(IPDELauncherConstants.OSGI_CONFIGURATION_TYPE))
				models = BundleLauncherHelper.getMergedBundles(fLaunchConfig, true);
		} catch (CoreException e) {
		}
		IMonitorBase[] result = new IMonitorBase[models == null ? 0 : models.length];
		for (int i = 0; i < result.length; i++)
			result[i] = models[i].getMonitorBase(true);
		return result;
	}

}
