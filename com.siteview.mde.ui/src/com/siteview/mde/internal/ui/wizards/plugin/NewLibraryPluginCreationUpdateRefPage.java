/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.plugin;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import com.siteview.mde.internal.ui.parts.WizardCheckboxTablePart;
import com.siteview.mde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class NewLibraryPluginCreationUpdateRefPage extends WizardPage {

	private IMonitorModelBase[] fSelected;
	private IMonitorModelBase[] fUnmigrated;
	private CheckboxTableViewer pluginListViewer;
	private TablePart tablePart;
	private LibraryPluginFieldData fData;

	public class BuildpathContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (fUnmigrated != null)
				return fUnmigrated;
			return new Object[0];
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}

		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
			return viewer;
		}
	}

	public NewLibraryPluginCreationUpdateRefPage(LibraryPluginFieldData data, Collection initialJarPaths, Collection selection) {
		super("UpdateReferences"); //$NON-NLS-1$
		setTitle(MDEUIMessages.UpdateBuildpathWizard_title);
		setDescription(MDEUIMessages.UpdateBuildpathWizard_desc);
		computeUnmigrated();
		computeSelected(selection);
		fData = data;
		tablePart = new TablePart(MDEUIMessages.UpdateBuildpathWizard_availablePlugins);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void computeSelected(Collection initialSelection) {
		if (initialSelection == null || initialSelection.size() == 0)
			return;
		Set selected = new HashSet();
		Iterator iter = initialSelection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			if (obj instanceof IProject) {
				IMonitorModelBase model = MonitorRegistry.findModel((IProject) obj);
				if (model != null) {
					selected.add(model);
				}
			}
		}
		fSelected = (IMonitorModelBase[]) selected.toArray(new IMonitorModelBase[selected.size()]);

	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		tablePart.createControl(container);

		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new BuildpathContentProvider());
		pluginListViewer.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;

		pluginListViewer.setInput(MDEPlugin.getDefault());
		if (fSelected != null && fSelected.length > 0) {
			tablePart.setSelection(fSelected);
		}
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.UPDATE_CLASSPATH);
	}

	private void computeUnmigrated() {
		IMonitorModelBase[] models = MonitorRegistry.getWorkspaceModels();
		ArrayList modelArray = new ArrayList();
		try {
			for (int i = 0; i < models.length; i++) {
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(models[i]);
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
		fUnmigrated = (IMonitorModelBase[]) modelArray.toArray(new IMonitorModelBase[modelArray.size()]);
	}

	public void setEnable(boolean enabled) {
		tablePart.setEnabled(enabled);
	}

	public void updateData() {
		IMonitorModelBase[] modelBase = new IMonitorModelBase[tablePart.getSelectionCount()];
		for (int i = 0; i < modelBase.length; ++i) {
			modelBase[i] = (IMonitorModelBase) tablePart.getSelection()[i];
		}
		fData.setPluginsToUpdate(modelBase);
	}

}
