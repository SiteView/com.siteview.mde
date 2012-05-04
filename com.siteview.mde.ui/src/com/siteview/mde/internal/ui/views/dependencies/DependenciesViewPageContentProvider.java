/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.dependencies;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.ModelEntry;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.service.resolver.BundleDescription;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;

public class DependenciesViewPageContentProvider extends DefaultContentProvider implements IMonitorModelListener {

	private DependenciesView fView;

	private StructuredViewer fViewer;

	/**
	 * Constructor.
	 */
	public DependenciesViewPageContentProvider(DependenciesView view) {
		this.fView = view;
		attachModelListener();
	}

	public void attachModelListener() {
		MDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	public void removeModelListener() {
		MDECore.getDefault().getModelManager().removePluginModelListener(this);
	}

	public void dispose() {
		removeModelListener();
	}

	private void handleModifiedModels(ModelEntry[] modified) {
		Object input = fViewer.getInput();
		if (input instanceof IMonitorModelBase) {
			BundleDescription desc = ((IMonitorModelBase) input).getBundleDescription();
			String inputID = (desc != null) ? desc.getSymbolicName() : ((IMonitorModelBase) input).getMonitorBase().getId();

			for (int i = 0; i < modified.length; i++) {
				ModelEntry entry = modified[i];
				if (entry.getId().equals(inputID)) {
					// if we find a matching id to our current input, check to see if the input still exists
					if (modelExists(entry, (IMonitorModelBase) input))
						fView.updateTitle(input);
					else
						// if input model does not exist, clear view
						fView.openTo(null);
					return;
				}
			}
		}
	}

	private boolean modelExists(ModelEntry entry, IMonitorModelBase input) {
		IMonitorModelBase[][] entries = new IMonitorModelBase[][] {entry.getExternalModels(), entry.getWorkspaceModels()};
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < entries[i].length; j++) {
				if (entries[i][j].equals(input))
					return true;
			}
		}
		return false;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fView.updateTitle(newInput);
		this.fViewer = (StructuredViewer) viewer;
	}

	public void modelsChanged(final MonitorModelDelta delta) {
		if (fViewer == null || fViewer.getControl().isDisposed())
			return;

		fViewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (fViewer.getControl().isDisposed())
					return;
				try {
					if ((kind & MonitorModelDelta.REMOVED) != 0) {
						// called when all instances of a Bundle-SymbolicName are all removed
						handleModifiedModels(delta.getRemovedEntries());
					}
					if ((kind & MonitorModelDelta.CHANGED) != 0) {
						// called when a plug-in is changed (possibly the input)
						// AND when the model for the ModelEntry changes (new bundle with existing id/remove bundle with 2 instances with same id)
						handleModifiedModels(delta.getChangedEntries());
					}
					if ((kind & MonitorModelDelta.ADDED) != 0) {
						// when user modifies Bundle-SymbolicName, a ModelEntry is created for the new name.  In this case, if the input matches
						// the modified model, we need to update the title.
						handleModifiedModels(delta.getAddedEntries());
					}
				} finally {
					// no matter what, refresh the viewer since bundles might un/resolve with changes
					fViewer.refresh();
				}
			}
		});
	}

}
