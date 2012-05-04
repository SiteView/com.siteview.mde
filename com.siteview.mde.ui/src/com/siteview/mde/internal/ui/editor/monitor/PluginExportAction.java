/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.core.IModel;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.wizards.ResizableWizardDialog;
import com.siteview.mde.internal.ui.wizards.exports.PluginExportWizard;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 *
 */
public class PluginExportAction extends Action {
	private MDEFormEditor fEditor;

	public PluginExportAction(MDEFormEditor editor) {
		fEditor = editor;
	}

	public PluginExportAction() {
	}

	private void ensureContentSaved() {
		if (fEditor.isDirty()) {
			try {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						fEditor.doSave(monitor);
					}
				};
				PlatformUI.getWorkbench().getProgressService().runInUI(MDEPlugin.getActiveWorkbenchWindow(), op, MDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				MDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	public void run() {
		if (fEditor != null)
			ensureContentSaved();
		IStructuredSelection selection;
		IResource resource = null;
		if (fEditor != null)
			resource = ((IModel) fEditor.getAggregateModel()).getUnderlyingResource();
		String customWizard = null;
		if (resource != null) {
			selection = new StructuredSelection(resource);
			IProject project = resource.getProject();
			if (project != null) {
				// a project can override the default export wizard
				customWizard = PDEProject.getExportWizard(project);
			}
		} else {
			selection = new StructuredSelection();
		}
		IWorkbenchWizard wizard = null;
		if (customWizard != null) {
			IWizardDescriptor descriptor = PlatformUI.getWorkbench().getExportWizardRegistry().findWizard(customWizard);
			if (descriptor != null) {
				try {
					wizard = descriptor.createWizard();
				} catch (CoreException e) {
					MDEPlugin.log(e);
					notifyResult(false);
					return;
				}
			}
		}
		if (wizard == null) {
			wizard = new PluginExportWizard();
		}
		wizard.init(PlatformUI.getWorkbench(), selection);
		WizardDialog wd = new ResizableWizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		//wd.getShell().setSize(450, 600);
		int result = wd.open();
		notifyResult(result == Window.OK);
	}
}
