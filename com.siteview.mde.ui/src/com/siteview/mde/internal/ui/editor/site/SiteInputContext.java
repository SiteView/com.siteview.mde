/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262977
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.site;

import java.io.*;
import java.util.ArrayList;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import com.siteview.mde.core.*;
import com.siteview.mde.internal.core.isite.ISiteModel;
import com.siteview.mde.internal.core.site.ExternalSiteModel;
import com.siteview.mde.internal.core.site.WorkspaceSiteModel;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.ui.*;

public class SiteInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "site-context"; //$NON-NLS-1$
	private boolean storageModel = false;

	/**
	 * @param editor
	 * @param input
	 */
	public SiteInputContext(MDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	protected IBaseModel createModel(IEditorInput input) {
		IBaseModel model = null;
		InputStream is = null;
		try {
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				is = new BufferedInputStream(file.getContents());
				model = createWorkspaceModel(file, is, true);
			} else if (input instanceof IStorageEditorInput) {
				is = new BufferedInputStream(((IStorageEditorInput) input).getStorage().getContents());
				model = createStorageModel(is);
			} else if (input instanceof IURIEditorInput) {
				IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
				is = store.openInputStream(EFS.CACHE, new NullProgressMonitor());
				model = createStorageModel(is);
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
			return null;
		}
		return model;
	}

	private IBaseModel createWorkspaceModel(IFile file, InputStream stream, boolean editable) {
		WorkspaceSiteModel model = new WorkspaceSiteModel(file);
		try {
			model.setEditable(editable);
			model.load(stream, false);
		} catch (CoreException e) {
		}
		try {
			stream.close();
		} catch (IOException e) {
			MDEPlugin.logException(e);
		}
		return model;
	}

	private IBaseModel createStorageModel(InputStream stream) {
		ExternalSiteModel model = new ExternalSiteModel();
		try {
			model.load(stream, true);
		} catch (CoreException e) {
		} finally {
			try {
				stream.close();
			} catch (IOException e1) {
			}
		}
		return model;
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getModel();
		if (storageModel) {
			model.dispose();
		}
		super.dispose();
	}

	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isEditable() == false)
			return;
		if (editableModel.isDirty() == false)
			return;
		try {
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			doc.set(swriter.toString());
		} catch (IOException e) {
			MDEPlugin.logException(e);
		}
	}

	protected boolean synchronizeModel(IDocument doc) {
		ISiteModel model = (ISiteModel) getModel();
		boolean cleanModel = true;
		String text = doc.get();
		try {
			InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8")); //$NON-NLS-1$
			try {
				model.reload(stream, false);
			} catch (CoreException e) {
				cleanModel = false;
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			MDEPlugin.logException(e);
		}
		return cleanModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList,
	 *      org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
	}

	protected String getPartitionName() {
		return "___site_partition"; //$NON-NLS-1$
	}
}
