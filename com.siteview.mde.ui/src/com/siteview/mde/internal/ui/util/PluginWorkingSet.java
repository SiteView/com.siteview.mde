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
package com.siteview.mde.internal.ui.util;

import com.siteview.mde.core.monitor.*;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.MDEPreferencesManager;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;

public class PluginWorkingSet extends WizardPage implements IWorkingSetPage {

	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object[] getElements(Object inputElement) {
			return MonitorRegistry.getAllModels();
		}

		public Object[] getChildren(Object parentElement) {
			return null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}
	}

	class WorkingSetLabelProvider extends LabelProvider {

		MDEPreferencesManager pref = MDEPlugin.getDefault().getPreferenceManager();

		public WorkingSetLabelProvider() {
			MDEPlugin.getDefault().getLabelProvider().connect(this);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof IMonitorModelBase) {
				IMonitorBase plugin = ((IMonitorModelBase) element).getMonitorBase();
				String showType = pref.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
				if (showType.equals(IPreferenceConstants.VALUE_USE_IDS))
					return plugin.getId();
				return plugin.getTranslatedName();
			}
			return super.getText(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return MDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		public void dispose() {
			super.dispose();
			MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		}

	}

	class CheckboxFilteredTree extends FilteredTree {

		public CheckboxFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
			super(parent, treeStyle, filter, true);
		}

		protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
			return new CheckboxTreeViewer(parent, style);
		}

		public CheckboxTreeViewer getCheckboxTreeViewer() {
			return (CheckboxTreeViewer) getViewer();
		}

	}

	private IWorkingSet fWorkingSet;
	private Text fWorkingSetName;
	private CheckboxFilteredTree fTree;
	private boolean fFirstCheck;

	public PluginWorkingSet() {
		super("page1", MDEUIMessages.PluginWorkingSet_title, MDEPluginImages.DESC_DEFCON_WIZ); //$NON-NLS-1$ 
		MDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#finish()
	 */
	public void finish() {
		Object[] checked = fTree.getCheckboxTreeViewer().getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < checked.length; i++) {
			String id = ((IMonitorModelBase) checked[i]).getMonitorBase().getId();
			if (id != null && id.length() > 0)
				list.add(new PersistablePluginObject(id));
		}
		PersistablePluginObject[] objects = (PersistablePluginObject[]) list.toArray(new PersistablePluginObject[list.size()]);

		String workingSetName = fWorkingSetName.getText().trim();
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet = workingSetManager.createWorkingSet(workingSetName, objects);
		} else {
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements(objects);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#getSelection()
	 */
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#setSelection(org.eclipse.ui.IWorkingSet)
	 */
	public void setSelection(IWorkingSet workingSet) {
		fWorkingSet = workingSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(MDEUIMessages.PluginWorkingSet_setName);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fWorkingSetName = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWorkingSetName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		fWorkingSetName.setFocus();

		label = new Label(composite, SWT.WRAP);
		label.setText(MDEUIMessages.PluginWorkingSet_setContent);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTree = new CheckboxFilteredTree(composite, SWT.BORDER, new PatternFilter());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fTree.getViewer().getControl().setLayoutData(gd);
		final IStructuredContentProvider fTableContentProvider = new ContentProvider();
		fTree.getCheckboxTreeViewer().setContentProvider(fTableContentProvider);
		fTree.getCheckboxTreeViewer().setLabelProvider(new WorkingSetLabelProvider());
		fTree.getCheckboxTreeViewer().setUseHashlookup(true);
		fTree.getCheckboxTreeViewer().setInput(MDECore.getDefault());

		fTree.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validatePage();
			}
		});

		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, true));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(MDEUIMessages.PluginWorkingSet_selectAll_label);
		selectAllButton.setToolTipText(MDEUIMessages.PluginWorkingSet_selectAll_toolTip);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.getCheckboxTreeViewer().setCheckedElements(fTableContentProvider.getElements(fTree.getCheckboxTreeViewer().getInput()));
				validatePage();
			}
		});
		selectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAllButton);

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText(MDEUIMessages.PluginWorkingSet_deselectAll_label);
		deselectAllButton.setToolTipText(MDEUIMessages.PluginWorkingSet_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTree.getCheckboxTreeViewer().setCheckedElements(new Object[0]);
				validatePage();
			}
		});
		deselectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deselectAllButton);
		setPageComplete(false);
		setMessage(MDEUIMessages.PluginWorkingSet_message);

		initialize();
		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PLUGIN_WORKING_SET);
	}

	/**
	 * 
	 */
	private void initialize() {
		if (fWorkingSet != null) {
			HashSet set = new HashSet();
			IAdaptable[] elements = fWorkingSet.getElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof PersistablePluginObject)
					set.add(((PersistablePluginObject) elements[i]).getPluginID());
			}

			IMonitorModelBase[] bases = MonitorRegistry.getAllModels();
			for (int i = 0; i < bases.length; i++) {

				String id = bases[i].getMonitorBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					fTree.getCheckboxTreeViewer().setChecked(bases[i], true);
					set.remove(id);
				}
				if (set.isEmpty())
					break;
			}
			fWorkingSetName.setText(fWorkingSet.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void validatePage() {
		String errorMessage = null;
		String newText = fWorkingSetName.getText();

		if (newText.trim().length() == 0) {
			errorMessage = MDEUIMessages.PluginWorkingSet_emptyName;
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck = false;
				return;
			}
		}
		if (errorMessage == null && fTree.getCheckboxTreeViewer().getCheckedElements().length == 0) {
			errorMessage = MDEUIMessages.PluginWorkingSet_noPluginsChecked;
		}

		if (errorMessage == null && fWorkingSet == null) {
			IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (int i = 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage = MDEUIMessages.PluginWorkingSet_nameInUse;
					break;
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

}
