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
package com.siteview.mde.internal.ui.views.dependencies;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class OpenDependenciesAction implements IWorkbenchWindowActionDelegate {
	private ISelection fSelection;

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			openDependencies(ssel.getFirstElement());
		}
	}

	private void openDependencies(Object el) {
		if (el instanceof IFile) {
			el = ((IFile) el).getProject();
		}
		if (el instanceof IJavaProject) {
			el = ((IJavaProject) el).getProject();
		}
		if (el instanceof IProject) {
			el = MonitorRegistry.findModel((IProject) el);
		}
		if (el instanceof IMonitorObject) {
			el = ((IMonitorObject) el).getModel();
		}
		if (el instanceof IMonitorModelBase) {
			new OpenPluginDependenciesAction((IMonitorModelBase) el).run();
		}
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}
}
