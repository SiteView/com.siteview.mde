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
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.text.build.Build;
import com.siteview.mde.internal.core.text.build.BuildEntry;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class RemoveSeperatorBuildEntryResolution extends BuildEntryMarkerResolution {

	public RemoveSeperatorBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			buildEntry.renameToken(fToken, fToken.substring(0, fToken.length() - 1));
		} catch (CoreException e) {
		}
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.RemoveSeperatorBuildEntryResolution_label, fToken, fEntry);
	}

}
