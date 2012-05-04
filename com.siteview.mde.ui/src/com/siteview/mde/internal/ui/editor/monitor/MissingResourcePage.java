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
package com.siteview.mde.internal.ui.editor.monitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class MissingResourcePage extends MDEFormPage {

	public MissingResourcePage(FormEditor editor) {
		super(editor, "missing", MDEUIMessages.MissingResourcePage_missingResource); //$NON-NLS-1$
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		Composite comp = managedForm.getToolkit().createComposite(form);
		comp.setLayout(new GridLayout());
		IPersistableElement persistable = getEditorInput().getPersistable();
		String text;
		if (persistable instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) persistable).getFile();
			text = NLS.bind(MDEUIMessages.MissingResourcePage_unableToOpenFull, new String[] {MDEUIMessages.MissingResourcePage_unableToOpen, file.getProjectRelativePath().toString(), file.getProject().getName()});
		} else
			text = MDEUIMessages.MissingResourcePage_unableToOpen;
		form.setText(text);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
}
