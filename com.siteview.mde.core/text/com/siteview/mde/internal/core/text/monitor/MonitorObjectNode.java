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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import com.siteview.mde.core.*;
import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.monitor.IWritableDelimiter;
import com.siteview.mde.internal.core.text.*;
import com.siteview.mde.internal.core.util.PDEXMLHelper;

public class MonitorObjectNode extends DocumentElementNode implements IMonitorObject, IWritableDelimiter {

	private transient boolean fInTheModel;
	private transient ISharedMonitorModel fModel;

	private static final long serialVersionUID = 1L;
	private String fName;

	/**
	 * 
	 */
	public MonitorObjectNode() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getModel()
	 */
	public ISharedMonitorModel getModel() {
		return fModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IMonitorModelBase getMonitorModel() {
		return (IMonitorModelBase) fModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return fName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getResourceString(getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IMonitorObject getParent() {
		return (IMonitorObject) getParentNode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IMonitorBase getMonitorBase() {
		return fModel != null ? ((IMonitorModelBase) fModel).getMonitorBase() : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return fModel != null ? fModel.getResourceString(key) : key;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		fName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}

	public void setModel(ISharedMonitorModel model) {
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLAttribute(java.lang.String,
	 *      java.lang.String)
	 */
	public boolean setXMLAttribute(String name, String value) {
		// Overrided by necessity - dealing with different objects
		String oldValue = getXMLAttributeValue(name);
		if (oldValue != null && oldValue.equals(value))
			return false;
		MonitorAttribute attr = (MonitorAttribute) getNodeAttributesMap().get(name);
		try {
			if (value == null)
				value = ""; //$NON-NLS-1$
			if (attr == null) {
				attr = new MonitorAttribute();
				attr.setName(name);
				attr.setEnclosingElement(this);
				attr.setModel(getModel());
				getNodeAttributesMap().put(name, attr);
			}
			attr.setValue(value == null ? "" : value); //$NON-NLS-1$
		} catch (CoreException e) {
		}
		if (fInTheModel)
			firePropertyChanged(attr.getEnclosingElement(), attr.getAttributeName(), oldValue, value);
		return true;
	}

	protected void firePropertyChanged(IDocumentRange node, String property, Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			fModel.fireModelObjectChanged(node, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IMonitorObject child, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType, new Object[] {child}, null);
			fireModelChanged(e);
		}
	}

	protected void fireStructureChanged(IMonitorObject[] children, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType, children, null);
			fireModelChanged(e);
		}
	}

	private void fireModelChanged(IModelChangedEvent e) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(e);
		}
	}

	public String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

	protected void appendAttribute(StringBuffer buffer, String attrName) {
		appendAttribute(buffer, attrName, ""); //$NON-NLS-1$
	}

	protected void appendAttribute(StringBuffer buffer, String attrName, String defaultValue) {
		IDocumentAttributeNode attr = getDocumentAttribute(attrName);
		if (attr != null) {
			String value = attr.getAttributeValue();
			if (value != null && value.trim().length() > 0 && !value.equals(defaultValue))
				buffer.append(" " + attr.write()); //$NON-NLS-1$
		}
	}

	public String getLineDelimiter() {
		ISharedMonitorModel model = getModel();
		IDocument document = ((IEditingModel) model).getDocument();
		return TextUtilities.getDefaultLineDelimiter(document);
	}

	public void addChildNode(IDocumentElementNode child, int position) {
		super.addChildNode(child, position);
		((IMonitorObject) child).setInTheModel(true);
	}

	public String toString() {
		return write(false);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginDocumentNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, com.siteview.mde.internal.core.ischema.ISchema, com.siteview.mde.internal.core.text.IDocumentElementNode)
	 */
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient field:  In The Model
		// Value set to true when added to the parent; however, serialized
		// children's value remains unchanged.  Since, reconnect and add calls
		// are made so close together, set value to true for parent and all
		// children
		fInTheModel = true;
		// Transient field:  Model
		if (model instanceof ISharedMonitorModel) {
			fModel = (ISharedMonitorModel) model;
		}
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// NO-OP
		// Child classes to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLAttributeValue(java.lang.String)
	 */
	public String getXMLAttributeValue(String name) {
		// Overrided by necessity - dealing with different objects
		MonitorAttribute attr = (MonitorAttribute) getNodeAttributesMap().get(name);
		return attr == null ? null : attr.getValue();
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.IDocumentElementNode#write(boolean)
	 */
	public String write(boolean indent) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.IDocumentElementNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.plugin.PluginDocumentNode#getFileEncoding()
	 */
	protected String getFileEncoding() {
		if ((fModel != null) && (fModel instanceof IEditingModel)) {
			return ((IEditingModel) fModel).getCharset();
		}
		return super.getFileEncoding();
	}

}
