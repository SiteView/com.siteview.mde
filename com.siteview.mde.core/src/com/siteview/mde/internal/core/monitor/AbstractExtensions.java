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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.monitor.IExtensions;
import com.siteview.mde.core.monitor.IMonitorObject;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorExtension;
import com.siteview.mde.core.monitor.IMonitorExtensionPoint;
import com.siteview.mde.core.monitor.ISharedMonitorModel;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.MDECoreMessages;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class AbstractExtensions extends MonitorObject implements IExtensions {

	private static final long serialVersionUID = 1L;

	protected String fSchemaVersion;

	protected List fExtensions = null;
	protected List fExtensionPoints = null;
	boolean fCache = false;

	public AbstractExtensions(boolean readOnly) {
		fCache = !readOnly;
	}

	public void add(IMonitorExtension extension) throws CoreException {
		ensureModelEditable();
		getExtensionsList().add(extension);
		((MonitorExtension) extension).setInTheModel(true);
		((MonitorExtension) extension).setParent(this);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}

	public void add(IMonitorExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		getExtensionPointsList().add(extensionPoint);
		((MonitorExtensionPoint) extensionPoint).setInTheModel(true);
		((MonitorExtensionPoint) extensionPoint).setParent(this);
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
	}

	public IMonitorExtensionPoint[] getExtensionPoints() {
		List extPoints = getExtensionPointsList();
		return (IMonitorExtensionPoint[]) extPoints.toArray(new IMonitorExtensionPoint[extPoints.size()]);
	}

	public IMonitorExtension[] getExtensions() {
		List extensions = getExtensionsList();
		return (IMonitorExtension[]) extensions.toArray(new IMonitorExtension[extensions.size()]);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_EXTENSION_ORDER)) {
			swap((IMonitorExtension) oldValue, (IMonitorExtension) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IExtensions srcExtensions) {
		addArrayToVector(getExtensionsList(), srcExtensions.getExtensions());
		addArrayToVector(getExtensionPointsList(), srcExtensions.getExtensionPoints());
	}

	protected void addArrayToVector(List vector, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			Object obj = array[i];
			if (obj instanceof MonitorObject)
				((MonitorObject) obj).setParent(this);
			vector.add(obj);
		}
	}

	public void remove(IMonitorExtension extension) throws CoreException {
		ensureModelEditable();
		getExtensionsList().remove(extension);
		((MonitorExtension) extension).setInTheModel(false);
		fireStructureChanged(extension, IModelChangedEvent.REMOVE);
	}

	public void remove(IMonitorExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		getExtensionPointsList().remove(extensionPoint);
		((MonitorExtensionPoint) extensionPoint).setInTheModel(false);
		fireStructureChanged(extensionPoint, IModelChangedEvent.REMOVE);
	}

	public void reset() {
		resetExtensions();
	}

	public void resetExtensions() {
		fExtensions = null;
		fExtensionPoints = null;
	}

	public int getExtensionCount() {
		return getExtensionsList().size();
	}

	public int getIndexOf(IMonitorExtension e) {
		return getExtensionsList().indexOf(e);
	}

	public void swap(IMonitorExtension e1, IMonitorExtension e2) throws CoreException {
		ensureModelEditable();
		List extensions = getExtensionsList();
		int index1 = extensions.indexOf(e1);
		int index2 = extensions.indexOf(e2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(MDECoreMessages.AbstractExtensions_extensionsNotFoundException);
		extensions.set(index2, e1);
		extensions.set(index2, e2);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}

	protected void writeChildren(String indent, String tag, Object[] children, PrintWriter writer) {
		writer.println(indent + "<" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.length; i++) {
			IMonitorObject obj = (IMonitorObject) children[i];
			obj.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "</" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean hasRequiredAttributes() {
		// validate extensions
		List extensions = getExtensionsList();
		int size = extensions.size();
		for (int i = 0; i < size; i++) {
			IMonitorExtension extension = (IMonitorExtension) extensions.get(i);
			if (!extension.isValid())
				return false;
		}
		// validate extension points
		List extPoints = getExtensionPointsList();
		size = extPoints.size();
		for (int i = 0; i < size; i++) {
			IMonitorExtensionPoint expoint = (IMonitorExtensionPoint) extPoints.get(i);
			if (!expoint.isValid())
				return false;
		}
		return true;
	}

	public String getSchemaVersion() {
		if (fSchemaVersion == null) {
			// since schema version is only needed on workspace models in very few situations, reading information from the file should suffice
			ISharedMonitorModel model = getModel();
			if (model != null) {
				org.eclipse.core.resources.IResource res = model.getUnderlyingResource();
				if (res != null && res instanceof IFile) {
					try {
						InputStream stream = new BufferedInputStream(((IFile) res).getContents(true));
						MonitorHandler handler = new MonitorHandler(true);
						SAXParserFactory.newInstance().newSAXParser().parse(stream, handler);
						return handler.getSchemaVersion();
					} catch (CoreException e) {
					} catch (SAXException e) {
					} catch (IOException e) {
					} catch (ParserConfigurationException e) {
					}
				}
			}
		}
		return fSchemaVersion;
	}

	public void setSchemaVersion(String schemaVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = fSchemaVersion;
		fSchemaVersion = schemaVersion;
		firePropertyChanged(IMonitorBase.P_SCHEMA_VERSION, oldValue, schemaVersion);
	}

	protected List getExtensionsList() {
		if (fExtensions == null) {
			IMonitorBase base = getMonitorBase();
			if (base != null) {
				if (fCache)
					fExtensions = new ArrayList(Arrays.asList(MDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getMonitorModel())));
				else
					return Arrays.asList(MDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getMonitorModel()));
			} else {
				return Collections.EMPTY_LIST;
			}
		}
		return fExtensions;
	}

	protected List getExtensionPointsList() {
		if (fExtensionPoints == null) {
			IMonitorBase base = getMonitorBase();
			if (base != null) {
				if (fCache)
					fExtensionPoints = new ArrayList(Arrays.asList(MDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getMonitorModel())));
				else
					return Arrays.asList(MDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getMonitorModel()));
			} else {
				return Collections.EMPTY_LIST;
			}
		}
		return fExtensionPoints;
	}

	/*
	 * If this function is used to load the model, the extension registry cache will not be used when querying model.
	 */
	protected void processChild(Node child) {
		String name = child.getNodeName();
		if (fExtensions == null)
			fExtensions = new ArrayList();
		if (fExtensionPoints == null)
			fExtensionPoints = new ArrayList();

		if (name.equals("extension")) { //$NON-NLS-1$
			MonitorExtension extension = new MonitorExtension();
			extension.setModel(getModel());
			extension.setParent(this);
			fExtensions.add(extension);
			extension.setInTheModel(true);
			extension.load(child);
		} else if (name.equals("extension-point")) { //$NON-NLS-1$
			MonitorExtensionPoint point = new MonitorExtensionPoint();
			point.setModel(getModel());
			point.setParent(this);
			point.setInTheModel(true);
			fExtensionPoints.add(point);
			point.load(child);
		}
	}
}
