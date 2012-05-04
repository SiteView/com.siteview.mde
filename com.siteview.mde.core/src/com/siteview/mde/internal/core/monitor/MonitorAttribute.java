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

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;

import com.siteview.mde.core.monitor.IMonitorAttribute;
import com.siteview.mde.internal.core.ischema.ISchema;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import org.w3c.dom.Node;

public class MonitorAttribute extends MonitorObject implements IMonitorAttribute {
	private static final long serialVersionUID = 1L;

	protected String fValue;

	private transient ISchemaAttribute attributeInfo;

	public MonitorAttribute() {
	}

	MonitorAttribute(IMonitorAttribute attribute) {
		setModel(attribute.getModel());
		setParent(attribute.getParent());
		fName = attribute.getName();
		fValue = attribute.getValue();
		this.attributeInfo = ((MonitorAttribute) attribute).getAttributeInfo();
	}

	public Object clone() {
		return new MonitorAttribute(this);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IMonitorAttribute) {
			IMonitorAttribute target = (IMonitorAttribute) obj;
			if (target.getModel().equals(getModel()))
				return false;
			if (stringEqualWithNull(getName(), target.getName()) && stringEqualWithNull(getValue(), target.getValue()))
				return true;
		}
		return false;
	}

	public ISchemaAttribute getAttributeInfo() {
		if (attributeInfo != null) {
			ISchema schema = attributeInfo.getSchema();
			if (schema.isDisposed()) {
				attributeInfo = null;
			}
		}
		if (attributeInfo == null) {
			MonitorElement element = (MonitorElement) getParent();
			ISchemaElement elementInfo = (ISchemaElement) element.getElementInfo();
			if (elementInfo != null) {
				attributeInfo = elementInfo.getAttribute(getName());
			}
		}
		return attributeInfo;
	}

	public String getValue() {
		return fValue;
	}

	void load(Node node) {
		fName = node.getNodeName();
		fValue = node.getNodeValue();
	}

	void load(String name, String value) {
		fName = name;
		fValue = value;
	}

	public void setAttributeInfo(ISchemaAttribute newAttributeInfo) {
		attributeInfo = newAttributeInfo;
	}

	public void setValue(String newValue) throws CoreException {
		ensureModelEditable();
		String oldValue = fValue;
		fValue = newValue;
		AttributeChangedEvent e = new AttributeChangedEvent(getModel(), getParent(), this, oldValue, newValue);
		fireModelChanged(e);
	}

	public void write(String indent, PrintWriter writer) {
		if (fValue == null)
			return;
		writer.print(indent);
		writer.print(getName() + "=\"" + getWritableString(fValue) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
