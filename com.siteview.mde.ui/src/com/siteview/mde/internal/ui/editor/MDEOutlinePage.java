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
package com.siteview.mde.internal.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import com.siteview.mde.internal.ui.search.MonitorSearchActionGroup;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public abstract class MDEOutlinePage extends ContentOutlinePage {

	protected MDEFormEditor fEditor;

	public MDEOutlinePage(MDEFormEditor editor) {
		fEditor = editor;
	}

	public MDEOutlinePage() {
	}

	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ISelection selection = getSelection();
				MonitorSearchActionGroup actionGroup = new MonitorSearchActionGroup();
				if (fEditor != null)
					actionGroup.setBaseModel(fEditor.getAggregateModel());
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
			}
		};

		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = getTreeViewer().getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}

}
