/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.product;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.iproduct.IProduct;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.widgets.Shell;

public class SynchronizationOperation extends ProductDefinitionOperation {

	public SynchronizationOperation(IProduct product, Shell shell, IProject project) {
		super(product, getPluginId(product), getProductId(product), product.getApplication(), shell, project);
	}

	private static String getProductId(IProduct product) {
		String full = product.getProductId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(index + 1) : full;
	}

	private static String getPluginId(IProduct product) {
		String full = product.getProductId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(0, index) : full;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IMonitorModelBase model = MonitorRegistry.findModel(fPluginId);
		if (model == null) {
			String message = MDEUIMessages.SynchronizationOperation_noDefiningPlugin;
			throw new InvocationTargetException(createCoreException(message));
		}

		if (model.getUnderlyingResource() == null) {
			String id = model.getMonitorBase().getId();
			String message = MDEUIMessages.SynchronizationOperation_externalPlugin;
			throw new InvocationTargetException(createCoreException(NLS.bind(message, id)));
		}

		super.run(monitor);
	}

	private CoreException createCoreException(String message) {
		IStatus status = new Status(IStatus.ERROR, "com.siteview.mde.ui", IStatus.ERROR, message, null); //$NON-NLS-1$
		return new CoreException(status);
	}

}
