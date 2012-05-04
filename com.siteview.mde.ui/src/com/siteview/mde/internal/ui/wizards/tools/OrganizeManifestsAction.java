/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.tools;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.refactoring.PDERefactor;
import org.eclipse.ui.*;

public class OrganizeManifestsAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;

	public OrganizeManifestsAction() {
		super();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {

		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return;

		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			Iterator it = ssel.iterator();
			ArrayList projects = new ArrayList();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				else if (element instanceof IProject)
					proj = (IProject) element;
				if (proj != null && PDEProject.getManifest(proj).exists())
					projects.add(proj);
			}
			if (projects.size() > 0) {
				OrganizeManifestsProcessor processor = new OrganizeManifestsProcessor(projects);
				PDERefactor refactor = new PDERefactor(processor);
				OrganizeManifestsWizard wizard = new OrganizeManifestsWizard(refactor);
				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);

				try {
					op.run(MDEPlugin.getActiveWorkbenchShell(), ""); //$NON-NLS-1$
				} catch (final InterruptedException irex) {
				}
			} else
				MessageDialog.openInformation(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.OrganizeManifestsWizardPage_title, MDEUIMessages.OrganizeManifestsWizardPage_errorMsg);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

}
