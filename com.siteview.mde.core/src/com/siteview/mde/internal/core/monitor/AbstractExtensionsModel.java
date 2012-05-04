/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.ModelChangedEvent;
import com.siteview.mde.core.monitor.IExtensions;
import com.siteview.mde.core.monitor.IExtensionsModel;
import com.siteview.mde.core.monitor.IExtensionsModelFactory;
import com.siteview.mde.core.monitor.IMonitorObject;
import com.siteview.mde.core.monitor.IMonitorAttribute;
import com.siteview.mde.core.monitor.IMonitorElement;
import com.siteview.mde.core.monitor.IMonitorExtension;
import com.siteview.mde.core.monitor.IPluginExtensionPoint;
import com.siteview.mde.internal.core.AbstractNLModel;
import com.siteview.mde.internal.core.MDEState;
import org.xml.sax.SAXException;

public abstract class AbstractExtensionsModel extends AbstractNLModel implements IExtensionsModel, IExtensionsModelFactory {

	private static final long serialVersionUID = 1L;
	protected Extensions fExtensions;

	public IExtensionsModelFactory getFactory() {
		return this;
	}

	protected Extensions createExtensions() {
		Extensions extensions = new Extensions(!isEditable());
		extensions.setModel(this);
		return extensions;
	}

	public IExtensions getExtensions() {
		return getExtensions(true);
	}

	public IExtensions getExtensions(boolean createIfMissing) {
		if (fExtensions == null && createIfMissing) {
			fExtensions = createExtensions();
			setLoaded(true);
		}
		return fExtensions;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		URL locations[] = {getNLLookupLocation()};
		return locations;
	}

	public synchronized void load(InputStream stream, boolean outOfSync) throws CoreException {

		if (fExtensions == null) {
			fExtensions = createExtensions();
			fExtensions.setModel(this);
		}
		fExtensions.reset();
		setLoaded(false);
		try {
			// TODO: possibly remove this work.
			// Need a good way to "setLoaded()" value
			// With the way we do it, we might be able to claim it is always loaded.
			SAXParser parser = getSaxParser();
			MonitorHandler handler = new MonitorHandler(true);
			parser.parse(stream, handler);
			fExtensions.load(handler.getSchemaVersion());
			setLoaded(true);
			if (!outOfSync)
				updateTimeStamp();
		} catch (ParserConfigurationException e) {
		} catch (SAXException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}
	}

	// loaded from workspace when creating workspace model
	public void load(BundleDescription desc, MDEState state) {
		fExtensions = createExtensions();
		fExtensions.setModel(this);
		updateTimeStamp();
		setLoaded(true);
	}

	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fExtensions}, null));
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
		extension.setParent(getExtensions());
		extension.setModel(this);
		return extension;
	}

	public IPluginExtensionPoint createExtensionPoint() {
		MonitorExtensionPoint extensionPoint = new MonitorExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getExtensions());
		return extensionPoint;
	}

	public boolean isValid() {
		if (!isLoaded())
			return false;
		if (fExtensions == null)
			return false;
		return fExtensions.isValid();
	}
}
