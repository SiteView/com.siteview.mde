/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.bundle;

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModel;

public class BundlePluginModel extends BundlePluginModelBase implements IBundlePluginModel {

	private static final long serialVersionUID = 1L;

	public IMonitorBase createMonitorBase() {
		BundlePlugin bplugin = new BundlePlugin();
		bplugin.setModel(this);
		return bplugin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModel#getPlugin()
	 */
	public IMonitor getMonitor() {
		return (IMonitor) getMonitorBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return false;
	}
}
