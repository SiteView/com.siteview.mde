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
package com.siteview.mde.internal.core.monitor;

import org.eclipse.core.resources.IFile;

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModel;

public class WorkspaceMonitorModel extends WorkspaceMonitorModelBase implements IMonitorModel {

	private static final long serialVersionUID = 1L;

	public WorkspaceMonitorModel(IFile file, boolean abbreviated) {
		super(file, abbreviated);
	}

	public IMonitorBase createMonitorBase() {
		Monitor plugin = new Monitor(!isEditable());
		plugin.setModel(this);
		return plugin;
	}

	public IMonitor getMonitor() {
		return (IMonitor) getMonitorBase();
	}
}
