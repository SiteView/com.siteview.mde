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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import com.siteview.mde.internal.core.schema.SchemaCompositor;
import com.siteview.mde.internal.core.schema.SchemaElementReference;
import com.siteview.mde.internal.ui.MDEPluginImages;

public class NewReferenceAction extends Action {
	private Object object;

	private ISchemaElement referencedElement;

	public NewReferenceAction(ISchemaElement source, Object object, ISchemaElement referencedElement) {
		this.object = object;
		this.referencedElement = referencedElement;
		setText(referencedElement.getName());
		ImageDescriptor desc = MDEPluginImages.DESC_ELREF_SC_OBJ;
		setImageDescriptor(desc);
		setEnabled(source.getSchema().isEditable());
	}

	public void run() {
		if (object != null && object instanceof SchemaCompositor) {
			SchemaCompositor parent = (SchemaCompositor) object;
			SchemaElementReference reference = new SchemaElementReference(parent, referencedElement.getName());
			reference.setReferencedObject(referencedElement);
			parent.addChild(reference);
		}
	}
}
