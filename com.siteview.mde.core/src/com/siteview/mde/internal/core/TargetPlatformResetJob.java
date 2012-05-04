/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pierre Carlson <mpcarl@us.ibm.com> - bug 233029
 *******************************************************************************/
package com.siteview.mde.internal.core;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

import com.siteview.mde.core.monitor.IMonitorModelBase;

public class TargetPlatformResetJob extends Job {

	private MDEState fState;

	public TargetPlatformResetJob(MDEState newState) {
		super(MDECoreMessages.TargetPlatformResetJob_resetTarget);
		fState = newState;
		setRule(ResourcesPlugin.getWorkspace().getRoot());
	}

	protected IStatus run(IProgressMonitor monitor) {
		EclipseHomeInitializer.resetEclipseHomeVariable();
		MDECore.getDefault().getSourceLocationManager().reset();
		MDECore.getDefault().getJavadocLocationManager().reset();
		IMonitorModelBase[] models = fState.getTargetModels();
		removeDisabledBundles(models);
		MonitorModelManager manager = MDECore.getDefault().getModelManager();
		manager.getExternalModelManager().setModels(models);
		// trigger Extension Registry reloaded before resetState call so listeners can update their extensions points accurately when target is reloaded
		MDECore.getDefault().getExtensionsRegistry().targetReloaded();
		manager.resetState(fState);
		MDECore.getDefault().getFeatureModelManager().targetReloaded();
		monitor.done();
		return Status.OK_STATUS;
	}

	private void removeDisabledBundles(IMonitorModelBase[] models) {
		int number = models.length;
		for (int i = 0; i < models.length; i++) {
			if (!models[i].isEnabled()) {
				fState.removeBundleDescription(models[i].getBundleDescription());
				number -= 1;
			}
		}
		if (number < models.length)
			fState.resolveState(true);
	}

}
