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

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.IProject;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;

public class JavaAttributeValue extends ResourceAttributeValue {
	private ISchemaAttribute attInfo;
	private IMonitorModelBase model;

	public JavaAttributeValue(IProject project, IMonitorModelBase model, ISchemaAttribute attInfo, String className) {
		super(project, className);
		this.attInfo = attInfo;
		this.model = model;
	}

	public ISchemaAttribute getAttributeInfo() {
		return attInfo;
	}

	public IMonitorModelBase getModel() {
		return model;
	}

	public String getClassName() {
		return getStringValue();
	}
}
