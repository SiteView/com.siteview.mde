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
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.ui.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class JavaAttributeWizard extends Wizard {

	private static String STORE_SECTION = "JavaAttributeWizard"; //$NON-NLS-1$

	private String fClassName;
	private IProject fProject;
	private ISchemaAttribute fAttInfo;
	private IMonitorModelBase fModel;
	protected NewTypeWizardPage fMainPage;

	public JavaAttributeWizard(JavaAttributeValue value) {
		this(value.getProject(), value.getModel(), value.getAttributeInfo(), value.getClassName());
	}

	public JavaAttributeWizard(IProject project, IMonitorModelBase model, ISchemaAttribute attInfo, String className) {
		fClassName = className;
		fModel = model;
		fProject = project;
		fAttInfo = attInfo;
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWPPRJ_WIZ);
		IDialogSettings masterSettings = MDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(MDEUIMessages.JavaAttributeWizard_wtitle);
		setNeedsProgressMonitor(true);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null)
			setting = master.addNewSection(STORE_SECTION);
		return setting;
	}

	public void addPages() {
		fMainPage = new JavaAttributeWizardPage(fProject, fModel, fAttInfo, fClassName);
		addPage(fMainPage);
		((JavaAttributeWizardPage) fMainPage).init();
	}

	public boolean performFinish() {
		IRunnableWithProgress op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException, InterruptedException {
				fMainPage.createType(monitor);
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(MDEPlugin.getActiveWorkbenchWindow(), op, MDEPlugin.getWorkspace().getRoot());
			IResource resource = fMainPage.getModifiedResource();
			if (resource != null) {
				selectAndReveal(resource);
				if (fProject.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProject = JavaCore.create(fProject);
					IJavaElement jElement = jProject.findElement(resource.getProjectRelativePath().removeFirstSegments(1));
					if (jElement != null)
						JavaUI.openInEditor(jElement);
				} else if (resource instanceof IFile) {
					IWorkbenchPage page = MDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				}
			}
		} catch (InvocationTargetException e) {
			MDEPlugin.logException(e);
		} catch (InterruptedException e) {
			MDEPlugin.logException(e);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
		return true;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, MDEPlugin.getActiveWorkbenchWindow());
	}

	public String getQualifiedName() {
		if (fMainPage.getCreatedType() == null)
			return null;
		return fMainPage.getCreatedType().getFullyQualifiedName('$');
	}

	public String getQualifiedNameWithArgs() {
		String name = getQualifiedName();
		if (name == null)
			return null;
		if (fMainPage instanceof JavaAttributeWizardPage) {
			String classArgs = ((JavaAttributeWizardPage) fMainPage).getClassArgs();
			if (classArgs != null && classArgs.length() > 0)
				return name + ':' + classArgs;
		}
		return name;
	}
}
