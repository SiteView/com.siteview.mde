/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.feature;

import com.siteview.mde.core.monitor.*;

import com.ibm.icu.text.Collator;
import java.util.TreeSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import com.siteview.mde.internal.ui.wizards.ListUtil;
import com.siteview.mde.launching.IPDELauncherConstants;
import com.siteview.mde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class PluginListPage extends BasePluginListPage {
	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return MonitorRegistry.getActiveModels();
		}
	}

	private Combo fLaunchConfigsCombo;
	private Button fInitLaunchConfigButton;
	private CheckboxTableViewer pluginViewer;
	private static final String S_INIT_LAUNCH = "initLaunch"; //$NON-NLS-1$

	public PluginListPage() {
		super("pluginListPage"); //$NON-NLS-1$
		setTitle(MDEUIMessages.NewFeatureWizard_PlugPage_title);
		setDescription(MDEUIMessages.NewFeatureWizard_PlugPage_desc);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		GridData gd;

		String[] launchConfigs = getLaunchConfigurations();

		IDialogSettings settings = getDialogSettings();
		boolean initLaunch = (settings != null) ? settings.getBoolean(S_INIT_LAUNCH) && launchConfigs.length > 0 : false;

		if (launchConfigs.length > 0) {
			fInitLaunchConfigButton = new Button(container, SWT.RADIO);
			fInitLaunchConfigButton.setText(MDEUIMessages.PluginListPage_initializeFromLaunch);
			fInitLaunchConfigButton.setSelection(initLaunch);
			fInitLaunchConfigButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean initLaunchConfigs = fInitLaunchConfigButton.getSelection();
					fLaunchConfigsCombo.setEnabled(initLaunchConfigs);
					tablePart.setEnabled(!initLaunchConfigs);
				}
			});

			fLaunchConfigsCombo = new Combo(container, SWT.READ_ONLY);
			fLaunchConfigsCombo.setItems(launchConfigs);
			gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			gd.horizontalSpan = 2;
			fLaunchConfigsCombo.setLayoutData(gd);
			fLaunchConfigsCombo.select(0);
			fLaunchConfigsCombo.setEnabled(initLaunch);

			Button initPluginsButton = new Button(container, SWT.RADIO);
			initPluginsButton.setText(MDEUIMessages.PluginListPage_initializeFromPlugins);
			gd = new GridData();
			gd.horizontalSpan = 3;
			initPluginsButton.setLayoutData(gd);
			initPluginsButton.setSelection(!initLaunch);
		}

		tablePart.createControl(container, 3, true);
		pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		gd = (GridData) tablePart.getControl().getLayoutData();
		if (launchConfigs.length > 0) {
			gd.horizontalIndent = 30;
			((GridData) tablePart.getCounterLabel().getLayoutData()).horizontalIndent = 30;
		}
		gd.heightHint = 250;
		gd.widthHint = 300;
		pluginViewer.setInput(MDECore.getDefault().getModelManager());
		tablePart.setSelection(new Object[0]);
		tablePart.setEnabled(!initLaunch);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_FEATURE_REFERENCED_PLUGINS);
		pluginViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				TableItem firstTI = pluginViewer.getTable().getSelection()[0];
				if (firstTI.getChecked()) {
					firstTI.setChecked(false);
				} else {
					firstTI.setChecked(true);
				}
				tablePart.updateCount(pluginViewer.getCheckedElements().length);
			}
		});
	}

	public IMonitorBase[] getSelectedPlugins() {
		if (fInitLaunchConfigButton == null || !fInitLaunchConfigButton.getSelection()) {
			Object[] result = tablePart.getSelection();
			IMonitorBase[] plugins = new IMonitorBase[result.length];
			for (int i = 0; i < result.length; i++) {
				IMonitorModelBase model = (IMonitorModelBase) result[i];
				plugins[i] = model.getMonitorBase();
			}
			return plugins;
		}
		return new IMonitorBase[0];
	}

	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_INIT_LAUNCH, fInitLaunchConfigButton != null && fInitLaunchConfigButton.getSelection());
	}

	private String[] getLaunchConfigurations() {
		TreeSet launcherNames = new TreeSet(Collator.getInstance());
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			String[] types = new String[] {EclipseLaunchShortcut.CONFIGURATION_TYPE, IPDELauncherConstants.OSGI_CONFIGURATION_TYPE};
			for (int j = 0; j < 2; j++) {
				ILaunchConfigurationType type = manager.getLaunchConfigurationType(types[j]);
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				for (int i = 0; i < configs.length; i++) {
					if (!DebugUITools.isPrivate(configs[i]))
						launcherNames.add(configs[i].getName());
				}
			}
		} catch (CoreException e) {
		}
		return (String[]) launcherNames.toArray(new String[launcherNames.size()]);
	}

	public ILaunchConfiguration getSelectedLaunchConfiguration() {
		if (fInitLaunchConfigButton == null || !fInitLaunchConfigButton.getSelection())
			return null;

		String configName = fLaunchConfigsCombo.getText();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			String[] types = new String[] {EclipseLaunchShortcut.CONFIGURATION_TYPE, IPDELauncherConstants.OSGI_CONFIGURATION_TYPE};
			for (int j = 0; j < 2; j++) {
				ILaunchConfigurationType type = manager.getLaunchConfigurationType(types[j]);
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				for (int i = 0; i < configs.length; i++) {
					if (configs[i].getName().equals(configName) && !DebugUITools.isPrivate(configs[i]))
						return configs[i];
				}
			}
		} catch (CoreException e) {
		}
		return null;
	}

}
