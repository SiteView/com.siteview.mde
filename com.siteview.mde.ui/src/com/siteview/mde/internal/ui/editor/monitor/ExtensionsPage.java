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

import com.siteview.mde.core.monitor.*;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import com.siteview.mde.internal.core.ischema.ISchemaSimpleType;
import com.siteview.mde.internal.core.text.*;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ExtensionsPage extends MDEFormPage {
	public static final String PAGE_ID = "extensions"; //$NON-NLS-1$

	private ExtensionsSection fSection;
	private ExtensionsBlock fBlock;

	public class ExtensionsBlock extends MDEMasterDetailsBlock implements IDetailsPageProvider {

		private ExtensionElementBodyTextDetails fBodyTextDetails;

		public ExtensionsBlock() {
			super(ExtensionsPage.this);
		}

		protected MDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fSection = new ExtensionsSection(getPage(), parent);
			return fSection;
		}

		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageLimit(10);
			// register static page for the extensions
			detailsPart.registerPage(IMonitorExtension.class, new ExtensionDetails(fSection));
			// Register a static page for the extension elements that contain 
			// only body text (no child elements or attributes)
			// (e.g. schema simple type)
			fBodyTextDetails = new ExtensionElementBodyTextDetails(fSection);
			detailsPart.registerPage(ExtensionElementBodyTextDetails.class, fBodyTextDetails);
			// register a dynamic provider for elements
			detailsPart.setPageProvider(this);
		}

		public Object getPageKey(Object object) {
			if (object instanceof IMonitorExtension)
				return IMonitorExtension.class;
			if (object instanceof IMonitorElement) {
				ISchemaElement element = ExtensionsSection.getSchemaElement((IMonitorElement) object);
				// Extension point schema exists
				if (element != null) {
					// Use the body text page if the element has no child 
					// elements or attributes
					if (element.getType() instanceof ISchemaSimpleType) {
						// Set the schema element (to provide hover text 
						// content)
						fBodyTextDetails.setSchemaElement(element);
						return ExtensionElementBodyTextDetails.class;
					}
					return element;
				}
				// No Extension point schema
				// no element - construct one
				IMonitorElement pelement = (IMonitorElement) object;
				// Use the body text page if the element has no child 
				// elements or attributes
				if ((pelement.getAttributeCount() == 0) && (pelement.getChildCount() == 0)) {
					// Unset the previous schema element (no hover text 
					// content)					
					fBodyTextDetails.setSchemaElement(null);
					return ExtensionElementBodyTextDetails.class;
				}
				String ename = pelement.getName();
				IMonitorExtension extension = ExtensionsSection.getExtension((IMonitorParent) pelement.getParent());
				return extension.getPoint() + "/" + ename; //$NON-NLS-1$
			}
			return object.getClass();
		}

		public IDetailsPage getPage(Object object) {
			if (object instanceof ISchemaElement)
				return new ExtensionElementDetails(fSection, (ISchemaElement) object);
			if (object instanceof String)
				return new ExtensionElementDetails(fSection, null);
			return null;
		}
	}

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionsPage(FormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.ExtensionsPage_tabName);
		fBlock = new ExtensionsBlock();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(MDEUIMessages.ExtensionsPage_title);
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_EXTENSIONS_OBJ));
		fBlock.createContent(managedForm);
		//refire selection
		fSection.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS);
		super.createFormContent(managedForm);
	}

	public void updateFormSelection() {
		super.updateFormSelection();
		IFormPage page = getMDEEditor().findPage(PluginInputContext.CONTEXT_ID);
		if (page instanceof ManifestSourcePage) {
			ISourceViewer viewer = ((ManifestSourcePage) page).getViewer();
			if (viewer == null)
				return;
			StyledText text = viewer.getTextWidget();
			if (text == null)
				return;
			int offset = text.getCaretOffset();
			if (offset < 0)
				return;

			IDocumentRange range = ((ManifestSourcePage) page).getRangeElement(offset, true);
			if (range instanceof IDocumentAttributeNode)
				range = ((IDocumentAttributeNode) range).getEnclosingElement();
			else if (range instanceof IDocumentTextNode)
				range = ((IDocumentTextNode) range).getEnclosingElement();
			if ((range instanceof IMonitorExtension) || (range instanceof IMonitorElement)) {
				fSection.selectExtensionElement(new StructuredSelection(range));
			}
		}
	}
}
