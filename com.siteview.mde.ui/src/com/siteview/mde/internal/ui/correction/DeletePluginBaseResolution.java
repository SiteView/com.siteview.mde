/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.builders.MDEMarkerFactory;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.IMarkerResolution;

public class DeletePluginBaseResolution extends AbstractPDEMarkerResolution {

	public DeletePluginBaseResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return MDEUIMessages.RemoveUselessPluginFile_description;
	}

	public void run(final IMarker marker) {
		try {
			marker.delete();
			marker.getResource().deleteMarkers(MDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
			marker.getResource().delete(true, new NullProgressMonitor());

			//Since the plugin.xml is now deleted, remove the corresponding entry from the build.properties as well
			IResource buildProperties = marker.getResource().getParent().findMember(IPDEBuildConstants.PROPERTIES_FILE);
			if (buildProperties == null)
				return;

			IMarker removePluginEntryMarker = buildProperties.createMarker(String.valueOf(AbstractPDEMarkerResolution.REMOVE_TYPE));
			removePluginEntryMarker.setAttribute(MDEMarkerFactory.BK_BUILD_ENTRY, IBuildEntry.BIN_INCLUDES);
			removePluginEntryMarker.setAttribute(MDEMarkerFactory.BK_BUILD_TOKEN, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);

			IMarkerResolution removeBuildEntryResolution = new RemoveBuildEntryResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, removePluginEntryMarker);
			removeBuildEntryResolution.run(removePluginEntryMarker);
		} catch (CoreException e) {
			MDEPlugin.log(e);
		}
	}

	protected void createChange(IBaseModel model) {
		// handled by run
	}

}