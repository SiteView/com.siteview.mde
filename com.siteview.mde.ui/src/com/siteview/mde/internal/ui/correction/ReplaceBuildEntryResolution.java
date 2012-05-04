/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
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

public class ReplaceBuildEntryResolution extends BuildEntryMarkerResolution {

	public ReplaceBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getLabel() {
		return NLS.bind(MDEUIMessages.ReplaceBuildEntryResolution_replaceToken, fToken, fEntry);
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			if (buildEntry == null)
				return;
			if (fToken == null)
				build.remove(buildEntry);
			else {
				String[] tokens = buildEntry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					buildEntry.removeToken(tokens[i]);
				}
				buildEntry.addToken(fToken);
			}
		} catch (CoreException e) {
		}
	}
}
