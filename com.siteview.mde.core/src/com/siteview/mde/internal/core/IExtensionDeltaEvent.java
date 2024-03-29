/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import com.siteview.mde.core.monitor.IMonitorModelBase;

public interface IExtensionDeltaEvent {
	/**
	 * Event is sent after the models have been added.
	 */
	int MODELS_ADDED = 0x1;
	/**
	 * Event is sent before the models will be removed.
	 */
	int MODELS_REMOVED = 0x2;
	/**
	 * Event is sent after the models have been changed.
	 */
	int MODELS_CHANGED = 0x4;

	public IMonitorModelBase[] getAddedModels();

	public IMonitorModelBase[] getChangedModels();

	public IMonitorModelBase[] getRemovedModels();

	public int getEventTypes();

}
