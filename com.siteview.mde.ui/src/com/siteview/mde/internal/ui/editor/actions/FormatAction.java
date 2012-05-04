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
package com.siteview.mde.internal.ui.editor.actions;

import com.siteview.mde.internal.ui.editor.monitor.BundleSourcePage;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FormatAction extends Action {

	protected ITextEditor fTextEditor;

	public FormatAction() {
		setText(MDEUIMessages.FormatManifestAction_actionText);
	}

	public void runWithEvent(Event event) {
		run();
	}

	public void run() {
		if (fTextEditor == null || fTextEditor.getEditorInput() == null)
			return;

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new FormatOperation(new Object[] {fTextEditor.getEditorInput()}));
		} catch (InvocationTargetException e) {
			MDEPlugin.log(e);
		} catch (InterruptedException e) {
			MDEPlugin.log(e);
		}
	}

	public void setTextEditor(ITextEditor textEditor) {
		// TODO Temporary:  Until plug-in manifest XML source page format
		// functionality is completed
		setEnabled(textEditor instanceof BundleSourcePage);
		fTextEditor = textEditor;
	}

}
