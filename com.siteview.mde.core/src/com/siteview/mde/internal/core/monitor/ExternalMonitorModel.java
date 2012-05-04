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

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModel;

public class ExternalMonitorModel extends ExternalMonitorModelBase implements IMonitorModel {

	private static final long serialVersionUID = 1L;

	public IMonitorBase createMonitorBase() {
		MonitorBase base = new Monitor(true);
		base.setModel(this);
		return base;
	}

	public IMonitor getMonitor() {
		return (IMonitor) getMonitorBase();
	}

}
