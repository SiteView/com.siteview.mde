/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Deepak Azad <deepak.azad@in.ibm.com> - bug 249066
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import org.eclipse.jface.dialogs.MessageDialog;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.builders.DependencyLoop;
import com.siteview.mde.internal.core.builders.DependencyLoopFinder;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import com.siteview.mde.internal.ui.editor.MDESection;
import com.siteview.mde.internal.ui.search.dependencies.UnusedDependenciesAction;
import com.siteview.mde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import com.siteview.mde.internal.ui.views.dependencies.OpenPluginReferencesAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class DependencyAnalysisSection extends MDESection {
	private FormText formText;

	public DependencyAnalysisSection(MDEFormPage page, Composite parent, int style) {
		super(page, parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | style);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	private String getFormText() {
		boolean editable = getPage().getModel().isEditable();
		if (getPage().getModel() instanceof IMonitorModel) {
			if (editable)
				return MDEUIMessages.DependencyAnalysisSection_plugin_editable;
			return MDEUIMessages.DependencyAnalysisSection_plugin_notEditable;
		}
		if (editable)
			return MDEUIMessages.DependencyAnalysisSection_fragment_editable;
		return MDEUIMessages.DependencyAnalysisSection_fragment_notEditable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(MDEUIMessages.DependencyAnalysisSection_title);

		formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(), true, false);
		MDELabelProvider lp = MDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(MDEPluginImages.DESC_LOOP_OBJ)); //$NON-NLS-1$
		formText.setImage("search", lp.get(MDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		formText.setImage("hierarchy", lp.get(MDEPluginImages.DESC_CALLEES)); //$NON-NLS-1$
		formText.setImage("dependencies", lp.get(MDEPluginImages.DESC_CALLERS)); //$NON-NLS-1$
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("unused")) //$NON-NLS-1$
					doFindUnusedDependencies();
				else if (e.getHref().equals("loops")) //$NON-NLS-1$
					doFindLoops();
				else if (e.getHref().equals("references")) //$NON-NLS-1$
					new OpenPluginReferencesAction(MonitorRegistry.findModel(getPlugin().getId())).run();
				else if (e.getHref().equals("hierarchy")) //$NON-NLS-1$
					new OpenPluginDependenciesAction(MonitorRegistry.findModel(getPlugin().getId())).run();
			}
		});

		section.setClient(formText);
	}

	protected IMonitor getPlugin() {
		IBaseModel model = getPage().getModel();
		IMonitor plugin = null;
		if (model instanceof IMonitorModel) {
			plugin = ((IMonitorModel) model).getMonitor();
		}
		return plugin;
	}

	protected void doFindLoops() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IMonitorModel) {
			IMonitor plugin = ((IMonitorModel) model).getMonitor();
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
			if (loops.length == 0)
				MessageDialog.openInformation(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.DependencyAnalysisSection_loops, MDEUIMessages.DependencyAnalysisSection_noCycles); // 
			else {
				LoopDialog dialog = new LoopDialog(MDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}
	}

	protected void doFindUnusedDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IMonitorModelBase) {
			new UnusedDependenciesAction((IMonitorModelBase) model, false).run();
		}
	}

}
