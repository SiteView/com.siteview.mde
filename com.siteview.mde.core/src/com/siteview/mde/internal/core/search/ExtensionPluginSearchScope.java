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
package com.siteview.mde.internal.core.search;

import java.util.HashSet;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.MDEExtensionRegistry;

public class ExtensionPluginSearchScope extends PluginSearchScope {

	PluginSearchInput fInput = null;

	public ExtensionPluginSearchScope(PluginSearchInput input) {
		super();
		fInput = input;
	}

	public ExtensionPluginSearchScope(int workspaceScope, int externalScope, HashSet selectedResources, PluginSearchInput input) {
		super(workspaceScope, externalScope, selectedResources);
		fInput = input;
	}

	public IMonitorModelBase[] getMatchingModels() {
		if (fInput == null)
			return new IMonitorModelBase[0];
		String pointId = fInput.getSearchString();
		MDEExtensionRegistry registry = MDECore.getDefault().getExtensionsRegistry();
		IMonitorModelBase[] models = null;
		if (fInput.getSearchLimit() == PluginSearchInput.LIMIT_REFERENCES) {
			models = registry.findExtensionPlugins(pointId, false);
		} else {
			IMonitorModelBase base = registry.findExtensionPointPlugin(pointId);
			models = (base == null) ? new IMonitorModelBase[0] : new IMonitorModelBase[] {base};
		}
		return addRelevantModels(models);
	}

}
