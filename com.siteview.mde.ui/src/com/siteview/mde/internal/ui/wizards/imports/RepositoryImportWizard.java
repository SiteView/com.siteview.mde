/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.imports;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import com.siteview.mde.internal.core.project.BundleProjectService;
import com.siteview.mde.internal.ui.*;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.team.ui.TeamUI;

/**
 * Wizard to import plug-ins from a repository.
 * 
 * @since 3.6
 */
public class RepositoryImportWizard extends Wizard {

	/**
	 * Map of import delegates to import descriptions as provided by the {@link BundleProjectService}
	 */
	private Map fImportMap;

	/**
	 * Map of importer identifier to associated wizard import page
	 */
	private Map fIdToPages = new HashMap();

	private static final String STORE_SECTION = "RepositoryImportWizard"; //$NON-NLS-1$

	/**
	 * Map of import delegates to import descriptions.
	 * 
	 * @param importMap
	 */
	public RepositoryImportWizard(Map importMap) {
		IDialogSettings masterSettings = MDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(MDEUIMessages.ImportWizard_title);
		fImportMap = importMap;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		Iterator iterator = fImportMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			IBundleImporter importer = (IBundleImporter) entry.getKey();
			ScmUrlImportDescription[] descriptions = (ScmUrlImportDescription[]) entry.getValue();
			IScmUrlImportWizardPage page = (IScmUrlImportWizardPage) fIdToPages.get(importer.getId());
			if (page == null) {
				try {
					page = TeamUI.getPages(descriptions)[0];
				} catch (CoreException e) {
					MDEPlugin.log(e);
				}
				if (page != null) {
					fIdToPages.put(importer.getId(), page);
					addPage(page);
				}
			}
		}
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// collect the bundle descriptions from each page and import
		List plugins = new ArrayList();
		IWizardPage[] pages = getPages();
		Map importMap = new HashMap();
		for (int i = 0; i < pages.length; i++) {
			IScmUrlImportWizardPage page = (IScmUrlImportWizardPage) pages[i];
			if (page.finish()) {
				ScmUrlImportDescription[] descriptions = page.getSelection();
				if (descriptions != null && descriptions.length > 0) {
					for (int j = 0; j < descriptions.length; j++) {
						if (j == 0) {
							Object importer = descriptions[j].getProperty(BundleProjectService.BUNDLE_IMPORTER);
							if (importer != null) {
								importMap.put(importer, descriptions);
							}
						}
						Object plugin = descriptions[j].getProperty(BundleProjectService.PLUGIN);
						if (plugin != null) {
							plugins.add(plugin);
						}
					}
				}
			} else {
				return false;
			}
		}
		if (!importMap.isEmpty()) {
			PluginImportWizard.doImportOperation(PluginImportOperation.IMPORT_FROM_REPOSITORY, (IMonitorModelBase[]) plugins.toArray(new IMonitorModelBase[plugins.size()]), false, false, null, importMap);
		}
		return true;
	}
}
