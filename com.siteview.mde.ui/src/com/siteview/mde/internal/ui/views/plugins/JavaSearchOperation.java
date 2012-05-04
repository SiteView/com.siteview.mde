/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - bug 201342
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.plugins;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.SearchablePluginsManager;

public class JavaSearchOperation implements IRunnableWithProgress {

	private IMonitorModelBase[] fModels;
	private boolean fAdd;

	public JavaSearchOperation(IMonitorModelBase[] models, boolean add) {
		fModels = models;
		fAdd = add;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			SearchablePluginsManager manager = MDECore.getDefault().getSearchablePluginsManager();
			if (fAdd)
				manager.addToJavaSearch(fModels);
			else
				manager.removeFromJavaSearch(fModels);
		} finally {
			monitor.done();
		}
	}

}
