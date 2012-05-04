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
/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.siteview.mde.internal.ui.editor.monitor.rows;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.ui.editor.IContextPart;
import com.siteview.mde.internal.ui.editor.text.PDETextHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public abstract class ReferenceAttributeRow extends TextAttributeRow {

	public ReferenceAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	protected void createLabel(Composite parent, FormToolkit toolkit) {
		if (!part.isEditable()) {
			super.createLabel(parent, toolkit);
			return;
		}

		Hyperlink link = toolkit.createHyperlink(parent, getPropertyLabel(), SWT.NULL);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (!isReferenceModel()) {
					openReference();
				} else {
					Display.getCurrent().beep();
				}
			}
		});
		PDETextHover.addHoverListenerToControl(fIC, link, this);
	}

	protected boolean isReferenceModel() {
		return ((IMonitorModelBase) part.getPage().getModel()).getUnderlyingResource() != null;
	}

	protected abstract void openReference();

}
