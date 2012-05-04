/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.ElementList;
import com.siteview.mde.internal.ui.wizards.*;
import com.siteview.mde.ui.IPluginContentWizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewPluginProjectWizard extends NewWizard implements IExecutableExtension {
	public static final String PLUGIN_POINT = "pluginContent"; //$NON-NLS-1$
	public static final String TAG_WIZARD = "wizard"; //$NON-NLS-1$
	public static final String DEF_PROJECT_NAME = "project_name"; //$NON-NLS-1$
	public static final String DEF_TEMPLATE_ID = "template-id"; //$NON-NLS-1$

	private IConfigurationElement fConfig;
	private PluginFieldData fPluginData;
	private IProjectProvider fProjectProvider;
	protected NewProjectCreationPage fMainPage;
	protected PluginContentPage fContentPage;
	private TemplateListSelectionPage fWizardListPage;

	public NewPluginProjectWizard() {
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(MDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(MDEUIMessages.NewProjectWizard_title);
		setNeedsProgressMonitor(true);
		fPluginData = new PluginFieldData();
	}

	public NewPluginProjectWizard(String osgiFramework) {
		this();
		fPluginData.setOSGiFramework(osgiFramework);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new NewProjectCreationPage("main", fPluginData, false, getSelection()); //$NON-NLS-1$
		fMainPage.setTitle(MDEUIMessages.NewProjectWizard_MainPage_title);
		fMainPage.setDescription(MDEUIMessages.NewProjectWizard_MainPage_desc);
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null)
			fMainPage.setInitialProjectName(pname);
		addPage(fMainPage);

		fProjectProvider = new IProjectProvider() {
			public String getProjectName() {
				return fMainPage.getProjectName();
			}

			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}

			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}
		};

		fContentPage = new PluginContentPage("page2", fProjectProvider, fMainPage, fPluginData); //$NON-NLS-1$

		fWizardListPage = new TemplateListSelectionPage(getAvailableCodegenWizards(), fContentPage, MDEUIMessages.WizardListSelectionPage_templates);
		String tid = getDefaultValue(DEF_TEMPLATE_ID);
		if (tid != null)
			fWizardListPage.setInitialTemplateId(tid);

		addPage(fContentPage);
		addPage(fWizardListPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return super.canFinish() && page != fMainPage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			fMainPage.updateData();
			fContentPage.updateData();
			IDialogSettings settings = getDialogSettings();
			if (settings != null) {
				fMainPage.saveSettings(settings);
				fContentPage.saveSettings(settings);
			}
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			IPluginContentWizard contentWizard = fWizardListPage.getSelectedWizard();
			getContainer().run(false, true, new NewProjectCreationOperation(fPluginData, fProjectProvider, contentWizard));

			IWorkingSet[] workingSets = fMainPage.getSelectedWorkingSets();
			if (workingSets.length > 0)
				getWorkbench().getWorkingSetManager().addToWorkingSets(fProjectProvider.getProject(), workingSets);

			return true;
		} catch (InvocationTargetException e) {
			MDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

	protected WizardElement createWizardElement(IConfigurationElement config) {
		String name = config.getAttribute(WizardElement.ATT_NAME);
		String id = config.getAttribute(WizardElement.ATT_ID);
		String className = config.getAttribute(WizardElement.ATT_CLASS);
		if (name == null || id == null || className == null)
			return null;
		WizardElement element = new WizardElement(config);
		String imageName = config.getAttribute(WizardElement.ATT_ICON);
		if (imageName != null) {
			String pluginID = config.getNamespaceIdentifier();
			Image image = MDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pluginID, imageName);
			element.setImage(image);
		}
		return element;
	}

	public ElementList getAvailableCodegenWizards() {
		ElementList wizards = new ElementList("CodegenWizards"); //$NON-NLS-1$
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(MDEPlugin.getPluginId(), PLUGIN_POINT);
		if (point == null)
			return wizards;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(TAG_WIZARD)) {
					WizardElement element = createWizardElement(elements[j]);
					if (element != null) {
						wizards.add(element);
					}
				}
			}
		}
		return wizards;
	}

	public String getPluginId() {
		return fPluginData.getId();
	}

	public String getPluginVersion() {
		return fPluginData.getVersion();
	}

}
