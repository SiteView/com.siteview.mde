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
package com.siteview.mde.internal.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import com.siteview.mde.core.IModelChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Wrapper for PDESections, implemens IDetailsPage for use in MasterDetailsBlock
 */
public abstract class MDEDetailsSections extends MDEDetails {
	private MDESection sections[];

	protected abstract MDESection[] createSections(MDEFormPage page, Composite parent);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		sections = createSections(getPage(), parent);
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		for (int i = 0; i < sections.length; i++) {
			getManagedForm().addPart(sections[i]);
		}
	}

	public void dispose() {
		for (int i = 0; i < sections.length; i++) {
			sections[i].dispose();
		}
	}

	public void fireSaveNeeded() {
		markDirty();
		getPage().getMDEEditor().fireSaveNeeded(getContextId(), false);
	}

	public abstract String getContextId();

	public MDEFormPage getPage() {
		return (MDEFormPage) getManagedForm().getContainer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	public boolean isDirty() {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].isDirty()) {
				return true;
			}
		}
		return super.isDirty();
	}

	public boolean isEditable() {
		return getPage().getMDEEditor().getAggregateModel().isEditable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isStale()
	 */
	public boolean isStale() {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].isStale()) {
				return true;
			}
		}
		return super.isStale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		if (sections.length > 0) {
			sections[0].setFocus();
		}
	}
}
