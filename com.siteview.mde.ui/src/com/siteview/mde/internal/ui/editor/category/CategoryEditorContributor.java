/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package com.siteview.mde.internal.ui.editor.category;

import com.siteview.mde.internal.ui.editor.MDEFormTextEditorContributor;
import org.eclipse.swt.dnd.Clipboard;

public class CategoryEditorContributor extends MDEFormTextEditorContributor {

	public CategoryEditorContributor() {
		super("Category"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}
