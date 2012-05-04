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
package com.siteview.mde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import com.siteview.mde.internal.launching.launcher.LaunchArgumentsHelper;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.launching.IPDELauncherConstants;
import com.siteview.mde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ConfigurationAreaBlock extends BaseBlock {

	private Button fUseDefaultLocationButton;
	private Button fClearConfig;
	private String fLastEnteredConfigArea;
	private String fLastKnownConfigName;
	private static String DEFAULT_DIR = "${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/"; //$NON-NLS-1$

	public ConfigurationAreaBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(MDEUIMessages.ConfigurationTab_configAreaGroup);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fUseDefaultLocationButton = new Button(group, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fUseDefaultLocationButton.setLayoutData(gd);
		fUseDefaultLocationButton.setText(MDEUIMessages.ConfigurationTab_useDefaultLoc);
		fUseDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean useDefaultArea = fUseDefaultLocationButton.getSelection();
				if (useDefaultArea)
					fLocationText.setText(DEFAULT_DIR + fLastKnownConfigName);
				else
					fLocationText.setText(fLastEnteredConfigArea);
				enableBrowseSection(!useDefaultArea);
				fLocationText.setEditable(!useDefaultArea);
				if (useDefaultArea)
					fLocationText.setEnabled(true);
			}
		});

		createText(group, MDEUIMessages.ConfigurationTab_configLog, 20);

		Composite buttons = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);

		fClearConfig = new Button(buttons, SWT.CHECK);
		fClearConfig.setText(MDEUIMessages.ConfigurationTab_clearArea);
		fClearConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fClearConfig.addSelectionListener(fListener);

		createButtons(buttons, new String[] {MDEUIMessages.BaseBlock_workspace, MDEUIMessages.BaseBlock_filesystem, MDEUIMessages.BaseBlock_variables});
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		fLastKnownConfigName = configuration.getName();
		boolean useDefaultArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true);
		fUseDefaultLocationButton.setSelection(useDefaultArea);
		enableBrowseSection(!useDefaultArea);
		fLocationText.setEditable(!useDefaultArea);
		if (useDefaultArea)
			fLocationText.setEnabled(true);

		fClearConfig.setSelection(configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false));
		fLastEnteredConfigArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, ""); //$NON-NLS-1$

		if (useDefaultArea)
			fLocationText.setText(DEFAULT_DIR + fLastKnownConfigName);
		else
			fLocationText.setText(fLastEnteredConfigArea);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, fUseDefaultLocationButton.getSelection());

		if (!fLastKnownConfigName.equals(configuration.getName())) {
			fLastKnownConfigName = configuration.getName();
			if (fUseDefaultLocationButton.getSelection()) {
				fLastEnteredConfigArea = DEFAULT_DIR + fLastKnownConfigName;
				fLocationText.setText(fLastEnteredConfigArea);
			} else {
				fLastEnteredConfigArea = getLocation();
			}
		}

		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, fLastEnteredConfigArea);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, fClearConfig.getSelection());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration, boolean isJUnit) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, isJUnit);

		boolean useDefaultArea = !isJUnit || LaunchArgumentsHelper.getDefaultJUnitWorkspaceIsContainer();
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, useDefaultArea);

		String location;
		if (isJUnit && !useDefaultArea) {
			location = LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation();
		} else {
			location = DEFAULT_DIR + configuration.getName();
		}
		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, location);
	}

	protected String getName() {
		return MDEUIMessages.ConfigurationAreaBlock_name;
	}

	protected boolean isFile() {
		return false;
	}

	public String validate() {
		if (fUseDefaultLocationButton.getSelection())
			return null;
		return super.validate();
	}
}
