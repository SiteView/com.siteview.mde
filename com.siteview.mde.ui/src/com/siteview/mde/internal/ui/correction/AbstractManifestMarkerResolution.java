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

import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.text.bundle.BundleModel;

public abstract class AbstractManifestMarkerResolution extends AbstractPDEMarkerResolution {

	public AbstractManifestMarkerResolution(int type) {
		super(type);
	}

	protected abstract void createChange(BundleModel model);

	protected void createChange(IBaseModel model) {
		if (model instanceof IBundlePluginModelBase)
			model = ((IBundlePluginModelBase) model).getBundleModel();
		if (model instanceof BundleModel)
			createChange((BundleModel) model);
	}

}
