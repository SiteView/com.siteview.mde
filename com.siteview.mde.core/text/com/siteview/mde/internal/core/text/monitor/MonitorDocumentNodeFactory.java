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

import org.eclipse.core.runtime.CoreException;

import com.siteview.mde.core.monitor.IMonitorObject;
import com.siteview.mde.core.monitor.IMonitorAttribute;
import com.siteview.mde.core.monitor.IMonitorElement;
import com.siteview.mde.core.monitor.IMonitorExtension;
import com.siteview.mde.core.monitor.IMonitorExtensionPoint;
import com.siteview.mde.core.monitor.IMonitorImport;
import com.siteview.mde.core.monitor.IMonitorLibrary;
import com.siteview.mde.core.monitor.IMonitorModelFactory;
import com.siteview.mde.internal.core.text.DocumentTextNode;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentNodeFactory;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.core.text.IDocumentTextNode;

public class MonitorDocumentNodeFactory implements IMonitorModelFactory, IDocumentNodeFactory {

	private MonitorModelBase fModel;

	public MonitorDocumentNodeFactory(MonitorModelBase model) {
		fModel = model;
	}

	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent) {
		if (parent == null)
			return createPluginBase(name);

		if (parent instanceof MonitorBaseNode) {
			if ("extension".equals(name)) //$NON-NLS-1$
				return (IDocumentElementNode) createExtension();
			if ("extension-point".equals(name)) //$NON-NLS-1$
				return (IDocumentElementNode) createExtensionPoint();
		} else {
			if (name.equals("import") && parent instanceof MonitorElementNode) { //$NON-NLS-1$
				if (((MonitorElementNode) parent).getName().equals("requires")) { //$NON-NLS-1$
					IDocumentElementNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof MonitorBaseNode) {
						return (IDocumentElementNode) createImport();
					}
				}
			} else if (name.equals("library") && parent instanceof MonitorElementNode) { //$NON-NLS-1$
				if (((MonitorElementNode) parent).getName().equals("runtime")) { //$NON-NLS-1$
					IDocumentElementNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof MonitorBaseNode) {
						return (IDocumentElementNode) createLibrary();
					}
				}
			}
		}
		IDocumentElementNode node = (IDocumentElementNode) createElement((IMonitorObject) parent);
		node.setXMLTagName(name);
		return node;
	}

	public IDocumentAttributeNode createAttribute(String name, String value, IDocumentElementNode enclosingElement) {
		MonitorAttribute attribute = new MonitorAttribute();
		try {
			attribute.setName(name);
			attribute.setValue(value);
		} catch (CoreException e) {
		}
		attribute.setEnclosingElement(enclosingElement);
		attribute.setModel(fModel);
		attribute.setInTheModel(true);
		return attribute;
	}

	private MonitorBaseNode createPluginBase(String name) {
		return (MonitorBaseNode) fModel.createMonitorBase(name.equals("fragment")); //$NON-NLS-1$

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelFactory#createImport()
	 */
	public IMonitorImport createImport() {
		MonitorImportNode node = new MonitorImportNode();
		node.setModel(fModel);
		node.setXMLTagName("import"); //$NON-NLS-1$
		return node;
	}

	public IMonitorImport createImport(String pluginId) {
		MonitorImportNode node = new MonitorImportNode(pluginId);
		node.setModel(fModel);
		node.setXMLTagName("import"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelFactory#createLibrary()
	 */
	public IMonitorLibrary createLibrary() {
		MonitorLibraryNode node = new MonitorLibraryNode();
		node.setModel(fModel);
		node.setXMLTagName("library"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createAttribute(org.eclipse.pde.core.plugin.IPluginElement)
	 */
	public IMonitorAttribute createAttribute(IMonitorElement element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createElement(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public IMonitorElement createElement(IMonitorObject parent) {
		MonitorElementNode node = new MonitorElementNode();
		node.setModel(fModel);
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createExtension()
	 */
	public IMonitorExtension createExtension() {
		MonitorExtensionNode node = new MonitorExtensionNode();
		node.setModel(fModel);
		node.setXMLTagName("extension"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createExtensionPoint()
	 */
	public IMonitorExtensionPoint createExtensionPoint() {
		MonitorExtensionPointNode node = new MonitorExtensionPointNode();
		node.setModel(fModel);
		node.setXMLTagName("extension-point"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.text.IDocumentNodeFactory#createDocumentTextNode(java.lang.String, com.siteview.mde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentTextNode createDocumentTextNode(String content, IDocumentElementNode parent) {
		DocumentTextNode textNode = new DocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content.trim());
		return textNode;
	}
}
