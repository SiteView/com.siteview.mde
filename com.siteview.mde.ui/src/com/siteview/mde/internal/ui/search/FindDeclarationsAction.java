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
package com.siteview.mde.internal.ui.search;

import com.siteview.mde.core.monitor.*;

import com.siteview.mde.internal.core.search.*;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;

public class FindDeclarationsAction extends BaseSearchAction {

	private Object fSelectedObject;

	public FindDeclarationsAction(Object object) {
		super(MDEUIMessages.SearchAction_Declaration);
		setImageDescriptor(MDEPluginImages.DESC_PSEARCH_OBJ);
		this.fSelectedObject = object;
	}

	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		PluginSearchScope scope = null;

		if (fSelectedObject instanceof IMonitorImport) {
			input.setSearchString(((IMonitorImport) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IMonitorExtension) {
			input.setSearchString(((IMonitorExtension) fSelectedObject).getPoint());
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			scope = new ExtensionPluginSearchScope(input);
		} else if (fSelectedObject instanceof IMonitor) {
			input.setSearchString(((IMonitor) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IFragment) {
			input.setSearchString(((IFragment) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_FRAGMENT);
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_DECLARATIONS);
		input.setSearchScope((scope == null) ? new PluginSearchScope() : scope);
		return new PluginSearchQuery(input);
	}

}
