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
package com.siteview.mde.internal.ui.wizards.tools;

import com.siteview.mde.core.monitor.IMonitorModelBase;

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

public class UpdateBuildpathWizardPage extends WizardPage {
	private IMonitorModelBase[] fSelected;
	private IMonitorModelBase[] fUnmigrated;
	private CheckboxTableViewer pluginListViewer;
	private TablePart tablePart;

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

		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}

		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
			return viewer;
		}
	}

	public UpdateBuildpathWizardPage(IMonitorModelBase[] models, IMonitorModelBase[] selected) {
		super("UpdateBuildpathWizardPage"); //$NON-NLS-1$
		setTitle(MDEUIMessages.UpdateBuildpathWizard_title);
		setDescription(MDEUIMessages.UpdateBuildpathWizard_desc);
		this.fUnmigrated = models;
		this.fSelected = selected;
		tablePart = new TablePart(MDEUIMessages.UpdateBuildpathWizard_availablePlugins);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void dispose() {
		super.dispose();
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
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
		tablePart.setSelection(fSelected);

		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.UPDATE_CLASSPATH);
	}

	public void storeSettings() {
	}

	public Object[] getSelected() {
		return tablePart.getSelection();
	}

	private void dialogChanged() {
		setPageComplete(tablePart.getSelectionCount() > 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return tablePart.getSelectionCount() > 0;
	}
}
