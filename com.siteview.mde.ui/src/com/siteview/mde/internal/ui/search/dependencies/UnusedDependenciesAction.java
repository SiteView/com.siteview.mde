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
package com.siteview.mde.internal.ui.search.dependencies;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.progress.IProgressConstants;

public class UnusedDependenciesAction extends Action {

	private IMonitorModelBase fModel;

	private boolean fReadOnly;

	public UnusedDependenciesAction(IMonitorModelBase model, boolean readOnly) {
		fModel = model;
		setText(MDEUIMessages.UnusedDependencies_action);
		fReadOnly = readOnly;
	}

	public void run() {
		Job job = new UnusedDependenciesJob(MDEUIMessages.UnusedDependenciesAction_jobName, fModel, fReadOnly);
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, MDEPluginImages.DESC_PSEARCH_OBJ.createImage());
		job.schedule();
	}

}
