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
package com.siteview.mde.internal.ui.wizards.extension;

import com.siteview.mde.core.monitor.IMonitorBase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardNode;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.elements.ElementList;
import com.siteview.mde.internal.ui.wizards.*;
import com.siteview.mde.ui.IBasePluginWizard;
import com.siteview.mde.ui.IExtensionEditorWizard;

/**
 *
 */
public class ExtensionEditorSelectionPage extends WizardListSelectionPage {
	private IProject fProject;
	private IMonitorBase fPluginBase;
	private IStructuredSelection fSelection;

	/**
	 * @param categories
	 * @param baseCategory
	 * @param message
	 */
	public ExtensionEditorSelectionPage(ElementList wizards) {
		super(wizards, MDEUIMessages.ExtensionEditorSelectionPage_message);
		setTitle(MDEUIMessages.ExtensionEditorSelectionPage_title);
		setDescription(MDEUIMessages.ExtensionEditorSelectionPage_desc);
	}

	public void init(IProject project, IMonitorBase pluginBase, IStructuredSelection selection) {
		this.fProject = project;
		this.fPluginBase = pluginBase;
		this.fSelection = selection;
	}

	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionEditorWizard wizard = createWizard(wizardElement);
				wizard.init(fProject, fPluginBase.getMonitorModel(), fSelection);
				return wizard;
			}

			protected IExtensionEditorWizard createWizard(WizardElement element) throws CoreException {
				return (IExtensionEditorWizard) element.createExecutableExtension();
			}
		};
	}
}
