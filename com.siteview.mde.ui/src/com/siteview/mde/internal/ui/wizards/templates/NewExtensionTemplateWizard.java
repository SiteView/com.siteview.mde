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
package com.siteview.mde.internal.ui.wizards.templates;

import com.siteview.mde.core.monitor.*;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.ui.IExtensionWizard;
import com.siteview.mde.ui.templates.BaseOptionTemplateSection;
import com.siteview.mde.ui.templates.ITemplateSection;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * This wizard should be used as a base class for 
 * wizards that provide new plug-in templates. 
 * These wizards are loaded during new plug-in or fragment
 * creation and are used to provide initial
 * content (Java classes, directory structure and
 * extensions).
 * <p>
 * This plug-in will be passed on to the templates to generate additional
 * content. After all templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public class NewExtensionTemplateWizard extends Wizard implements IExtensionWizard {
	private ITemplateSection fSection;
	private IProject fProject;
	private IMonitorModelBase fModel;
	private boolean fUpdatedDependencies;

	/**
	 * Creates a new template wizard.
	 */

	public NewExtensionTemplateWizard(ITemplateSection section) {
		Assert.isNotNull(section);
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWEX_WIZ);
		setNeedsProgressMonitor(true);
		fSection = section;
	}

	public void init(IProject project, IMonitorModelBase model) {
		this.fProject = project;
		this.fModel = model;
	}

	public void addPages() {
		fSection.addPages(this);
		setWindowTitle(fSection.getLabel());
		if (fSection instanceof BaseOptionTemplateSection) {
			((BaseOptionTemplateSection) fSection).initializeFields(fModel);
		}
	}

	public boolean performFinish() {
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					int totalWork = fSection.getNumberOfWorkUnits();
					monitor.beginTask(MDEUIMessages.NewExtensionTemplateWizard_generating, totalWork);
					updateDependencies();
					fSection.execute(fProject, fModel, monitor); // nsteps
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			MDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			MDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	private void updateDependencies() throws CoreException {
		IMonitorReference[] refs = fSection.getDependencies(fModel.getMonitorBase().getSchemaVersion());
		for (int i = 0; i < refs.length; i++) {
			IMonitorReference ref = refs[i];
			if (!modelContains(ref)) {
				IMonitorImport iimport = fModel.getMonitorFactory().createImport();
				iimport.setId(ref.getId());
				iimport.setMatch(ref.getMatch());
				iimport.setVersion(ref.getVersion());
				fModel.getMonitorBase().add(iimport);
				fUpdatedDependencies = true;
			}
		}
	}

	private boolean modelContains(IMonitorReference ref) {
		IMonitorBase plugin = fModel.getMonitorBase();
		IMonitorImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IMonitorImport iimport = imports[i];
			if (iimport.getId().equals(ref.getId())) {
				// good enough
				return true;
			}
		}
		return false;
	}

	public boolean updatedDependencies() {
		return fUpdatedDependencies;
	}
}
