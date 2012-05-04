/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.product;

import com.siteview.mde.core.monitor.*;

import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;

import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.core.iproduct.IProduct;
import com.siteview.mde.internal.ui.IHelpContextIds;
import com.siteview.mde.internal.ui.MDEUIMessages;
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

public class ProductDefinitonWizardPage extends WizardPage implements IHyperlinkListener {

	private Text fProductName;
	private Text fPluginText;
	private Text fProductText;
	private Set fProductSet;
	private Combo fApplicationCombo;
	private IProduct fProduct;

	private ModifyListener fListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};

	public ProductDefinitonWizardPage(String pageName, IProduct product) {
		super(pageName);
		fProduct = product;
		setTitle(MDEUIMessages.ProductDefinitonWizardPage_title);
		if (productNameDefined())
			setDescription(MDEUIMessages.ProductDefinitonWizardPage_desc);
		else
			setDescription(MDEUIMessages.ProductDefinitonWizardPage_descNoName);
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 20;
		comp.setLayout(layout);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		createProductGroup(toolkit, comp);
		createApplicationGroup(toolkit, comp);
		toolkit.dispose();
		setControl(comp);
		setPageComplete(getPluginId() != null && productNameDefined());
		Dialog.applyDialogFont(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IHelpContextIds.PRODUCT_DEFINITIONS_WIZARD);
	}

	private void createFormText(FormToolkit toolkit, Composite parent, String content, int span) {
		FormText text = toolkit.createFormText(parent, false);
		text.setText(content, true, false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		gd.widthHint = 400;
		text.setLayoutData(gd);
		text.setBackground(null);
		text.addHyperlinkListener(this);
	}

	private void createProductGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(MDEUIMessages.ProductDefinitonWizardPage_productGroup);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, MDEUIMessages.ProductDefinitonWizardPage_productDefinition, 3);

		Label label;
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;

		if (!productNameDefined()) { 
			label = new Label(group, SWT.NONE);
			label.setText(MDEUIMessages.ProductDefinitonWizardPage_productName);

			fProductName = new Text(group, SWT.SINGLE | SWT.BORDER);
			fProductName.setLayoutData(gd);
			fProductName.addModifyListener(fListener);
		}

		label = new Label(group, SWT.NONE);
		label.setText(MDEUIMessages.ProductDefinitonWizardPage_plugin);

		fPluginText = new Text(group, SWT.SINGLE | SWT.BORDER);
		fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button button = new Button(group, SWT.PUSH);
		button.setText(MDEUIMessages.ProductDefinitonWizardPage_browse);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		label = new Label(group, SWT.NONE);
		label.setText(MDEUIMessages.ProductDefinitonWizardPage_productId);

		fProductText = new Text(group, SWT.SINGLE | SWT.BORDER);
		fProductText.setLayoutData(gd);

		String pluginId = getPluginId();
		if (pluginId != null) {
			fPluginText.setText(pluginId);
			String productId = "product"; //$NON-NLS-1$
			String numString = ""; //$NON-NLS-1$
			int idNum = 1;
			while (getProductNameSet().contains(pluginId + "." + productId + numString)) { //$NON-NLS-1$
				numString = Integer.toString(idNum++);
			}
			fProductText.setText(productId + numString);
		}
		fPluginText.addModifyListener(fListener);
		fProductText.addModifyListener(fListener);

	}

	private void createApplicationGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(MDEUIMessages.ProductDefinitonWizardPage_applicationGroup);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, MDEUIMessages.ProductDefinitonWizardPage_applicationDefinition, 2);

		Label label = new Label(group, SWT.NONE);
		label.setText(MDEUIMessages.ProductDefinitonWizardPage_application);

		fApplicationCombo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
		fApplicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fApplicationCombo.setItems(TargetPlatform.getApplications());
		if (fApplicationCombo.getItemCount() > 0)
			fApplicationCombo.setText(fApplicationCombo.getItem(0));
	}

	public void setVisible(boolean visible) {
		if (visible) {
			if (fProductName != null)
				fProductName.setFocus();
			else
				fPluginText.setFocus();
		}
		super.setVisible(visible);
	}

	private void validatePage() {
		String error = null;
		String productName = getProductName();
		if (productName != null && productName.length() == 0) {
			error = MDEUIMessages.ProductDefinitonWizardPage_noProductName;
		}
		validateIdAndProduct(error);
	}

	private void validateIdAndProduct(String error) {
		if (error == null) {
			String pluginId = getDefiningPlugin();
			IMonitorModelBase model = MonitorRegistry.findModel(pluginId);
			if (pluginId.length() == 0) {
				error = MDEUIMessages.ProductDefinitonWizardPage_noPluginId;
			} else if (model == null) {
				error = MDEUIMessages.ProductDefinitonWizardPage_noPlugin;
			} else if (model.getUnderlyingResource() == null) {
				error = MDEUIMessages.ProductDefinitonWizardPage_notInWorkspace;
			}
			if (error == null)
				error = validateId();
			if (error == null && getProductNameSet().contains(pluginId + "." + fProductText.getText().trim())) { //$NON-NLS-1$
				error = MDEUIMessages.ProductDefinitonWizardPage_productExists;
			}
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	private String validateId() {
		String id = fProductText.getText().trim();
		if (id.length() == 0)
			return MDEUIMessages.ProductDefinitonWizardPage_noProductID;

		for (int i = 0; i < id.length(); i++) {
			if (!id.substring(i, i + 1).matches("[a-zA-Z0-9_]")) //$NON-NLS-1$
				return MDEUIMessages.ProductDefinitonWizardPage_invalidId;
		}
		return null;
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}

	public void linkActivated(HyperlinkEvent e) {
		String extPoint = Platform.PI_RUNTIME + "." + e.getHref().toString(); //$NON-NLS-1$
		IPluginExtensionPoint point = MDECore.getDefault().getExtensionsRegistry().findExtensionPoint(extPoint);
		if (point != null)
			new ShowDescriptionAction(point, true).run();
	}

	private void handleBrowse() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), MonitorRegistry.getWorkspaceModels(), false);
		if (dialog.open() == Window.OK) {
			IMonitorModelBase model = (IMonitorModelBase) dialog.getFirstResult();
			fPluginText.setText(model.getMonitorBase().getId());
		}
	}

	private Set getProductNameSet() {
		if (fProductSet == null)
			fProductSet = TargetPlatformHelper.getProductNameSet();
		return fProductSet;
	}

	public String getDefiningPlugin() {
		return fPluginText.getText().trim();
	}

	public String getProductId() {
		return fProductText.getText().trim();
	}

	public String getApplication() {
		return fApplicationCombo.getText();
	}

	public String getProductName() {
		return (fProductName == null) ? null : fProductName.getText().trim();
	}

	private boolean productNameDefined() {
		return (fProduct.getName() != null && !fProduct.getName().equals("")); //$NON-NLS-1$
	}

	private String getPluginId() {
		IProject project = fProduct.getModel().getUnderlyingResource().getProject();
		IMonitorModelBase model = MonitorRegistry.findModel(project);
		return (model == null) ? null : model.getMonitorBase().getId();
	}
}
