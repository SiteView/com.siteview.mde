/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.text.bundle.BundleModel;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class CreateJREBundleHeaderResolution extends AbstractManifestMarkerResolution {

	public CreateJREBundleHeaderResolution(int type) {
		super(type);
	}

	public String getDescription() {
		return MDEUIMessages.CreateJREBundleHeaderResolution_desc;
	}

	public String getLabel() {
		return MDEUIMessages.CreateJREBundleHeaderResolution_label;
	}

	protected void createChange(BundleModel model) {
		model.getBundle().setHeader(ICoreConstants.ECLIPSE_JREBUNDLE, Boolean.toString(true));
	}
}
