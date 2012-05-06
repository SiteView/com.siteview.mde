/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.product;

import com.siteview.mde.core.monitor.*;

import java.util.TreeSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.iproduct.IProduct;
import com.siteview.mde.internal.ui.IHelpContextIds;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;
import com.siteview.mde.internal.ui.search.ShowDescriptionAction;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ProductIntroWizardPage extends WizardPage implements IHyperlinkListener {

	private Text fPluginText;
	private Text fIntroIdText;
	private TreeSet fIntroIds;
	private IProduct fProduct;

	private ModifyListener fListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};

	public ProductIntroWizardPage(String pageName, IProduct product) {
		super(pageName);
		setTitle(MDEUIMessages.ProductIntroWizardPage_title);
		setDescription(MDEUIMessages.ProductIntroWizardPage_description);
		fIntroIds = getCurrentIntroIds();
		fProduct = product;
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 20;
		comp.setLayout(layout);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		createProductGroup(toolkit, comp);
		toolkit.dispose();

		setControl(comp);
		setPageComplete(getPluginId() != null);
		Dialog.applyDialogFont(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IHelpContextIds.PRODUCT_DEFINITIONS_WIZARD);
	}

	private void createProductGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(MDEUIMessages.ProductIntroWizardPage_groupText);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		FormText text = toolkit.createFormText(group, false);
		text.setText(MDEUIMessages.ProductIntroWizardPage_formText, true, false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.widthHint = 400;
		text.setLayoutData(gd);
		text.setBackground(null);
		text.addHyperlinkListener(this);

		Label label = new Label(group, SWT.NONE);
		label.setText(MDEUIMessages.ProductIntroWizardPage_targetLabel);

		fPluginText = new Text(group, SWT.SINGLE | SWT.BORDER);
		fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button button = new Button(group, SWT.PUSH);
		button.setText(MDEUIMessages.ProductIntroWizardPage_browse);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		label = new Label(group, SWT.NONE);
		label.setText(MDEUIMessages.ProductIntroWizardPage_introLabel);

		fIntroIdText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fIntroIdText.setLayoutData(gd);

		String pluginId = getPluginId();
		if (pluginId != null) {
			fPluginText.setText(pluginId);
			fIntroIdText.setText(getAvailableIntroId(pluginId));
		}
		fPluginText.addModifyListener(fListener);
		fIntroIdText.addModifyListener(fListener);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fPluginText.setVisible(visible);
			fPluginText.setFocus();
		}
	}

	private void validatePage() {
		String error = null;
		String pluginId = getDefiningPlugin();
		IMonitorModelBase model = MonitorRegistry.findModel(pluginId);
		if (model == null) {
			error = MDEUIMessages.ProductDefinitonWizardPage_noPlugin;
		} else if (model.getUnderlyingResource() == null) {
			error = MDEUIMessages.ProductDefinitonWizardPage_notInWorkspace;
		} else if (pluginId.length() == 0) {
			error = MDEUIMessages.ProductIntroWizardPage_targetNotSet;
		}
		validateId(error);

	}

	private void validateId(String error) {
		if (error == null) {
			String id = fIntroIdText.getText().trim();

			if (id.length() == 0)
				error = MDEUIMessages.ProductIntroWizardPage_introNotSet;

			if (error == null)
				for (int i = 0; i < id.length(); i++)
					if (!id.substring(i, i + 1).matches("[a-zA-Z0-9.]")) //$NON-NLS-1$
						error = MDEUIMessages.ProductIntroWizardPage_invalidIntroId;

			if (error == null && fIntroIds.contains(id))
				error = MDEUIMessages.ProductIntroWizardPage_introIdExists;
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	private void handleBrowse() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), MonitorRegistry.getWorkspaceModels(), false);
		if (dialog.open() == Window.OK) {
			IMonitorModelBase model = (IMonitorModelBase) dialog.getFirstResult();
			String id = model.getMonitorBase().getId();
			fPluginText.setText(id);
			fIntroIdText.setText(getAvailableIntroId(id));
		}
	}

	private String getAvailableIntroId(String id) {
		String introId = "intro"; //$NON-NLS-1$
		String numString = ""; //$NON-NLS-1$
		int idNum = 1;
		while (fIntroIds.contains(id + "." + introId + numString)) { //$NON-NLS-1$
			numString = Integer.toString(idNum++);
		}
		return id + "." + introId + numString; //$NON-NLS-1$
	}

	private TreeSet getCurrentIntroIds() {
		String introId;
		TreeSet result = new TreeSet();
		IExtension[] extensions = MDECore.getDefault().getExtensionsRegistry().findExtensions("org.eclipse.ui.intro", true); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] children = extensions[i].getConfigurationElements();
			for (int j = 0; j < children.length; j++) {
				if ("intro".equals(children[j].getName())) {//$NON-NLS-1$
					introId = children[j].getAttribute("id"); //$NON-NLS-1$
					if (introId != null)
						result.add(introId);
				}
			}
		}
		return result;
	}

	public String getDefiningPlugin() {
		return fPluginText.getText().trim();
	}

	public String getIntroId() {
		return fIntroIdText.getText().trim();
	}

	private String getPluginId() {
		IProject project = fProduct.getModel().getUnderlyingResource().getProject();
		IMonitorModelBase model = MonitorRegistry.findModel(project);
		return (model == null) ? null : model.getMonitorBase().getId();
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}

	public void linkActivated(HyperlinkEvent e) {
		String extPoint = "org.eclipse.ui." + e.getHref().toString(); //$NON-NLS-1$
		IMonitorExtensionPoint point = MDECore.getDefault().getExtensionsRegistry().findExtensionPoint(extPoint);
		if (point != null)
			new ShowDescriptionAction(point, true).run();

	}
}
