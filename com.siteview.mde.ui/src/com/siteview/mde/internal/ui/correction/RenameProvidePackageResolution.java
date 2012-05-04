/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.osgi.framework.Constants;

public class RenameProvidePackageResolution extends AbstractManifestMarkerResolution {

	public RenameProvidePackageResolution(int type) {
		super(type);
	}

	public String getDescription() {
		return MDEUIMessages.RenameProvidePackageResolution_desc;
	}

	public String getLabel() {
		return MDEUIMessages.RenameProvidePackageResolution_label;
	}

	protected void createChange(BundleModel model) {
		model.getBundle().renameHeader(ICoreConstants.PROVIDE_PACKAGE, Constants.EXPORT_PACKAGE);
	}

}
