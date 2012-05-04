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

import com.siteview.mde.core.monitor.IMonitorAttribute;
import com.siteview.mde.internal.core.text.DocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentElementNode;

public class MonitorAttribute extends MonitorObjectNode implements IMonitorAttribute, IDocumentAttributeNode {

	private static final long serialVersionUID = 1L;

	// The plugin attribute interface requires this class to extend PluginObjectNode
	// However, by doing that this class also extends the document
	// element node class - which is wrong when implementing 
	// the document attribute node interface
	// To work around this issue, we use an adaptor.
	private DocumentAttributeNode fAttribute;

	private String fValue;

	/**
	 * 
	 */
	public MonitorAttribute() {
		super();
		fAttribute = new DocumentAttributeNode();
		fValue = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#getValue()
	 */
	public String getValue() {
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#setValue(java.lang.String)
	 */
	public void setValue(String value) throws CoreException {
		fValue = value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentElementNode node) {
		fAttribute.setEnclosingElement(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getEnclosingElement()
	 */
	public IDocumentElementNode getEnclosingElement() {
		return fAttribute.getEnclosingElement();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameOffset(int)
	 */
	public void setNameOffset(int offset) {
		fAttribute.setNameOffset(offset);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameOffset()
	 */
	public int getNameOffset() {
		return fAttribute.getNameOffset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameLength(int)
	 */
	public void setNameLength(int length) {
		fAttribute.setNameLength(length);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameLength()
	 */
	public int getNameLength() {
		return fAttribute.getNameLength();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueOffset(int)
	 */
	public void setValueOffset(int offset) {
		fAttribute.setValueOffset(offset);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueOffset()
	 */
	public int getValueOffset() {
		return fAttribute.getValueOffset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueLength(int)
	 */
	public void setValueLength(int length) {
		fAttribute.setValueLength(length);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueLength()
	 */
	public int getValueLength() {
		return fAttribute.getValueLength();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeName()
	 */
	public String getAttributeName() {
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeValue()
	 */
	public String getAttributeValue() {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#write()
	 */
	public String write() {
		return getName() + "=\"" + getWritableString(getValue()) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#getWritableString(java.lang.String)
	 */
	public String getWritableString(String source) {
		return super.getWritableString(source).replaceAll("\\r", "&#x0D;") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\\n", "&#x0A;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.IDocumentAttributeNode#setAttributeName(java.lang.String)
	 */
	public void setAttributeName(String name) throws CoreException {
		setName(name);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.IDocumentAttributeNode#setAttributeValue(java.lang.String)
	 */
	public void setAttributeValue(String value) throws CoreException {
		setValue(value);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, com.siteview.mde.internal.core.ischema.ISchema, com.siteview.mde.internal.core.text.IDocumentElementNode)
	 */
	public void reconnect(IDocumentElementNode parent) {
		// Inconsistency in model
		// A document attribute node should not extend plugin object because plugin object extends 
		// document element node
		super.reconnect(parent, getModel());
		fAttribute.reconnect(parent);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		// Although attributes cannot be copied directly
		writer.write(write());
	}

}
