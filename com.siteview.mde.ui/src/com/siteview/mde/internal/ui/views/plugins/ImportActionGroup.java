/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.plugins;

import com.siteview.mde.core.monitor.*;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.internal.core.project.BundleProjectService;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.wizards.imports.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class ImportActionGroup extends ActionGroup {

	class ImportAction extends Action {
		IStructuredSelection fSel;
		int fImportType;

		ImportAction(int importType, IStructuredSelection selection) {
			fSel = selection;
			fImportType = importType;
			switch (fImportType) {
				case PluginImportOperation.IMPORT_BINARY :
					setText(MDEUIMessages.PluginsView_asBinaryProject);
					break;
				case PluginImportOperation.IMPORT_BINARY_WITH_LINKS :
					setText(MDEUIMessages.ImportActionGroup_binaryWithLinkedContent);
					break;
				case PluginImportOperation.IMPORT_WITH_SOURCE :
					setText(MDEUIMessages.PluginsView_asSourceProject);
					break;
				case PluginImportOperation.IMPORT_FROM_REPOSITORY :
					setText(MDEUIMessages.ImportActionGroup_Repository_project);
					break;
			}
		}

		public void run() {
			handleImport(fImportType, fSel);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			String menuName = null;
			if (sSelection.getFirstElement() instanceof IMonitorExtension || sSelection.getFirstElement() instanceof IMonitorExtensionPoint)
				menuName = MDEUIMessages.ImportActionGroup_importContributingPlugin;
			else
				menuName = MDEUIMessages.PluginsView_import;
			MenuManager importMenu = new MenuManager(menuName);
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY_WITH_LINKS, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_WITH_SOURCE, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_FROM_REPOSITORY, sSelection));
			menu.add(importMenu);
		}
	}

	private void handleImport(int importType, IStructuredSelection selection) {
		ArrayList externalModels = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IMonitorModelBase model = getModel(iter.next());
			if (model != null && model.getUnderlyingResource() == null)
				externalModels.add(model);
		}
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		IMonitorModelBase[] models = (IMonitorModelBase[]) externalModels.toArray(new IMonitorModelBase[externalModels.size()]);
		if (importType == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			Map importMap = getImportDescriptions(display.getActiveShell(), models);
			if (importMap != null) {
				RepositoryImportWizard wizard = new RepositoryImportWizard(importMap);
				WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
				dialog.open();
			}
		} else {
			PluginImportWizard.doImportOperation(display.getActiveShell(), importType, models, false);
		}
	}

	/**
	 * Return a map of {@link IBundleImporter} > Array of {@link ScmUrlImportDescription} to be imported.
	 * 
	 * @param shell shell to open message dialogs on, if required
	 * @param models candidate models
	 * @return  map of importer to import descriptions
	 */
	private Map getImportDescriptions(Shell shell, IMonitorModelBase[] models) {
		BundleProjectService service = (BundleProjectService) BundleProjectService.getDefault();
		try {
			Map descriptions = service.getImportDescriptions(models); // all possible descriptions
			if (!descriptions.isEmpty()) {
				return descriptions;
			}
			// no applicable importers for selected models
			MessageDialog.openInformation(shell, MDEUIMessages.ImportWizard_title, MDEUIMessages.ImportActionGroup_cannot_import);
		} catch (CoreException e) {
			MDEPlugin.log(e);
			MessageDialog.openError(shell, MDEUIMessages.ImportWizard_title, e.getMessage());
		}
		return null;
	}

	public static boolean canImport(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IMonitorModelBase model = getModel(iter.next());
			if (model != null && model.getUnderlyingResource() == null)
				return true;
		}
		return false;
	}

	private static IMonitorModelBase getModel(Object next) {
		IMonitorModelBase model = null;
		if (next instanceof IMonitorModelBase)
			model = (IMonitorModelBase) next;
		else if (next instanceof IMonitorBase)
			model = ((IMonitorBase) next).getMonitorModel();
		else if (next instanceof IMonitorExtension)
			model = ((IMonitorExtension) next).getMonitorModel();
		else if (next instanceof IMonitorExtensionPoint)
			model = ((IMonitorExtensionPoint) next).getMonitorModel();
		return model;
	}
}
