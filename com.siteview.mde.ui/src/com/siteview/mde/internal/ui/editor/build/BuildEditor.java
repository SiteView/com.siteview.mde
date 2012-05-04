/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.build;

import com.siteview.mde.internal.ui.editor.monitor.PluginExportAction;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IToolBarManager;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.build.IBuildObject;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class BuildEditor extends MultiSourceEditor {

	private PluginExportAction fExportAction;

	public BuildEditor() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IMDEUIConstants.BUILD_EDITOR_ID;
	}

	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();

		manager.putContext(input, new BuildInputContext(this, input, true));
		manager.monitorFile(file);
	}

	protected InputContextManager createInputContextManager() {
		BuildInputContextManager manager = new BuildInputContextManager(this);
		manager.setUndoManager(new BuildUndoManager(this));
		return manager;
	}

	public void monitoredFileAdded(IFile file) {
		if (fInputContextManager == null)
			return;
		String name = file.getName();
		if (name.equalsIgnoreCase(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}

	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	public void contextRemoved(InputContext context) {
		close(false);
	}

	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	protected void addEditorPages() {
		try {
			if (getEditorInput() instanceof IFileEditorInput)
				addPage(new BuildPage(this));
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#computeInitialPageId()
	 */
	protected String computeInitialPageId() {
		// Retrieve the initial page
		String firstPageId = super.computeInitialPageId();
		// If none is defined, return the default
		if (firstPageId == null) {
			return BuildPage.PAGE_ID;
		}

		return firstPageId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected MDESourcePage createSourcePage(MDEFormEditor editor, String title, String name, String contextId) {
		return new BuildSourcePage(editor, title, name);
	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new BuildOutlinePage(this);
	}

	protected IPropertySheetPage getPropertySheet(MDEFormPage page) {
		return null;
	}

	public String getTitle() {
		return super.getTitle();
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IBuildModel) model).isValid() : false;
	}

	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
			for (int i = 0; i < types.length; i++) {
				for (int j = 0; j < transfers.length; j++) {
					if (transfers[j].isSupportedType(types[i]))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	public Object getAdapter(Class key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		}
		return context;
	}

	public void contributeToToolbar(IToolBarManager manager) {
		manager.add(getExportAction());
	}

	private PluginExportAction getExportAction() {
		if (fExportAction == null) {
			fExportAction = new PluginExportAction(this);
			fExportAction.setToolTipText(MDEUIMessages.PluginEditor_exportTooltip);
			fExportAction.setImageDescriptor(MDEPluginImages.DESC_EXPORT_PLUGIN_TOOL);
		}
		return fExportAction;
	}
}
