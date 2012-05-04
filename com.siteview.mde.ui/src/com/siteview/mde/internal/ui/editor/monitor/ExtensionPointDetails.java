/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.*;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.schema.Schema;
import com.siteview.mde.internal.core.schema.SchemaDescriptor;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.actions.OpenSchemaAction;
import com.siteview.mde.internal.ui.parts.FormEntry;
import com.siteview.mde.internal.ui.search.FindReferencesAction;
import com.siteview.mde.internal.ui.search.ShowDescriptionAction;
import com.siteview.mde.internal.ui.util.SWTUtil;
import com.siteview.mde.internal.ui.wizards.extension.NewSchemaFileWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class ExtensionPointDetails extends MDEDetails {
	private IPluginExtensionPoint fInput;
	private FormEntry fIdEntry;
	private FormEntry fNameEntry;
	private FormEntry fSchemaEntry;
	private FormText fRichText;
	private String fRichTextData;

	private static final String SCHEMA_RTEXT_DATA = MDEUIMessages.ExtensionPointDetails_schemaLinks;
	private static final String NO_SCHEMA_RTEXT_DATA = MDEUIMessages.ExtensionPointDetails_noSchemaLinks;

	public ExtensionPointDetails() {
	}

	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}

	public void fireSaveNeeded() {
		markDirty();
		getPage().getMDEEditor().fireSaveNeeded(getContextId(), false);
	}

	public MDEFormPage getPage() {
		return (MDEFormPage) getManagedForm().getContainer();
	}

	public boolean isEditable() {
		return getPage().getMDEEditor().getAggregateModel().isEditable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		FormToolkit toolkit = getManagedForm().getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(MDEUIMessages.ExtensionPointDetails_title);
		section.setDescription(MDEUIMessages.ExtensionPointDetails_desc);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fIdEntry = new FormEntry(client, toolkit, MDEUIMessages.ExtensionPointDetails_id, null, false);
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setId(fIdEntry.getValue());
					} catch (CoreException e) {
						MDEPlugin.logException(e);
					}
				}
			}
		});
		fNameEntry = new FormEntry(client, toolkit, MDEUIMessages.ExtensionPointDetails_name, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null)
					try {
						fInput.setName(fNameEntry.getValue());
					} catch (CoreException e) {
						MDEPlugin.logException(e);
					}
			}
		});
		boolean editable = getPage().getModel().isEditable();
		fSchemaEntry = new FormEntry(client, toolkit, MDEUIMessages.ExtensionPointDetails_schema, MDEUIMessages.ExtensionPointDetails_browse, editable); // 
		fSchemaEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setSchema(fSchemaEntry.getValue());
					} catch (CoreException e) {
						MDEPlugin.logException(e);
					}
					updateRichText();
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				IProject project = getPage().getMDEEditor().getCommonProject();
				if (fSchemaEntry.getValue() == null || fSchemaEntry.getValue().length() == 0) {
					generateSchema();
					return;
				}
				IFile file = project.getFile(fSchemaEntry.getValue());
				if (file.exists())
					openSchemaFile(file);
				else
					generateSchema();
			}

			public void browseButtonSelected(FormEntry entry) {
				final IProject project = getPage().getMDEEditor().getCommonProject();
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(MDEPlugin.getActiveWorkbenchShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
				dialog.setTitle(MDEUIMessages.ManifestEditor_ExtensionPointDetails_schemaLocation_title);
				dialog.setMessage(MDEUIMessages.ManifestEditor_ExtensionPointDetails_schemaLocation_desc);
				dialog.setDoubleClickSelects(false);
				dialog.setAllowMultiple(false);
				dialog.addFilter(new ViewerFilter() {
					public boolean select(Viewer viewer, Object parent, Object element) {
						if (element instanceof IFile) {
							String ext = ((IFile) element).getFullPath().getFileExtension();
							return "exsd".equals(ext) || "mxsd".equals(ext); //$NON-NLS-1$ //$NON-NLS-2$
						} else if (element instanceof IContainer) { // i.e. IProject, IFolder
							try {
								IResource[] resources = ((IContainer) element).members();
								for (int i = 0; i < resources.length; i++) {
									if (select(viewer, parent, resources[i]))
										return true;
								}
							} catch (CoreException e) {
								MDEPlugin.logException(e);
							}
						}
						return false;
					}
				});
				dialog.setValidator(new ISelectionStatusValidator() {
					public IStatus validate(Object[] selection) {
						IMonitorModelBase model = (IMonitorModelBase) getPage().getMDEEditor().getAggregateModel();
						String pluginName = model.getMonitorBase().getId();

						if (selection == null || selection.length != 1 || !(selection[0] instanceof IFile))
							return new Status(IStatus.ERROR, pluginName, IStatus.ERROR, MDEUIMessages.ManifestEditor_ExtensionPointDetails_validate_errorStatus, null);
						IFile file = (IFile) selection[0];
						String ext = file.getFullPath().getFileExtension();
						if ("exsd".equals(ext) || "mxsd".equals(ext)) //$NON-NLS-1$ //$NON-NLS-2$
							return new Status(IStatus.OK, pluginName, IStatus.OK, "", null); //$NON-NLS-1$
						return new Status(IStatus.ERROR, pluginName, IStatus.ERROR, MDEUIMessages.ManifestEditor_ExtensionPointDetails_validate_errorStatus, null);
					}
				});
				dialog.setDoubleClickSelects(true);
				dialog.setStatusLineAboveButtons(true);
				dialog.setInput(project);
				dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
				String filePath = fSchemaEntry.getValue();
				if (filePath != null && filePath.length() != 0 && project.exists(new Path(filePath)))
					dialog.setInitialSelection(project.getFile(new Path(filePath)));
				else
					dialog.setInitialSelection(null);
				dialog.create();
				PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.BROWSE_EXTENSION_POINTS_SCHEMAS);
				if (dialog.open() == Window.OK) {
					Object[] elements = dialog.getResult();
					if (elements.length > 0) {
						IResource elem = (IResource) elements[0];
						fSchemaEntry.setValue(elem.getProjectRelativePath().toString());
					}
				}
			}
		});
		createSpacer(toolkit, client, 2);

		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		fRichText = toolkit.createFormText(container, true);
		fRichText.setImage("open", MDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				MDEPluginImages.DESC_SCHEMA_OBJ));
		fRichText.setImage("desc", MDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				MDEPluginImages.DESC_DOC_SECTION_OBJ));
		fRichText.setImage("search", MDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				MDEPluginImages.DESC_PSEARCH_OBJ));
		fRichText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				IBaseModel model = getPage().getMDEEditor().getAggregateModel();
				String pointID = null;
				IMonitorBase base = ((IMonitorModelBase) model).getMonitorBase();
				String pluginID = base.getId();
				String schemaVersion = base.getSchemaVersion();
				if (schemaVersion != null && Double.parseDouble(schemaVersion) >= 3.2) {
					if (fInput.getId().indexOf('.') != -1)
						pointID = fInput.getId();
				}
				if (pointID == null)
					pointID = pluginID + "." + fInput.getId(); //$NON-NLS-1$
				IPluginExtensionPoint extPoint = MDECore.getDefault().getExtensionsRegistry().findExtensionPoint(pointID);
				if (e.getHref().equals("search")) { //$NON-NLS-1$
					new FindReferencesAction(fInput, pluginID).run();
				} else if (e.getHref().equals("open")) { //$NON-NLS-1$					
					if (extPoint == null) {
						IProject project = getPage().getMDEEditor().getCommonProject();
						IFile file = project.getFile(fSchemaEntry.getValue());
						if (file.exists())
							openSchemaFile(file);
						else
							generateSchema();
						return;
					}
					OpenSchemaAction action = new OpenSchemaAction();
					action.setInput(pointID);
					action.setEnabled(true);
					action.run();
				} else {
					if (extPoint == null) {
						IProject project = getPage().getMDEEditor().getCommonProject();
						IFile file = project.getFile(fSchemaEntry.getValue());
						URL url;
						try {
							url = file.getLocationURI().toURL();
						} catch (MalformedURLException e1) {
							return;
						}
						SchemaDescriptor schemaDesc = new SchemaDescriptor(pointID, url);
						Schema schema = new Schema(schemaDesc, url, false);
						schema.setPluginId(pluginID);
						schema.setPointId(fInput.getId());
						schema.setName(fNameEntry.getValue());
						new ShowDescriptionAction(schema).run();
						return;
					}
					new ShowDescriptionAction(pointID).run();
				}
			}
		});

		fIdEntry.setEditable(isEditable());
		fNameEntry.setEditable(isEditable());
		fSchemaEntry.setEditable(isEditable());
		toolkit.paintBordersFor(client);
		section.setClient(client);
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		markDetailsPart(section);
	}

	public void dispose() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(fInput))
				refresh();
		}
	}

	private void update() {
		fIdEntry.setValue(fInput != null && fInput.getId() != null ? fInput.getId() : "", //$NON-NLS-1$
				true);
		fNameEntry.setValue(fInput != null && fInput.getName() != null ? fInput.getName() : "", true); //$NON-NLS-1$
		fSchemaEntry.setValue(fInput != null && fInput.getSchema() != null ? fInput.getSchema() : "", true); //$NON-NLS-1$
		updateRichText();
	}

	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fSchemaEntry.cancelEdit();
		updateRichText();
		super.cancelEdit();
	}

	private void updateRichText() {
		boolean hasSchema = fSchemaEntry.getValue().length() > 0;
		if (hasSchema && fRichTextData == SCHEMA_RTEXT_DATA)
			return;
		if (!hasSchema && fRichTextData == NO_SCHEMA_RTEXT_DATA)
			return;
		fRichTextData = hasSchema ? SCHEMA_RTEXT_DATA : NO_SCHEMA_RTEXT_DATA;
		fRichText.setText(fRichTextData, true, false);
		getManagedForm().getForm().reflow(true);
	}

	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = MDEPlugin.getActiveWorkbenchWindow();

		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = IMDEUIConstants.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(new FileEditorInput(file), editorId);
				} catch (PartInitException e) {
					MDEPlugin.logException(e);
				}
			}
		});
	}

	private void generateSchema() {
		final IProject project = getPage().getMDEEditor().getCommonProject();
		BusyIndicator.showWhile(getPage().getPartControl().getDisplay(), new Runnable() {
			public void run() {
				NewSchemaFileWizard wizard = new NewSchemaFileWizard(project, fInput, true);
				WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				if (dialog.open() == Window.OK)
					update();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			fInput = (IPluginExtensionPoint) ssel.getFirstElement();
		} else
			fInput = null;
		update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fNameEntry.commit();
		fSchemaEntry.commit();
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		fIdEntry.getText().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}

}
