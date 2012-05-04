/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.actions;

import com.siteview.mde.internal.core.text.monitor.MonitorBaseNode;

import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.text.bundle.Bundle;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.ui.IFileEditorInput;

public class FormatOperation implements IRunnableWithProgress {

	private Object[] fObjects;

	public FormatOperation(Object[] objects) {
		fObjects = objects;
	}

	public void run(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
		mon.beginTask(MDEUIMessages.FormatManifestOperation_task, fObjects.length);
		for (int i = 0; !mon.isCanceled() && i < fObjects.length; i++) {
			Object obj = fObjects[i];
			if (obj instanceof IFileEditorInput)
				obj = ((IFileEditorInput) obj).getFile();
			if (obj instanceof IFile) {
				mon.subTask(NLS.bind(MDEUIMessages.FormatManifestOperation_subtask, ((IFile) obj).getFullPath().toString()));
				format((IFile) obj, mon);
			}
			mon.worked(1);
		}
	}

	public static void format(IFile file, IProgressMonitor mon) {
		PDEModelUtility.modifyModel(new ModelModification(file) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					IBundleModel bundleModel = ((IBundlePluginModelBase) model).getBundleModel();
					if (bundleModel.getBundle() instanceof Bundle)
						formatBundle((Bundle) bundleModel.getBundle());
				} else if (model instanceof IMonitorModelBase) {
					IMonitorBase pluginModel = ((IMonitorModelBase) model).getMonitorBase();
					if (pluginModel instanceof MonitorBaseNode)
						formatXML((MonitorBaseNode) pluginModel);
				}
			}

			public boolean saveOpenEditor() {
				return false;
			}
		}, mon);
	}

	private static void formatBundle(Bundle bundle) {
		Iterator headers = bundle.getHeaders().values().iterator();
		while (headers.hasNext())
			((IManifestHeader) headers.next()).update(true);
		BundleModel model = (BundleModel) bundle.getModel();
		model.adjustOffsets(model.getDocument());
	}

	private static void formatXML(MonitorBaseNode node) {
		// TODO Auto-generated method stub

	}
}
