/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.plugins;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import com.siteview.mde.internal.ui.MDEPlugin;
import org.eclipse.ui.PlatformUI;

public class AddAllPluginsToJavaSearchHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IMonitorModelBase[] models = MonitorRegistry.getExternalModels();
		IRunnableWithProgress op = new JavaSearchOperation(models, true);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
			MDEPlugin.logException(e);
		} catch (InvocationTargetException e) {
			MDEPlugin.logException(e);
		}
		return null;
	}

}
