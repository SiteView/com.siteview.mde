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
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ClasspathComputer;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class UpdateClasspathResolution extends AbstractPDEMarkerResolution {

	public UpdateClasspathResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return MDEUIMessages.UpdateClasspathResolution_label;
	}

	public void run(IMarker marker) {
		IProject project = marker.getResource().getProject();
		IMonitorModelBase model = MonitorRegistry.findModel(project);
		try {
			ClasspathComputer.setClasspath(project, model);
		} catch (CoreException e) {
		}
	}

	protected void createChange(IBaseModel model) {
		// handled by run
	}

}
