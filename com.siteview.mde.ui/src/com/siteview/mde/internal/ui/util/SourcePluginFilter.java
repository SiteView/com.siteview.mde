/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.util;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import com.siteview.mde.internal.core.MDEState;
import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.ui.IPreferenceConstants;
import com.siteview.mde.internal.ui.MDEPlugin;

public class SourcePluginFilter extends ViewerFilter {

	private MDEState fState;

	public SourcePluginFilter() {
		fState = TargetPlatformHelper.getPDEState();
	}

	public SourcePluginFilter(MDEState state) {
		fState = state;
	}

	public void setState(MDEState state) {
		fState = state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IMonitorModelBase) {
			IPreferenceStore store = MDEPlugin.getDefault().getPreferenceStore();
			boolean showSourceBundles = store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES);
			if (fState != null && !showSourceBundles) {
				BundleDescription description = ((IMonitorModelBase) element).getBundleDescription();
				if (description != null) {
					return fState.getBundleSourceEntry(description.getBundleId()) == null;
				}
			}
		}
		return true;
	}

}
