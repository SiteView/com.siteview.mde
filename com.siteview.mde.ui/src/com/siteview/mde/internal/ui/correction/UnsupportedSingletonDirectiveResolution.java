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

import com.siteview.mde.internal.core.ibundle.IBundle;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.osgi.framework.Constants;

public class UnsupportedSingletonDirectiveResolution extends AbstractManifestMarkerResolution {

	public UnsupportedSingletonDirectiveResolution(int type) {
		super(type);
	}

	public String getDescription() {
		return MDEUIMessages.RevertUnsupportSingletonResolution_desc;
	}

	public String getLabel() {
		return MDEUIMessages.RevertUnsupportSingletonResolution_desc;
	}

	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header instanceof BundleSymbolicNameHeader) {
				((BundleSymbolicNameHeader) header).fixUnsupportedDirective();
			}
		}
	}
}
