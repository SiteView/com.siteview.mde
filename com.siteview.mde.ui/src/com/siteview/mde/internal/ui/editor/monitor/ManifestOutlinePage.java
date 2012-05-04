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
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import com.siteview.mde.core.build.IBuildEntry;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.build.BuildPage;

public class ManifestOutlinePage extends FormOutlinePage {
	/**
	 * @param editor
	 */
	public ManifestOutlinePage(MDEFormEditor editor) {
		super(editor);
	}

	protected Object[] getChildren(Object parent) {
		if (parent instanceof MDEFormPage) {
			MDEFormPage page = (MDEFormPage) parent;
			IMonitorModelBase model = (IMonitorModelBase) page.getModel();
			if (model != null && model.isValid()) {
				IMonitorBase pluginBase = model.getMonitorBase();
				if (page.getId().equals(DependenciesPage.PAGE_ID))
					return pluginBase.getImports();
				if (page.getId().equals(RuntimePage.PAGE_ID))
					return pluginBase.getLibraries();
				if (page.getId().equals(ExtensionsPage.PAGE_ID))
					return pluginBase.getExtensions();
				if (page.getId().equals(ExtensionPointsPage.PAGE_ID))
					return pluginBase.getExtensionPoints();
			}
		}
		return new Object[0];
	}

	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IMonitorImport || item instanceof ImportPackageObject)
			pageId = DependenciesPage.PAGE_ID;
		else if (item instanceof IMonitorLibrary || item instanceof ExportPackageObject || item instanceof PackageFriend)
			pageId = RuntimePage.PAGE_ID;
		else if (item instanceof IMonitorExtension)
			pageId = ExtensionsPage.PAGE_ID;
		else if (item instanceof IPluginExtensionPoint)
			pageId = ExtensionPointsPage.PAGE_ID;
		else if (item instanceof IBuildEntry)
			pageId = BuildPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}
}
