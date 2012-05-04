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
package com.siteview.mde.internal.ui.wizards.target;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.target.WorkspaceFileTargetHandle;
import com.siteview.mde.internal.core.target.provisional.ITargetDefinition;
import com.siteview.mde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewTargetDefinitionWizard extends BasicNewResourceWizard {

	TargetDefinitionWizardPage fPage;
	TargetCreationPage ftargetCreationPage;
	IPath fInitialPath = null;
	IPath fFilePath = null;

	public void addPages() {
		ftargetCreationPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		fPage = new TargetDefinitionWizardPage("profile", getSelection()); //$NON-NLS-1$
		if (fInitialPath != null)
			fPage.setContainerFullPath(fInitialPath);
		addPage(fPage);
	}

	public boolean performFinish() {
		try {
			int option = fPage.getInitializationOption();
			ftargetCreationPage.setTargetId(fPage.getTargetId());
			ITargetDefinition targetDef = ftargetCreationPage.createTarget(option);
			fFilePath = fPage.getContainerFullPath().append(fPage.getFileName());
			IFile targetFile = MDECore.getWorkspace().getRoot().getFile(fFilePath);
			if (option == TargetDefinitionWizardPage.USE_EMPTY) {
				//extract the file name
				String name = targetFile.getName();
				int index = name.lastIndexOf(targetFile.getFileExtension()) - 1;
				if (index > 0) {
					name = name.substring(0, index);
					targetDef.setName(name);
				}
			}
			WorkspaceFileTargetHandle wrkspcTargetHandle = new WorkspaceFileTargetHandle(targetFile);
			wrkspcTargetHandle.save(targetDef);

			// Open the editor
			IWorkbenchWindow ww = MDEPlugin.getActiveWorkbenchWindow();
			if (ww != null) {
				IWorkbenchPage page = ww.getActivePage();
				IFile file = wrkspcTargetHandle.getTargetFile();
				if (page != null && file.exists())
					try {
						IDE.openEditor(page, file);
					} catch (PartInitException e) {
					}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(MDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_TARGET_WIZ);
	}

	public void setInitialPath(IPath path) {
		fInitialPath = path;
	}

	public IPath getFilePath() {
		return fFilePath;
	}

}
