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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class RemoveStaticProjectReferences extends AbstractPDEMarkerResolution {

	public RemoveStaticProjectReferences(int type) {
		super(type);
	}

	public String getDescription() {
		return MDEUIMessages.RemoveBuildOrderEntries_desc;
	}

	public String getLabel() {
		return MDEUIMessages.RemoveBuildOrderEntries_label;
	}

	public void run(IMarker marker) {
		try {
			IProject project = marker.getResource().getProject();
			if (project == null)
				return;
			IProjectDescription projDesc = project.getDescription();
			if (projDesc == null)
				return;
			projDesc.setReferencedProjects(new IProject[0]);
			project.setDescription(projDesc, null);
		} catch (CoreException e) {
		}
	}

	protected void createChange(IBaseModel model) {
		// overridden run method handles everything
	}
}
