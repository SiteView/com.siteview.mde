/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.monitor;

import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.ModelChangedEvent;
import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.*;

public abstract class AbstractMonitorModelBase extends AbstractNLModel implements IMonitorModelBase, IMonitorModelFactory {

	private static final long serialVersionUID = 1L;
	protected IMonitorBase fPluginBase;
	private boolean enabled;
	private BundleDescription fBundleDescription;
	protected boolean fAbbreviated;

	public AbstractMonitorModelBase() {
		super();
	}

	public abstract String getInstallLocation();

	public abstract IMonitorBase createMonitorBase();

	public IExtensions createExtensions() {
		return createMonitorBase();
	}

	public IExtensionsModelFactory getFactory() {
		return this;
	}

	public IMonitorModelFactory getMonitorFactory() {
		return this;
	}

	public IMonitorBase getMonitorBase() {
		return getMonitorBase(true);
	}

	public IMonitorBase getMonitorBase(boolean createIfMissing) {
		if (fPluginBase == null && createIfMissing) {
			fPluginBase = createMonitorBase();
			setLoaded(true);
		}
		return fPluginBase;
	}

	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync, new MonitorHandler(fAbbreviated));
	}

	public void load(InputStream stream, boolean outOfSync, MonitorHandler handler) {
		if (fPluginBase == null)
			fPluginBase = createMonitorBase();

		((MonitorBase) fPluginBase).reset();
		setLoaded(false);
		try {
			SAXParser parser = getSaxParser();
			parser.parse(stream, handler);
			((MonitorBase) fPluginBase).load(handler.getDocumentElement(), handler.getSchemaVersion());
			setLoaded(true);
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
			MDECore.log(e);
		}
	}

	public void load(BundleDescription description, MDEState state) {
		setBundleDescription(description);
		IMonitorBase base = getMonitorBase();
		if (base instanceof Monitor)
			((Monitor) base).load(description, state);
		else
			((Fragment) base).load(description, state);
		updateTimeStamp();
		setLoaded(true);
	}

	public IExtensions getExtensions() {
		return getMonitorBase();
	}

	public IExtensions getExtensions(boolean createIfMissing) {
		return getMonitorBase(createIfMissing);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isFragmentModel() {
		return false;
	}

	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fPluginBase}, null));
	}

	public void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}

	public String toString() {
		IMonitorBase pluginBase = getMonitorBase();
		if (pluginBase != null)
			return pluginBase.getId();
		return super.toString();
	}

	protected abstract void updateTimeStamp();

	public IMonitorAttribute createAttribute(IMonitorElement element) {
		MonitorAttribute attribute = new MonitorAttribute();
		attribute.setModel(this);
		attribute.setParent(element);
		return attribute;
	}

	public IMonitorElement createElement(IMonitorObject parent) {
		MonitorElement element = new MonitorElement();
		element.setModel(this);
		element.setParent(parent);
		return element;
	}

	public IMonitorExtension createExtension() {
		MonitorExtension extension = new MonitorExtension();
		extension.setParent(getMonitorBase());
		extension.setModel(this);
		return extension;
	}

	public IPluginExtensionPoint createExtensionPoint() {
		MonitorExtensionPoint extensionPoint = new MonitorExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getMonitorBase());
		return extensionPoint;
	}

	public IMonitorImport createImport() {
		MonitorImport iimport = new MonitorImport();
		iimport.setModel(this);
		iimport.setParent(getMonitorBase());
		return iimport;
	}

	public IMonitorImport createImport(String pluginId) {
		MonitorImport iimport = new MonitorImport(this, pluginId);
		iimport.setParent(getMonitorBase());
		return iimport;
	}

	public IMonitorLibrary createLibrary() {
		MonitorLibrary library = new MonitorLibrary();
		library.setModel(this);
		library.setParent(getMonitorBase());
		return library;
	}

	public boolean isValid() {
		if (!isLoaded())
			return false;
		if (fPluginBase == null)
			return false;
		return fPluginBase.isValid();
	}

	public boolean isBundleModel() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#dispose()
	 */
	public void dispose() {
		fBundleDescription = null;
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBundleDescription()
	 */
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setBundleDescription(org.eclipse.osgi.service.resolver.BundleDescription)
	 */
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

}
