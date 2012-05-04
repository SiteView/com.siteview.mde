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
package com.siteview.mde.internal.ui.editor.schema;

import org.eclipse.jface.action.*;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.MDEFormTextEditorContributor;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

public class SchemaEditorContributor extends MDEFormTextEditorContributor {
	private PreviewAction fPreviewAction;

	class PreviewAction extends Action {
		public PreviewAction() {
		}

		public void run() {
			if (getEditor() != null) {
				final SchemaEditor schemaEditor = (SchemaEditor) getEditor();
				BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
					public void run() {
						schemaEditor.previewReferenceDocument();
					}
				});
			}
		}
	}

	public SchemaEditorContributor() {
		super("&Schema"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}

	public void contextMenuAboutToShow(IMenuManager mm, boolean addClipboard) {
		super.contextMenuAboutToShow(mm, addClipboard);
		mm.add(new Separator());
		mm.add(fPreviewAction);
	}

	public Action getPreviewAction() {
		return fPreviewAction;
	}

	protected void makeActions() {
		super.makeActions();
		fPreviewAction = new PreviewAction();
		fPreviewAction.setText(MDEUIMessages.SchemaEditorContributor_previewAction);
	}
}
