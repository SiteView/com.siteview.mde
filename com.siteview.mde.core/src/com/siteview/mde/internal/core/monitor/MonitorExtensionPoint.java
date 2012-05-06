/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.monitor;

import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionPoint;

import com.siteview.mde.core.monitor.*;

import org.w3c.dom.Node;

public class MonitorExtensionPoint extends IdentifiableMonitorObject implements IMonitorExtensionPoint {

	private static final long serialVersionUID = 1L;

	private IExtensionPoint fPoint = null;

	protected String fSchema;

	public MonitorExtensionPoint() {
	}

	public MonitorExtensionPoint(IExtensionPoint point) {
		fPoint = point;
	}

	public boolean isValid() {
		return getId() != null && getName() != null;
	}

	public String getFullId() {
		if (fPoint != null)
			return fPoint.getUniqueIdentifier();
		String pointId = getId();
		IMonitorModelBase modelBase = getMonitorModel();
		IMonitorBase pluginBase = modelBase.getMonitorBase();
		String schemaVersion = pluginBase.getSchemaVersion();
		if (schemaVersion != null && Double.parseDouble(schemaVersion) >= 3.2) {
			if (pointId.indexOf('.') > 0)
				return pointId;
		}

		if (pluginBase instanceof IFragment)
			return ((IFragment) pluginBase).getPluginId() + '.' + pointId;
		return pluginBase.getId() + '.' + pointId;
	}

	public String getSchema() {
		if (fSchema == null && fPoint != null)
			fSchema = fPoint.getSchemaReference();
		return fSchema;
	}

	void load(Node node) {
		this.fID = getNodeAttribute(node, "id"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fSchema = getNodeAttribute(node, "schema"); //$NON-NLS-1$
		fStartLine = Integer.parseInt(getNodeAttribute(node, "line")); //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof IMonitorExtensionPoint) {
			IMonitorExtensionPoint target = (IMonitorExtensionPoint) obj;
			// Objects from the same model must be
			// binary equal
			if (target.getModel().equals(getModel()))
				return false;
			if (stringEqualWithNull(target.getFullId(), getId()) && stringEqualWithNull(target.getName(), getName()) && stringEqualWithNull(target.getSchema(), getSchema()))
				return true;
		}
		return false;
	}

	public void setSchema(String newSchema) throws CoreException {
		ensureModelEditable();
		String oldValue = fSchema;
		fSchema = newSchema;
		firePropertyChanged(P_SCHEMA, oldValue, fSchema);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_SCHEMA)) {
			setSchema(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<extension-point"); //$NON-NLS-1$
		if (getId() != null)
			writer.print(" id=\"" + getWritableString(getId()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (getName() != null)
			writer.print(" name=\"" + getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (getSchema() != null)
			writer.print(" schema=\"" + getSchema() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}

	public String getName() {
		if (fName == null)
			fName = fPoint.getLabel();
		return fName;
	}

	public String getId() {
		if (fID == null) {
			fID = fPoint.getUniqueIdentifier();
			if (fID != null) {
				String pluginId = getMonitorBase().getId();
				if (fID.startsWith(pluginId)) {
					String sub = fID.substring(pluginId.length());
					if (sub.lastIndexOf('.') == 0)
						fID = sub.substring(1);
				}
			}
		}
		return fID;
	}
}
