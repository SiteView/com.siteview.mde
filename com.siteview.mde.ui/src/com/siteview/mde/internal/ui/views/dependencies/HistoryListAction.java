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
package com.siteview.mde.internal.ui.views.dependencies;

import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import com.siteview.mde.internal.ui.*;
import org.eclipse.ui.PlatformUI;

public class HistoryListAction extends Action {

	private DependenciesView fView;

	public HistoryListAction(DependenciesView view) {
		fView = view;
		setText(MDEUIMessages.HistoryListAction_label);
		setImageDescriptor(MDEPluginImages.DESC_HISTORY_LIST);
		setDisabledImageDescriptor(MDEPluginImages.DESC_HISTORY_LIST_DISABLED);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IHelpContextIds.HISTORY_LIST_ACTION);
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		String[] historyEntries = fView.getHistoryEntries();
		HistoryListDialog dialog = new HistoryListDialog(MDEPlugin.getActiveWorkbenchShell(), historyEntries);
		if (dialog.open() == Window.OK) {
			fView.setHistoryEntries(dialog.getRemaining());
			String id = dialog.getResult();
			if (id == null) {
				fView.openTo(null);
			} else {
				fView.openTo(MonitorRegistry.findModel(id));
			}
		}
	}

}
