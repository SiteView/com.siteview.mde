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
package com.siteview.mde.internal.ui.editor.site;

import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;

public class SiteInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public SiteInputContextManager(MDEFormEditor editor) {
		super(editor);
	}

	public IBaseModel getAggregateModel() {
		return findSiteModel();
	}

	private IBaseModel findSiteModel() {
		InputContext scontext = findContext(SiteInputContext.CONTEXT_ID);
		return (scontext != null) ? scontext.getModel() : null;
	}
}
