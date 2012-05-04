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

import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveInternalDirectiveEntryResolution extends AbstractManifestMarkerResolution {

	private String fPackageName;

	public RemoveInternalDirectiveEntryResolution(int type, String packageName) {
		super(type);
		fPackageName = packageName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header instanceof ExportPackageHeader) {
			ExportPackageObject exportedPackage = ((ExportPackageHeader) header).getPackage(fPackageName);
			if (exportedPackage != null)
				exportedPackage.removeInternalDirective();
		}
	}

	public String getLabel() {
		return MDEUIMessages.RemoveInternalDirective_label;
	}

	public String getDescription() {
		return MDEUIMessages.RemoveInternalDirective_desc;
	}

}
