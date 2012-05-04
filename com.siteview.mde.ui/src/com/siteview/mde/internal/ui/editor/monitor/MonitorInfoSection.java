/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 199431
 *******************************************************************************/
//TODO: change the underlining model to monitor, using java source code as the storage
// 		add a layer on top of jdt (javamodel) to parse the java code to generate the 
//		monitor model: should change from IPluginObject and under
// FormEntry: valuechanges event is the model connection

package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import com.siteview.mde.core.*;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.text.bundle.Bundle;
import com.siteview.mde.internal.core.text.bundle.BundleSymbolicNameHeader;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;
import com.siteview.mde.internal.ui.editor.validation.ControlValidationUtility;
import com.siteview.mde.internal.ui.editor.validation.TextValidator;
import com.siteview.mde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.Constants;

/**
 * Provides the first section of the manifest editor describing the bundle id/name/version/etc.
 */
public abstract class MonitorInfoSection extends MDESection {
	private static String PLATFORM_FILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$

	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fNameEntry;
	private FormEntry fProviderEntry;
	private FormEntry fPlatformFilterEntry;

	private TextValidator fIdEntryValidator;

	private TextValidator fVersionEntryValidator;

	private TextValidator fNameEntryValidator;

	private TextValidator fProviderEntryValidator;

	private TextValidator fPlatformEntryValidator;

	private IMonitorModelBase fModel;

	protected Button fSingleton;

	public MonitorInfoSection(MDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(MDEUIMessages.ManifestEditor_PluginSpecSection_title);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);

		section.setDescription(getSectionDescription());
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 3));
		section.setClient(client);

		IActionBars actionBars = getPage().getMDEEditor().getEditorSite().getActionBars();
		createIDEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createProviderEntry(client, toolkit, actionBars);
		if (isBundle() && ((MonitorEditor) getPage().getEditor()).isEquinox())
			createPlatformFilterEntry(client, toolkit, actionBars);
		createSpecificControls(client, toolkit, actionBars);
		toolkit.paintBordersFor(client);

		addListeners();
	}

	protected abstract String getSectionDescription();

	protected abstract void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars);

	protected IMonitorBase getPluginBase() {
		IBaseModel model = getPage().getMDEEditor().getAggregateModel();
		return ((IMonitorModelBase) model).getMonitorBase();
	}

	/**
	 * Not using the aggregate model from the form editor because it is 
	 * a different model instance from the one used by the bundle error
	 * reporter.  Things get out of sync between the form validator and 
	 * source validator
	 */
	protected IMonitorModelBase getModelBase() {
		// Find the model only on the first call
		if (fModel == null) {
			fModel = MonitorRegistry.findModel(getProject());
		}
		return fModel;
	}

	protected boolean isBundle() {
		return getBundleContext() != null;
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getMDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}

	protected IBundle getBundle() {
		BundleInputContext context = getBundleContext();
		if (context != null) {
			IBundleModel model = (IBundleModel) context.getModel();
			return model.getBundle();
		}
		return null;
	}

	private void createIDEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_id, null, false);
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setId(entry.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fIdEntry.setEditable(isEditable());
		// Create validator
		fIdEntryValidator = new TextValidator(getManagedForm(), fIdEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateIdEntry();
			}
		};
	}

	private boolean validateIdEntry() {
		// Value must be specified
		return ControlValidationUtility.validateRequiredField(fIdEntry.getText().getText(), fIdEntryValidator, IMessageProvider.ERROR);
	}

	private void createVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fVersionEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_version, null, false);
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setVersion(entry.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fVersionEntry.setEditable(isEditable());
		// Create validator
		fVersionEntryValidator = new TextValidator(getManagedForm(), fVersionEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateVersionEntry();
			}
		};
	}

	private boolean validateVersionEntry() {
		// Value must be specified
		if (ControlValidationUtility.validateRequiredField(fVersionEntry.getText().getText(), fVersionEntryValidator, IMessageProvider.ERROR) == false) {
			return false;
		}
		// Value must be a valid version
		return ControlValidationUtility.validateVersionField(fVersionEntry.getText().getText(), fVersionEntryValidator);
	}

	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_name, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setName(entry.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fNameEntry.setEditable(isEditable());
		// Create validator
		fNameEntryValidator = new TextValidator(getManagedForm(), fNameEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateNameEntry();
			}
		};
	}

	private boolean validateNameEntry() {
		// Value must be externalized
		return ControlValidationUtility.validateTranslatableField(fNameEntry.getText().getText(), fNameEntryValidator, getModelBase(), getProject());
	}

	private void createProviderEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fProviderEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_provider, null, false);
		fProviderEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setProviderName(entry.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fProviderEntry.setEditable(isEditable());
		// Create validator
		fProviderEntryValidator = new TextValidator(getManagedForm(), fProviderEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateProviderEntry();
			}
		};
	}

	private boolean validateProviderEntry() {
		// No validation required for an optional field
		if (fProviderEntry.getText().getText().length() == 0) {
			return true;
		}
		// Value must be externalized
		return ControlValidationUtility.validateTranslatableField(fProviderEntry.getText().getText(), fProviderEntryValidator, getModelBase(), getProject());
	}

	private void createPlatformFilterEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fPlatformFilterEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_platformFilter, null, false);
		fPlatformFilterEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getBundle().setHeader(PLATFORM_FILTER, fPlatformFilterEntry.getValue());
			}
		});
		fPlatformFilterEntry.setEditable(isEditable());
		// Create validator
		fPlatformEntryValidator = new TextValidator(getManagedForm(), fPlatformFilterEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validatePlatformEntry();
			}
		};
	}

	private boolean validatePlatformEntry() {
		// No validation required for an optional field
		if (fPlatformFilterEntry.getText().getText().length() == 0) {
			return true;
		}
		// Value must match the current environment and contain valid syntax
		return ControlValidationUtility.validatePlatformFilterField(fPlatformFilterEntry.getText().getText(), fPlatformEntryValidator);
	}

	public void commit(boolean onSave) {
		fIdEntry.commit();
		fVersionEntry.commit();
		fNameEntry.commit();
		fProviderEntry.commit();
		if (fPlatformFilterEntry != null)
			fPlatformFilterEntry.commit();
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IMonitorBase) {
				String property = e.getChangedProperty();
				if (property != null && property.equals(getPage().getMDEEditor().getTitleProperty()))
					getPage().getMDEEditor().updateTitle();
			}
		}
	}

	public void refresh() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getMDEEditor().getContextManager().getAggregateModel();
		IMonitorBase pluginBase = model.getMonitorBase();
		fIdEntry.setValue(pluginBase.getId(), true);
		fNameEntry.setValue(pluginBase.getName(), true);
		fVersionEntry.setValue(pluginBase.getVersion(), true);
		fProviderEntry.setValue(pluginBase.getProviderName(), true);
		if (fPlatformFilterEntry != null) {
			IBundle bundle = getBundle();
			if (bundle != null)
				fPlatformFilterEntry.setValue(bundle.getHeader(PLATFORM_FILTER), true);
		}
		getPage().getMDEEditor().updateTitle();
		if (fSingleton != null) {
			IManifestHeader header = getSingletonHeader();
			fSingleton.setSelection(header instanceof BundleSymbolicNameHeader && ((BundleSymbolicNameHeader) header).isSingleton());
		}
		super.refresh();
	}

	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		fProviderEntry.cancelEdit();
		if (fPlatformFilterEntry != null)
			fPlatformFilterEntry.cancelEdit();
		super.cancelEdit();
	}

	public void dispose() {
		removeListeners();
		super.dispose();
	}

	protected void removeListeners() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
	}

	protected void addListeners() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return (d.getFocusControl() instanceof Text);
	}

	IManifestHeader getSingletonHeader() {
		IBundle bundle = getBundle();
		if (bundle instanceof Bundle) {
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			return header;
		}
		return null;
	}

	protected void createSingleton(Composite parent, FormToolkit toolkit, IActionBars actionBars, String label) {
		fSingleton = toolkit.createButton(parent, label, SWT.CHECK);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fSingleton.setLayoutData(td);
		fSingleton.setEnabled(isEditable());
		fSingleton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IManifestHeader header = getSingletonHeader();
				if (header instanceof BundleSymbolicNameHeader)
					((BundleSymbolicNameHeader) header).setSingleton(fSingleton.getSelection());
			}
		});
	}

}
