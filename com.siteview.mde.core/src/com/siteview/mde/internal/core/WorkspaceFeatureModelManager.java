/*******************************************************************************
 *  Copyright (c) 2006, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import java.util.HashMap;
import org.eclipse.core.resources.*;
import com.siteview.mde.core.IModelProviderEvent;
import com.siteview.mde.internal.core.feature.WorkspaceFeatureModel;
import com.siteview.mde.internal.core.ifeature.IFeatureModel;
import com.siteview.mde.internal.core.project.PDEProject;

public class WorkspaceFeatureModelManager extends WorkspaceModelManager {

	protected boolean isInterestingProject(IProject project) {
		return isFeatureProject(project);
	}

	protected void createModel(IProject project, boolean notify) {
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (featureXml.exists()) {
			WorkspaceFeatureModel model = new WorkspaceFeatureModel(featureXml);
			loadModel(model, false);
			if (fModels == null)
				fModels = new HashMap();
			fModels.put(project, model);
			if (notify)
				addChange(model, IModelProviderEvent.MODELS_ADDED);
		}
	}

	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		IProject project = file.getProject();
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (file.equals(featureXml)) {
			Object model = getModel(project);
			int kind = delta.getKind();
			if (kind == IResourceDelta.REMOVED && model != null) {
				removeModel(project);
			} else if (kind == IResourceDelta.ADDED || model == null) {
				createModel(file.getProject(), true);
			} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				loadModel((IFeatureModel) model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		}
	}

	protected void addListeners() {
		int event = IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE;
		MDECore.getWorkspace().addResourceChangeListener(this, event);
	}

	protected void removeListeners() {
		MDECore.getWorkspace().removeResourceChangeListener(this);
		super.removeListeners();
	}

	protected IFeatureModel[] getFeatureModels() {
		initialize();
		return (IFeatureModel[]) fModels.values().toArray(new IFeatureModel[fModels.size()]);
	}

	protected IFeatureModel getFeatureModel(IProject project) {
		return (IFeatureModel) getModel(project);
	}

}
