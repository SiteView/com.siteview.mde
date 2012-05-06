/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.core.text.monitor.*;

import java.io.File;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.text.AbstractEditingModel;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.editor.JarEntryEditorInput;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;

public class MonitorInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "plugin-context"; //$NON-NLS-1$
	private boolean fIsFragment;

	public MonitorInputContext(MDEFormEditor editor, IEditorInput input, boolean primary, boolean isFragment) {
		super(editor, input, primary);
		fIsFragment = isFragment;
		create();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		MonitorModelBase model = null;
		boolean isReconciling = input instanceof IFileEditorInput;
		IDocument document = getDocumentProvider().getDocument(input);
		if (fIsFragment) {
			model = new FragmentModel(document, isReconciling);
		} else {
			model = new MonitorModel(document, isReconciling);
		}
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			model.setUnderlyingResource(file);
			model.setCharset(file.getCharset());
		} else if (input instanceof IURIEditorInput) {
			IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
			model.setInstallLocation(store.getParent().toString());
			model.setCharset(getDefaultCharset());
		} else if (input instanceof JarEntryEditorInput) {
			File file = (File) ((JarEntryEditorInput) input).getAdapter(File.class);
			model.setInstallLocation(file.toString());
			model.setCharset(getDefaultCharset());
		} else {
			model.setCharset(getDefaultCharset());
		}
		model.load();
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	public boolean isFragment() {
		return fIsFragment;
	}

	protected void reorderInsertEdits(ArrayList ops) {
		HashMap map = getOperationTable();
		Iterator iter = map.keySet().iterator();
		TextEdit runtimeInsert = null;
		TextEdit requiresInsert = null;
		ArrayList extensionPointInserts = new ArrayList();
		ArrayList extensionInserts = new ArrayList();

		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof IDocumentElementNode) {
				IDocumentElementNode node = (IDocumentElementNode) object;
				if (node.getParentNode() instanceof MonitorBaseNode) {
					TextEdit edit = (TextEdit) map.get(node);
					if (edit instanceof InsertEdit) {
						if (node.getXMLTagName().equals("runtime")) { //$NON-NLS-1$
							runtimeInsert = edit;
						} else if (node.getXMLTagName().equals("requires")) { //$NON-NLS-1$
							requiresInsert = edit;
						} else if (node.getXMLTagName().equals("extension")) { //$NON-NLS-1$
							extensionInserts.add(edit);
						} else if (node.getXMLTagName().equals("extension-point")) { //$NON-NLS-1$
							extensionPointInserts.add(edit);
						}
					}
				}
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			TextEdit edit = (TextEdit) ops.get(i);
			if (edit instanceof InsertEdit) {
				if (extensionPointInserts.contains(edit)) {
					ops.remove(edit);
					ops.add(0, edit);
				}
			}
		}

		if (requiresInsert != null) {
			ops.remove(requiresInsert);
			ops.add(0, requiresInsert);
		}

		if (runtimeInsert != null) {
			ops.remove(runtimeInsert);
			ops.add(0, runtimeInsert);
		}
	}

	public void doRevert() {
		fEditOperations.clear();
		fOperationTable.clear();
		fMoveOperations.clear();
		AbstractEditingModel model = (AbstractEditingModel) getModel();
		model.reconciled(model.getDocument());
	}

	protected String getPartitionName() {
		return "___plugin_partition"; //$NON-NLS-1$
	}
}
