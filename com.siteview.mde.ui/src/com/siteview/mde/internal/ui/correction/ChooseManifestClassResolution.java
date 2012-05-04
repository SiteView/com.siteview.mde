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

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.PDEJavaHelperUI;

public class ChooseManifestClassResolution extends AbstractManifestMarkerResolution {

	private String fHeader;

	public ChooseManifestClassResolution(int type, String headerName) {
		super(type);
		fHeader = headerName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);
		String type = PDEJavaHelperUI.selectType(fResource, IJavaElementSearchConstants.CONSIDER_CLASSES);
		if (type != null)
			header.setValue(type);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.ChooseManifestClassResolution_label, fHeader);
	}

}
