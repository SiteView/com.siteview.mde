/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.build;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.*;
import com.siteview.mde.internal.core.ClasspathHelper;
import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.core.builders.BuildErrorReporter;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class BuildPluginAction extends BaseBuildAction {

	protected void makeScripts(IProgressMonitor monitor) throws InvocationTargetException, CoreException {

		IProject project = fManifestFile.getProject();
		IMonitorModelBase model = MonitorRegistry.findModel(project);
		BuildErrorReporter buildErrorReporter = new BuildErrorReporter(fManifestFile);
		IResource buildXML = project.findMember("build.xml"); //$NON-NLS-1$
		if (buildXML != null && buildXML.exists() == true && buildErrorReporter.isCustomBuild() == true) {
			IStatus warnFail = new Status(IStatus.WARNING, model.getMonitorBase().getId(), MDEUIMessages.BuildPluginAction_WarningCustomBuildExists);
			throw new CoreException(warnFail);
		}
		BuildScriptGenerator generator = new BuildScriptGenerator();
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());
		AbstractScriptGenerator.setForceUpdateJar(AbstractScriptGenerator.getForceUpdateJarFormat());
		AbstractScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos());

		generator.setWorkingDirectory(project.getLocation().toOSString());
		String url = ClasspathHelper.getDevEntriesProperties(project.getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(url);
		generator.setPDEState(TargetPlatformHelper.getState());
		generator.setNextId(TargetPlatformHelper.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()));
		generator.setBuildingOSGi(true);
		// allow binary cycles
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES, "true"); //$NON-NLS-1$
		generator.setImmutableAntProperties(properties);
		if (model != null && model.getMonitorBase().getId() != null) {
			generator.setBundles(new BundleDescription[] {model.getBundleDescription()});
			generator.generate();
		} else {
			MessageDialog.openError(null, MDEUIMessages.BuildPluginAction_ErrorDialog_Title, MDEUIMessages.BuildPluginAction_ErrorDialog_Message);
		}
	}

}
