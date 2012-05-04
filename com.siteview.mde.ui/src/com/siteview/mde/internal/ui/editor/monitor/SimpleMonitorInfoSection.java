/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 199431
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.ManifestElement;
import com.siteview.mde.internal.core.ibundle.IBundle;
import com.siteview.mde.internal.core.ibundle.IBundleModel;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;
import com.siteview.mde.internal.ui.editor.FormEntryAdapter;
import com.siteview.mde.internal.ui.editor.MDEFormPage;
import com.siteview.mde.internal.ui.editor.validation.ControlValidationUtility;
import com.siteview.mde.internal.ui.editor.validation.TextValidator;
import com.siteview.mde.internal.ui.parts.ComboPart;
import com.siteview.mde.internal.ui.parts.FormEntry;
import com.siteview.mde.internal.ui.util.SWTUtil;
import com.siteview.mde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class SimpleMonitorInfoSection extends MonitorInfoSection {

	private FormEntry fPluginIdEntry;
	private FormEntry fPluginMinVersionEntry;
	private FormEntry fPluginMaxVersionEntry;
	private ComboPart fPluginMinVersionBound;
	private ComboPart fPluginMaxVersionBound;
	private ComboPart fMatchCombo;

	private TextValidator fPluginIdValidator;

	private TextValidator fPluginMinVersionValidator;

	private TextValidator fPluginMaxVersionValidator;

	public SimpleMonitorInfoSection(MDEFormPage page, Composite parent) {
		super(page, parent);
	}

	protected String getSectionDescription() {
		return MDEUIMessages.MonitorEditor_MonitorSpecSection_desc;
	}

	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createPluginIdEntry(parent, toolkit, actionBars);
		createPluginVersionEntry(parent, toolkit, actionBars);
		if (!isBundle())
			createMatchCombo(parent, toolkit, actionBars);
		createSingleton(parent, toolkit, actionBars, MDEUIMessages.FragmentGeneralInfoSection_singleton);
	}

	private void createPluginIdEntry(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fPluginIdEntry = new FormEntry(parent, toolkit, MDEUIMessages.GeneralInfoSection_pluginId, MDEUIMessages.GeneralInfoSection_browse, // 
				isEditable());
		fPluginIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginId(fPluginIdEntry.getValue());
				} catch (CoreException e1) {
					MDEPlugin.logException(e1);
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				String plugin = fPluginIdEntry.getValue();
				if (!(MonitorRegistry.findModel(plugin) instanceof IMonitorModel)) {
					createFragmentPlugin();
				}
				MonitorEditor.openPluginEditor(fPluginIdEntry.getValue());
			}

			public void browseButtonSelected(FormEntry entry) {
				handleOpenDialog();
			}

			private void createFragmentPlugin() {
				NewPluginProjectWizard wizard = new NewPluginProjectWizard("Equinox"); //$NON-NLS-1$
				wizard.init(MDEPlugin.getActiveWorkbenchWindow().getWorkbench(), new StructuredSelection());
				WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 500);
				if (dialog.open() == Window.OK) {
					String plugin = wizard.getPluginId();
					try {
						((IFragment) getPluginBase()).setPluginId(plugin);
						fPluginIdEntry.setValue(plugin, false);
					} catch (CoreException ce) {
						MDEPlugin.logException(ce);
					}
				}
			}
		});
		fPluginIdEntry.setEditable(isEditable());
		// Create validator
		fPluginIdValidator = new TextValidator(getManagedForm(), fPluginIdEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validatePluginId();
			}
		};
	}

	private boolean validatePluginId() {
		// Validate host plugin
		return ControlValidationUtility.validateFragmentHostPluginField(fPluginIdEntry.getText().getText(), fPluginIdValidator, getProject());
	}

	protected void handleOpenDialog() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getSection().getShell(), false, false);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IMonitorModel model = (IMonitorModel) dialog.getFirstResult();
			IMonitor plugin = model.getMonitor();
			try {
				((IFragment) getPluginBase()).setPluginId(plugin.getId());
				fPluginMinVersionEntry.setValue(plugin.getVersion());
				((IFragment) getPluginBase()).setPluginVersion(getVersion());
			} catch (CoreException e) {
			}

		}
	}

	private void createPluginVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		if (isBundle()) {
			createBundlePluginVersionEntry(client, toolkit, actionBars);
		} else {
			createNonBundlePluginVersionEntry(client, toolkit, actionBars);
		}

	}

	private void createBundlePluginVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {

		FormEntryAdapter textListener = new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}

			public void textDirty(FormEntry entry) {
				setFieldsEnabled();
				super.textDirty(entry);
			}
		};
		String[] items = new String[] {MDEUIMessages.DependencyPropertiesDialog_comboInclusive, MDEUIMessages.DependencyPropertiesDialog_comboExclusive};
		fPluginMinVersionEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_hostMinVersionRange, 0, 1);
		fPluginMinVersionEntry.setFormEntryListener(textListener);
		fPluginMinVersionEntry.setEditable(isEditable());
		// Create validator
		fPluginMinVersionValidator = new TextValidator(getManagedForm(), fPluginMinVersionEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validatePluginMinVersion();
			}
		};

		SelectionAdapter comboListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(getVersion());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		};
		fPluginMinVersionBound = new ComboPart();
		fPluginMinVersionBound.createControl(client, toolkit, SWT.READ_ONLY);
		fPluginMinVersionBound.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL));
		fPluginMinVersionBound.setItems(items);
		fPluginMinVersionBound.getControl().setEnabled(isEditable());
		fPluginMinVersionBound.addSelectionListener(comboListener);

		fPluginMaxVersionEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_hostMaxVersionRange, 0, 1);
		fPluginMaxVersionEntry.setFormEntryListener(textListener);
		fPluginMaxVersionEntry.setEditable(isEditable());
		// Create validator
		fPluginMaxVersionValidator = new TextValidator(getManagedForm(), fPluginMaxVersionEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validatePluginMaxVersion();
			}
		};
		fPluginMaxVersionBound = new ComboPart();
		fPluginMaxVersionBound.createControl(client, toolkit, SWT.READ_ONLY);
		fPluginMaxVersionBound.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL));
		fPluginMaxVersionBound.setItems(items);
		fPluginMaxVersionBound.getControl().setEnabled(isEditable());
		fPluginMaxVersionBound.addSelectionListener(comboListener);
	}

	private boolean validatePluginMaxVersion() {
		// No validation required for an optional field
		if (fPluginMaxVersionEntry.getText().getText().length() == 0) {
			return true;
		}
		// Value must be a valid version
		return ControlValidationUtility.validateVersionField(fPluginMaxVersionEntry.getText().getText(), fPluginMaxVersionValidator);
	}

	private boolean validatePluginMinVersion() {
		// No validation required for an optional field
		if (fPluginMinVersionEntry.getText().getText().length() == 0) {
			return true;
		}
		// Value must be a valid version
		return ControlValidationUtility.validateVersionField(fPluginMinVersionEntry.getText().getText(), fPluginMinVersionValidator);
	}

	private void createNonBundlePluginVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fPluginMinVersionEntry = new FormEntry(client, toolkit, MDEUIMessages.GeneralInfoSection_pluginVersion, null, false);
		fPluginMinVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(entry.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fPluginMinVersionEntry.setEditable(isEditable());
	}

	private void createMatchCombo(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		Label matchLabel = toolkit.createLabel(client, MDEUIMessages.ManifestEditor_PluginSpecSection_versionMatch);
		matchLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		matchLabel.setLayoutData(td);

		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		td.valign = TableWrapData.MIDDLE;
		fMatchCombo.getControl().setLayoutData(td);

		String[] items = new String[] {"", //$NON-NLS-1$
				MDEUIMessages.ManifestEditor_MatchSection_equivalent, MDEUIMessages.ManifestEditor_MatchSection_compatible, MDEUIMessages.ManifestEditor_MatchSection_perfect, MDEUIMessages.ManifestEditor_MatchSection_greater};
		fMatchCombo.setItems(items);
		fMatchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int match = fMatchCombo.getSelectionIndex();
				try {
					((IFragment) getPluginBase()).setRule(match);
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		fMatchCombo.getControl().setEnabled(isEditable());
	}

	public void commit(boolean onSave) {
		fPluginIdEntry.commit();
		fPluginMinVersionEntry.commit();
		fPluginMaxVersionEntry.commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fPluginIdEntry.cancelEdit();
		fPluginMinVersionEntry.cancelEdit();
		fPluginMaxVersionEntry.cancelEdit();
		super.cancelEdit();
	}

	public void refresh() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		IFragment fragment = (IFragment) model.getMonitorBase();
		fPluginIdEntry.setValue(fragment.getPluginId(), true);
		if (isBundle()) {
			refreshVersion();
		} else {
			fPluginMinVersionEntry.setValue(fragment.getPluginVersion(), true);
		}
		if (fMatchCombo != null)
			fMatchCombo.select(fragment.getRule());
		super.refresh();
	}

	protected String getAttribute(String header, String attribute) {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		String value = bundle.getHeader(header);
		if (value == null)
			return null;
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(header, value);
			if (elements.length > 0)
				return elements[0].getAttribute(attribute);
		} catch (BundleException e) {
		}
		return null;
	}

	private void setFieldsEnabled() {
		boolean singleVersion = fPluginMaxVersionEntry.getText().getText().trim().length() == 0;
		boolean enabled = fPluginMinVersionEntry.getText().getText().trim().length() != 0;
		fPluginMaxVersionEntry.getText().setEnabled(enabled);
		fPluginMaxVersionBound.getControl().setEnabled(!singleVersion && enabled && isEditable());
		fPluginMinVersionBound.getControl().setEnabled(!singleVersion && isEditable());
	}

	private String getVersion() {
		if (isBundle()) {
			if (!fPluginMinVersionEntry.getValue().equals(fPluginMaxVersionEntry.getValue()) && fPluginMaxVersionEntry.getText().getEnabled()) {
				if (fPluginMaxVersionEntry.getValue().length() == 0)
					return fPluginMinVersionEntry.getValue();
				String version;
				if (fPluginMinVersionBound.getSelectionIndex() == 0)
					version = "["; //$NON-NLS-1$
				else
					version = "("; //$NON-NLS-1$
				version += fPluginMinVersionEntry.getValue() + "," + fPluginMaxVersionEntry.getValue(); //$NON-NLS-1$
				if (fPluginMaxVersionBound.getSelectionIndex() == 0)
					version += "]"; //$NON-NLS-1$
				else
					version += ")"; //$NON-NLS-1$
				return version;
			}
		}
		return fPluginMinVersionEntry.getValue();
	}

	private void refreshVersion() {
		String version = getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (version == null) {
			setVersionFields("", true, "", false); //$NON-NLS-1$ //$NON-NLS-2$
			setFieldsEnabled();
			return;
		}
		version = version.trim();
		int comInd = version.indexOf(","); //$NON-NLS-1$
		int lastPos = version.length() - 1;
		char first = version.charAt(0);
		char last = version.charAt(lastPos);
		if (comInd == -1) {
			setVersionFields(version, true, "", false); //$NON-NLS-1$
		} else if ((first == '[' || first == '(') && (last == ']' || last == ')')) {
			version = version.substring(1, lastPos);
			setVersionFields(version.substring(0, comInd - 1), first == '[', version.substring(comInd), last == ']');
		}
		setFieldsEnabled();
	}

	private void setVersionFields(String minVersion, boolean minInclusive, String maxVersion, boolean maxInclusive) {
		fPluginMinVersionEntry.setValue(minVersion, true);
		fPluginMinVersionBound.select(minInclusive ? 0 : 1);
		fPluginMaxVersionEntry.setValue(maxVersion, true);
		fPluginMaxVersionBound.select(maxInclusive ? 0 : 1);
	}

	// added for bug 172675
	protected void addListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.addModelChangedListener(this);
		}
		super.addListeners();
	}

	protected void removeListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.removeModelChangedListener(this);
		}
		super.removeListeners();
	}
}
