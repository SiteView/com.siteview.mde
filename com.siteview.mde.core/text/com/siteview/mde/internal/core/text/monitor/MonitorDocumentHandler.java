/*******************************************************************************
 *  Copyright (c) 2003, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.text.monitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.core.text.*;
import org.xml.sax.SAXException;

public class MonitorDocumentHandler extends DocumentHandler {

	private MonitorModelBase fModel;
	private String fSchemaVersion;
	protected MonitorDocumentNodeFactory fFactory;

	/**
	 * @param model
	 */
	public MonitorDocumentHandler(MonitorModelBase model, boolean reconciling) {
		super(reconciling);
		fModel = model;
		fFactory = (MonitorDocumentNodeFactory) getModel().getMonitorFactory();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		IMonitorBase pluginBase = fModel.getMonitorBase(false);
		try {
			if (pluginBase != null)
				pluginBase.setSchemaVersion(fSchemaVersion);
		} catch (CoreException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			// Data should be of the form: version="<version>"
			if (data.length() > 10 && data.substring(0, 9).equals("version=\"") && data.charAt(data.length() - 1) == '\"') { //$NON-NLS-1$
				fSchemaVersion = TargetPlatformHelper.getSchemaVersionForTargetVersion(data.substring(9, data.length() - 1));
			} else {
				fSchemaVersion = TargetPlatformHelper.getSchemaVersion();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.DocumentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		super.startDocument();
		fSchemaVersion = null;
	}

	protected MonitorModelBase getModel() {
		return fModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentElementNode getDocumentNode(String name, IDocumentElementNode parent) {
		IDocumentElementNode node = null;
		if (parent == null) {
			node = (IDocumentElementNode) getModel().getMonitorBase(false);
			if (node != null) {
				node.setOffset(-1);
				node.setLength(-1);
			}
		} else {
			IDocumentElementNode[] children = parent.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i].getOffset() < 0) {
					if (name.equals(children[i].getXMLTagName())) {
						node = children[i];
					}
					break;
				}
			}
		}

		if (node == null)
			return fFactory.createDocumentNode(name, parent);

		IDocumentAttributeNode[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			attrs[i].setNameOffset(-1);
			attrs[i].setNameLength(-1);
			attrs[i].setValueOffset(-1);
			attrs[i].setValueLength(-1);
		}

		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentElementNode child = node.getChildAt(i);
			child.setOffset(-1);
			child.setLength(-1);
		}

		// clear text nodes if the user is typing on the source page
		// they will be recreated in the characters() method
		if (isReconciling()) {
			node.removeTextNode();
			node.setIsErrorNode(false);
		}

		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent) {
		IDocumentAttributeNode attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = fFactory.createAttribute(name, value, parent);
			} else {
				if (!name.equals(attr.getAttributeName()))
					attr.setAttributeName(name);
				if (!value.equals(attr.getAttributeValue()))
					attr.setAttributeValue(value);
			}
		} catch (CoreException e) {
		}
		return attr;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.DocumentHandler#getDocumentTextNode()
	 */
	protected IDocumentTextNode getDocumentTextNode(String content, IDocumentElementNode parent) {

		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (content.trim().length() > 0) {
				textNode = fFactory.createDocumentTextNode(content, parent);
			}
		} else {
			String newContent = textNode.getText() + content;
			textNode.setText(newContent);
		}
		return textNode;
	}

}
