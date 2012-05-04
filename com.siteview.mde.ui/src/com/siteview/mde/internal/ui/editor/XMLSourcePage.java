/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.internal.ui.editor.text.*;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public abstract class XMLSourcePage extends MDEProjectionSourcePage {

	public XMLSourcePage(MDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setRangeIndicator(new DefaultRangeIndicator());
	}

	public boolean canLeaveThePage() {
		return true;
	}

	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		String[] more = new String[ids.length + 1];
		more[0] = "com.siteview.mde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(IColorManager colorManager) {
		if (getEditor() instanceof ManifestEditor)
			return new PluginXMLConfiguration(colorManager, this);
		return new XMLConfiguration(colorManager, this);
	}
}
