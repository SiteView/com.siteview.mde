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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.builders.MDEMarkerFactory;
import com.siteview.mde.internal.core.text.build.Build;
import com.siteview.mde.internal.core.text.build.BuildModel;

public abstract class BuildEntryMarkerResolution extends AbstractPDEMarkerResolution {

	protected String fEntry;
	protected String fToken;

	public BuildEntryMarkerResolution(int type, IMarker marker) {
		super(type);
		try {
			fEntry = (String) marker.getAttribute(MDEMarkerFactory.BK_BUILD_ENTRY);
			fToken = (String) marker.getAttribute(MDEMarkerFactory.BK_BUILD_TOKEN);
		} catch (CoreException e) {
		}
	}

	protected abstract void createChange(Build build);

	protected void createChange(IBaseModel model) {
		if (model instanceof BuildModel)
			createChange((Build) ((BuildModel) model).getBuild());
	}
}
