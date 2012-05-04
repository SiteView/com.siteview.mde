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
import com.siteview.mde.internal.core.text.monitor.MonitorBaseNode;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class RemoveNodeXMLResolution extends AbstractXMLMarkerResolution {

	public RemoveNodeXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IMonitorModelBase model) {
		Object node = findNode(model);
		if (!(node instanceof IMonitorObject))
			return;
		try {
			IMonitorObject pluginObject = (IMonitorObject) node;
			IMonitorObject parent = pluginObject.getParent();
			if (parent instanceof IMonitorParent)
				((IMonitorParent) parent).remove(pluginObject);
			else if (parent instanceof MonitorBaseNode)
				((MonitorBaseNode) parent).remove(pluginObject);
			else if (pluginObject instanceof MonitorAttribute) {
				MonitorAttribute attr = (MonitorAttribute) pluginObject;
				attr.getEnclosingElement().setXMLAttribute(attr.getName(), null);
			}

		} catch (CoreException e) {
		}
	}

	public String getLabel() {
		if (isAttrNode())
			return NLS.bind(MDEUIMessages.RemoveNodeXMLResolution_attrLabel, getNameOfNode());
		return NLS.bind(MDEUIMessages.RemoveNodeXMLResolution_label, getNameOfNode());
	}

}
