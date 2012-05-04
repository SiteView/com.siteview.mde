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
package com.siteview.mde.internal.ui.views.dependencies;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.ui.IMDEUIConstants;
import com.siteview.mde.internal.ui.MDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

public class OpenPluginReferencesAction extends Action {

	private IMonitorModelBase fModel;

	public OpenPluginReferencesAction(IMonitorModelBase base) {
		fModel = base;
	}

	public void run() {
		try {
			IViewPart view = MDEPlugin.getActivePage().showView(IMDEUIConstants.DEPENDENCIES_VIEW_ID);
			((DependenciesView) view).openCallersFor(fModel);
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
	}

}
