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
package com.siteview.mde.internal.ui.search.dependencies;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.dialogs.ListDialog;

public class ShowResultsAction extends Action {

	private IMonitorModelBase fModel;
	Object[] fUnusedImports;
	private boolean fReadOnly;

	public ShowResultsAction(IMonitorModelBase model, Object[] unused, boolean readOnly) {
		fModel = model;
		fUnusedImports = unused;
		fReadOnly = readOnly;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fUnusedImports.length == 0) {
			MessageDialog.openInformation(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.UnusedDependencies_title, MDEUIMessages.UnusedDependencies_notFound);
		} else {
			Dialog dialog;
			if (fReadOnly) {
				// Launched from Dependencies View, show information dialog
				dialog = getUnusedDependeciesInfoDialog();
			} else {
				dialog = new UnusedImportsDialog(MDEPlugin.getActiveWorkbenchShell(), fModel, fUnusedImports);
				dialog.create();
			}
			dialog.getShell().setText(MDEUIMessages.UnusedDependencies_title);
			dialog.open();
		}
	}

	/**
	 * @return Dialog
	 */
	private Dialog getUnusedDependeciesInfoDialog() {
		ListDialog dialog = new ListDialog(MDEPlugin.getActiveWorkbenchShell());
		dialog.setAddCancelButton(false);
		dialog.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return fUnusedImports;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		dialog.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		dialog.setInput(this);
		dialog.create();
		dialog.getTableViewer().setComparator(new UnusedImportsDialog.Comparator());
		return dialog;
	}
}
