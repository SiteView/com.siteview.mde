/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.ui.templates;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.IMonitorReference;

import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.ui.IBundleContentWizard;
import com.siteview.mde.ui.IFieldData;

/**
 * This class is used as a common base for plug-in content wizards that are
 * implemented using PDE template support. The assumption is that one or more
 * templates will be used to generate plug-in content. Dependencies, new files
 * and wizard pages are all computed based on the templates.
 * 
 * @since 2.0
 */
public abstract class AbstractNewPluginTemplateWizard extends Wizard implements IBundleContentWizard {
	private IFieldData data;

	/**
	 * Creates a new template wizard.
	 */
	public AbstractNewPluginTemplateWizard() {
		super();
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setNeedsProgressMonitor(true);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.ui.IPluginContentWizard#init(com.siteview.mde.ui.IFieldData)
	 */
	public void init(IFieldData data) {
		this.data = data;
		setWindowTitle(MDEUIMessages.PluginCodeGeneratorWizard_title);
	}

	/**
	 * Returns the field data passed to the wizard during the initialization.
	 * 
	 * @return the parent wizard field data
	 */
	public IFieldData getData() {
		return data;
	}

	/**
	 * This wizard adds a mandatory first page. Subclasses implement this method
	 * to add additional pages to the wizard.
	 */
	protected abstract void addAdditionalPages();

	/**
	 * Implements wizard method. Subclasses cannot override it.
	 */
	public final void addPages() {
		addAdditionalPages();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// do nothing - all the work is in the other 'performFinish'
		return true;
	}

	/**
	 * Implements the interface method by looping through template sections and
	 * executing them sequentially.
	 * 
	 * @param project
	 *            the project
	 * @param model
	 *            the plug-in model
	 * @param monitor
	 *            the progress monitor to track the execution progress as part
	 *            of the overall new project creation operation
	 * @return <code>true</code> if the wizard completed the operation with
	 *         success, <code>false</code> otherwise.
	 */
	public boolean performFinish(IProject project, IMonitorModelBase model, IProgressMonitor monitor) {
		try {
			ITemplateSection[] sections = getTemplateSections();
			monitor.beginTask("", sections.length); //$NON-NLS-1$
			for (int i = 0; i < sections.length; i++) {
				sections[i].execute(project, model, new SubProgressMonitor(monitor, 1));
			}
			//No reason to do this any more with the new editors
			//saveTemplateFile(project, null);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}

	/**
	 * Returns the template sections used in this wizard.
	 * 
	 * @return the array of template sections
	 */
	public abstract ITemplateSection[] getTemplateSections();

	/* (non-Javadoc)
	 * @see com.siteview.mde.ui.IPluginContentWizard#getDependencies(java.lang.String)
	 */
	public IMonitorReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		ITemplateSection[] sections = getTemplateSections();
		for (int i = 0; i < sections.length; i++) {
			IMonitorReference[] refs = sections[i].getDependencies(schemaVersion);
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return (IMonitorReference[]) result.toArray(new IMonitorReference[result.size()]);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.ui.IPluginContentWizard#getNewFiles()
	 */
	public String[] getNewFiles() {
		ArrayList result = new ArrayList();
		ITemplateSection[] sections = getTemplateSections();
		for (int i = 0; i < sections.length; i++) {
			String[] newFiles = sections[i].getNewFiles();
			for (int j = 0; j < newFiles.length; j++) {
				if (!result.contains(newFiles[j]))
					result.add(newFiles[j]);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Returns whether this wizard has at least one page
	 * @return whether this wizard has at least one page
	 */
	public boolean hasPages() {
		return getTemplateSections().length > 0;
	}

	public String[] getImportPackages() {
		return new String[0];
	}
}
