/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.MDEFormTextEditorContributor;
import com.siteview.mde.internal.ui.nls.GetNonExternalizedStringsAction;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class MonitorEditorContributor extends MDEFormTextEditorContributor {

	private ExternalizeAction fExternalizeAction;

	class ExternalizeAction extends Action {
		public ExternalizeAction() {
		}

		public void run() {
			if (getEditor() != null) {
				BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
					public void run() {
						GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
						IStructuredSelection selection = new StructuredSelection(getEditor().getCommonProject());
						fGetExternAction.selectionChanged(ExternalizeAction.this, selection);
						fGetExternAction.run(ExternalizeAction.this);
					}
				});
			}
		}
	}

	public MonitorEditorContributor() {
		super("&Plugin"); //$NON-NLS-1$
	}

	public void contextMenuAboutToShow(IMenuManager mm, boolean addClipboard) {
		super.contextMenuAboutToShow(mm, addClipboard);
		IBaseModel model = getEditor().getAggregateModel();
		if (model != null && model.isEditable()) {
			mm.add(new Separator());
			mm.add(fExternalizeAction);
		}
	}

	protected void makeActions() {
		super.makeActions();
		fExternalizeAction = new ExternalizeAction();
		fExternalizeAction.setText(MDEUIMessages.ManifestEditorContributor_externStringsActionName);
	}

	public boolean supportsContentAssist() {
		return true;
	}

	public boolean supportsFormatAction() {
		return true;
	}

	public boolean supportsCorrectionAssist() {
		return true;
	}

	public boolean supportsHyperlinking() {
		return true;
	}
}
