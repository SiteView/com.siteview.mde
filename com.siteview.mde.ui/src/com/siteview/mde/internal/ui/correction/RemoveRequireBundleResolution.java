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
import org.osgi.framework.Constants;

public class RemoveRequireBundleResolution extends AbstractManifestMarkerResolution {

	private String fBundleId;

	public RemoveRequireBundleResolution(int type, String bundleID) {
		super(type);
		fBundleId = bundleID;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null)
			header.removeBundle(fBundleId);
	}

	public String getDescription() {
		return NLS.bind(MDEUIMessages.RemoveRequireBundleResolution_description, fBundleId);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.RemoveRequireBundleResolution_label, fBundleId);
	}

}