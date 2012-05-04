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

import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeExportPackageResolution extends AbstractManifestMarkerResolution {

	public OrganizeExportPackageResolution(int type) {
		super(type);
	}

	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeExportPackages(model.getBundle(), fResource.getProject(), true, true);
	}

	public String getDescription() {
		return MDEUIMessages.OrganizeExportPackageResolution_Description;
	}

	public String getLabel() {
		return MDEUIMessages.OrganizeExportPackageResolution_Label;
	}

}
