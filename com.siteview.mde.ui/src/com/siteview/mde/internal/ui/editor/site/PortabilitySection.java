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
package com.siteview.mde.internal.ui.editor.site;

import java.util.Locale;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.ifeature.IEnvironment;
import com.siteview.mde.internal.core.isite.ISiteFeature;
import com.siteview.mde.internal.core.isite.ISiteModel;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.feature.Choice;
import com.siteview.mde.internal.ui.editor.feature.PortabilityChoicesDialog;
import com.siteview.mde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PortabilitySection extends MDESection implements IFormPart, IPartSelectionListener {
	public static Choice[] getArchChoices() {
		return getKnownChoices(Platform.knownOSArchValues());
	}

	private static Choice[] getKnownChoices(String[] values) {
		Choice[] choices = new Choice[values.length];
		for (int i = 0; i < choices.length; i++) {
			choices[i] = new Choice(values[i], values[i]);
		}
		return choices;
	}

	public static Choice[] getNLChoices() {
		Locale[] locales = Locale.getAvailableLocales();
		Choice[] choices = new Choice[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			choices[i] = new Choice(locale.toString(), locale.toString() + " - " + locale.getDisplayName()); //$NON-NLS-1$
		}
		return choices;
	}

	public static Choice[] getOSChoices() {
		return getKnownChoices(Platform.knownOSValues());
	}

	public static Choice[] getWSChoices() {
		return getKnownChoices(Platform.knownWSValues());
	}

	private FormEntry fArchText;

	private ISiteFeature fCurrentSiteFeature;

	private FormEntry fNlText;

	private FormEntry fOsText;

	private FormEntry fWsText;

	public PortabilitySection(MDEFormPage page, Composite parent) {
		this(page, parent, MDEUIMessages.SiteEditor_PortabilitySection_title, MDEUIMessages.SiteEditor_PortabilitySection_desc, SWT.NULL);
	}

	public PortabilitySection(MDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyValue(String property, String value) throws CoreException {
		if (fCurrentSiteFeature == null)
			return;
		if (property.equals(IEnvironment.P_NL))
			fCurrentSiteFeature.setNL(value);
		else if (property.equals(IEnvironment.P_OS))
			fCurrentSiteFeature.setOS(value);
		else if (property.equals(IEnvironment.P_WS))
			fCurrentSiteFeature.setWS(value);
		else if (property.equals(IEnvironment.P_ARCH))
			fCurrentSiteFeature.setArch(value);
	}

	public void cancelEdit() {
		fOsText.cancelEdit();
		fWsText.cancelEdit();
		fNlText.cancelEdit();
		fArchText.cancelEdit();
		super.cancelEdit();
	}

	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

	private void clearField(String property) {
		if (property.equals(IEnvironment.P_OS))
			fOsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_WS))
			fWsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_ARCH))
			fArchText.setValue(null, true);
	}

	private void clearFields() {
		fOsText.setValue(null, true);
		fWsText.setValue(null, true);
		if (fNlText != null)
			fNlText.setValue(null, true);
		fArchText.setValue(null, true);
	}

	public void commit(boolean onSave) {
		fOsText.commit();
		fWsText.commit();
		if (fNlText != null)
			fNlText.commit();
		fArchText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		String editLabel = MDEUIMessages.SiteEditor_PortabilitySection_edit;

		fOsText = new FormEntry(container, toolkit, MDEUIMessages.SiteEditor_PortabilitySection_os, editLabel, false);
		fOsText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fOsText.getText().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = getOSChoices();
						openPortabilityChoiceDialog(IEnvironment.P_OS, fOsText, choices);
					}
				});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_OS, text.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fOsText);
		fOsText.setEditable(isEditable());

		fWsText = new FormEntry(container, toolkit, MDEUIMessages.SiteEditor_PortabilitySection_ws, editLabel, false);
		fWsText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fWsText.getText().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = getWSChoices();
						openPortabilityChoiceDialog(IEnvironment.P_WS, fWsText, choices);
					}
				});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_WS, text.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fWsText);
		fWsText.setEditable(isEditable());

		fNlText = new FormEntry(container, toolkit, MDEUIMessages.SiteEditor_PortabilitySection_nl, editLabel, false);

		fNlText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fNlText.getText().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = getNLChoices();
						openPortabilityChoiceDialog(IEnvironment.P_NL, fNlText, choices);
					}
				});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_NL, text.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fNlText);
		fNlText.setEditable(isEditable());

		fArchText = new FormEntry(container, toolkit, MDEUIMessages.SiteEditor_PortabilitySection_arch, editLabel, false);
		fArchText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fArchText.getText().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = getArchChoices();
						openPortabilityChoiceDialog(IEnvironment.P_ARCH, fArchText, choices);
					}
				});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_ARCH, text.getValue());
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}

		});
		limitTextWidth(fArchText);
		fArchText.setEditable(isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
		super.initialize(form);
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	private void openPortabilityChoiceDialog(String property, FormEntry text, Choice[] choices) {
		String value = text.getValue();

		PortabilityChoicesDialog dialog = new PortabilityChoicesDialog(MDEPlugin.getActiveWorkbenchShell(), choices, value);
		dialog.create();
		dialog.getShell().setText(MDEUIMessages.SiteEditor_PortabilityChoicesDialog_title);

		int result = dialog.open();
		if (result == Window.OK) {
			value = dialog.getValue();
			text.setValue(value);
			try {
				applyValue(property, value);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	public void refresh() {
		if (fCurrentSiteFeature == null) {
			clearFields();
			super.refresh();
			return;
		}
		setValue(IEnvironment.P_OS);
		setValue(IEnvironment.P_WS);
		setValue(IEnvironment.P_ARCH);
		setValue(IEnvironment.P_NL);
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof SiteFeatureAdapter) {
				fCurrentSiteFeature = ((SiteFeatureAdapter) o).feature;
			} else {
				fCurrentSiteFeature = null;
			}
		} else
			fCurrentSiteFeature = null;
		refresh();
	}

	public void setFocus() {
		if (fOsText != null)
			fOsText.getText().setFocus();
	}

	private void setValue(String property) {
		if (fCurrentSiteFeature == null) {
			clearField(property);
		} else {
			if (property.equals(IEnvironment.P_NL))
				fNlText.setValue(fCurrentSiteFeature.getNL(), true);
			else if (property.equals(IEnvironment.P_OS))
				fOsText.setValue(fCurrentSiteFeature.getOS(), true);
			else if (property.equals(IEnvironment.P_WS))
				fWsText.setValue(fCurrentSiteFeature.getWS(), true);
			else if (property.equals(IEnvironment.P_ARCH))
				fArchText.setValue(fCurrentSiteFeature.getArch(), true);
		}
	}
}
