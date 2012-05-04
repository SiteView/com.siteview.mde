/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.internal.ui.editor.monitor.JavaAttributeValue;

import com.siteview.mde.internal.core.text.monitor.MonitorAttribute;

import com.siteview.mde.core.monitor.IMonitorExtension;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ischema.*;
import com.siteview.mde.internal.core.schema.SchemaRegistry;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.PDEJavaHelperUI;
import com.siteview.mde.internal.ui.util.TextUtil;

public class CreateClassXMLResolution extends AbstractXMLMarkerResolution {

	public CreateClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	// create class code copied from org.eclipse.pde.internal.ui.editor.plugin.rows.ClassAttributeRow
	protected void createChange(IMonitorModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof MonitorAttribute))
			return;

		MonitorAttribute attr = (MonitorAttribute) object;
		String name = TextUtil.trimNonAlphaChars(attr.getValue()).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();

		JavaAttributeValue value = new JavaAttributeValue(project, model, getAttribute(attr), name);
		name = PDEJavaHelperUI.createClass(name, project, value, true);
		if (name != null && !name.equals(attr.getValue()))
			attr.getEnclosingElement().setXMLAttribute(attr.getName(), name);
	}

	private ISchemaAttribute getAttribute(MonitorAttribute attr) {
		SchemaRegistry registry = MDECore.getDefault().getSchemaRegistry();
		IDocumentElementNode element = attr.getEnclosingElement();
		IMonitorExtension extension = null;
		while (element.getParentNode() != null) {
			if (element instanceof IMonitorExtension) {
				extension = (IMonitorExtension) element;
				break;
			}
			element = element.getParentNode();
		}
		if (extension == null)
			return null;

		ISchema schema = registry.getSchema(extension.getPoint());
		ISchemaElement schemaElement = schema.findElement(attr.getEnclosingElement().getXMLTagName());
		if (schemaElement == null)
			return null;
		return schemaElement.getAttribute(attr.getName());
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.CreateClassXMLResolution_label, getNameOfNode());
	}
}
