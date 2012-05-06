/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.exports;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import com.siteview.mde.core.IModel;
import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.TargetPlatform;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.ifeature.IFeature;
import com.siteview.mde.internal.core.ifeature.IFeatureModel;

public abstract class FeatureBasedExportOperation extends FeatureExportOperation {

	protected String fFeatureLocation;

	public FeatureBasedExportOperation(FeatureExportInfo info, String name) {
		super(info, name);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.exports.FeatureExportOperation#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			createDestination();
			monitor.beginTask("Exporting...", 33); //$NON-NLS-1$
			// create a feature to contain all plug-ins
			String featureID = "com.siteview.mde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;
			String[][] config = new String[][] {{TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()}};
			createFeature(featureID, fFeatureLocation, config, false);
			createBuildPropertiesFile(fFeatureLocation);
			if (fInfo.useJarFormat)
				createPostProcessingFiles();
			IStatus status = testBuildWorkspaceBeforeExport(new SubProgressMonitor(monitor, 10));
			doExport(featureID, null, fFeatureLocation, config, new SubProgressMonitor(monitor, 20));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			return status;
		} catch (IOException e) {
			return new Status(IStatus.ERROR, MDECore.PLUGIN_ID, MDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e);
		} catch (CoreException e) {
			return e.getStatus();
		} catch (InvocationTargetException e) {
			return new Status(IStatus.ERROR, MDECore.PLUGIN_ID, MDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e.getTargetException());
		} finally {
			for (int i = 0; i < fInfo.items.length; i++) {
				if (fInfo.items[i] instanceof IModel)
					try {
						deleteBuildFiles(fInfo.items[i]);
					} catch (CoreException e) {
						MDECore.log(e);
					}
			}
			cleanup(null, new SubProgressMonitor(monitor, 3));
			monitor.done();
		}
	}

	protected abstract void createPostProcessingFiles();

	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR;
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$

		if (fInfo.exportSource && fInfo.exportSourceBundle) {
			prop.put("individualSourceBundles", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			Dictionary environment = new Hashtable(4);
			environment.put("osgi.os", TargetPlatform.getOS()); //$NON-NLS-1$
			environment.put("osgi.ws", TargetPlatform.getWS()); //$NON-NLS-1$
			environment.put("osgi.arch", TargetPlatform.getOSArch()); //$NON-NLS-1$
			environment.put("osgi.nl", TargetPlatform.getNL()); //$NON-NLS-1$

			for (int i = 0; i < fInfo.items.length; i++) {
				if (fInfo.items[i] instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) fInfo.items[i]).getFeature();
					prop.put("generate.feature@" + feature.getId() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					BundleDescription bundle = null;
					if (fInfo.items[i] instanceof IMonitorModelBase) {
						bundle = ((IMonitorModelBase) fInfo.items[i]).getBundleDescription();
					}
					if (bundle == null) {
						if (fInfo.items[i] instanceof BundleDescription)
							bundle = (BundleDescription) fInfo.items[i];
					}
					if (bundle == null)
						continue;
					if (shouldAddPlugin(bundle, environment)) {
						prop.put("generate.plugin@" + bundle.getSymbolicName() + ".source", bundle.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		save(new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR), prop, "Marker File"); //$NON-NLS-1$
	}
}
