/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.ui.editor.ILauncherFormPageHelper;
import com.siteview.mde.internal.ui.editor.MDELauncherFormEditor;

public class MonitorLauncherFormPageHelper implements ILauncherFormPageHelper {
	MDELauncherFormEditor fEditor;

	public MonitorLauncherFormPageHelper(MDELauncherFormEditor editor) {
		fEditor = editor;
	}

	public Object getLaunchObject() {
		return fEditor.getCommonProject();
	}

	public boolean isOSGi() {
		return !((MonitorEditor) fEditor).showExtensionTabs();
	}

	public void preLaunch() {
	}
}
