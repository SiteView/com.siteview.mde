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
package com.siteview.mde.internal.ui.launcher;

import com.siteview.mde.launching.IPDELauncherConstants;

import java.util.TreeSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import com.siteview.mde.internal.launching.IPDEConstants;
import com.siteview.mde.internal.launching.launcher.LauncherUtils;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.ui.launcher.AbstractLauncherTab;

public class JUnitProgramBlock extends ProgramBlock {

	public JUnitProgramBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	protected String getApplicationAttribute() {
		return IPDELauncherConstants.APP_TO_TEST;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (!LauncherUtils.requiresUI(config))
			config.setAttribute(IPDELauncherConstants.APPLICATION, IPDEConstants.CORE_TEST_APPLICATION);
		else
			super.setDefaults(config);
	}

	protected String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		result.add(MDEUIMessages.JUnitProgramBlock_headless);
		String[] appNames = super.getApplicationNames();
		for (int i = 0; i < appNames.length; i++) {
			result.add(appNames[i]);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#initializeApplicationSection(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected void initializeApplicationSection(ILaunchConfiguration config) throws CoreException {
		String application = config.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
		if (IPDEConstants.CORE_TEST_APPLICATION.equals(application))
			fApplicationCombo.setText(MDEUIMessages.JUnitProgramBlock_headless);
		else
			super.initializeApplicationSection(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#saveApplicationSection(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		if (fApplicationCombo.getText().equals(MDEUIMessages.JUnitProgramBlock_headless)) {
			String appName = fApplicationCombo.isEnabled() ? IPDEConstants.CORE_TEST_APPLICATION : null;
			config.setAttribute(IPDELauncherConstants.APPLICATION, appName);
			config.setAttribute(IPDELauncherConstants.APP_TO_TEST, (String) null);
		} else {
			config.setAttribute(IPDELauncherConstants.APPLICATION, (String) null);
			super.saveApplicationSection(config);
		}
	}

}
