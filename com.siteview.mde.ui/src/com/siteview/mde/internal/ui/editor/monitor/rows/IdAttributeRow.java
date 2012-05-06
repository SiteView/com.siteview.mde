/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor.rows;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;
import com.siteview.mde.internal.ui.editor.monitor.MonitorInputContext;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.Map;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.core.util.PDESchemaHelper;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.IContextPart;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.search.ManifestEditorOpener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class IdAttributeRow extends ButtonAttributeRow {

	private class IdAttributeLabelProvider extends LabelProvider {

		public Image getImage(Object element) {
			return MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_GENERIC_XML_OBJ);
		}

		public String getText(Object element) {
			if (element instanceof Map.Entry) {
				Map.Entry entry = (Map.Entry) element;
				String text = (String) entry.getKey();
				if (entry.getValue() instanceof IConfigurationElement) {
					IConfigurationElement value = (IConfigurationElement) entry.getValue();
					String name = value.getAttribute("name"); //$NON-NLS-1$
					if (name == null) {
						name = value.getAttribute("label"); //$NON-NLS-1$
						if (name == null) {
							name = value.getAttribute("description"); //$NON-NLS-1$
						}
					}

					String contributor = value.getContributor().getName();

					if (input != null && name != null && name.startsWith("%") && contributor != null) { //$NON-NLS-1$
						IMonitorModelBase model = MonitorRegistry.findModel(contributor);
						name = model.getResourceString(name);
					}

					if (name != null) {
						text += " - " + name; //$NON-NLS-1$
					}
					if (contributor != null)
						text += " [" + contributor + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return text;
			}
			return super.getText(element);
		}
	}

	public IdAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.plugin.rows.ButtonAttributeRow#browse()
	 */
	protected void browse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(MDEPlugin.getActiveWorkbenchShell(), new IdAttributeLabelProvider());
		dialog.setTitle(MDEUIMessages.IdAttributeRow_title);
		dialog.setMessage(MDEUIMessages.IdAttributeRow_message);
		dialog.setEmptyListMessage(MDEUIMessages.IdAttributeRow_emptyMessage);
		Map attributeMap = PDESchemaHelper.getValidAttributes(getAttribute());
		dialog.setElements(attributeMap.entrySet().toArray());
		if (dialog.open() == Window.OK) {
			Map.Entry entry = (Map.Entry) dialog.getFirstResult();
			text.setText(entry.getKey().toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.plugin.rows.ReferenceAttributeRow#openReference()
	 */
	protected void openReference() {
		Map attributeMap = PDESchemaHelper.getValidAttributes(getAttribute());
		String id = text.getText();
		// TODO this is hackish
		IConfigurationElement element = (IConfigurationElement) attributeMap.get(id);
		if (element != null) {
			String pluginId = element.getContributor().getName();
			IMonitorModelBase model = MonitorRegistry.findModel(pluginId);
			IEditorPart editorPart = ManifestEditor.open(model.getMonitorBase(), true);
			ManifestEditor editor = (ManifestEditor) editorPart;
			if (editor != null) {
				InputContext context = editor.getContextManager().findContext(MonitorInputContext.CONTEXT_ID);
				IDocument document = context.getDocumentProvider().getDocument(context.getInput());
				IRegion region = ManifestEditorOpener.getAttributeMatch(editor, id, document);
				if (region == null) {
					// see bug 248248 for why we have this check
					id = id.substring(id.lastIndexOf('.') + 1, id.length());
					region = ManifestEditorOpener.getAttributeMatch(editor, id, document);
				}
				editor.openToSourcePage(context, region.getOffset(), region.getLength());
			}
		}
	}
}
