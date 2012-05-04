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
package com.siteview.mde.internal.ui.editor.product;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.jface.window.Window;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.iproduct.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SplashLocationSection extends MDESection {

	private FormEntry fPluginEntry;

	public SplashLocationSection(MDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {

		// Configure section
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);
		section.setText(MDEUIMessages.SplashSection_title);
		section.setDescription(MDEUIMessages.SplashSection_desc);
		// Create and configure client
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// Create label
		Label label = toolkit.createLabel(client, MDEUIMessages.SplashSection_label, SWT.WRAP);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		// Create form entry
		IActionBars actionBars = getPage().getMDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, MDEUIMessages.SplashSection_plugin, MDEUIMessages.SplashSection_browse, false);
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue(), false);
			}

			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fPluginEntry.setEditable(isEditable());

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ISplashInfo info = getSplashInfo();
		fPluginEntry.setValue(info.getLocation(), true);
		super.refresh();
	}

	public void commit(boolean onSave) {
		fPluginEntry.commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fPluginEntry.cancelEdit();
		super.cancelEdit();
	}

	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getMDEEditor().getAggregateModel();
	}

	private void handleBrowse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(MDEPlugin.getActiveWorkbenchShell(), MDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(MonitorRegistry.getActiveModels());
		dialog.setMultipleSelection(false);
		dialog.setTitle(MDEUIMessages.SplashSection_selection);
		dialog.setMessage(MDEUIMessages.SplashSection_message);
		if (dialog.open() == Window.OK) {
			IMonitorModelBase model = (IMonitorModelBase) dialog.getFirstResult();
			fPluginEntry.setValue(model.getMonitorBase().getId());
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

}
