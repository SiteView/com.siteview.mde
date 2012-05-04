/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.text.IEditingModel;
import com.siteview.mde.internal.ui.IPreferenceConstants;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.actions.MDEActionConstants;
import com.siteview.mde.internal.ui.editor.text.*;
import org.eclipse.swt.widgets.Composite;

public abstract class MDEProjectionSourcePage extends MDESourcePage implements IProjectionListener {

	private ProjectionSupport fProjectionSupport;
	private IFoldingStructureProvider fFoldingStructureProvider;
	private IColorManager fColorManager;
	private ChangeAwareSourceViewerConfiguration fConfiguration;

	public MDEProjectionSourcePage(MDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		fColorManager = ColorManager.getDefault();
		fConfiguration = createSourceViewerConfiguration(fColorManager);
		if (fConfiguration != null)
			setSourceViewerConfiguration(fConfiguration);
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
		createFoldingSupport(projectionViewer);

		if (isFoldingEnabled()) {
			projectionViewer.doOperation(ProjectionViewer.TOGGLE);
		}
	}

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new MDEProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles, isQuickOutlineEnabled());
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	public abstract boolean isQuickOutlineEnabled();

	public void dispose() {
		((ProjectionViewer) getSourceViewer()).removeProjectionListener(this);
		if (fProjectionSupport != null) {
			fProjectionSupport.dispose();
			fProjectionSupport = null;
		}
		fColorManager.dispose();
		if (fConfiguration != null)
			fConfiguration.dispose();
		super.dispose();
	}

	private void createFoldingSupport(ProjectionViewer projectionViewer) {
		fProjectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());

		fProjectionSupport.install();
		((ProjectionViewer) getSourceViewer()).addProjectionListener(this);

	}

	public void projectionEnabled() {
		IBaseModel model = getInputContext().getModel();
		if (model instanceof IEditingModel) {
			fFoldingStructureProvider = getFoldingStructureProvider((IEditingModel) model);
			if (fFoldingStructureProvider != null) {
				fFoldingStructureProvider.initialize();
				IReconciler rec = getSourceViewerConfiguration().getReconciler(getSourceViewer());
				IReconcilingStrategy startegy = rec.getReconcilingStrategy(new String());
				if (startegy instanceof ReconcilingStrategy) {
					((ReconcilingStrategy) startegy).addParticipant(fFoldingStructureProvider);
				}
			}
		}
	}

	protected IFoldingStructureProvider getFoldingStructureProvider(IEditingModel model) {
		return null;
	}

	public void projectionDisabled() {
		fFoldingStructureProvider = null;
	}

	private boolean isFoldingEnabled() {
		IPreferenceStore store = MDEPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED);
	}

	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		if (fConfiguration == null)
			return false;
		return fConfiguration.affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}

	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		try {
			if (fConfiguration != null) {
				ISourceViewer sourceViewer = getSourceViewer();
				if (sourceViewer != null)
					fConfiguration.adaptToPreferenceChange(event);
			}
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	public Object getAdapter(Class key) {
		if (fProjectionSupport != null) {
			Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), key);
			if (adapter != null) {
				return adapter;
			}
		}
		return super.getAdapter(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		// Add the quick outline menu entry to the context menu
		addQuickOutlineMenuEntry(menu);
		// Add the rest
		super.editorContextMenuAboutToShow(menu);
	}

	/**
	 * @param menu
	 */
	private void addQuickOutlineMenuEntry(IMenuManager menu) {
		// Only add the action if the source page supports it
		if (isQuickOutlineEnabled() == false) {
			return;
		}
		// Get the appropriate quick outline action associated with the active
		// source page
		IAction quickOutlineAction = getAction(MDEActionConstants.COMMAND_ID_QUICK_OUTLINE);
		// Ensure it is defined
		if (quickOutlineAction == null) {
			return;
		}
		// Insert the quick outline action after the "Show In" menu contributed
		menu.add(quickOutlineAction);
	}

	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(IColorManager colorManager) {
		return null;
	}

}
