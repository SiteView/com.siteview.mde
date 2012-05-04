/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
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
import com.siteview.mde.core.monitor.IPluginExtensionPoint;
import com.siteview.mde.internal.core.text.IDocumentElementNode;

public class MonitorExtensionPointNode extends MonitorObjectNode implements IPluginExtensionPoint {

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getFullId()
	 */
	public String getFullId() {
		String id = getId();
		String version = getMonitorBase().getSchemaVersion();
		if (version != null && Double.parseDouble(version) >= 3.2 && id != null && id.indexOf('.') != -1)
			return id;
		String pluginID = getMonitorBase().getId();
		return (pluginID != null) ? pluginID + "." + id : id; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getSchema()
	 */
	public String getSchema() {
		return getXMLAttributeValue("schema"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#setSchema(java.lang.String)
	 */
	public void setSchema(String schema) throws CoreException {
		setXMLAttribute(P_SCHEMA, schema);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		return indent ? getIndent() + writeShallow(true) : writeShallow(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		StringBuffer buffer = new StringBuffer("<extension-point"); //$NON-NLS-1$
		appendAttribute(buffer, P_ID);
		appendAttribute(buffer, P_NAME);
		appendAttribute(buffer, P_SCHEMA);

		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, com.siteview.mde.internal.core.ischema.ISchema, com.siteview.mde.internal.core.text.IDocumentElementNode)
	 */
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginObjectNode#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		writer.println(getIndent());
	}

}
