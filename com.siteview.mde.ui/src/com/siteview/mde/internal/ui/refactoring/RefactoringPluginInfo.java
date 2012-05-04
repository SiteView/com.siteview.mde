/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.refactoring;

import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.osgi.service.resolver.BundleDescription;

public class RefactoringPluginInfo extends RefactoringInfo {

	private boolean fRenameProject;

	public boolean isRenameProject() {
		return fRenameProject;
	}

	public void setRenameProject(boolean renameProject) {
		fRenameProject = renameProject;
	}

	public String getCurrentValue() {
		IMonitorModelBase base = getBase();
		if (base == null)
			return null;
		BundleDescription desc = base.getBundleDescription();
		if (desc != null)
			return desc.getSymbolicName();
		IMonitorBase pb = base.getMonitorBase();
		return pb.getId();
	}

	public IMonitorModelBase getBase() {
		return (fSelection instanceof IMonitorModelBase) ? (IMonitorModelBase) fSelection : null;
	}

}
