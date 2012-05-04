/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.tools;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.internal.core.WorkspaceModelManager;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class UpdateClasspathAction implements IViewActionDelegate {
	private ISelection fSelection;

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IMonitorModelBase[] fUnupdated = getModelsToUpdate();
		if (fUnupdated.length == 0) {
			MessageDialog.openInformation(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.UpdateClasspathAction_find, MDEUIMessages.UpdateClasspathAction_none);
			return;
		}
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			ArrayList models = new ArrayList(elems.length);
			for (int i = 0; i < elems.length; i++) {
				Object elem = elems[i];
				IProject project = null;

				if (elem instanceof IFile) {
					IFile file = (IFile) elem;
					project = file.getProject();
				} else if (elem instanceof IProject) {
					project = (IProject) elem;
				} else if (elem instanceof IJavaProject) {
					project = ((IJavaProject) elem).getProject();
				}
				try {
					if (project != null && WorkspaceModelManager.isPluginProject(project) && project.hasNature(JavaCore.NATURE_ID)) {
						IMonitorModelBase model = MonitorRegistry.findModel(project);
						if (model != null) {
							models.add(model);
						}
					}
				} catch (CoreException e) {
					MDEPlugin.log(e);
				}
			}

			final IMonitorModelBase[] modelArray = (IMonitorModelBase[]) models.toArray(new IMonitorModelBase[models.size()]);

			UpdateBuildpathWizard wizard = new UpdateBuildpathWizard(fUnupdated, modelArray);
			final WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
			BusyIndicator.showWhile(MDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IViewPart view) {
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	private IMonitorModelBase[] getModelsToUpdate() {
		IMonitorModelBase[] models = MonitorRegistry.getWorkspaceModels();
		ArrayList modelArray = new ArrayList();
		try {
			for (int i = 0; i < models.length; i++) {
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(models[i]);
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
		return (IMonitorModelBase[]) modelArray.toArray(new IMonitorModelBase[modelArray.size()]);
	}

}
