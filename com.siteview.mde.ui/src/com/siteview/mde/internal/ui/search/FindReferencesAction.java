/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.search;

import com.siteview.mde.core.monitor.*;

import com.siteview.mde.internal.core.search.*;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;
import org.osgi.framework.Version;

public class FindReferencesAction extends BaseSearchAction {

	private Object fSelectedObject;
	private String fPluginID;

	public FindReferencesAction(Object object) {
		this(object, null);
	}

	public FindReferencesAction(Object object, String pluginID) {
		super(MDEUIMessages.SearchAction_references);
		fSelectedObject = object;
		fPluginID = pluginID;
		initialize();
	}

	private void initialize() {
		setImageDescriptor(MDEPluginImages.DESC_PSEARCH_OBJ);
	}

	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		PluginSearchScope scope = null;
		if (fSelectedObject instanceof IMonitor) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IMonitor) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginExtensionPoint) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			String extensionID = ((IPluginExtensionPoint) fSelectedObject).getId();
			IMonitorModelBase model = ((IPluginExtensionPoint) fSelectedObject).getMonitorModel();

			// Only plug-in xmls created with 3.2 or later support fully qualified names, assume no file version means a > 3.2 version
			String schemaVersion = model.getMonitorBase().getSchemaVersion();
			Version fileVersion = schemaVersion != null ? new Version(schemaVersion) : null;
			if ((fileVersion == null || fileVersion.compareTo(new Version("3.2")) >= 0) && extensionID.indexOf('.') >= 0) { //$NON-NLS-1$
				// Fully qualified extension point, don't prefix with plug-in id
				input.setSearchString(extensionID);
			} else {
				String id = model.getMonitorBase().getId();
				if (id == null || id.trim().length() == 0)
					id = fPluginID;
				if (id == null || id.trim().length() == 0)
					id = "*"; //$NON-NLS-1$
				input.setSearchString(id + "." + extensionID); //$NON-NLS-1$
			}
			scope = new ExtensionPluginSearchScope(input);
		} else if (fSelectedObject instanceof IMonitorImport) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IMonitorImport) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IMonitorExtension) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			input.setSearchString(((IMonitorExtension) fSelectedObject).getPoint());
			input.setSearchScope(new ExtensionPluginSearchScope(input));
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchScope((scope == null) ? new PluginSearchScope() : scope);
		return new PluginSearchQuery(input);
	}
}
