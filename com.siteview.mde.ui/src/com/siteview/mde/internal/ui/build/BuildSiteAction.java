/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Oct 6, 2003
 */
package com.siteview.mde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.exports.SiteBuildOperation;
import com.siteview.mde.internal.core.ifeature.IFeatureModel;
import com.siteview.mde.internal.core.isite.ISiteFeature;
import com.siteview.mde.internal.core.isite.ISiteModel;
import com.siteview.mde.internal.core.site.WorkspaceSiteModel;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.site.SiteEditor;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.IProgressConstants;

public class BuildSiteAction implements IObjectActionDelegate {

	private ISiteModel fModel;

	private IFile fSiteXML;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fModel == null)
			return;
		ISiteFeature[] sbFeatures = fModel.getSite().getFeatures();
		IFeatureModel[] models = getFeatureModels(sbFeatures);

		if (models.length > 0) {
			try {
				ensureContentSaved();
				fModel.load();
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
			Job job = new SiteBuildOperation(models, fModel, MDEUIMessages.BuildSiteJob_name);
			job.setUser(true);
			job.schedule();
			job.setProperty(IProgressConstants.ICON_PROPERTY, MDEPluginImages.DESC_SITE_OBJ);
		}
	}

	private IFeatureModel[] getFeatureModels(ISiteFeature[] sFeatures) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < sFeatures.length; i++) {
			ISiteFeature siteFeature = sFeatures[i];
			IFeatureModel model = MDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(siteFeature.getId(), siteFeature.getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				fSiteXML = (IFile) obj;
				fModel = new WorkspaceSiteModel(fSiteXML);
				try {
					fModel.load();
					ISiteFeature[] features = fModel.getSite().getFeatures();
					if (features.length <= 0)
						action.setEnabled(false);
				} catch (CoreException e) {
					action.setEnabled(false);
				}
			}
		}
	}

	private void ensureContentSaved() {
		if (fModel != null && fModel.getUnderlyingResource() != null) {
			IProject project = fModel.getUnderlyingResource().getProject();
			final SiteEditor editor = PDEModelUtility.getOpenUpdateSiteEditor(project);
			if (editor != null && editor.isDirty()) {
				try {
					IRunnableWithProgress op = new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							editor.doSave(monitor);
						}
					};
					PlatformUI.getWorkbench().getProgressService().runInUI(MDEPlugin.getActiveWorkbenchWindow(), op, MDEPlugin.getWorkspace().getRoot());
				} catch (InvocationTargetException e) {
					MDEPlugin.logException(e);
				} catch (InterruptedException e) {
				}
			}
		}
	}

}
