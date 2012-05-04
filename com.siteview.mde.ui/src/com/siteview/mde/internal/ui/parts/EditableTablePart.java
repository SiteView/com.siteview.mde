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
package com.siteview.mde.internal.ui.parts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.wizards.RenameDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class EditableTablePart extends TablePart {
	private boolean editable;
	private Action renameAction;

	class RenameAction extends Action {
		public RenameAction() {
			super(MDEUIMessages.EditableTablePart_renameAction);
		}

		public void run() {
			doRename();
		}
	}

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}

		public void modify(Object object, String property, Object value) {
			entryModified(object, value.toString());
		}

		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}

	/**
	 * Constructor for EditableTablePart.
	 * @param buttonLabels
	 */
	public EditableTablePart(String[] buttonLabels) {
		super(buttonLabels);
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public IAction getRenameAction() {
		if (renameAction == null)
			renameAction = new RenameAction();
		return renameAction;
	}

	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		TableViewer tableViewer = (TableViewer) super.createStructuredViewer(parent, style, toolkit);
		return tableViewer;
	}

	private void doRename() {
		TableViewer viewer = getTableViewer();
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection.size() == 1 && isEditable()) {
			Object obj = selection.getFirstElement();
			String oldName = obj.toString();
			RenameDialog dialog = new RenameDialog(getControl().getShell(), oldName);
			dialog.create();
			dialog.getShell().setText(MDEUIMessages.EditableTablePart_renameTitle);
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Window.OK) {
				entryModified(obj, dialog.getNewName());
			}
		}
	}

	protected void entryModified(Object entry, String value) {
	}
}
