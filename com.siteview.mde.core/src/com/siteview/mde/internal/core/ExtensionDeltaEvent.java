/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import com.siteview.mde.core.monitor.IMonitorModelBase;

public class ExtensionDeltaEvent implements IExtensionDeltaEvent {

	private IMonitorModelBase[] added;
	private IMonitorModelBase[] changed;
	private IMonitorModelBase[] removed;
	private int types;

	public ExtensionDeltaEvent(int types, IMonitorModelBase[] added, IMonitorModelBase[] removed, IMonitorModelBase[] changed) {
		this.types = types;
		this.added = added;
		this.changed = changed;
		this.removed = removed;
	}

	public IMonitorModelBase[] getAddedModels() {
		return added;
	}

	public IMonitorModelBase[] getChangedModels() {
		return changed;
	}

	public IMonitorModelBase[] getRemovedModels() {
		return removed;
	}

	public int getEventTypes() {
		return types;
	}

}
