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

import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.framework.Constants;

public class RemoveExportPackageResolution extends AbstractManifestMarkerResolution implements IMarkerResolution {

	String fPackage;

	public RemoveExportPackageResolution(int type, String pkgName) {
		super(type);
		fPackage = pkgName;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header != null)
			header.removePackage(fPackage);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.RemoveExportPkgs_label, fPackage);
	}

	public String getDescription() {
		return NLS.bind(MDEUIMessages.RemoveExportPkgs_description, fPackage);
	}

}
