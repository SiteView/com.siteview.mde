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

import com.siteview.mde.internal.core.monitor.MonitorLibrary;

import com.siteview.mde.core.monitor.*;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import com.siteview.mde.core.*;
import com.siteview.mde.core.build.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import com.siteview.mde.internal.core.ClasspathUtilCore;
import com.siteview.mde.internal.core.bundle.BundlePluginBase;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.build.*;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import com.siteview.mde.internal.ui.parts.EditableTablePart;
import com.siteview.mde.internal.ui.parts.TablePart;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class LibrarySection extends TableSection implements IModelChangedListener, IBuildPropertiesConstants {

	private static final int NEW_INDEX = 0;
	private static final int ADD_INDEX = 1;
	private static final int REMOVE_INDEX = 2;
	private static final int UP_INDEX = 3;
	private static final int DOWN_INDEX = 4;

	private Action fRenameAction;
	private Action fRemoveAction;
	private Action fNewAction;

	private TableViewer fLibraryTable;

	class LibraryFilter extends JARFileFilter {
		public LibraryFilter(HashSet set) {
			super(set);
		}

		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IFolder)
				return isPathValid(((IFolder) element).getProjectRelativePath());
			if (element instanceof IFile)
				return isFileValid(((IFile) element).getProjectRelativePath());
			return false;
		}
	}

	class LibrarySelectionValidator extends JarSelectionValidator {

		public LibrarySelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
			super(acceptedTypes, allowMultipleSelection);
		}

		public boolean isValid(Object element) {
			return (element instanceof IFolder) ? true : super.isValid(element);
		}
	}

	class TableContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModel().getMonitorBase().getLibraries();
		}
	}

	public LibrarySection(MDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {MDEUIMessages.NewManifestEditor_LibrarySection_new, MDEUIMessages.NewManifestEditor_LibrarySection_add, MDEUIMessages.NewManifestEditor_LibrarySection_remove, MDEUIMessages.ManifestEditor_LibrarySection_up, MDEUIMessages.ManifestEditor_LibrarySection_down});
	}

	private String getSectionDescription() {
		IMonitorModelBase model = getModel();
		if (isBundle()) {
			return (model.isFragmentModel()) ? MDEUIMessages.ClasspathSection_fragment : MDEUIMessages.ClasspathSection_plugin;
		}
		return (model.isFragmentModel()) ? MDEUIMessages.ManifestEditor_LibrarySection_fdesc : MDEUIMessages.ManifestEditor_LibrarySection_desc;
	}

	protected boolean isBundle() {
		return getBundleContext() != null;
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getMDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		section.setText(MDEUIMessages.ManifestEditor_LibrarySection_title);
		section.setDescription(getSectionDescription());

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fLibraryTable = tablePart.getTableViewer();
		fLibraryTable.setContentProvider(new TableContentProvider());
		fLibraryTable.setLabelProvider(MDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);

		makeActions();
		updateButtons();
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);

		IMonitorModelBase model = getModel();
		fLibraryTable.setInput(model.getMonitorBase());
		model.addModelChangedListener(this);
	}

	private void updateButtons() {
		Table table = fLibraryTable.getTable();
		boolean hasSelection = table.getSelection().length > 0;
		boolean singleSelection = table.getSelection().length == 1;
		int count = table.getItemCount();
		int index = table.getSelectionIndex();
		boolean canMoveUp = singleSelection && index > 0;
		boolean canMoveDown = singleSelection && index < count - 1;

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		tablePart.setButtonEnabled(NEW_INDEX, isEditable());
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
		tablePart.setButtonEnabled(UP_INDEX, isEditable() && canMoveUp);
		tablePart.setButtonEnabled(DOWN_INDEX, isEditable() && canMoveDown);
	}

	private void makeActions() {
		fNewAction = new Action(MDEUIMessages.ManifestEditor_LibrarySection_newLibrary) {
			public void run() {
				handleNew();
			}
		};
		fNewAction.setEnabled(isEditable());

		fRenameAction = new Action(MDEUIMessages.EditableTablePart_renameAction) {
			public void run() {
				getRenameAction().run();
			}
		};
		fRenameAction.setEnabled(isEditable());

		fRemoveAction = new Action(MDEUIMessages.NewManifestEditor_LibrarySection_remove) {
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getMDEEditor().setSelection(selection);
		if (getPage().getModel().isEditable())
			updateButtons();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case NEW_INDEX :
				handleNew();
				break;
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
		}
	}

	public void dispose() {
		IMonitorModelBase model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
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

	public boolean setFormInput(Object object) {
		if (object instanceof IMonitorLibrary) {
			fLibraryTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fNewAction);
		if (!fLibraryTable.getSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRenameAction);
			manager.add(fRemoveAction);
		}
		// Copy, cut, and paste operations not supported for plug-ins that do 
		// not have a MANIFEST.MF (not a Bundle)		
		getPage().getMDEEditor().getContributor().contextMenuAboutToShow(manager, isBundle());
	}

	private void handleRemove() {
		Object[] selection = ((IStructuredSelection) fLibraryTable.getSelection()).toArray();
		int index = fLibraryTable.getTable().getSelectionIndex();
		int[] indices = fLibraryTable.getTable().getSelectionIndices();
		for (int i = 0; i < indices.length; i++)
			if (indices[i] < index)
				index = indices[i];

		String[] remove = new String[selection.length];
		for (int i = 0; i < selection.length; i++) {
			if (selection[i] != null && selection[i] instanceof IMonitorLibrary) {
				IMonitorLibrary ep = (IMonitorLibrary) selection[i];
				IMonitorBase plugin = ep.getMonitorBase();
				try {
					plugin.remove(ep);
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
				remove[i] = ep.getName();
			}
		}
		updateBuildProperties(remove, new String[remove.length], true);
		updateJavaClasspathLibs(remove, new String[remove.length]);

		int itemCount = fLibraryTable.getTable().getItemCount();
		if (itemCount > 0) {
			if (index >= itemCount)
				index = itemCount - 1;
			fLibraryTable.getTable().setSelection(index);
			fLibraryTable.getTable().setFocus();
		}
		updateButtons();
	}

	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index != table.getItemCount() - 1)
			swap(index, index + 1);
	}

	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index >= 1)
			swap(index, index - 1);
	}

	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IMonitorLibrary l1 = (IMonitorLibrary) table.getItem(index1).getData();
		IMonitorLibrary l2 = (IMonitorLibrary) table.getItem(index2).getData();

		try {
			IMonitorModelBase model = getModel();
			IMonitorBase pluginBase = model.getMonitorBase();
			pluginBase.swap(l1, l2);
			refresh();
			table.setSelection(index2);
			table.setFocus();
			updateButtons();
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void handleNew() {
		IMonitorModelBase model = getModel();
		NewRuntimeLibraryDialog dialog = new NewRuntimeLibraryDialog(getPage().getSite().getShell(), model.getMonitorBase().getLibraries());
		dialog.create();
		dialog.getShell().setText(MDEUIMessages.ManifestEditor_LibrarySection_newLibraryEntry);
		SWTUtil.setDialogSize(dialog, 250, 175);

		if (dialog.open() == Window.OK) {
			String libName = dialog.getLibraryName();
			if (libName == null || libName.length() == 0)
				return;
			try {
				IMonitorLibrary library = model.getMonitorFactory().createLibrary();
				library.setName(libName);
				library.setExported(true);
				model.getMonitorBase().add(library);
				checkSourceRootEntry();
				updateBuildProperties(new String[] {null}, new String[] {library.getName()}, true);
				fLibraryTable.setSelection(new StructuredSelection(library));
				fLibraryTable.getTable().setFocus();
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	private void checkSourceRootEntry() {
		IMonitorModelBase pluginModel = getModel();
		IMonitorLibrary[] libraries = pluginModel.getMonitorBase().getLibraries();
		for (int i = 0; i < libraries.length; i++)
			if (libraries[i].getName().equals(".")) //$NON-NLS-1$
				return;
		IBuildModel model = getBuildModel();
		if (model == null)
			return;

		IBuildEntry[] entires = model.getBuild().getBuildEntries();
		for (int i = 0; i < entires.length; i++) {
			if (entires[i].getName().equals(PROPERTY_SOURCE_PREFIX + '.')) {
				IMonitorLibrary library = pluginModel.getMonitorFactory().createLibrary();
				try {
					library.setName("."); //$NON-NLS-1$
					pluginModel.getMonitorBase().add(library);
				} catch (CoreException e) {
				}
			}
		}
	}

	private IBuildModel getBuildModel() {
		IFormPage page = getPage().getEditor().findPage(BuildInputContext.CONTEXT_ID);
		IBaseModel model = null;
		if (page instanceof BuildSourcePage)
			model = ((BuildSourcePage) page).getInputContext().getModel();

		if (model != null && model instanceof IBuildModel)
			return (IBuildModel) model;
		return null;
	}

	private void configureSourceBuildEntry(IBuildModel bmodel, String oldPath, String newPath) throws CoreException {
		IBuild build = bmodel.getBuild();
		IBuildEntry entry = build.getEntry(PROPERTY_SOURCE_PREFIX + (oldPath != null ? oldPath : newPath));
		try {
			if (newPath != null) {
				if (entry == null) {
					IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
					IJavaProject jproject = JavaCore.create(project);
					ArrayList tokens = new ArrayList();
					IClasspathEntry[] entries = jproject.getRawClasspath();
					for (int i = 0; i < entries.length; i++)
						if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
							tokens.add(entries[i].getPath().removeFirstSegments(1).addTrailingSeparator().toString());
					if (tokens.size() == 0)
						return;

					entry = bmodel.getFactory().createEntry(PROPERTY_SOURCE_PREFIX + newPath);
					for (int i = 0; i < tokens.size(); i++)
						entry.addToken((String) tokens.get(i));
					build.add(entry);
				} else
					entry.setName(PROPERTY_SOURCE_PREFIX + newPath);
			} else if (entry != null && newPath == null)
				build.remove(entry);
		} catch (JavaModelException e) {
		}
	}

	private void handleAdd() {
		final boolean[] updateClasspath = new boolean[] {true};
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider()) {
			protected Control createDialogArea(Composite parent) {
				Composite comp = (Composite) super.createDialogArea(parent);
				final Button button = new Button(comp, SWT.CHECK);
				button.setText(MDEUIMessages.LibrarySection_addDialogButton);
				button.setSelection(updateClasspath[0]);
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						updateClasspath[0] = button.getSelection();
					}
				});
				applyDialogFont(button);
				return comp;
			}
		};

		Class[] acceptedClasses = new Class[] {IFile.class};
		dialog.setValidator(new LibrarySelectionValidator(acceptedClasses, true));
		dialog.setTitle(MDEUIMessages.BuildEditor_ClasspathSection_jarsTitle);
		dialog.setMessage(MDEUIMessages.ClasspathSection_jarsMessage);
		IMonitorLibrary[] libraries = getModel().getMonitorBase().getLibraries();
		IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
		HashSet set = new HashSet();
		for (int i = 0; i < libraries.length; i++) {
			IPath bundlePath = new Path(ClasspathUtilCore.expandLibraryName(libraries[i].getName()));
			IPath buildPath = PDEProject.getBundleRoot(project).getProjectRelativePath().append(bundlePath);
			set.add(buildPath);
		}

		dialog.addFilter(new LibraryFilter(set));
		dialog.setInput(project);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.ADD_LIBRARY);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			String[] bundlePaths = new String[elements.length];
			String[] buildPaths = new String[elements.length];
			IMonitorModelBase model = getModel();
			ArrayList list = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				IContainer bundleRoot = PDEProject.getBundleRoot(project);
				IPath rootPath = bundleRoot.getFullPath();
				// make path relative to bundle root
				IPath bundlePath = elem.getFullPath().makeRelativeTo(rootPath);
				IPath buildPath = elem.getProjectRelativePath();
				if (elem instanceof IFolder) {
					bundlePath = bundlePath.addTrailingSeparator();
					buildPath = buildPath.addTrailingSeparator();
				}
				bundlePaths[i] = bundlePath.toString();
				buildPaths[i] = buildPath.toString();
				IMonitorLibrary library = model.getMonitorFactory().createLibrary();
				try {
					library.setName(bundlePaths[i]);
					library.setExported(true);
					model.getMonitorBase().add(library);
					list.add(library);
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				}
			}
			checkSourceRootEntry();
			updateBuildProperties(new String[bundlePaths.length], bundlePaths, false);
			if (updateClasspath[0])
				updateJavaClasspathLibs(new String[buildPaths.length], buildPaths);
			fLibraryTable.setSelection(new StructuredSelection(list.toArray()));
			fLibraryTable.getTable().setFocus();
		}
	}

	private void updateBuildProperties(final String[] oldPaths, final String[] newPaths, boolean modifySourceEntry) {
		IBuildModel bmodel = getBuildModel();
		if (bmodel == null)
			return;

		IBuild build = bmodel.getBuild();

		IBuildEntry entry = build.getEntry(PROPERTY_BIN_INCLUDES);
		if (entry == null)
			entry = bmodel.getFactory().createEntry(PROPERTY_BIN_INCLUDES);

		try {
			// adding new entries
			if (oldPaths[0] == null) {
				for (int i = 0; i < newPaths.length; i++)
					if (newPaths[i] != null) {
						entry.addToken(newPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, null, newPaths[i]);
					}
				// removing entries
			} else if (newPaths[0] == null) {
				for (int i = 0; i < oldPaths.length; i++)
					if (oldPaths[i] != null) {
						entry.removeToken(oldPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPaths[i], null);
					}
				if (entry.getTokens().length == 0)
					build.remove(entry);
				// rename entries
			} else {
				for (int i = 0; i < oldPaths.length; i++)
					if (newPaths[i] != null && oldPaths[i] != null) {
						entry.renameToken(oldPaths[i], newPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPaths[i], newPaths[i]);
					}
			}
		} catch (CoreException e) {
		}
	}

	private void updateJavaClasspathLibs(String[] oldPaths, String[] newPaths) {
		IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
		IJavaProject jproject = JavaCore.create(project);
		try {
			IClasspathEntry[] entries = jproject.getRawClasspath();
			ArrayList toBeAdded = new ArrayList();
			int index = -1;
			entryLoop: for (int i = 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (index == -1)
						index = i;
					// do not add the old paths (handling deletion/renaming)
					IPath path = entries[i].getPath().removeFirstSegments(1).removeTrailingSeparator();
					for (int j = 0; j < oldPaths.length; j++)
						if (oldPaths[j] != null && path.equals(new Path(oldPaths[j]).removeTrailingSeparator()))
							continue entryLoop;
				} else if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					if (index == -1)
						index = i;
				toBeAdded.add(entries[i]);
			}
			if (index == -1)
				index = entries.length;

			// add paths
			for (int i = 0; i < newPaths.length; i++) {
				if (newPaths[i] == null)
					continue;
				IClasspathEntry entry = JavaCore.newLibraryEntry(project.getFullPath().append(newPaths[i]), null, null, true);
				if (!toBeAdded.contains(entry))
					toBeAdded.add(index++, entry);
			}

			if (toBeAdded.size() == entries.length)
				return;

			IClasspathEntry[] updated = (IClasspathEntry[]) toBeAdded.toArray(new IClasspathEntry[toBeAdded.size()]);
			jproject.setRawClasspath(updated, null);
		} catch (JavaModelException e) {
		}
	}

	public void refresh() {
		if (fLibraryTable.getControl().isDisposed())
			return;
		fLibraryTable.setSelection(null);
		fLibraryTable.refresh();
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IMonitorLibrary) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fLibraryTable.refresh();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				fLibraryTable.remove(changeObject);
			} else {
				fLibraryTable.update(changeObject, null);
			}
		} else if (changeObject.equals(fLibraryTable.getInput())) {
			markStale();
		} else if (changeObject instanceof IMonitorElement && ((IMonitorElement) changeObject).getParent() instanceof IMonitorLibrary) {
			fLibraryTable.update(((IMonitorElement) changeObject).getParent(), null);
		}
	}

	public void setFocus() {
		fLibraryTable.getTable().setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the model
		IMonitorModelBase model = getModel();
		IMonitorBase plugin = model.getMonitorBase();
		try {
			// Paste all source objects
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];
				if (sourceObject instanceof MonitorLibrary) {
					// Plugin library object
					MonitorLibrary library = (MonitorLibrary) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					library.reconnect(model, plugin);
					// Add the library to the plug-in
					plugin.add(library);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		HashSet librarySet = null;
		// Only source objects that are plugin libraries that have not already
		// been specified can be pasted
		for (int i = 0; i < sourceObjects.length; i++) {
			// Only plugin libraries are allowed
			if ((sourceObjects[i] instanceof IMonitorLibrary) == false) {
				return false;
			}
			// We have a plugin library
			// Get the current libraries already specified and store them in
			// a set to assist with searching
			if (librarySet == null) {
				librarySet = createPluginLibrarySet();
			}
			// No duplicate libraries are allowed
			IMonitorLibrary library = (IMonitorLibrary) sourceObjects[i];
			if (librarySet.contains(new Path(ClasspathUtilCore.expandLibraryName(library.getName())))) {
				return false;
			}
		}
		return true;
	}

	private HashSet createPluginLibrarySet() {
		// Get the current libraries and add them to a set for easy searching
		IMonitorLibrary[] libraries = getModel().getMonitorBase().getLibraries();
		HashSet librarySet = new HashSet();
		for (int i = 0; i < libraries.length; i++) {
			librarySet.add(new Path(ClasspathUtilCore.expandLibraryName(libraries[i].getName())));
		}
		return librarySet;
	}

	protected void entryModified(Object entry, String value) {
		try {
			IMonitorModelBase model = getModel();
			IProject project = model.getUnderlyingResource().getProject();
			IMonitorLibrary library = (IMonitorLibrary) entry;
			model.getMonitorBase().remove(library);
			String[] oldValue = {library.getName()};
			String[] newValue = {value};
			library.setName(value);
			boolean memberExists = project.findMember(value) != null;
			updateBuildProperties(oldValue, newValue, !memberExists);
			updateJavaClasspathLibs(oldValue, memberExists ? newValue : new String[] {null});
			model.getMonitorBase().add(library);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private IMonitorModelBase getModel() {
		return (IMonitorModelBase) getPage().getModel();
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
		} else if ((sourceObjects[0] instanceof IMonitorLibrary) == false) {
			// Must be the right type
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
		IMonitorLibrary sourcePluginLibrary = (IMonitorLibrary) sourceObjects[0];
		IMonitorLibrary targetPluginLibrary = (IMonitorLibrary) targetObject;
		// Validate model
		if (validateDropMoveModel(sourcePluginLibrary, targetPluginLibrary) == false) {
			return false;
		}
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getMonitorBase();
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous element of the target 
			IMonitorLibrary previousLibrary = bundlePluginBase.getPreviousLibrary(targetPluginLibrary);
			// Ensure the previous element is not the source
			if (sourcePluginLibrary.equals(previousLibrary)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next element of the target 
			IMonitorLibrary nextLibrary = bundlePluginBase.getNextLibrary(targetPluginLibrary);
			// Ensure the next element is not the source
			if (sourcePluginLibrary.equals(nextLibrary)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}
		return false;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 */
	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof IMonitorLibrary) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourcePluginLibrary
	 * @param targetPluginLibrary
	 */
	private boolean validateDropMoveModel(IMonitorLibrary sourcePluginLibrary, IMonitorLibrary targetPluginLibrary) {
		// Objects have to be from the same model
		ISharedMonitorModel sourceModel = sourcePluginLibrary.getModel();
		ISharedMonitorModel targetModel = targetPluginLibrary.getModel();
		if (sourceModel.equals(targetModel) == false) {
			return false;
		} else if ((getModel().getMonitorBase() instanceof BundlePluginBase) == false) {
			return false;
		}
		return true;
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
		IMonitorLibrary sourcePluginLibrary = (IMonitorLibrary) sourceObjects[0];
		IMonitorLibrary targetPluginLibrary = (IMonitorLibrary) targetObject;
		// Validate move
		if ((targetLocation == ViewerDropAdapter.LOCATION_BEFORE) || (targetLocation == ViewerDropAdapter.LOCATION_AFTER)) {
			// Do move
			doDropMove(sourcePluginLibrary, targetPluginLibrary, targetLocation);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
		}
	}

	/**
	 * @param sourcePluginLibrary
	 * @param targetPluginLibrary
	 * @param targetLocation
	 */
	private void doDropMove(IMonitorLibrary sourcePluginLibrary, IMonitorLibrary targetPluginLibrary, int targetLocation) {
		// Remove the original source object
		// Normally we remove the original source object after inserting the
		// serialized source object; however, the libraries are removed via ID
		// and having both objects with the same ID co-existing will confound
		// the remove operation
		doDragRemove();
		// Get the bundle plug-in base
		IMonitorModelBase model = getModel();
		BundlePluginBase bundlePluginBase = (BundlePluginBase) model.getMonitorBase();
		// Get the index of the target
		int index = bundlePluginBase.getIndexOf(targetPluginLibrary);
		// Ensure the target index was found
		if (index == -1) {
			return;
		}
		// Determine the location index
		int targetIndex = index;
		if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			targetIndex++;
		}
		// Ensure the plugin library is concrete  
		if ((sourcePluginLibrary instanceof MonitorLibrary) == false) {
			return;
		}
		// Adjust all the source object transient field values to
		// acceptable values
		((MonitorLibrary) sourcePluginLibrary).reconnect(model, bundlePluginBase);
		// Add source as sibling of target		
		try {
			bundlePluginBase.add(sourcePluginLibrary, targetIndex);
		} catch (CoreException e) {
			// CoreException only if model is not editable, which should never be the case
		}
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
		// Remove the library
		IMonitorLibrary sourcePluginLibrary = (IMonitorLibrary) sourceObjects[0];
		try {
			bundlePluginBase.remove(sourcePluginLibrary);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

}
