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
import com.siteview.mde.core.monitor.IMonitorElement;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import com.siteview.mde.internal.core.ischema.ISchemaAttribute;
import com.siteview.mde.internal.ui.editor.IContextPart;
import com.siteview.mde.internal.ui.editor.text.IControlHoverContentProvider;
import com.siteview.mde.internal.ui.editor.text.PDETextHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public abstract class ExtensionAttributeRow implements IControlHoverContentProvider {
	protected IContextPart part;
	protected Object att;
	protected IMonitorElement input;
	protected boolean blockNotification;
	protected boolean dirty;
	protected IInformationControl fIC;

	public ExtensionAttributeRow(IContextPart part, ISchemaAttribute att) {
		this.part = part;
		this.att = att;
	}

	public ExtensionAttributeRow(IContextPart part, IMonitorAttribute att) {
		this.part = part;
		this.att = att;
	}

	public ISchemaAttribute getAttribute() {
		return (att instanceof ISchemaAttribute) ? (ISchemaAttribute) att : null;
	}

	public String getName() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getName();

		return ((IMonitorAttribute) att).getName();
	}

	protected int getUse() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getUse();
		return ISchemaAttribute.OPTIONAL;
	}

	protected String getDescription() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getDescription();
		return ""; //$NON-NLS-1$
	}

	protected String getValue() {
		String value = ""; //$NON-NLS-1$
		if (input != null) {
			IMonitorAttribute patt = input.getAttribute(getName());
			if (patt != null)
				value = patt.getValue();
		}
		return value;
	}

	protected String getPropertyLabel() {
		String label = getName();
		if (getUse() == ISchemaAttribute.REQUIRED)
			label += "*:"; //$NON-NLS-1$
		else
			label += ":"; //$NON-NLS-1$
		return label;
	}

	protected void createLabel(Composite parent, FormToolkit toolkit) {
		Label label = toolkit.createLabel(parent, getPropertyLabel(), SWT.NULL);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		PDETextHover.addHoverListenerToControl(fIC, label, this);
	}

	/**
	 * @param control
	 */
	protected void createTextHover(Control control) {
		fIC = PDETextHover.getInformationControlCreator().createInformationControl(control.getShell());
		fIC.setSizeConstraints(300, 600);
	}

	public String getHoverContent(Control c) {
		if (c instanceof Label || c instanceof Hyperlink)
			return getDescription();
		if (c instanceof Text) {
			String text = ((Text) c).getText();
			ISchemaAttribute sAtt = getAttribute();
			String translated = null;
			if (input != null && sAtt != null && sAtt.isTranslatable() && text.startsWith("%")) //$NON-NLS-1$
				translated = input.getResourceString(text);
			if (!text.equals(translated))
				return translated;
		}
		return null;
	}

	/**
	 * @param parent
	 * @param toolkit
	 * @param span
	 */
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createTextHover(parent);
	}

	protected abstract void update();

	public abstract void commit();

	public abstract void setFocus();

	public boolean isDirty() {
		return dirty;
	}

	protected void markDirty() {
		dirty = true;
		part.fireSaveNeeded();
	}

	public void dispose() {
		if (fIC != null)
			fIC.dispose();
	}

	public void setInput(IMonitorElement input) {
		this.input = input;
		update();
	}

	protected IProject getProject() {
		return part.getPage().getMDEEditor().getCommonProject();
	}
}
