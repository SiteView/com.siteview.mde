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
package com.siteview.mde.internal.ui.editor.product;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.ifeature.*;
import com.siteview.mde.internal.core.iproduct.*;
import com.siteview.mde.internal.launching.launcher.*;
import com.siteview.mde.internal.ui.*;

public class ProductValidateAction extends Action {

	IProduct fProduct;

	public ProductValidateAction(IProduct product) {
		super(MDEUIMessages.ProductValidateAction_validate, IAction.AS_PUSH_BUTTON);
		setImageDescriptor(MDEPluginImages.DESC_VALIDATE_TOOL);
		fProduct = product;
	}

	public void run() {
		HashMap map = new HashMap();
		if (fProduct.useFeatures()) {
			IFeatureModel[] features = getUniqueFeatures();
			for (int i = 0; i < features.length; i++) {
				addFeaturePlugins(features[i].getFeature(), map);
			}
		} else {
			IProductPlugin[] plugins = fProduct.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getId();
				if (id == null || map.containsKey(id))
					continue;
				IMonitorModelBase model = MonitorRegistry.findModel(id);
				if (model != null && TargetPlatformHelper.matchesCurrentEnvironment(model))
					map.put(id, model);
			}
		}
		try {
			IMonitorModelBase[] models = (IMonitorModelBase[]) map.values().toArray(new IMonitorModelBase[map.size()]);
			LaunchValidationOperation operation = new ProductValidationOperation(models);
			LaunchPluginValidator.runValidationOperation(operation, new NullProgressMonitor());
			if (!operation.hasErrors()) {
				MessageDialog.openInformation(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.PluginStatusDialog_pluginValidation, MDEUIMessages.AbstractLauncherToolbar_noProblems);
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void addFeaturePlugins(IFeature feature, HashMap map) {
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].getId();
			if (id == null || map.containsKey(id))
				continue;
			IMonitorModelBase model = MonitorRegistry.findModel(id);
			if (model != null && TargetPlatformHelper.matchesCurrentEnvironment(model))
				map.put(id, model);
		}
	}

	private IFeatureModel[] getUniqueFeatures() {
		ArrayList list = new ArrayList();
		IProductFeature[] features = fProduct.getFeatures();
		for (int i = 0; i < features.length; i++) {
			String id = features[i].getId();
			String version = features[i].getVersion();
			addFeatureAndChildren(id, version, list);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private void addFeatureAndChildren(String id, String version, List list) {
		FeatureModelManager manager = MDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel(id, version);
		if (model == null || list.contains(model))
			return;

		list.add(model);

		IFeatureChild[] children = model.getFeature().getIncludedFeatures();
		for (int i = 0; i < children.length; i++) {
			addFeatureAndChildren(children[i].getId(), children[i].getVersion(), list);
		}
	}
}
