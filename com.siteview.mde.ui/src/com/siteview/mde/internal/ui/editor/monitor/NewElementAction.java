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
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.core.text.monitor.MonitorElementNode;

import com.siteview.mde.core.monitor.IMonitorElement;
import com.siteview.mde.core.monitor.IMonitorParent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.contentassist.XMLInsertionComputer;

public class NewElementAction extends Action {
	public static final String UNKNOWN_ELEMENT_TAG = MDEUIMessages.NewElementAction_generic;

	private ISchemaElement elementInfo;

	private IMonitorParent parent;

	public NewElementAction(ISchemaElement elementInfo, IMonitorParent parent) {
		this.elementInfo = elementInfo;
		// this.project = project;
		this.parent = parent;
		setText(getElementName());
		setImageDescriptor(MDEPluginImages.DESC_GENERIC_XML_OBJ);
		setEnabled(parent.getModel().isEditable());
	}

	private String getElementName() {
		return elementInfo != null ? elementInfo.getName() : UNKNOWN_ELEMENT_TAG;
	}

	public void run() {
		IMonitorElement newElement = parent.getModel().getFactory().createElement(parent);
		try {
			newElement.setName(getElementName());
			((MonitorElementNode) newElement).setParentNode((IDocumentElementNode) parent);

			// If there is an associated schema, recursively auto-insert 
			// required child elements and attributes respecting multiplicity
			if (elementInfo != null) {
				XMLInsertionComputer.computeInsertion(elementInfo, newElement);
			}
			parent.add(newElement);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

}
