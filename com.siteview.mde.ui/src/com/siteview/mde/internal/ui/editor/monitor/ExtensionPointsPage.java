/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.IMonitorExtensionPoint;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentRange;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ExtensionPointsPage extends MDEFormPage {

	public static final String PAGE_ID = "ex-points"; //$NON-NLS-1$

	private ExtensionPointsSection fExtensionPointsSection;
	private ExtensionPointsBlock fBlock;

	public class ExtensionPointsBlock extends MDEMasterDetailsBlock {

		public ExtensionPointsBlock() {
			super(ExtensionPointsPage.this);
		}

		protected MDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fExtensionPointsSection = new ExtensionPointsSection(getPage(), parent);
			return fExtensionPointsSection;
		}

		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageProvider(new IDetailsPageProvider() {
				public Object getPageKey(Object object) {
					if (object instanceof IMonitorExtensionPoint)
						return IMonitorExtensionPoint.class;
					return object.getClass();
				}

				public IDetailsPage getPage(Object key) {
					if (key.equals(IMonitorExtensionPoint.class))
						return new ExtensionPointDetails();
					return null;
				}
			});
		}
	}

	public ExtensionPointsPage(FormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.ExtensionPointsPage_tabName);
		fBlock = new ExtensionPointsBlock();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_PLUGIN_EXT_POINTS;
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_EXT_POINTS_OBJ));
		form.setText(MDEUIMessages.ExtensionPointsPage_title);
		fBlock.createContent(managedForm);
		fExtensionPointsSection.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_EXT_POINTS);
	}

	public void updateFormSelection() {
		super.updateFormSelection();
		IFormPage page = getMDEEditor().findPage(MonitorInputContext.CONTEXT_ID);
		if (page instanceof MonitorSourcePage) {
			ISourceViewer viewer = ((MonitorSourcePage) page).getViewer();
			if (viewer == null)
				return;
			StyledText text = viewer.getTextWidget();
			if (text == null)
				return;
			int offset = text.getCaretOffset();
			if (offset < 0)
				return;

			IDocumentRange range = ((MonitorSourcePage) page).getRangeElement(offset, true);
			if (range instanceof IDocumentAttributeNode)
				range = ((IDocumentAttributeNode) range).getEnclosingElement();
			if (range instanceof IMonitorExtensionPoint)
				fExtensionPointsSection.selectExtensionPoint(new StructuredSelection(range));
		}
	}
}
