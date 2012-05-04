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

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.PDEJavaHelperUI;
import com.siteview.mde.internal.ui.util.TextUtil;

public class CreateManifestClassResolution extends AbstractManifestMarkerResolution {

	private String fHeader;

	public CreateManifestClassResolution(int type, String headerName) {
		super(type);
		fHeader = headerName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);

		String name = TextUtil.trimNonAlphaChars(header.getValue()).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();

		IMonitorModelBase modelBase = MonitorRegistry.findModel(project);
		if (modelBase == null)
			return;

		JavaAttributeValue value = new JavaAttributeValue(project, modelBase, null, name);
		name = PDEJavaHelperUI.createClass(name, project, value, true);
		if (name != null && !name.equals(header.getValue()))
			header.setValue(name);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.CreateManifestClassResolution_label, fHeader);
	}

}
