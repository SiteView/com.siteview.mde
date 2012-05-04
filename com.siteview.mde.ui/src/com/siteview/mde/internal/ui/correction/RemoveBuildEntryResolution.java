/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.text.build.Build;
import com.siteview.mde.internal.core.text.build.BuildEntry;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class RemoveBuildEntryResolution extends BuildEntryMarkerResolution {

	public RemoveBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getLabel() {
		if (fToken == null)
			return NLS.bind(MDEUIMessages.RemoveBuildEntryResolution_removeEntry, fEntry);
		return NLS.bind(MDEUIMessages.RemoveBuildEntryResolution_removeToken, fToken, fEntry);
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			if (buildEntry == null)
				return;
			if (fToken == null)
				build.remove(buildEntry);
			else {
				buildEntry.removeToken(fToken);
				if (buildEntry.getTokens().length == 0) {
					build.remove(buildEntry);
				}
			}
		} catch (CoreException e) {
		}
	}

}
