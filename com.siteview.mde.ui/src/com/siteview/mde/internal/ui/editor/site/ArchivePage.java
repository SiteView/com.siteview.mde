/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.site;

import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * 
 * Features page.
 */
public class ArchivePage extends MDEFormPage {
	public static final String PAGE_ID = "archives"; //$NON-NLS-1$
	private DescriptionSection fDescSection;
	private MirrorsSection fMirrorsSection;
	private ArchiveSection fArchiveSection;

	public ArchivePage(MDEFormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.ArchivePage_name);
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		fDescSection = new DescriptionSection(this, form.getBody());
		fArchiveSection = new ArchiveSection(this, form.getBody());
		fMirrorsSection = new MirrorsSection(this, form.getBody());

		mform.addPart(fDescSection);
		mform.addPart(fMirrorsSection);
		mform.addPart(fArchiveSection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_SITE_ARCHIVES);
		form.setText(MDEUIMessages.ArchivePage_title);
		form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_JAVA_LIB_OBJ));
	}

	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_SITE_ARCHIVES;
	}
}
