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
package com.siteview.mde.internal.core.bundle;

import org.eclipse.core.resources.IFile;

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModel;

public class WorkspaceBundlePluginModel extends WorkspaceBundlePluginModelBase implements IMonitorModel {

	private static final long serialVersionUID = 1L;

	public WorkspaceBundlePluginModel(IFile manifestFile, IFile pluginFile) {
		super(manifestFile, pluginFile);
	}

	public IMonitorBase createMonitorBase() {
		BundlePlugin base = new BundlePlugin();
		base.setModel(this);
		return base;
	}

	public IMonitor getMonitor() {
		return (IMonitor) getMonitorBase();
	}

}
