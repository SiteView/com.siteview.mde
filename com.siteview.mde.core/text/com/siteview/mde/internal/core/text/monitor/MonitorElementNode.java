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
package com.siteview.mde.internal.core.text.monitor;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModel;
import com.siteview.mde.core.monitor.IMonitorAttribute;
import com.siteview.mde.core.monitor.IMonitorElement;
import com.siteview.mde.core.monitor.IMonitorExtension;
import com.siteview.mde.internal.core.ischema.ISchema;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import com.siteview.mde.internal.core.text.DocumentTextNode;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.core.text.IDocumentTextNode;

public class MonitorElementNode extends MonitorParentNode implements IMonitorElement {

	private static final long serialVersionUID = 1L;

	private transient ISchemaElement elementInfo;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#createCopy()
	 */
	public IMonitorElement createCopy() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttribute(java.lang.String)
	 */
	public IMonitorAttribute getAttribute(String name) {
		return (IMonitorAttribute) getNodeAttributesMap().get(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributes()
	 */
	public IMonitorAttribute[] getAttributes() {
		return (IMonitorAttribute[]) getNodeAttributesMap().values().toArray(new IMonitorAttribute[getNodeAttributesMap().size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributeCount()
	 */
	public int getAttributeCount() {
		return getNodeAttributesMap().size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getText()
	 */
	public String getText() {
		IDocumentTextNode node = getTextNode();
		return node == null ? "" : node.getText(); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String name, String value) throws CoreException {
		setXMLAttribute(name, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setText(java.lang.String)
	 */
	public void setText(String text) throws CoreException {
		IDocumentTextNode node = getTextNode();
		String oldText = node == null ? null : node.getText();
		if (node == null) {
			node = new DocumentTextNode();
			node.setEnclosingElement(this);
			addTextNode(node);
		}
		node.setText(text.trim());
		firePropertyChanged(node, P_TEXT, oldText, text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());

		IDocumentElementNode[] children = getChildNodes();
		String text = getText();
		buffer.append(writeShallow(false));
		if (getAttributeCount() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(sep);
		if (children.length > 0 || text.length() > 0) {
			if (text.length() > 0) {
				buffer.append(getIndent());
				buffer.append("   "); //$NON-NLS-1$
				buffer.append(text);
				buffer.append(sep);
			}
			for (int i = 0; i < children.length; i++) {
				children[i].setLineIndent(getLineIndent() + 3);
				buffer.append(children[i].write(true));
				buffer.append(sep);
			}
		}
		if (getAttributeCount() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(getIndent());

		buffer.append("</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$	
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName()); //$NON-NLS-1$

		IDocumentAttributeNode[] attrs = getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getAttributeValue().length() > 0)
				buffer.append(sep + getIndent() + "      " + attrs[i].write()); //$NON-NLS-1$
		}
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLTagName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLTagName(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getElementInfo()
	 */
	public Object getElementInfo() {
		if (elementInfo == null) {
			IDocumentElementNode node = getParentNode();
			for (;;) {
				if (node == null || node instanceof IMonitorExtension)
					break;
				node = node.getParentNode();
			}
			if (node != null) {
				IMonitorExtension extension = (IMonitorExtension) node;
				ISchema schema = (ISchema) extension.getSchema();
				if (schema != null) {
					elementInfo = schema.findElement(getName());
				}
			}
		}
		return elementInfo;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, com.siteview.mde.internal.core.ischema.ISchema, com.siteview.mde.internal.core.text.IDocumentElementNode)
	 */
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient Field:  Element Info
		// Not necessary to reconnect schema.
		// getElementInfo will retrieve the schema on demand if it is null	
		elementInfo = null;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}

}
