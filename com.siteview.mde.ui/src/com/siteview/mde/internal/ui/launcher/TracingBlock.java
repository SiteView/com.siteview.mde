/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262885
 *******************************************************************************/
package com.siteview.mde.internal.ui.launcher;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.TracingOptionsManager;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.SWTUtil;
import com.siteview.mde.internal.ui.wizards.ListUtil;
import com.siteview.mde.launching.IPDELauncherConstants;
import com.siteview.mde.ui.launcher.AbstractLauncherTab;
import com.siteview.mde.ui.launcher.TracingTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;

public class TracingBlock {

	private TracingTab fTab;
	private Button fTracingCheck;
	private CheckboxTableViewer fPluginViewer;
	private IMonitorModelBase[] fTraceableModels;
	private Properties fMasterOptions = new Properties();
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Hashtable fPropertySources = new Hashtable();
	private FormToolkit fToolkit;
	private ScrolledPageBook fPageBook;

	/**
	 * The last selected item in the list is stored in the dialog settings.
	 */
	private static final String TRACING_SETTINGS = "TracingTab"; //$NON-NLS-1$
	private static final String SETTINGS_SELECTED_PLUGIN = "selectedPlugin"; //$NON-NLS-1$

	public TracingBlock(TracingTab tab) {
		fTab = tab;
	}

	public AbstractLauncherTab getTab() {
		return fTab;
	}

	public void createControl(Composite parent) {
		fTracingCheck = new Button(parent, SWT.CHECK);
		fTracingCheck.setText(MDEUIMessages.TracingLauncherTab_tracing);
		fTracingCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTracingCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				masterCheckChanged(true);
				fTab.updateLaunchConfigurationDialog();
				if (fTracingCheck.getSelection()) {
					IStructuredSelection selection = (IStructuredSelection) fPluginViewer.getSelection();
					if (!selection.isEmpty()) {
						pluginSelected((IMonitorModelBase) selection.getFirstElement(), fPluginViewer.getChecked(selection.getFirstElement()));
					}
				}
			}
		});

		createSashSection(parent);
		createButtonSection(parent);
	}

	private void createSashSection(Composite container) {
		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		createPluginViewer(sashForm);
		createPropertySheetClient(sashForm);
	}

	private void createPluginViewer(Composite sashForm) {
		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 1;
		composite.setLayout(layout);

		fPluginViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		fPluginViewer.setContentProvider(ArrayContentProvider.getInstance());
		fPluginViewer.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		fPluginViewer.setComparator(new ListUtil.PluginComparator());
		fPluginViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				CheckboxTableViewer tableViewer = (CheckboxTableViewer) e.getSource();
				boolean selected = tableViewer.getChecked(getSelectedModel());
				pluginSelected(getSelectedModel(), selected);
				storeSelectedModel();
			}
		});
		fPluginViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				CheckboxTableViewer tableViewer = (CheckboxTableViewer) event.getSource();
				tableViewer.setSelection(new StructuredSelection(event.getElement()));
				pluginSelected(getSelectedModel(), event.getChecked());
				fTab.updateLaunchConfigurationDialog();
			}
		});
		fPluginViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				CheckboxTableViewer tableViewer = (CheckboxTableViewer) event.getSource();
				Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
				boolean addingCheck = !tableViewer.getChecked(selection);
				tableViewer.setChecked(selection, addingCheck);
				pluginSelected(getSelectedModel(), addingCheck);
				fTab.updateLaunchConfigurationDialog();
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 125;
		gd.heightHint = 100;
		fPluginViewer.getTable().setLayoutData(gd);
	}

	private void createPropertySheetClient(Composite sashForm) {
		Composite tableChild = new Composite(sashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		tableChild.setLayout(layout);
		int margin = createPropertySheet(tableChild);
		layout.marginWidth = layout.marginHeight = margin;
	}

	private void createButtonSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		fSelectAllButton = new Button(container, SWT.PUSH);
		fSelectAllButton.setText(MDEUIMessages.TracingLauncherTab_selectAll);
		fSelectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fSelectAllButton);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginViewer.setAllChecked(true);
				pluginSelected(getSelectedModel(), true);
				fTab.updateLaunchConfigurationDialog();
			}
		});

		fDeselectAllButton = new Button(container, SWT.PUSH);
		fDeselectAllButton.setText(MDEUIMessages.TracinglauncherTab_deselectAll);
		fDeselectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fDeselectAllButton);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginViewer.setAllChecked(false);
				pluginSelected(getSelectedModel(), false);
				fTab.updateLaunchConfigurationDialog();
			}
		});
	}

	protected int createPropertySheet(Composite parent) {
		fToolkit = new FormToolkit(parent.getDisplay());
		int toolkitBorderStyle = fToolkit.getBorderStyle();
		int style = toolkitBorderStyle == SWT.BORDER ? SWT.NULL : SWT.BORDER;

		Composite container = new Composite(parent, style);
		FillLayout flayout = new FillLayout();
		flayout.marginWidth = 1;
		flayout.marginHeight = 1;
		container.setLayout(flayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPageBook = new ScrolledPageBook(container, style | SWT.V_SCROLL | SWT.H_SCROLL);
		fToolkit.adapt(fPageBook, false, false);

		if (style == SWT.NULL) {
			fPageBook.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
			fToolkit.paintBordersFor(container);
		}
		return style == SWT.NULL ? 2 : 0;
	}

	public void initializeFrom(ILaunchConfiguration config) {
		fMasterOptions.clear();
		disposePropertySources();
		try {
			fTracingCheck.setSelection(config.getAttribute(IPDELauncherConstants.TRACING, false));
			Map options = config.getAttribute(IPDELauncherConstants.TRACING_OPTIONS, (Map) null);
			if (options == null)
				options = MDECore.getDefault().getTracingOptionsManager().getTracingTemplateCopy();
			else
				options = MDECore.getDefault().getTracingOptionsManager().getTracingOptions(options);
			fMasterOptions.putAll(options);
			masterCheckChanged(false);
			String checked = config.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
			if (checked == null) {
				fPluginViewer.setAllChecked(true);
			} else if (checked.equals(IPDELauncherConstants.TRACING_NONE)) {
				fPluginViewer.setAllChecked(false);
			} else {
				StringTokenizer tokenizer = new StringTokenizer(checked, ","); //$NON-NLS-1$
				ArrayList list = new ArrayList();
				while (tokenizer.hasMoreTokens()) {
					String id = tokenizer.nextToken();
					IMonitorModelBase model = MonitorRegistry.findModel(id);
					model = MonitorRegistry.findModel(id);
					if (model != null) {
						list.add(model);
					}
				}
				fPluginViewer.setCheckedElements(list.toArray());
				IMonitorModelBase model = getLastSelectedPlugin();
				if (model != null) {
					fPluginViewer.setSelection(new StructuredSelection(model), true);
					pluginSelected(model, list.contains(model));
				} else {
					pluginSelected(null, false);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		boolean tracingEnabled = fTracingCheck.getSelection();
		config.setAttribute(IPDELauncherConstants.TRACING, tracingEnabled);
		if (tracingEnabled) {
			boolean changes = false;
			for (Enumeration elements = fPropertySources.elements(); elements.hasMoreElements();) {
				TracingPropertySource source = (TracingPropertySource) elements.nextElement();
				if (source.isModified()) {
					changes = true;
					source.save();
				}
			}
			if (changes)
				config.setAttribute(IPDELauncherConstants.TRACING_OPTIONS, fMasterOptions);
		}

		Object[] checked = fPluginViewer.getCheckedElements();
		if (checked.length == 0) {
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
		} else if (checked.length == fPluginViewer.getTable().getItemCount()) {
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
		} else {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < checked.length; i++) {
				IMonitorModelBase model = (IMonitorModelBase) checked[i];
				buffer.append(model.getMonitorBase().getId());
				if (i < checked.length - 1)
					buffer.append(',');
			}
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, buffer.toString());
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.TRACING, false);
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
	}

	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		fPageBook.getParent().getParent().layout(true);
	}

	public void dispose() {
		if (fToolkit != null)
			fToolkit.dispose();
	}

	public FormToolkit getToolkit() {
		return fToolkit;
	}

	private IMonitorModelBase getSelectedModel() {
		if (fTracingCheck.isEnabled()) {
			Object item = ((IStructuredSelection) fPluginViewer.getSelection()).getFirstElement();
			if (item instanceof IMonitorModelBase)
				return ((IMonitorModelBase) item);
		}
		return null;
	}

	private void pluginSelected(IMonitorModelBase model, boolean checked) {
		TracingPropertySource source = getPropertySource(model);
		if (source == null) {
			fPageBook.showEmptyPage();
		} else {
			PageBookKey key = new PageBookKey(model, checked);
			if (!fPageBook.hasPage(key)) {
				Composite parent = fPageBook.createPage(key);
				source.createContents(parent, checked);
			}
			fPageBook.showPage(key);
		}
	}

	private IMonitorModelBase[] getTraceableModels() {
		if (fTraceableModels == null) {
			IMonitorModelBase[] models = MonitorRegistry.getActiveModels();
			ArrayList result = new ArrayList();
			for (int i = 0; i < models.length; i++) {
				if (TracingOptionsManager.isTraceable(models[i]))
					result.add(models[i]);
			}
			fTraceableModels = (IMonitorModelBase[]) result.toArray(new IMonitorModelBase[result.size()]);
		}
		return fTraceableModels;
	}

	/**
	 * Returns the last selected plug-in as stored in dialog settings or <code>null</code> if no
	 * previous selection is found.
	 * 
	 * @return model for the last selected plug-in or <code>null</code>
	 */
	private IMonitorModelBase getLastSelectedPlugin() {
		IDialogSettings settings = MDEPlugin.getDefault().getDialogSettings().getSection(TRACING_SETTINGS);
		if (settings != null) {
			String id = settings.get(SETTINGS_SELECTED_PLUGIN);
			if (id != null && id.trim().length() > 0) {
				return MonitorRegistry.findModel(id);
			}
		}
		return null;
	}

	/**
	 * Stores the currently selected model in the dialog settings for later retrieval using
	 * {@link #getLastSelectedPlugin()}.  If no model is selected, the settings are cleared.
	 */
	private void storeSelectedModel() {
		IDialogSettings settings = MDEPlugin.getDefault().getDialogSettings().getSection(TRACING_SETTINGS);
		if (settings == null) {
			settings = MDEPlugin.getDefault().getDialogSettings().addNewSection(TRACING_SETTINGS);
		}
		IMonitorModelBase model = getSelectedModel();
		String id = (model == null) ? null : model.getMonitorBase().getId();
		settings.put(SETTINGS_SELECTED_PLUGIN, id);
	}

	private TracingPropertySource getPropertySource(IMonitorModelBase model) {
		if (model == null)
			return null;
		TracingPropertySource source = (TracingPropertySource) fPropertySources.get(model);
		if (source == null) {
			String id = model.getMonitorBase().getId();
			Hashtable defaults = MDECore.getDefault().getTracingOptionsManager().getTemplateTable(id);
			source = new TracingPropertySource(model, fMasterOptions, defaults, this);
			fPropertySources.put(model, source);
		}
		return source;
	}

	private void masterCheckChanged(boolean userChange) {
		boolean enabled = fTracingCheck.getSelection();
		fPluginViewer.getTable().setEnabled(enabled);
		Control currentPage = fPageBook.getCurrentPage();
		if (currentPage != null && enabled == false) {
			fPageBook.showEmptyPage();
		}
		if (enabled) {
			fPluginViewer.setInput(getTraceableModels());
		}
		fSelectAllButton.setEnabled(enabled);
		fDeselectAllButton.setEnabled(enabled);
	}

	private void disposePropertySources() {
		Enumeration elements = fPropertySources.elements();
		while (elements.hasMoreElements()) {
			TracingPropertySource source = (TracingPropertySource) elements.nextElement();
			fPageBook.removePage(source.getModel());
		}
		fPropertySources.clear();
	}

	private class PageBookKey {
		IMonitorModelBase fModel;
		boolean fEnabled;

		PageBookKey(IMonitorModelBase model, boolean enabled) {
			fModel = model;
			fEnabled = enabled;
		}

		public boolean equals(Object object) {
			if (object instanceof PageBookKey) {
				return fEnabled == ((PageBookKey) object).fEnabled && fModel.equals(((PageBookKey) object).fModel);
			}
			return false;
		}

		public int hashCode() {
			return fModel.hashCode() + (fEnabled ? 1 : 0);
		}
	}

}
