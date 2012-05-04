/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.bundle.Bundle;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.IMarkerResolution;

public class RemoveImportExportServicesResolution extends AbstractManifestMarkerResolution implements IMarkerResolution {

	String fServiceHeader;

	public RemoveImportExportServicesResolution(int type, String serviceHeader) {
		super(type);
		fServiceHeader = serviceHeader;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		IManifestHeader header = bundle.getManifestHeader(fServiceHeader);
		if (header != null)
			bundle.setHeader(fServiceHeader, null);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.RemoveImportExportServices_label, fServiceHeader);
	}

	public String getDescription() {
		return NLS.bind(MDEUIMessages.RemoveImportExportServices_description, fServiceHeader);
	}

}
