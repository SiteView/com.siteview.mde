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

import com.siteview.mde.internal.core.text.monitor.MonitorAttribute;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.PDEJavaHelperUI;

public class ChooseClassXMLResolution extends AbstractXMLMarkerResolution {

	public ChooseClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IMonitorModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof MonitorAttribute))
			return;
		MonitorAttribute attrib = (MonitorAttribute) object;
		IDocumentElementNode element = attrib.getEnclosingElement();
		String type = PDEJavaHelperUI.selectType(fResource, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES);
		if (type != null)
			element.setXMLAttribute(attrib.getName(), type);
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return MDEUIMessages.ChooseClassXMLResolution_label;
	}

}
