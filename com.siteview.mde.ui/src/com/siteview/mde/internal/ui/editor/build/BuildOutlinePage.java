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
package com.siteview.mde.internal.ui.editor.build;

import com.siteview.mde.core.build.*;
import com.siteview.mde.internal.ui.editor.*;

public class BuildOutlinePage extends FormOutlinePage {
	/**
	 * @param editor
	 */
	public BuildOutlinePage(MDEFormEditor editor) {
		super(editor);
	}

	protected Object[] getChildren(Object parent) {
		if (parent instanceof MDEFormPage) {
			MDEFormPage page = (MDEFormPage) parent;
			IBuildModel model = (IBuildModel) page.getModel();
			if (model.isValid()) {
				IBuild build = model.getBuild();
				if (page.getId().equals(BuildPage.PAGE_ID))
					return build.getBuildEntries();
			}
		}
		return new Object[0];
	}

	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IBuildEntry)
			pageId = BuildPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}
}
