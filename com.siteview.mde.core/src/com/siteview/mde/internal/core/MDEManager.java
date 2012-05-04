/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;

import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.monitor.ExternalMonitorModelBase;

import org.osgi.framework.Constants;

public class MDEManager {

	public static IFragmentModel[] findFragmentsFor(IMonitorModelBase model) {
		ArrayList result = new ArrayList();
		BundleDescription desc = getBundleDescription(model);
		if (desc != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				IMonitorModelBase candidate = MonitorRegistry.findModel(fragments[i]);
				if (candidate instanceof IFragmentModel) {
					result.add(candidate);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

	public static IMonitorModel findHostFor(IFragmentModel fragment) {
		BundleDescription desc = getBundleDescription(fragment);
		if (desc != null) {
			HostSpecification spec = desc.getHost();
			if (spec != null) {
				IMonitorModelBase host = MonitorRegistry.findModel(spec.getName());
				if (host instanceof IMonitorModel)
					return (IMonitorModel) host;
			}
		}
		return null;
	}

	private static BundleDescription getBundleDescription(IMonitorModelBase model) {
		BundleDescription desc = model.getBundleDescription();

		if (desc == null && model.getUnderlyingResource() != null) {
			// the model may be an editor model. 
			// editor models don't carry a bundle description
			// get the core model counterpart.
			IProject project = model.getUnderlyingResource().getProject();
			IMonitorModelBase coreModel = MonitorRegistry.findModel(project);
			if (coreModel != null)
				desc = coreModel.getBundleDescription();
		}
		return desc;
	}

	public static URL[] getNLLookupLocations(IMonitorModelBase model) {
		ArrayList urls = new ArrayList();
		addNLLocation(model, urls);
		if (model instanceof IMonitorModel) {
			IFragmentModel[] fragments = findFragmentsFor(model);
			for (int i = 0; i < fragments.length; i++) {
				addNLLocation(fragments[i], urls);
			}
		} else if (model instanceof IFragmentModel) {
			IMonitorModel host = findHostFor((IFragmentModel) model);
			if (host != null)
				addNLLocation(host, urls);
		}
		return (URL[]) urls.toArray(new URL[urls.size()]);
	}

	private static void addNLLocation(IMonitorModelBase model, ArrayList urls) {
		URL location = model.getNLLookupLocation();
		if (location != null)
			urls.add(location);
	}

	public static String getBundleLocalization(IMonitorModelBase model) {
		if (model instanceof IBundlePluginModelBase && model.getUnderlyingResource() != null)
			return ((IBundlePluginModelBase) model).getBundleLocalization();

		if (model instanceof ExternalMonitorModelBase)
			return ((ExternalMonitorModelBase) model).getLocalization();

		return Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

}
