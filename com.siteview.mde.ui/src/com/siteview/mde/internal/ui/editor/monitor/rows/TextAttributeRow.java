/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor.rows;

import com.siteview.mde.core.monitor.IMonitorAttribute;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.IContextPart;
import com.siteview.mde.internal.ui.editor.text.PDETextHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class TextAttributeRow extends ExtensionAttributeRow {
	protected Text text;

	/**
	 * @param att
	 */
	public TextAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	public TextAttributeRow(IContextPart part, IMonitorAttribute att) {
		super(part, att);
	}

	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		createLabel(parent, toolkit);
		text = toolkit.createText(parent, "", SWT.SINGLE); //$NON-NLS-1$
		text.setLayoutData(createGridData(span));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockNotification)
					markDirty();
				PDETextHover.updateHover(fIC, getHoverContent(text));
			}
		});
		text.setEditable(part.isEditable());
		PDETextHover.addHoverListenerToControl(fIC, text, this);
		// Create a focus listener to update selectable actions
		createUITextFocusListener();
	}

	/**
	 * 
	 */
	private void createUITextFocusListener() {
		// Required to enable Ctrl-V paste operations
		text.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				ITextSelection selection = new TextSelection(1, 1);
				part.getPage().getMDEEditor().getContributor().updateSelectableActions(selection);
			}
		});
	}

	protected GridData createGridData(int span) {
		GridData gd = new GridData(span == 2 ? GridData.FILL_HORIZONTAL : GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.horizontalSpan = span - 1;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		return gd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#update(org.eclipse.pde.internal.ui.neweditor.plugin.DummyExtensionElement)
	 */
	protected void update() {
		blockNotification = true;
		text.setText(getValue());
		blockNotification = false;
	}

	public void commit() {
		if (dirty && input != null) {
			String value = text.getText();
			try {
				input.setAttribute(getName(), value);
				dirty = false;
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	public void setFocus() {
		text.setFocus();
	}
}
