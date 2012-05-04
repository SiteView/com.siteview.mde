/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.monitor;

import java.io.PrintWriter;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.ischema.ISchema;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import org.w3c.dom.*;

public class MonitorElement extends MonitorParent implements IMonitorElement {
	private static final long serialVersionUID = 1L;

	static final String ATTRIBUTE_SHIFT = "      "; //$NON-NLS-1$

	static final String ELEMENT_SHIFT = "   "; //$NON-NLS-1$

	private transient ISchemaElement fElementInfo;

	protected String fText;

	protected Hashtable fAttributes;

	private IConfigurationElement fElement = null;

	public MonitorElement() {
	}

	public MonitorElement(IConfigurationElement element) {
		fElement = element;
	}

	MonitorElement(MonitorElement element) {
		setModel(element.getModel());
		setParent(element.getParent());
		fName = element.getName();
		IMonitorAttribute[] atts = element.getAttributes();
		for (int i = 0; i < atts.length; i++) {
			MonitorAttribute att = (MonitorAttribute) atts[i];
			getAttributeMap().put(att.getName(), att.clone());
		}
		fText = element.getText();
		fElementInfo = (ISchemaElement) element.getElementInfo();
		fElement = element.fElement;
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IMonitorElement) {
			IMonitorElement target = (IMonitorElement) obj;
			// Equivalent models must return false to get proper source range selection, see bug 267954.
			if (target.getModel().equals(getModel()))
				return false;
			if (target.getAttributeCount() != getAttributeCount())
				return false;
			IMonitorAttribute tatts[] = target.getAttributes();
			for (int i = 0; i < tatts.length; i++) {
				IMonitorAttribute tatt = tatts[i];
				IMonitorAttribute att = (IMonitorAttribute) getAttributeMap().get(tatt.getName());
				if (att == null || att.equals(tatt) == false)
					return false;
			}
			return super.equals(obj);
		}
		return false;
	}

	public IMonitorElement createCopy() {
		return new MonitorElement(this);
	}

	public IMonitorAttribute getAttribute(String name) {
		return (IMonitorAttribute) getAttributeMap().get(name);
	}

	public IMonitorAttribute[] getAttributes() {
		Collection values = getAttributeMap().values();
		IMonitorAttribute[] result = new IMonitorAttribute[values.size()];
		return (IMonitorAttribute[]) values.toArray(result);
	}

	public int getAttributeCount() {
		// if attributes are initialized, don't load the entire map to find the # of elements
		if (fAttributes == null && fElement != null)
			return fElement.getAttributeNames().length;
		return getAttributeMap().size();
	}

	public Object getElementInfo() {
		if (fElementInfo != null) {
			ISchema schema = fElementInfo.getSchema();
			if (schema.isDisposed()) {
				fElementInfo = null;
			}
		}
		if (fElementInfo == null) {
			IMonitorObject parent = getParent();
			while (parent != null && !(parent instanceof IMonitorExtension)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				MonitorExtension extension = (MonitorExtension) parent;
				ISchema schema = (ISchema) extension.getSchema();
				if (schema != null) {
					fElementInfo = schema.findElement(getName());
				}
			}
		}
		return fElementInfo;
	}

	public String getText() {
		if (fText == null && fElement != null)
			fText = fElement.getValue();
		return fText;
	}

	void load(Node node) {
		fName = node.getNodeName();
		if (fAttributes == null)
			fAttributes = new Hashtable();
		NamedNodeMap attributes = node.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			IMonitorAttribute att = getModel().getFactory().createAttribute(this);
			((MonitorAttribute) att).load(attribute);
			((MonitorAttribute) att).setInTheModel(true);
			this.fAttributes.put(attribute.getNodeName(), att);
		}

		if (fChildren == null)
			fChildren = new ArrayList();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				MonitorElement childElement = new MonitorElement();
				childElement.setModel(getModel());
				childElement.setInTheModel(true);
				this.fChildren.add(childElement);
				childElement.setParent(this);
				childElement.load(child);
			} else if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue() != null) {
				String text = child.getNodeValue();
				text = text.trim();
				if (isNotEmpty(text))
					this.fText = text;
			}
		}
	}

	public void removeAttribute(String name) throws CoreException {
		ensureModelEditable();
		MonitorAttribute att = (MonitorAttribute) getAttributeMap().remove(name);
		String oldValue = att.getValue();
		if (att != null) {
			att.setInTheModel(false);
		}
		firePropertyChanged(P_ATTRIBUTE, oldValue, null);
	}

	public void setAttribute(String name, String value) throws CoreException {
		ensureModelEditable();
		if (value == null) {
			removeAttribute(name);
			return;
		}
		IMonitorAttribute attribute = getAttribute(name);
		if (attribute == null) {
			attribute = getModel().getFactory().createAttribute(this);
			attribute.setName(name);
			getAttributeMap().put(name, attribute);
			((MonitorAttribute) attribute).setInTheModel(true);
		}
		attribute.setValue(value);
	}

	public void setElementInfo(ISchemaElement newElementInfo) {
		fElementInfo = newElementInfo;
		if (fElementInfo == null) {
			for (Enumeration atts = getAttributeMap().elements(); atts.hasMoreElements();) {
				MonitorAttribute att = (MonitorAttribute) atts.nextElement();
				att.setAttributeInfo(null);
			}
		}
	}

	public void setText(String newText) throws CoreException {
		ensureModelEditable();
		String oldValue = fText;
		fText = newText;
		firePropertyChanged(P_TEXT, oldValue, fText);

	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<" + getName()); //$NON-NLS-1$
		String newIndent = indent + ATTRIBUTE_SHIFT;
		if (getAttributeMap().isEmpty() == false) {
			writer.println();
			for (Iterator iter = getAttributeMap().values().iterator(); iter.hasNext();) {
				IMonitorAttribute attribute = (IMonitorAttribute) iter.next();
				attribute.write(newIndent, writer);
				if (iter.hasNext())
					writer.println();
			}
		}
		writer.println(">"); //$NON-NLS-1$
		newIndent = indent + ELEMENT_SHIFT;
		IMonitorObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			IMonitorElement element = (IMonitorElement) children[i];
			element.write(newIndent, writer);
		}
		if (getText() != null) {
			writer.println(newIndent + getWritableString(getText()));
		}
		writer.println(indent + "</" + getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected Hashtable getAttributeMap() {
		if (fAttributes == null) {
			fAttributes = new Hashtable();
			if (fElement != null) {
				String[] names = fElement.getAttributeNames();
				for (int i = 0; i < names.length; i++) {
					IMonitorAttribute attr = createAttribute(names[i], fElement.getAttribute(names[i]));
					if (attr != null)
						fAttributes.put(names[i], attr);
				}
			}
		}
		return fAttributes;
	}

	private IMonitorAttribute createAttribute(String name, String value) {
		if (name == null || value == null)
			return null;
		try {
			IMonitorAttribute attr = getMonitorModel().getFactory().createAttribute(this);
			if (attr instanceof MonitorAttribute)
				((MonitorAttribute) attr).load(name, value);
			else {
				attr.setName(name);
				attr.setValue(value);
			}
			return attr;
		} catch (CoreException e) {
		}
		return null;
	}

	protected ArrayList getChildrenList() {
		if (fChildren == null) {
			fChildren = new ArrayList();
			if (fElement != null) {
				IConfigurationElement[] elements = fElement.getChildren();
				for (int i = 0; i < elements.length; i++) {
					MonitorElement element = new MonitorElement(elements[i]);
					element.setModel(getModel());
					element.setParent(this);
					fChildren.add(element);
				}
			}
		}
		return fChildren;
	}

	public String getName() {
		if (fName == null && fElement != null) {
			fName = fElement.getName();
		}
		return fName;
	}
}
