/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.core.text.monitor.MonitorBaseNode;
import com.siteview.mde.internal.core.text.monitor.MonitorDocumentNodeFactory;

import com.siteview.mde.internal.core.monitor.*;

import com.siteview.mde.core.monitor.*;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import com.siteview.mde.core.*;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.bundle.BundlePluginBase;
import com.siteview.mde.internal.core.bundle.BundlePluginModelBase;
import com.siteview.mde.internal.core.util.VersionUtil;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.editor.TableSection;
import com.siteview.mde.internal.ui.editor.actions.SortAction;
import com.siteview.mde.internal.ui.elements.DefaultTableProvider;
import com.siteview.mde.internal.ui.parts.TablePart;
import com.siteview.mde.internal.ui.search.MonitorSearchActionGroup;
import com.siteview.mde.internal.ui.search.dependencies.UnusedDependenciesAction;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;

public class RequiresSection extends TableSection implements IModelChangedListener, IMonitorModelListener, IPropertyChangeListener {

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int UP_INDEX = 2;
	private static final int DOWN_INDEX = 3;
	private static final int PROPERTIES_INDEX = 4;

	private TableViewer fImportViewer;
	private Vector fImports;
	private Action fOpenAction;
	private Action fAddAction;
	private Action fRemoveAction;
	private Action fPropertiesAction;
	private Action fSortAction;

	private int fImportInsertIndex;

	class ImportContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fImports == null)
				createImportObjects();
			return fImports.toArray();
		}
	}

	public RequiresSection(DependenciesPage page, Composite parent, String[] labels) {
		super(page, parent, Section.DESCRIPTION, labels);
		getSection().setText(MDEUIMessages.RequiresSection_title);
		boolean fragment = ((IMonitorModelBase) getPage().getModel()).isFragmentModel();
		if (fragment)
			getSection().setDescription(MDEUIMessages.RequiresSection_fDesc);
		else
			getSection().setDescription(MDEUIMessages.RequiresSection_desc);
		getTablePart().setEditable(false);
		resetImportInsertIndex();
	}

	public RequiresSection(MonitorDependenciesPage page, Composite parent, String[] labels) {
		super(page, parent, Section.DESCRIPTION, labels);
		getSection().setText(MDEUIMessages.RequiresSection_title);
		boolean fragment = ((IMonitorModelBase) getPage().getModel()).isFragmentModel();
		if (fragment)
			getSection().setDescription(MDEUIMessages.RequiresSection_fDesc);
		else
			getSection().setDescription(MDEUIMessages.RequiresSection_desc);
		getTablePart().setEditable(false);
		resetImportInsertIndex();
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fImportViewer = tablePart.getTableViewer();

		fImportViewer.setContentProvider(new ImportContentProvider());
		fImportViewer.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.minimumWidth = 250;
		gd.grabExcessVerticalSpace = true;
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(gd);
		section.setText(MDEUIMessages.RequiresSection_title);
		createSectionToolbar(section, toolkit);
		initialize();
	}

	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});

		// Add sort action to the tool bar
		fSortAction = new SortAction(fImportViewer, MDEUIMessages.RequiresSection_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getMDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		updateUpDownButtons();
		if (isBundle())
			tablePart.setButtonEnabled(PROPERTIES_INDEX, selection.length == 1);
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
	}

	private void updateUpDownButtons() {
		TablePart tablePart = getTablePart();
		if (fSortAction.isChecked()) {
			tablePart.setButtonEnabled(UP_INDEX, false);
			tablePart.setButtonEnabled(DOWN_INDEX, false);
			return;
		}
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1 && selection.length == 1;

		tablePart.setButtonEnabled(UP_INDEX, canMove && isEditable() && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(DOWN_INDEX, canMove && hasSelection && isEditable() && table.getSelectionIndex() < table.getItemCount() - 1);
	}

	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX :
				handleAdd();
				break;
			case REMOVE_INDEX :
				handleRemove();
				break;
			case UP_INDEX :
				handleUp();
				break;
			case DOWN_INDEX :
				handleDown();
				break;
			case PROPERTIES_INDEX :
				handleOpenProperties();
				break;
		}
	}

	private void handleOpenProperties() {
		Object changeObject = ((IStructuredSelection) fImportViewer.getSelection()).getFirstElement();
		IMonitorImport importObject = ((ImportObject) changeObject).getImport();

		DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(), importObject);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.IMPORTED_PLUGIN_PROPERTIES);
		SWTUtil.setDialogSize(dialog, 400, -1);
		dialog.setTitle(importObject.getId());
		if (dialog.open() == Window.OK && isEditable()) {
			try {
				importObject.setOptional(dialog.isOptional());
				importObject.setReexported(dialog.isReexported());
				importObject.setVersion(dialog.getVersion());
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	public void dispose() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		MDECore.getDefault().getModelManager().removePluginModelListener(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {

		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleRemove();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		HashSet existingImportsSet = null;
		// Only import objects that are not already existing imports can be
		// pasted
		for (int i = 0; i < sourceObjects.length; i++) {
			// Only import objects allowed
			if ((sourceObjects[i] instanceof ImportObject) == false) {
				return false;
			}
			// Get the current import objects and store them for searching
			// purposes
			if (existingImportsSet == null) {
				existingImportsSet = PluginSelectionDialog.getExistingImports(getModel(), false);
			}
			// Only import object that do not exist are allowed
			ImportObject importObject = (ImportObject) sourceObjects[i];
			if (existingImportsSet.contains(importObject.getImport().getId())) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the model
		IMonitorModelBase model = getModel();
		IMonitorBase pluginBase = model.getMonitorBase();
		try {
			// Paste all source objects
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];
				if (sourceObject instanceof ImportObject) {
					// Import object
					ImportObject importObject = (ImportObject) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					// TODO: MP: CCP: Remove unnecessary reconnected Plugin attributes
					// This may not be necessary.  The model object is discarded when
					// the import object wrapping the plugin import object is converted 
					// into a require bundle object					
					importObject.reconnect(model);
					// Add the import object to the plugin
					pluginBase.add(importObject.getImport());
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private IMonitorModelBase getModel() {
		return (IMonitorModelBase) getPage().getModel();
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IMonitorImport) {
			ImportObject iobj = new ImportObject((IMonitorImport) object);
			fImportViewer.setSelection(new StructuredSelection(iobj), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fImportViewer.getSelection();
		manager.add(fAddAction);
		if (!selection.isEmpty()) {
			manager.add(fOpenAction);
		}
		manager.add(new Separator());
		getPage().contextMenuAboutToShow(manager);

		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getMDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());

		MonitorSearchActionGroup actionGroup = new MonitorSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (((IModel) getPage().getModel()).getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction((IMonitorModelBase) getPage().getModel(), false));
		}
		if (fPropertiesAction != null && !fImportViewer.getSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fPropertiesAction);
		}
		manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void registerPopupMenu(MenuManager popupMenuManager) {
		IEditorSite site = (IEditorSite) getPage().getSite();
		site.registerContextMenu(site.getId() + ".requires", popupMenuManager, fViewerPart.getViewer(), false); //$NON-NLS-1$
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof ImportObject) {
					IMonitor plugin = ((ImportObject) obj).getPlugin();
					if (plugin != null)
						ManifestEditor.open(plugin, false);
				}
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fImportViewer.getSelection();
		if (!ssel.isEmpty()) {
			IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
			IMonitorBase pluginBase = model.getMonitorBase();
			IMonitorImport[] imports = new IMonitorImport[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext(); i++)
				imports[i] = ((ImportObject) iter.next()).getImport();

			try {
				removeImports(pluginBase, imports);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
			updateButtons();
		}
	}

	private void removeImports(IMonitorBase base, IMonitorImport[] imports) throws CoreException {
		if (base instanceof BundlePluginBase)
			((BundlePluginBase) base).remove(imports);
		else if (base instanceof MonitorBase)
			((MonitorBase) base).remove(imports);
		else if (base instanceof MonitorBaseNode)
			((MonitorBaseNode) base).remove(imports);
	}

	private void handleAdd() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		PluginSelectionDialog dialog = new PluginSelectionDialog(MDEPlugin.getActiveWorkbenchShell(), getAvailablePlugins(model), true);
		dialog.create();
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			IMonitorImport[] imports = new IMonitorImport[models.length];
			try {
				for (int i = 0; i < models.length; i++) {
					IMonitorModel candidate = (IMonitorModel) models[i];
					String pluginId = candidate.getMonitor().getId();
					IMonitorImport importNode = createImport(model.getMonitorFactory(), pluginId);
					String version = VersionUtil.computeInitialPluginVersion(candidate.getMonitor().getVersion());
					importNode.setVersion(version);
					imports[i] = importNode;
				}
				addImports(model.getMonitorBase(), imports);
			} catch (CoreException e) {
			}
		}
	}

	private IMonitorImport createImport(IMonitorModelFactory factory, String id) {
		if (factory instanceof AbstractMonitorModelBase)
			return ((AbstractMonitorModelBase) factory).createImport(id);
		else if (factory instanceof BundlePluginModelBase)
			return ((BundlePluginModelBase) factory).createImport(id);
		else if (factory instanceof MonitorDocumentNodeFactory)
			return ((MonitorDocumentNodeFactory) factory).createImport(id);
		return null;
	}

	private void addImports(IMonitorBase base, IMonitorImport[] imports) throws CoreException {
		if (base instanceof BundlePluginBase)
			((BundlePluginBase) base).add(imports);
		else if (base instanceof MonitorBase)
			((MonitorBase) base).add(imports);
		else if (base instanceof MonitorBaseNode)
			((MonitorBaseNode) base).add(imports);
	}

	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index < 1)
			return;
		swap(index, index - 1);
	}

	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index == table.getItemCount() - 1)
			return;
		swap(index, index + 1);
	}

	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IMonitorImport dep1 = ((ImportObject) table.getItem(index1).getData()).getImport();
		IMonitorImport dep2 = ((ImportObject) table.getItem(index2).getData()).getImport();

		try {
			IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
			IMonitorBase pluginBase = model.getMonitorBase();
			pluginBase.swap(dep1, dep2);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private IMonitorModelBase[] getAvailablePlugins(IMonitorModelBase model) {
		IMonitorModelBase[] plugins = MonitorRegistry.getActiveModels(false);
		HashSet existingImports = PluginSelectionDialog.getExistingImports(model, false);
		ArrayList result = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			if (!existingImports.contains(plugins[i].getMonitorBase().getId())) {
				result.add(plugins[i]);
			}
		}

		if (!existingImports.contains("system.bundle")) //$NON-NLS-1$
			addSystemBundle(result);
		return (IMonitorModelBase[]) result.toArray(new IMonitorModelBase[result.size()]);
	}

	private void addSystemBundle(java.util.List list) {
		try {
			ExternalMonitorModel model = new ExternalMonitorModel();

			// Need Install Location to load model.  Giving it org.eclipse.osgi's install location
			IMonitorModelBase osgi = MonitorRegistry.findModel("system.bundle"); //$NON-NLS-1$
			if (osgi == null)
				return;
			model.setInstallLocation(osgi.getInstallLocation());

			// Load model from a String representing the contents of an equivalent plugin.xml file
			String pluginInfo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><plugin id=\"system.bundle\" name=\"System Bundle\"></plugin>"; //$NON-NLS-1$
			InputStream is = new BufferedInputStream(new ByteArrayInputStream(pluginInfo.getBytes()));
			model.load(is, false);

			list.add(model);

		} catch (CoreException e) {
		}
	}

	public void initialize() {
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		if (model == null)
			return;
		fImportViewer.setInput(model.getMonitorBase());
		updateButtons();
		model.addModelChangedListener(this);
		MDECore.getDefault().getModelManager().addPluginModelListener(this);
		fAddAction.setEnabled(model.isEditable());
		fRemoveAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		fAddAction = new Action(MDEUIMessages.RequiresSection_add) {
			public void run() {
				handleAdd();
			}
		};
		fOpenAction = new Action(MDEUIMessages.RequiresSection_open) {
			public void run() {
				handleOpen(fImportViewer.getSelection());
			}
		};
		fRemoveAction = new Action(MDEUIMessages.RequiresSection_delete) {
			public void run() {
				handleRemove();
			}
		};
		if (isBundle()) {
			fPropertiesAction = new Action(MDEUIMessages.RequiresSection_properties) {
				public void run() {
					handleOpenProperties();
				}
			};
		}
	}

	public void refresh() {
		fImports = null;
		fImportViewer.refresh();
		super.refresh();
	}

	public void modelChanged(final IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}

		// Model change may have come from a non UI thread such as the auto add dependencies operation. See bug 333533 
		UIJob job = new UIJob("Update required bundles") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {

				if (event.getChangedProperty() == IMonitorBase.P_IMPORT_ORDER) {
					refresh();
					updateButtons();
					return Status.OK_STATUS;
				}

				Object[] changedObjects = event.getChangedObjects();
				if (changedObjects[0] instanceof IMonitorImport) {
					int index = 0;
					for (int i = 0; i < changedObjects.length; i++) {
						Object changeObject = changedObjects[i];
						IMonitorImport iimport = (IMonitorImport) changeObject;
						if (event.getChangeType() == IModelChangedEvent.INSERT) {
							ImportObject iobj = new ImportObject(iimport);
							if (fImports == null) {
								// createImportObjects method will find new addition
								createImportObjects();
							} else {
								int insertIndex = getImportInsertIndex();
								if (insertIndex < 0) {
									// Add Button
									fImports.add(iobj);
								} else {
									// DND
									fImports.add(insertIndex, iobj);
								}
							}
						} else {
							ImportObject iobj = findImportObject(iimport);
							if (iobj != null) {
								if (event.getChangeType() == IModelChangedEvent.REMOVE) {
									if (fImports == null)
										// createImportObjects method will not include the removed import
										createImportObjects();
									else
										fImports.remove(iobj);
									Table table = fImportViewer.getTable();
									index = table.getSelectionIndex();
									fImportViewer.remove(iobj);
								} else {
									fImportViewer.update(iobj, null);
								}
							}
						}
					}
					if (event.getChangeType() == IModelChangedEvent.INSERT) {
						if (changedObjects.length > 0) {
							// Refresh the viewer
							fImportViewer.refresh();
							// Get the last import added to the viewer
							IMonitorImport lastImport = (IMonitorImport) changedObjects[changedObjects.length - 1];
							// Find the corresponding bundle object for the plug-in import
							ImportObject lastImportObject = findImportObject(lastImport);
							if (lastImportObject != null) {
								fImportViewer.setSelection(new StructuredSelection(lastImportObject));
							}
							fImportViewer.getTable().setFocus();
						}
					} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
						Table table = fImportViewer.getTable();
						table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
						updateButtons();
					}
				} else {
					fImportViewer.update(((IStructuredSelection) fImportViewer.getSelection()).toArray(), null);
				}

				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	public void modelsChanged(MonitorModelDelta delta) {
		fImports = null;
		final Control control = fImportViewer.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						fImportViewer.refresh();
				}
			});
		}
	}

	private ImportObject findImportObject(IMonitorImport iimport) {
		if (fImports == null)
			return null;
		for (int i = 0; i < fImports.size(); i++) {
			ImportObject iobj = (ImportObject) fImports.get(i);
			if (iobj.getImport().equals(iimport))
				return iobj;
		}
		return null;
	}

	private void createImportObjects() {
		fImports = new Vector();
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		IMonitorImport[] iimports = model.getMonitorBase().getImports();
		for (int i = 0; i < iimports.length; i++) {
			IMonitorImport iimport = iimports[i];
			fImports.add(new ImportObject(iimport));
		}
	}

	public void setFocus() {
		if (fImportViewer != null)
			fImportViewer.getTable().setFocus();
	}

	private boolean isBundle() {
		return getPage().getMDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	protected boolean createCount() {
		return true;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateUpDownButtons();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#isDragAndDropEnabled()
	 */
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		} else if (isTreeViewerSorted()) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceObjects
	 */
	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof ImportObject) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 */
	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof ImportObject) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceImportObject
	 * @param targetImportObject
	 */
	private boolean validateDropMoveModel(ImportObject sourceImportObject, ImportObject targetImportObject) {
		// Objects have to be from the same model
		IMonitorModelBase sourceModel = sourceImportObject.getImport().getMonitorModel();
		IMonitorModelBase targetModel = targetImportObject.getImport().getMonitorModel();
		if (sourceModel.equals(targetModel) == false) {
			return false;
		} else if ((getModel().getMonitorBase() instanceof BundlePluginBase) == false) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multiple selection not supported
		ImportObject sourceImportObject = (ImportObject) sourceObjects[0];
		ImportObject targetImportObject = (ImportObject) targetObject;
		IMonitorImport sourcePluginImport = sourceImportObject.getImport();
		IMonitorImport targetPluginImport = targetImportObject.getImport();
		// Validate model
		if (validateDropMoveModel(sourceImportObject, targetImportObject) == false) {
			return false;
		}
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getMonitorBase();
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous import of the target 
			IMonitorImport previousImport = bundlePluginBase.getPreviousImport(targetPluginImport);
			// Ensure the previous element is not the source
			if (sourcePluginImport.equals(previousImport)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next import of the target 
			IMonitorImport nextImport = bundlePluginBase.getNextImport(targetPluginImport);
			// Ensure the next import is not the source
			if (sourcePluginImport.equals(nextImport)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multiple selection not supported
		ImportObject sourceImportObject = (ImportObject) sourceObjects[0];
		ImportObject targetImportObject = (ImportObject) targetObject;
		IMonitorImport sourcePluginImport = sourceImportObject.getImport();
		IMonitorImport targetPluginImport = targetImportObject.getImport();
		// Validate move
		if ((targetLocation == ViewerDropAdapter.LOCATION_BEFORE) || (targetLocation == ViewerDropAdapter.LOCATION_AFTER)) {
			// Do move
			doDropMove(sourceImportObject, sourcePluginImport, targetPluginImport, targetLocation);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
		}
	}

	/**
	 * @param sourceImportObject
	 * @param sourcePluginImport
	 * @param targetPluginImport
	 * @param targetLocation
	 */
	private void doDropMove(ImportObject sourceImportObject, IMonitorImport sourcePluginImport, IMonitorImport targetPluginImport, int targetLocation) {
		// Remove the original source object
		// Normally we remove the original source object after inserting the
		// serialized source object; however, the imports are removed via ID
		// and having both objects with the same ID co-existing will confound
		// the remove operation
		doDragRemove();
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getMonitorBase();
		// Get the index of the target
		int index = bundlePluginBase.getIndexOf(targetPluginImport);
		// Ensure the target index was found
		if (index == -1) {
			return;
		}
		// Determine the location index
		int targetIndex = index;
		if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			targetIndex++;
		}
		// Adjust all the source object transient field values to
		// acceptable values
		sourceImportObject.reconnect(getModel());
		// Store index so that the import can be inserted properly into
		// the table viewer
		setImportInsertIndex(targetIndex);
		// Add source as sibling of target	
		try {
			bundlePluginBase.add(sourcePluginImport, targetIndex);
		} catch (CoreException e) {
			// CoreException if model is not editable, which should never be the case
		}
		// Reset the index
		resetImportInsertIndex();
	}

	/**
	 * 
	 */
	private void resetImportInsertIndex() {
		fImportInsertIndex = -1;
	}

	/**
	 * @param index
	 */
	private void setImportInsertIndex(int index) {
		fImportInsertIndex = index;
	}

	private int getImportInsertIndex() {
		return fImportInsertIndex;
	}

	/**
	 * 
	 */
	private void doDragRemove() {
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getMonitorBase();
		// Retrieve the original non-serialized source objects dragged initially
		Object[] sourceObjects = getDragSourceObjects();
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		IMonitorImport sourcePluginImport = ((ImportObject) sourceObjects[0]).getImport();
		try {
			bundlePluginBase.remove(sourcePluginImport);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private boolean isTreeViewerSorted() {
		if (fSortAction == null) {
			return false;
		}
		return fSortAction.isChecked();
	}

}
