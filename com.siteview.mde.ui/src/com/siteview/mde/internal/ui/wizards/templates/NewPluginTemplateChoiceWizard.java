/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.templates;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.ui.templates.AbstractNewPluginTemplateWizard;
import com.siteview.mde.ui.templates.ITemplateSection;
import org.eclipse.swt.widgets.Composite;

public class NewPluginTemplateChoiceWizard extends AbstractNewPluginTemplateWizard {
	private TemplateSelectionPage fSelectionPage;
	private ITemplateSection[] fCandidates;

	public NewPluginTemplateChoiceWizard() {
	}

	public ITemplateSection[] getTemplateSections() {
		if (fSelectionPage != null) {
			return fSelectionPage.getSelectedTemplates();
		}
		return getCandidates();
	}

	public void addAdditionalPages() {
		fSelectionPage = new TemplateSelectionPage(getCandidates());
		addPage(fSelectionPage);
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (fSelectionPage == null)
			return null;
		return fSelectionPage.getNextVisiblePage(page);
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}

	private ITemplateSection[] getCandidates() {
		if (fCandidates == null) {
			createCandidates();
		}
		return fCandidates;

	}

	// calculate canFinish only on selected templateSections and the status of their pages
	public boolean canFinish() {
		ITemplateSection[] sections = fSelectionPage.getSelectedTemplates();
		for (int i = 0; i < sections.length; i++) {
			int pageCount = sections[i].getPageCount();
			for (int j = 0; j < pageCount; j++) {
				WizardPage page = sections[i].getPage(j);
				if (page != null && !page.isPageComplete())
					return false;
			}
		}
		return true;
	}

	private void createCandidates() {
		ArrayList candidates;
		candidates = new ArrayList();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(MDEPlugin.getPluginId(), "templates"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			addTemplate(element, candidates);
		}
		fCandidates = (ITemplateSection[]) candidates.toArray(new ITemplateSection[candidates.size()]);
	}

	private void addTemplate(IConfigurationElement config, ArrayList result) {
		if (config.getName().equalsIgnoreCase("template") == false) //$NON-NLS-1$
			return;

		try {
			Object template = config.createExecutableExtension("class"); //$NON-NLS-1$
			if (template instanceof ITemplateSection) {
				result.add(template);
			}
		} catch (CoreException e) {
			MDEPlugin.log(e);
		}
	}

	// by default, all pages in wizard get created.  We add all the pages from the template sections and we don't want to initialize them yet
	// Therefore, the createPageControls only initializes the first page, allowing the other to be created as needed.
	public void createPageControls(Composite pageContainer) {
		fSelectionPage.createControl(pageContainer);
	}
}
