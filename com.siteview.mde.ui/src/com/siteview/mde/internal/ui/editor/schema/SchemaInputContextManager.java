/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.schema;

import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;

public class SchemaInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public SchemaInputContextManager(MDEFormEditor editor) {
		super(editor);
	}

	public IBaseModel getAggregateModel() {
		return findSchema();
	}

	private IBaseModel findSchema() {
		InputContext scontext = findContext(SchemaInputContext.CONTEXT_ID);
		return (scontext != null) ? scontext.getModel() : null;
	}
}
