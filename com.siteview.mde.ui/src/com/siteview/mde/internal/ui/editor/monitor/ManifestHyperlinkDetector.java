/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import com.siteview.mde.internal.core.ischema.IMetaAttribute;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.core.schema.SchemaRootElement;
import com.siteview.mde.internal.core.text.*;
import com.siteview.mde.internal.ui.editor.MDEHyperlinkDetector;
import com.siteview.mde.internal.ui.editor.MDESourcePage;
import com.siteview.mde.internal.ui.editor.text.*;

public class ManifestHyperlinkDetector extends MDEHyperlinkDetector {

	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public ManifestHyperlinkDetector(MDESourcePage page) {
		super(page);
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;

		IMonitorObject node = XMLUtil.getTopLevelParent(attr);
		if (node == null || !node.getModel().isEditable())
			return null;

		IMonitorModelBase base = node.getMonitorModel();
		IResource res = base.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (node instanceof IMonitorExtensionPoint) {
			if (attr.getAttributeName().equals(IMonitorExtensionPoint.P_SCHEMA))
				link[0] = new SchemaHyperlink(linkRegion, attrValue, res);
			else if (attr.getAttributeName().equals(IMonitorObject.P_NAME))
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);

		} else if (node instanceof IMonitorExtension) {
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IMonitorExtension) node).getPoint());
			if (sAttr == null)
				return null;

			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getParent() instanceof SchemaRootElement) {
				if (attr.getAttributeName().equals(IMonitorExtension.P_POINT))
					link[0] = new ExtensionHyperLink(linkRegion, attrValue);
			} else if (sAttr.isTranslatable()) {
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);
			}
		}

		if (link[0] != null)
			return link;
		return null;
	}

	protected IHyperlink[] detectNodeHyperlink(IDocumentElementNode node) {
		// TODO what can we do here?
		// suggestions:
		//   - use SchemaEditor.openToElement(IPath path, ISchemaElement element)
		//     to directly highlight this particular element in a schema editor
		//      ? too fancy ?
		/*
				IPluginObject parent = XMLUtil.getTopLevelParent(node);
				if (parent == null || !parent.getModel().isEditable())
					return null;
				
				if (parent instanceof IPluginExtension) { 
					ISchemaElement sElement = XMLUtil.getSchemaElement(node, ((IPluginExtension)parent).getPoint());
					if (sElement == null)
						return null;
					URL url = sElement.getSchema().getURL();
					// only have access to URL now - extend SchemaEditor?
					SchemaEditor.openToElement(url, sElement);
				}
		*/
		return null;
	}

	protected IHyperlink[] detectTextNodeHyperlink(IDocumentTextNode node) {
		IDocumentElementNode enclosing = node.getEnclosingElement();
		if (!(enclosing instanceof IMonitorObject))
			return null;
		IMonitorModelBase base = ((IMonitorObject) enclosing).getMonitorModel();
		if (node.getText().charAt(0) == '%')
			return new IHyperlink[] {new TranslationHyperlink(new Region(node.getOffset(), node.getLength()), node.getText(), base)};
		return null;
	}
}
