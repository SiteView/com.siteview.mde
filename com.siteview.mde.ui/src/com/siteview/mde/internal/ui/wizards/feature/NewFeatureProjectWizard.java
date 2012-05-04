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
package com.siteview.mde.internal.ui.wizards.feature;

import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.operation.IRunnableWithProgress;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class NewFeatureProjectWizard extends AbstractNewFeatureWizard {

	private String fId;
	private String fVersion;

	public NewFeatureProjectWizard() {
		super();
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setWindowTitle(MDEUIMessages.NewFeatureWizard_wtitle);
	}

	public void addPages() {
		super.addPages();
		if (hasInterestingProjects()) {
			fSecondPage = new PluginListPage();
			addPage(fSecondPage);
		}
	}

	private boolean hasInterestingProjects() {
		return MonitorRegistry.getActiveModels().length > 0;
	}

	protected AbstractFeatureSpecPage createFirstPage() {
		return new FeatureSpecPage();
	}

	public String getFeatureId() {
		return fId;
	}

	public String getFeatureVersion() {
		return fVersion;
	}

	protected IRunnableWithProgress getOperation() {
		FeatureData data = fProvider.getFeatureData();
		fId = data.id;
		fVersion = data.version;
		ILaunchConfiguration config = fProvider.getLaunchConfiguration();
		if (config == null)
			return new CreateFeatureProjectOperation(fProvider.getProject(), fProvider.getLocationPath(), data, fProvider.getPluginListSelection(), getShell());
		return new CreateFeatureProjectFromLaunchOperation(fProvider.getProject(), fProvider.getLocationPath(), data, config, getShell());
	}

}
