/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.refactoring;

import com.siteview.mde.internal.core.text.monitor.*;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.*;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ischema.*;
import com.siteview.mde.internal.core.schema.SchemaRegistry;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.*;

public class PluginManifestChange {

	public static Change createRenameChange(IFile file, Object[] affectedElements, String[] newNames, TextChange textChange, IProgressMonitor monitor) throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);

			MultiTextEdit multiEdit = new MultiTextEdit();

			IDocument document = buffer.getDocument();

			try {
				MonitorModelBase model;
				if (ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR.equals(file.getName()))
					model = new FragmentModel(document, false);
				else
					model = new MonitorModel(document, false);

				model.load();
				if (!model.isLoaded())
					return null;

				for (int i = 0; i < affectedElements.length; i++) {
					if (model instanceof MonitorModel && affectedElements[i] instanceof IJavaElement) {
						MonitorNode plugin = (MonitorNode) model.getMonitorBase();
						IDocumentAttributeNode attr = plugin.getDocumentAttribute("class"); //$NON-NLS-1$
						TextEdit edit = createTextEdit(attr, (IJavaElement) affectedElements[i], newNames[i]);
						if (edit != null)
							multiEdit.addChild(edit);
					}

					SchemaRegistry registry = MDECore.getDefault().getSchemaRegistry();
					IMonitorExtension[] extensions = model.getMonitorBase().getExtensions();
					for (int j = 0; j < extensions.length; j++) {
						ISchema schema = registry.getSchema(extensions[j].getPoint());
						if (schema != null)
							addExtensionAttributeEdit(schema, extensions[j], multiEdit, affectedElements[i], newNames[i]);
					}
				}

				if (multiEdit.hasChildren()) {
					// add to existing text edits.  If you create a new MultiText edit, the file will get corrupted since the edits are applied independently
					if (textChange != null) {
						TextEdit edit = textChange.getEdit();
						if (edit instanceof MultiTextEdit) {
							((MultiTextEdit) edit).addChild(multiEdit);
							multiEdit = ((MultiTextEdit) edit);
						} else
							multiEdit.addChild(edit);
					}
					TextFileChange change = new TextFileChange("", file); //$NON-NLS-1$
					change.setEdit(multiEdit);
					PDEModelUtility.setChangeTextType(change, file);
					return change;
				}
			} catch (CoreException e) {
				return null;
			}
			return null;
		} finally {
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
		}
	}

	private static void addExtensionAttributeEdit(ISchema schema, IMonitorParent parent, MultiTextEdit multi, Object element, String newName) {
		IMonitorObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IMonitorElement child = (IMonitorElement) children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IMonitorAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IMonitorAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null) {
						if (element instanceof IJavaElement && attInfo.getKind() == IMetaAttribute.JAVA) {
							IDocumentAttributeNode docAttr = (IDocumentAttributeNode) attr;
							TextEdit edit = createTextEdit(docAttr, (IJavaElement) element, newName);
							if (edit != null)
								multi.addChild(edit);
						} else if (element instanceof IResource && attInfo.getKind() == IMetaAttribute.RESOURCE) {
							IDocumentAttributeNode docAttr = (IDocumentAttributeNode) attr;
							TextEdit edit = createTextEdit(docAttr, (IResource) element, newName);
							if (edit != null)
								multi.addChild(edit);
						}
					}
				}
			}
			addExtensionAttributeEdit(schema, child, multi, element, newName);
		}
	}

	private static TextEdit createTextEdit(IDocumentAttributeNode attr, IJavaElement element, String newName) {
		if (attr == null)
			return null;

		String oldName = (element instanceof IType) ? ((IType) element).getFullyQualifiedName('$') : element.getElementName();
		String value = attr.getAttributeValue();
		if (oldName.equals(value) || isGoodMatch(value, oldName, element instanceof IPackageFragment)) {
			int offset = attr.getValueOffset();
			if (offset >= 0)
				return new ReplaceEdit(offset, oldName.length(), newName);
		}
		return null;
	}

	private static TextEdit createTextEdit(IDocumentAttributeNode attr, IResource resource, String newName) {
		if (attr != null) {
			String oldName = resource.getProjectRelativePath().toString();
			String value = attr.getAttributeValue();
			if (oldName.equals(value) || ((resource instanceof IContainer) && isGoodFolderMatch(value, oldName))) {
				int offset = attr.getValueOffset();
				if (offset >= 0)
					return new ReplaceEdit(offset, oldName.length(), newName);
			}
		}
		return null;
	}

	private static boolean isGoodMatch(String value, String oldName, boolean isPackage) {
		if (value == null || value.length() <= oldName.length())
			return false;
		boolean goodLengthMatch = isPackage ? value.lastIndexOf('.') <= oldName.length() : value.charAt(oldName.length()) == '$';
		return value.startsWith(oldName) && goodLengthMatch;
	}

	private static boolean isGoodFolderMatch(String value, String oldName) {
		return new Path(oldName).isPrefixOf(new Path(value));
	}
}
