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

public class OrganizeImportPackageResolution extends AbstractManifestMarkerResolution {

	private boolean fRemoveImports;

	public OrganizeImportPackageResolution(int type, boolean removeImports) {
		super(type);
		fRemoveImports = removeImports;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeImportPackages(model.getBundle(), fRemoveImports);
	}

	public String getDescription() {
		return MDEUIMessages.OrganizeImportPackageResolution_Description;
	}

	public String getLabel() {
		return MDEUIMessages.OrganizeImportPackageResolution_Label;
	}

}
