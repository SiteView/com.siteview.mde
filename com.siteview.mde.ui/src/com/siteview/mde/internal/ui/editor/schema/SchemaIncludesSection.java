/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.schema;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.ischema.ISchema;
import com.siteview.mde.internal.core.ischema.ISchemaInclude;
import com.siteview.mde.internal.core.natures.MDE;
import com.siteview.mde.internal.core.schema.Schema;
import com.siteview.mde.internal.core.schema.SchemaInclude;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.TableSection;
import com.siteview.mde.internal.ui.parts.TablePart;
import com.siteview.mde.internal.ui.util.FileExtensionFilter;
import com.siteview.mde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SchemaIncludesSection extends TableSection {

	private TableViewer fViewer;

	class PDEProjectFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IProject) {
				try {
					return ((IProject) element).hasNature(MDE.PLUGIN_NATURE);
				} catch (CoreException e) {
				}
			} else if (element instanceof IFile) {
				return isUnlistedInclude((IFile) element);
			}
			return true;
		}
	}

	public SchemaIncludesSection(SchemaOverviewPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {MDEUIMessages.SchemaIncludesSection_addButton, MDEUIMessages.SchemaIncludesSection_removeButton});
		getSection().setText(MDEUIMessages.SchemaIncludesSection_title);
		getSection().setDescription(MDEUIMessages.SchemaIncludesSection_description);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fViewer = tablePart.getTableViewer();
		fViewer.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		MDEPlugin.getDefault().getLabelProvider().connect(this);
		fViewer.setContentProvider(ArrayContentProvider.getInstance());

		getSchema().addModelChangedListener(this);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		initialize();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNewInclude();
		else
			handleRemoveInclude();
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getManagedForm().fireSelectionChanged(this, selection);
		getPage().getMDEEditor().setSelection(selection);
		if (!getSchema().isEditable())
			return;
		Object object = ((IStructuredSelection) fViewer.getSelection()).getFirstElement();
		getTablePart().setButtonEnabled(1, object instanceof ISchemaInclude);
	}

	public void dispose() {
		ISchema schema = getSchema();
		if (schema != null)
			schema.removeModelChangedListener(this);
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent e) {
		int changeType = e.getChangeType();
		if (changeType == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ISchemaInclude) {
				if (changeType == IModelChangedEvent.INSERT) {
					fViewer.add(objects[i]);
				} else if (changeType == IModelChangedEvent.REMOVE) {
					fViewer.remove(objects[i]);
				}
			}
		}
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemoveInclude();
			return true;
		}
		return false;
	}

	private ISchema getSchema() {
		return (ISchema) getPage().getModel();
	}

	protected void handleRemoveInclude() {
		Object[] selected = new Object[0];
		ISelection selection = fViewer.getSelection();
		if (selection.isEmpty())
			return;
		if (selection instanceof IStructuredSelection) {
			selected = ((IStructuredSelection) selection).toArray();
			Schema schema = (Schema) getSchema();
			for (int i = 0; i < selected.length; i++) {
				schema.removeInclude((ISchemaInclude) selected[i]);
			}
		}
	}

	protected void handleNewInclude() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(MDEUIMessages.ProductExportWizardPage_fileSelection);
		dialog.setMessage(MDEUIMessages.SchemaIncludesSection_dialogMessage);
		dialog.addFilter(new FileExtensionFilter("exsd")); //$NON-NLS-1$
		dialog.addFilter(new PDEProjectFilter());
		dialog.setInput(MDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			Object result = dialog.getFirstResult();
			if (!(result instanceof IFile))
				return;
			IFile newInclude = (IFile) result;

			String location = getIncludeLocation(newInclude);
			ISchemaInclude include = new SchemaInclude(getSchema(), location, false);
			ISchema schema = getSchema();
			if (schema instanceof Schema)
				((Schema) schema).addInclude(include);
		}
	}

	private void initialize() {
		refresh();
	}

	private String getIncludeLocation(IFile file) {
		IEditorInput input = getPage().getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		IPath schemaPath = ((IFileEditorInput) input).getFile().getFullPath();
		IPath currPath = file.getFullPath();
		int matchinSegments = schemaPath.matchingFirstSegments(currPath);
		if (matchinSegments > 0) {
			schemaPath = schemaPath.removeFirstSegments(matchinSegments);
			currPath = currPath.removeFirstSegments(matchinSegments);
			if (schemaPath.segmentCount() == 1)
				return currPath.toString();
			StringBuffer sb = new StringBuffer();
			while (schemaPath.segmentCount() > 1) {
				sb.append("../"); //$NON-NLS-1$
				schemaPath = schemaPath.removeFirstSegments(1);
			}
			String location = sb.toString() + currPath.toString();
			return location.trim().length() > 0 ? location : null;
		}
		IMonitorModelBase model = MonitorRegistry.findModel(file.getProject());
		String id = model.getMonitorBase().getId();
		if (id != null)
			return "schema://" + id + "/" + file.getProjectRelativePath().toString(); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	private boolean isUnlistedInclude(IFile file) {
		String location = getIncludeLocation(file);
		if (location == null)
			return false;
		boolean unlisted = true;
		ISchemaInclude[] includes = getSchema().getIncludes();
		for (int i = 0; i < includes.length; i++) {
			if (includes[i].getLocation().equals(location)) {
				unlisted = false;
				break;
			}
		}
		return unlisted;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof ISchemaInclude) {
			IEditorInput edinput = getPage().getEditorInput();
			if (!(edinput instanceof IFileEditorInput))
				return;
			String path = ((ISchemaInclude) object).getLocation();
			IPath includePath = new Path(((ISchemaInclude) object).getLocation());
			boolean result = false;
			if (path.startsWith("schema:")) { //$NON-NLS-1$
				result = SchemaEditor.openSchema(includePath);
			} else {
				IFile currSchemaFile = ((IFileEditorInput) edinput).getFile();
				IProject project = currSchemaFile.getProject();
				IPath currSchemaPath = currSchemaFile.getProjectRelativePath();
				IFile file = project.getFile(currSchemaPath.removeLastSegments(1).append(includePath));
				result = SchemaEditor.openSchema(file);
			}
			if (!result)
				MessageDialog.openWarning(getPage().getSite().getShell(), MDEUIMessages.SchemaIncludesSection_missingWarningTitle, NLS.bind(MDEUIMessages.SchemaIncludesSection_missingWarningMessage, includePath.toString()));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		getTablePart().setButtonEnabled(0, getSchema().isEditable());
		getTablePart().setButtonEnabled(1, false);
		fViewer.setInput(getSchema().getIncludes());
		super.refresh();
	}
}
