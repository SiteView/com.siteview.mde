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

import org.eclipse.core.runtime.PlatformObject;

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorModel;
import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

public class MonitorReference extends PlatformObject {
	private String fId;

	private transient IMonitor fPlugin;

	public MonitorReference() {
	}

	public MonitorReference(String id) {
		fId = id;
	}

	public MonitorReference(IMonitor plugin) {
		fId = plugin.getId();
		fPlugin = plugin;
	}

	public String getId() {
		return fId;
	}

	public IMonitor getPlugin() {
		if (fPlugin == null && fId != null) {
			IMonitorModelBase model = MonitorRegistry.findModel(fId);
			fPlugin = model instanceof IMonitorModel ? ((IMonitorModel) model).getMonitor() : null;
		}
		return fPlugin;
	}

	public String toString() {
		if (fPlugin != null) {
			return fPlugin.getTranslatedName();
		}
		return fId != null ? fId : "?"; //$NON-NLS-1$
	}

	public boolean isResolved() {
		return getPlugin() != null;
	}

	/**
	 * @param plugin
	 */
	public void reconnect(IMonitorModelBase model) {
		// Transient Field:  Plugin
		IMonitor plugin = null;
		if (model instanceof IMonitorModel) {
			plugin = ((IMonitorModel) model).getMonitor();
		}
		// It could also be an IFragmentModel
		// Having IPlugin has an instance variable for both models does not
		// make sense
		// If we have a fragment model, leave the plugin as null
		fPlugin = plugin;
	}

}
