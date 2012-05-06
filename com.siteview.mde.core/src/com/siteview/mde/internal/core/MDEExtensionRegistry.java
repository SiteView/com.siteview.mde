/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BundleDescription;

import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.monitor.MonitorExtension;
import com.siteview.mde.internal.core.monitor.MonitorExtensionPoint;
import com.siteview.mde.internal.core.util.CoreUtility;

public class MDEExtensionRegistry {

	private Object fMasterKey = new Object();
	private Object fUserKey = new Object();
	private IExtensionRegistry fRegistry = null;
	private MDERegistryStrategy fStrategy = null;

	private IMonitorModelBase[] fModels = null;
	private ArrayList fListeners = new ArrayList();

	private static final String EXTENSION_DIR = ".extensions"; //$NON-NLS-1$

	public MDEExtensionRegistry() {
		if (fStrategy == null) {
			File extensionsDir = new File(MDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR);
			// create the strategy without creating registry.  That way we create the registry at the last possible moment.
			// This way we can listen to events in PDE without creating the registry until we need it.
			fStrategy = new MDERegistryStrategy(new File[] {extensionsDir}, new boolean[] {false}, fMasterKey, this);
		}
	}

	public MDEExtensionRegistry(IMonitorModelBase[] models) {
		fModels = models;
		if (fStrategy == null) {
			File extensionsDir = new File(MDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR);
			// Use TargetPDERegistryStrategy so we don't connect listeners to PluginModelManager.  This is used only in target so we don't need change events.
			fStrategy = new TargetPDERegistryStrategy(new File[] {extensionsDir}, new boolean[] {false}, fMasterKey, this);
		}
	}

	// Methods used to control information/status of Extension Registry

	protected IMonitorModelBase[] getModels() {
		if (fModels == null) {
			// get all workspace and external models.  Make sure workspace models come first
			IMonitorModelBase[] workspaceModels = MonitorRegistry.getWorkspaceModels();
			IMonitorModelBase[] externalModels = MonitorRegistry.getExternalModels();
			IMonitorModelBase[] allModels = new IMonitorModelBase[workspaceModels.length + externalModels.length];
			System.arraycopy(workspaceModels, 0, allModels, 0, workspaceModels.length);
			System.arraycopy(externalModels, 0, allModels, workspaceModels.length, externalModels.length);
			return allModels;
		}
		return fModels;
	}

	public void stop() {
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
		dispose();
	}

	protected synchronized IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry = createRegistry();
			for (ListIterator li = fListeners.listIterator(); li.hasNext();)
				fRegistry.addRegistryChangeListener((IRegistryChangeListener) li.next());
		}
		return fRegistry;
	}

	private IExtensionRegistry createRegistry() {
		return RegistryFactory.createRegistry(fStrategy, fMasterKey, fUserKey);
	}

	public void targetReloaded() {
		// stop old registry (which will write contents to FS) and delete the cache it creates
		// might see if we can dispose of a registry without writing to file system.  NOTE: Don't call stop() because we want to still reuse fStrategy
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
		CoreUtility.deleteContent(new File(MDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR));
		fRegistry = null;
	}

	// dispose of registry without writing contents.
	public void dispose() {
		fStrategy.dispose();
		fRegistry = null;
	}

	// Methods to access data in Extension Registry

	public IMonitorModelBase[] findExtensionPlugins(String pointId, boolean activeOnly) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			// if extension point for extension does not exist, search all plug-ins manually
			return activeOnly ? MonitorRegistry.getActiveModels() : MonitorRegistry.getAllModels();
		}
		IExtension[] exts = point.getExtensions();
		HashSet plugins = new HashSet();
		for (int i = 0; i < exts.length; i++) {
			IMonitorModelBase base = getPlugin(exts[i].getContributor(), false);
			if (base != null && !plugins.contains(base) && (!activeOnly || base.isEnabled()))
				plugins.add(base);
		}
		return (IMonitorModelBase[]) plugins.toArray(new IMonitorModelBase[plugins.size()]);
	}

	/*
	 * Returns IPluginModelBase even if the model is not enabled
	 */
	public IMonitorModelBase findExtensionPointPlugin(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			return null;
		}
		IContributor contributor = point.getContributor();
		return getPlugin(contributor, true);
	}

	private IExtensionPoint getExtensionPoint(String pointId) {
		return getRegistry().getExtensionPoint(pointId);
	}

	/*
	 * Return true if the extension registry has any bundle (enabled/disabled) with the Extension Point specified
	 */
	public boolean hasExtensionPoint(String pointId) {
		//		IExtensionPoint point = getExtensionPoint(pointId);
		//		IPluginModelBase base = (point != null) ? getPlugin(point.getContributor(), false) : null;
		//		return (base != null) ? base.isEnabled() : false;
		return getExtensionPoint(pointId) != null;
	}

	/*
	 * Returns IPluginExtenionPoint for extension point id for any model (both enabled/disabled)
	 */
	public IMonitorExtensionPoint findExtensionPoint(String pointId) {
		IExtensionPoint extPoint = getExtensionPoint(pointId);
		if (extPoint != null) {
			IMonitorModelBase model = getPlugin(extPoint.getContributor(), true);
			if (model != null) {
				IMonitorExtensionPoint[] points = model.getMonitorBase().getExtensionPoints();
				for (int i = 0; i < points.length; i++) {
					IMonitorExtensionPoint point = points[i];
					if (points[i].getFullId().equals(pointId)) {
						return point;
					}
				}
			}
		}
		return null;
	}

	public IMonitorExtension[] findExtensionsForPlugin(IMonitorModelBase base) {
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null)
			return new IMonitorExtension[0];
		IExtension[] extensions = getRegistry().getExtensions(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			MonitorExtension extension = new MonitorExtension(extensions[i]);
			extension.setModel(getExtensionsModel(base));
			extension.setParent(base.getExtensions());
			list.add(extension);
		}
		return (IMonitorExtension[]) list.toArray(new IMonitorExtension[list.size()]);
	}

	public IMonitorExtensionPoint[] findExtensionPointsForPlugin(IMonitorModelBase base) {
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null)
			return new IMonitorExtensionPoint[0];
		IExtensionPoint[] extensions = getRegistry().getExtensionPoints(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			MonitorExtensionPoint point = new MonitorExtensionPoint(extensions[i]);
			point.setModel(getExtensionsModel(base));
			point.setParent(base.getExtensions());
			list.add(point);
		}
		return (IMonitorExtensionPoint[]) list.toArray(new IMonitorExtensionPoint[list.size()]);
	}

	private ISharedMonitorModel getExtensionsModel(IMonitorModelBase base) {
		if (base instanceof IBundlePluginModelBase)
			return ((IBundlePluginModelBase) base).getExtensionsModel();
		return base;
	}

	public IExtension[] findExtensions(String extensionPointId, boolean activeOnly) {
		ArrayList list = new ArrayList();
		IExtensionPoint point = getExtensionPoint(extensionPointId);
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			if (!activeOnly)
				return extensions;
			for (int i = 0; i < extensions.length; i++) {
				IMonitorModelBase base = getPlugin(extensions[i].getContributor(), true);
				if (base != null && base.isEnabled())
					list.add(extensions[i]);
			}
		} else {
			IMonitorModelBase[] bases = activeOnly ? MonitorRegistry.getActiveModels() : MonitorRegistry.getAllModels();
			for (int i = 0; i < bases.length; i++) {
				IContributor contributor = fStrategy.createContributor(bases[i]);
				if (contributor == null)
					continue;
				IExtension[] extensions = getRegistry().getExtensions(contributor);
				for (int j = 0; j < extensions.length; j++) {
					if (extensions[j].getExtensionPointUniqueIdentifier().equals(extensionPointId))
						list.add(extensions[j]);
				}
			}
		}
		return (IExtension[]) list.toArray(new IExtension[list.size()]);
	}

	// make sure we return the right IPluginModelBase when we have multiple versions of a plug-in Id
	private IMonitorModelBase getPlugin(IContributor icontributor, boolean searchAll) {
		if (!(icontributor instanceof RegistryContributor))
			return null;
		RegistryContributor contributor = (RegistryContributor) icontributor;
		long bundleId = Long.parseLong(contributor.getActualId());
		BundleDescription desc = MDECore.getDefault().getModelManager().getState().getState().getBundle(Long.parseLong(contributor.getActualId()));
		if (desc != null)
			return MonitorRegistry.findModel(desc);
		// desc might be null if the workspace contains a plug-in with the same Bundle-SymbolicName
		ModelEntry entry = MonitorRegistry.findEntry(contributor.getActualName());
		if (entry != null) {
			if (!searchAll && entry.getWorkspaceModels().length > 0)
				return null;
			IMonitorModelBase externalModels[] = entry.getExternalModels();
			for (int j = 0; j < externalModels.length; j++) {
				BundleDescription extDesc = externalModels[j].getBundleDescription();
				if (extDesc != null && extDesc.getBundleId() == bundleId)
					return externalModels[j];
			}
		}
		return null;
	}

	// Methods to add/remove listeners

	public void addListener(IRegistryChangeListener listener) {
		fRegistry.addRegistryChangeListener(listener);
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}

	public void removeListener(IRegistryChangeListener listener) {
		fRegistry.removeRegistryChangeListener(listener);
		fListeners.remove(listener);
	}

}
