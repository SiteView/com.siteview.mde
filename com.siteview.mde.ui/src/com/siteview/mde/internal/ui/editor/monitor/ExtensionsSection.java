/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 194529, bug 196867
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.core.text.monitor.MonitorBaseNode;

import com.siteview.mde.core.monitor.*;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.IModelChangedListener;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.ischema.*;
import com.siteview.mde.internal.core.schema.SchemaRegistry;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.actions.CollapseAction;
import com.siteview.mde.internal.ui.editor.actions.SortAction;
import com.siteview.mde.internal.ui.editor.contentassist.XMLElementProposalComputer;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import com.siteview.mde.internal.ui.parts.TreePart;
import com.siteview.mde.internal.ui.search.MonitorSearchActionGroup;
import com.siteview.mde.internal.ui.util.SWTUtil;
import com.siteview.mde.internal.ui.wizards.extension.ExtensionEditorWizard;
import com.siteview.mde.internal.ui.wizards.extension.NewExtensionWizard;
import com.siteview.mde.ui.IExtensionEditorWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.BidiUtil;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExtensionsSection extends TreeSection implements IModelChangedListener, IPropertyChangeListener {
	private static final int BUTTON_MOVE_DOWN = 4;
	private static final int BUTTON_MOVE_UP = 3;
	private static final int BUTTON_EDIT = 2;
	private static final int BUTTON_REMOVE = 1;
	private TreeViewer fExtensionTree;
	private Image fExtensionImage;
	private Image fGenericElementImage;
	private FormFilteredTree fFilteredTree;
	private SchemaRegistry fSchemaRegistry;
	private Hashtable fEditorWizards;
	private SortAction fSortAction;
	private CollapseAction fCollapseAction;

	private static final int BUTTON_ADD = 0;

	private static final String[] COMMON_LABEL_PROPERTIES = {"label", //$NON-NLS-1$
			"name", //$NON-NLS-1$
			"id"}; //$NON-NLS-1$

	private static final String[] VALID_IMAGE_TYPES = {"png", "bmp", "ico", "gif", "jpg", "tiff"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	class ExtensionContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof IMonitorBase)
				children = ((IMonitorBase) parent).getExtensions();
			else if (parent instanceof IMonitorExtension) {
				children = ((IMonitorExtension) parent).getChildren();
			} else if (parent instanceof IMonitorElement) {
				children = ((IMonitorElement) parent).getChildren();
			}
			if (children == null)
				children = new Object[0];
			return children;
		}

		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}

		public Object getParent(Object child) {
			if (child instanceof IMonitorExtension) {
				return ((IMonitorModelBase) getPage().getModel()).getMonitorBase();
			}
			if (child instanceof IMonitorObject)
				return ((IMonitorObject) child).getParent();
			return null;
		}

		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}

	class ExtensionLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}

		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}
	}

	public ExtensionsSection(MDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {MDEUIMessages.ManifestEditor_DetailExtension_new, MDEUIMessages.ManifestEditor_DetailExtension_remove, MDEUIMessages.ManifestEditor_DetailExtension_edit, MDEUIMessages.ManifestEditor_DetailExtension_up, MDEUIMessages.ManifestEditor_DetailExtension_down});
		fHandleDefaultButton = false;
	}

	private static void addItemsForExtensionWithSchema(MenuManager menu, IMonitorExtension extension, IMonitorParent parent) {
		ISchema schema = getSchema(extension);
		// Bug 213457 - look up elements based on the schema in which the parent is found
		ISchemaElement elementInfo = null;
		if (schema.getIncludes().length == 0 || parent == extension) {
			String tagName = (parent == extension ? "extension" : parent.getName()); //$NON-NLS-1$
			elementInfo = schema.findElement(tagName);
		} else {
			Stack stack = new Stack();
			IMonitorParent parentParent = parent;
			while (parentParent != extension && parentParent != null) {
				stack.push(parentParent.getName());
				parentParent = (IMonitorParent) parentParent.getParent();
			}
			while (!stack.isEmpty()) {
				elementInfo = schema.findElement((String) stack.pop());
				schema = elementInfo.getSchema();
			}
		}

		if ((elementInfo != null) && (elementInfo.getType() instanceof ISchemaComplexType) && (parent instanceof IDocumentElementNode)) {
			// We have a schema complex type.  Either the element has attributes
			// or the element has children.
			// Generate the list of element proposals
			TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(elementInfo, (IDocumentElementNode) parent);

			// Create a corresponding menu entry for each element proposal
			Iterator iterator = elementSet.iterator();
			while (iterator.hasNext()) {
				Action action = new NewElementAction((ISchemaElement) iterator.next(), parent);
				menu.add(action);
			}
		}
	}

	/**
	 * @param parent
	 */
	private static ISchema getSchema(IMonitorParent parent) {
		if (parent instanceof IMonitorExtension) {
			return getSchema((IMonitorExtension) parent);
		} else if (parent instanceof IMonitorElement) {
			return getSchema((IMonitorElement) parent);
		} else {
			return null;
		}
	}

	private static ISchema getSchema(IMonitorExtension extension) {
		String point = extension.getPoint();
		SchemaRegistry registry = MDECore.getDefault().getSchemaRegistry();
		return registry.getSchema(point);
	}

	/**
	 * @param element
	 */
	static ISchemaElement getSchemaElement(IMonitorElement element) {
		ISchema schema = getSchema(element);
		if (schema != null) {
			return schema.findElement(element.getName());
		}
		return null;
	}

	/**
	 * @param element
	 */
	private static ISchema getSchema(IMonitorElement element) {
		IMonitorObject parent = element.getParent();
		while (parent != null && !(parent instanceof IMonitorExtension)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			return getSchema((IMonitorExtension) parent);
		}
		return null;
	}

	public void createClient(Section section, FormToolkit toolkit) {
		initializeImages();
		Composite container = createClientContainer(section, 2, toolkit);
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fExtensionTree = treePart.getTreeViewer();
		fExtensionTree.setContentProvider(new ExtensionContentProvider());
		fExtensionTree.setLabelProvider(new ExtensionLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setDescription(MDEUIMessages.ExtensionsSection_sectionDescExtensionsMaster);
		// See Bug # 160554: Set text before text client
		section.setText(MDEUIMessages.ManifestEditor_DetailExtension_title);
		initialize((IMonitorModelBase) getPage().getModel());
		createSectionToolbar(section, toolkit);
		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
		Text filterText = fFilteredTree.getFilterControl();
		if (filterText != null) {
			filterText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					StructuredViewer viewer = getStructuredViewerPart().getViewer();
					IStructuredSelection ssel = (IStructuredSelection) viewer.getSelection();
					updateButtons(ssel.size() != 1 ? null : ssel);
				}
			});
		}
	}

	/**
	 * @param section
	 * @param toolkit
	 */
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
		fSortAction = new SortAction(fExtensionTree, MDEUIMessages.ExtensionsPage_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fExtensionTree, MDEUIMessages.ExtensionsPage_collapseAll);
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getMDEEditor().setSelection(selection);
		updateButtons(selection);
		getTreePart().getButton(BUTTON_EDIT).setVisible(isSelectionEditable(selection));
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case BUTTON_ADD :
				handleNew();
				break;
			case BUTTON_REMOVE :
				handleDelete();
				break;
			case BUTTON_EDIT :
				handleEdit();
				break;
			case BUTTON_MOVE_UP :
				handleMove(true);
				break;
			case BUTTON_MOVE_DOWN :
				handleMove(false);
				break;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Explicitly call the dispose method on the extensions tree
		if (fFilteredTree != null) {
			fFilteredTree.dispose();
		}
		fEditorWizards = null;
		IMonitorModelBase model = (IMonitorModelBase) getPage().getMDEEditor().getAggregateModel();
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
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			if (isSingleSelection()) {
				handleDelete();
			}
			return true;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			if (isSingleSelection()) {
				doPaste();
			}
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			handleSelectAll();
			return true;
		}

		return false;
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IMonitorExtension || object instanceof IMonitorElement) {
			fExtensionTree.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fExtensionTree.getSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			Object object = ssel.getFirstElement();
			if (object instanceof IMonitorParent) {
				IMonitorParent parent = (IMonitorParent) object;
				if (parent.getModel().getUnderlyingResource() != null) {
					fillContextMenu(getPage(), parent, manager);
					manager.add(new Separator());
				}
			}
			manager.add(new Separator());
			if (object instanceof IMonitorExtension) {
				MonitorSearchActionGroup actionGroup = new MonitorSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
		} else if (ssel.size() > 1) {
			// multiple
			Action delAction = new Action() {
				public void run() {
					handleDelete();
				}
			};
			delAction.setText(MDEUIMessages.Actions_delete_label);
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(isEditable());
		}
		manager.add(new Separator());
		if (ssel.size() < 2) { // only cut things when the selection is one
			getPage().getMDEEditor().getContributor().addClipboardActions(manager);
		}
		getPage().getMDEEditor().getContributor().contextMenuAboutToShow(manager, false);

	}

	static IMenuManager fillContextMenu(MDEFormPage page, final IMonitorParent parent, IMenuManager manager) {
		return fillContextMenu(page, parent, manager, false);
	}

	static IMenuManager fillContextMenu(MDEFormPage page, final IMonitorParent parent, IMenuManager manager, boolean addSiblingItems) {
		return fillContextMenu(page, parent, manager, addSiblingItems, true);
	}

	static IMenuManager fillContextMenu(MDEFormPage page, final IMonitorParent parent, IMenuManager manager, boolean addSiblingItems, boolean fullMenu) {
		MenuManager menu = new MenuManager(MDEUIMessages.Menus_new_label);
		IMonitorExtension extension = getExtension(parent);
		ISchema schema = getSchema(extension);
		if (schema == null) {
			menu.add(new NewElementAction(null, parent));
		} else {
			addItemsForExtensionWithSchema(menu, extension, parent);
			if (addSiblingItems) {
				IMonitorObject parentsParent = parent.getParent();
				if (!(parentsParent instanceof IMonitorExtension)) {
					IMonitorParent pparent = (IMonitorParent) parentsParent;
					menu.add(new Separator());
					addItemsForExtensionWithSchema(menu, extension, pparent);
				}
			}
		}
		manager.add(menu);
		manager.add(new Separator());
		if (fullMenu) {
			Action deleteAction = new Action(MDEUIMessages.Actions_delete_label) {
				public void run() {
					try {
						IMonitorObject parentsParent = parent.getParent();
						if (parent instanceof IMonitorExtension) {
							IMonitorBase plugin = (IMonitorBase) parentsParent;
							plugin.remove((IMonitorExtension) parent);
						} else {
							IMonitorParent parentElement = (IMonitorParent) parent.getParent();
							parentElement.remove(parent);
						}
					} catch (CoreException e) {
					}
				}
			};
			deleteAction.setEnabled(page.getModel().isEditable());
			manager.add(deleteAction);
		}
		return menu;
	}

	static IMonitorExtension getExtension(IMonitorParent parent) {
		while (parent != null && !(parent instanceof IMonitorExtension)) {
			parent = (IMonitorParent) parent.getParent();
		}
		return (IMonitorExtension) parent;
	}

	private void handleDelete() {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
		if (sel.isEmpty())
			return;
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			IMonitorObject object = (IMonitorObject) iter.next();
			try {
				IStructuredSelection newSelection = null;
				boolean sorted = fSortAction != null && fSortAction.isChecked();
				if (object instanceof IMonitorElement) {
					IMonitorElement ee = (IMonitorElement) object;
					IMonitorParent parent = (IMonitorParent) ee.getParent();
					if (!sorted) {
						int index = getNewSelectionIndex(parent.getIndexOf(ee), parent.getChildCount());
						newSelection = index == -1 ? new StructuredSelection(parent) : new StructuredSelection(parent.getChildren()[index]);
					} else {
						IMonitorObject original[] = parent.getChildren();
						IMonitorObject objects[] = new IMonitorObject[original.length];
						for (int i = 0; i < original.length; i++)
							objects[i] = original[i];
						fExtensionTree.getComparator().sort(fExtensionTree, objects);
						int index = getNewSelectionIndex(getArrayIndex(objects, ee), objects.length);
						newSelection = index == -1 ? new StructuredSelection(parent) : new StructuredSelection(objects[index]);
					}
					parent.remove(ee);
				} else if (object instanceof IMonitorExtension) {
					IMonitorExtension extension = (IMonitorExtension) object;
					IMonitorBase plugin = extension.getMonitorBase();
					if (!sorted) {
						int index = getNewSelectionIndex(plugin.getIndexOf(extension), plugin.getExtensions().length);
						if (index != -1)
							newSelection = new StructuredSelection(plugin.getExtensions()[index]);
					} else {
						IMonitorExtension original[] = plugin.getExtensions();
						IMonitorExtension extensions[] = new IMonitorExtension[original.length];
						for (int i = 0; i < original.length; i++)
							extensions[i] = original[i];
						fExtensionTree.getComparator().sort(fExtensionTree, extensions);
						int index = getNewSelectionIndex(getArrayIndex(extensions, extension), extensions.length);
						if (index != -1)
							newSelection = new StructuredSelection(extensions[index]);
					}
					plugin.remove(extension);
				}
				if (newSelection != null)
					fExtensionTree.setSelection(newSelection);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	private void handleNew() {
		final IProject project = getPage().getMDEEditor().getCommonProject();
		BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
			public void run() {
				((ManifestEditor) getPage().getEditor()).ensurePluginContextPresence();
				NewExtensionWizard wizard = new NewExtensionWizard(project, (IMonitorModelBase) getPage().getModel(), (ManifestEditor) getPage().getMDEEditor()) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		});
	}

	private void handleEdit(IConfigurationElement element, IStructuredSelection selection) {
		IProject project = getPage().getMDEEditor().getCommonProject();
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		try {
			final IExtensionEditorWizard wizard = (IExtensionEditorWizard) element.createExecutableExtension("class"); //$NON-NLS-1$
			wizard.init(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void handleEdit() {
		final IStructuredSelection selection = (IStructuredSelection) fExtensionTree.getSelection();
		ArrayList editorWizards = getEditorWizards(selection);
		if (editorWizards == null)
			return;
		if (editorWizards.size() == 1) {
			// open the wizard directly			
			handleEdit((IConfigurationElement) editorWizards.get(0), selection);
		} else {
			IProject project = getPage().getMDEEditor().getCommonProject();
			IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
			final ExtensionEditorWizard wizard = new ExtensionEditorWizard(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(MDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
	}

	private void handleSelectAll() {
		fExtensionTree.getTree().selectAll();
	}

	private ArrayList getEditorWizards(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object obj = selection.getFirstElement();
		String pointId = null;
		if (obj instanceof IMonitorExtension) {
			pointId = ((IMonitorExtension) obj).getPoint();
		} else if (obj instanceof IMonitorElement) {
			IMonitorObject parent = ((IMonitorElement) obj).getParent();
			while (parent != null) {
				if (parent instanceof IMonitorExtension) {
					pointId = ((IMonitorExtension) parent).getPoint();
					break;
				}
				parent = parent.getParent();
			}
		}
		if (pointId == null)
			return null;
		if (fEditorWizards == null)
			loadExtensionWizards();
		return (ArrayList) fEditorWizards.get(pointId);
	}

	private void loadExtensionWizards() {
		fEditorWizards = new Hashtable();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.siteview.mde.ui.newExtension"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("editorWizard")) { //$NON-NLS-1$
				String pointId = element.getAttribute("point"); //$NON-NLS-1$
				if (pointId == null)
					continue;
				ArrayList list = (ArrayList) fEditorWizards.get(pointId);
				if (list == null) {
					list = new ArrayList();
					fEditorWizards.put(pointId, list);
				}
				list.add(element);
			}
		}
	}

	private boolean isSelectionEditable(IStructuredSelection selection) {
		if (!getPage().getModel().isEditable())
			return false;
		return getEditorWizards(selection) != null;
	}

	public void initialize(IMonitorModelBase model) {
		fExtensionTree.setInput(model.getMonitorBase());
		selectFirstExtension();
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(BUTTON_ADD, editable);
		treePart.setButtonEnabled(BUTTON_REMOVE, false);
		treePart.setButtonEnabled(BUTTON_EDIT, false);
		treePart.setButtonEnabled(BUTTON_MOVE_UP, false);
		treePart.setButtonEnabled(BUTTON_MOVE_DOWN, false);
		model.addModelChangedListener(this);
	}

	private void selectFirstExtension() {
		Tree tree = fExtensionTree.getTree();
		TreeItem[] items = tree.getItems();
		if (items.length == 0)
			return;
		TreeItem firstItem = items[0];
		Object obj = firstItem.getData();
		fExtensionTree.setSelection(new StructuredSelection(obj));
	}

	void fireSelection() {
		fExtensionTree.setSelection(fExtensionTree.getSelection());
	}

	public void initializeImages() {
		MDELabelProvider provider = MDEPlugin.getDefault().getLabelProvider();
		fExtensionImage = provider.get(MDEPluginImages.DESC_EXTENSION_OBJ);
		fGenericElementImage = provider.get(MDEPluginImages.DESC_GENERIC_XML_OBJ);
	}

	public void refresh() {
		// The model changed but the editor is still open, we should try to retain expansion, selection will be retained on its own
		Object[] expanded = fExtensionTree.getExpandedElements();
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		fExtensionTree.getControl().setRedraw(false);
		fExtensionTree.setInput(model.getMonitorBase());
		fExtensionTree.setExpandedElements(expanded);
		fExtensionTree.getControl().setRedraw(true);
		reportMissingExtensionPointSchemas(model.getMonitorBase());
		getManagedForm().fireSelectionChanged(ExtensionsSection.this, fExtensionTree.getSelection());
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IMonitorBase && event.getChangeType() == IModelChangedEvent.CHANGE && event.getChangedProperty().equals(IExtensions.P_EXTENSION_ORDER)) {
			IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
			IMonitorExtension extension = (IMonitorExtension) sel.getFirstElement();
			fExtensionTree.refresh();
			fExtensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IMonitorExtension || (changeObject instanceof IMonitorElement && ((IMonitorElement) changeObject).getParent() instanceof IMonitorParent)) {
			IMonitorObject pobj = (IMonitorObject) changeObject;
			IMonitorObject parent = changeObject instanceof IMonitorExtension ? ((IMonitorModelBase) getPage().getModel()).getMonitorBase() : pobj.getParent();
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fExtensionTree.refresh(parent);
				if (changeObject instanceof IMonitorExtension) {
					IMonitorExtension ext = (IMonitorExtension) changeObject;
					if (ext.getSchema() == null)
						reportMissingExtensionPointSchema(ext.getPoint());
				}
				fExtensionTree.setSelection(new StructuredSelection(changeObject), true);
				fExtensionTree.getTree().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				if (changeObject instanceof IMonitorExtension) {
					IMonitorExtension ext = (IMonitorExtension) changeObject;
					IMonitorExtension[] extensions = ((IMonitorBase) parent).getExtensions();
					boolean found = false;
					// search if there is at least another extension extending the same point than the one being removed
					for (int i = 0; i < extensions.length; i++) {
						String point = extensions[i].getPoint();
						if (ext.getPoint().equals(point)) {
							found = true;
							break;
						}
					}
					if (!found)
						getManagedForm().getMessageManager().removeMessage(ext.getPoint());
				}
				fExtensionTree.remove(pobj);
			} else {
				if (event.getChangedProperty().equals(IMonitorParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
					IMonitorObject child = (IMonitorObject) sel.getFirstElement();
					fExtensionTree.refresh(child.getParent());
					fExtensionTree.setSelection(new StructuredSelection(child));
				} else {
					fExtensionTree.update(changeObject, null);
				}
			}
		}
	}

	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IMonitorExtension) {
			return fExtensionImage;
		}
		Image elementImage = fGenericElementImage;
		if (obj instanceof IMonitorElement) {
			IMonitorElement element = (IMonitorElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
		}
		return elementImage;
	}

	private static boolean isStorageModel(IMonitorObject object) {
		IMonitorModelBase modelBase = object.getMonitorModel();
		return modelBase.getInstallLocation() == null;
	}

	static Image getCustomImage(IMonitorElement element) {
		if (isStorageModel(element))
			return null;
		ISchemaElement elementInfo = getSchemaElement(element);
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IMonitorAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			// we have a value from a resource attribute
			if (iconPath != null) {
				String ext = new Path(iconPath).getFileExtension();
				// if the resource targets a folder, the file extension will be null
				if (ext == null)
					return null;
				boolean valid = false;
				// ensure the resource is an image
				for (int i = 0; i < VALID_IMAGE_TYPES.length; i++) {
					if (ext.equalsIgnoreCase(VALID_IMAGE_TYPES[i])) {
						valid = true;
						break;
					}
				}
				// if the resource is an image, get the image, otherwise return null
				return valid ? getImageFromPlugin(element, iconPath) : null;
			}
		}
		return null;
	}

	private static Image getImageFromPlugin(IMonitorElement element, String iconPathName) {
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;

		IMonitorModelBase model = element.getMonitorModel();
		if (model == null)
			return null;

		return MDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(model, iconPathName);
	}

	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), obj);
	}

	private SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = MDECore.getDefault().getSchemaRegistry();
		return fSchemaRegistry;
	}

	public static String resolveObjectName(SchemaRegistry schemaRegistry, Object obj) {
		boolean fullNames = MDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IMonitorExtension) {
			IMonitorExtension extension = (IMonitorExtension) obj;
			if (!fullNames) {
				return extension.getPoint();
			}
			if (extension.getName() != null)
				return extension.getTranslatedName();
			ISchema schema = schemaRegistry.getSchema(extension.getPoint());
			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			return extension.getPoint();
		} else if (obj instanceof IMonitorElement) {
			IMonitorElement element = (IMonitorElement) obj;
			String baseName = element.getName();
			String fullName = null;
			ISchemaElement elementInfo = getSchemaElement(element);
			IMonitorAttribute labelAtt = null;
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_PROPERTIES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_PROPERTIES[i]);
					if (labelAtt != null)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null)
				fullName = stripShortcuts(labelAtt.getValue());
			fullName = element.getResourceString(fullName);
			if (fullNames)
				return fullName != null ? fullName : baseName;
			if (fullName == null)
				return baseName;
			// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
			// add RTL zero length character just before the ( and the LTR character just after to ensure:
			// 1. The leading parenthesis takes proper orientation when running in bidi configuration
			// Assumption: baseName (taken from the schema definition), is only latin characters and is therefore always displayed LTR
			if (BidiUtil.isBidiPlatform())
				return fullName + " \u200f(\u200e" + baseName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return fullName + " (" + baseName + ')'; //$NON-NLS-1$
		}
		return obj.toString();
	}

	public void setFocus() {
		if (fExtensionTree != null)
			fExtensionTree.getTree().setFocus();
	}

	public static String stripShortcuts(String input) {
		StringBuffer output = new StringBuffer();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '&')
				continue;
			else if (c == '@')
				break;
			output.append(c);
		}
		return output.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// Note: Multi-select in is enabled and this function can support 
		// multiple source object but it needs to be investigated
		// Rule:  Element source objects are always pasted as children of the
		// target object (if allowable)
		// Rule:  Extension source objects are always pasted and are independent
		// of the target object
		// Ensure all the sourceObjects are either extensions or elements
		boolean allExtensions = true;
		boolean allElements = true;
		for (int i = 0; i < sourceObjects.length; i++) {
			if (sourceObjects[i] instanceof IMonitorExtension) {
				allElements = false;
			} else if (sourceObjects[i] instanceof IMonitorElement) {
				allExtensions = false;
			} else {
				return false;
			}
		}
		// Because of the extension rule, we can paste all extension source
		// objects
		if (allExtensions) {
			return true;
		}
		// Pasting a mixture of elements and extensions is not supported
		// (or wise from the users perspective)
		if (allElements == false) {
			return false;
		}
		// Ensure the target object can have children 
		if ((targetObject instanceof IMonitorParent) == false) {
			return false;
		} else if ((targetObject instanceof IDocumentElementNode) == false) {
			return false;
		}
		// Retrieve the schema corresponding to the target object		
		IMonitorParent targetParent = (IMonitorParent) targetObject;
		ISchema schema = getSchema(targetParent);
		// If there is no schema, then a source object can be pasted as a 
		// child of any target object
		if (schema == null) {
			return true;
		}
		// Determine the element name of the target object
		String tagName = ((IDocumentElementNode) targetParent).getXMLTagName();
		// Retrieve the element schema for the target object
		ISchemaElement schemaElement = schema.findElement(tagName);
		// Ensure we found a schema element and it is a schema complex type
		if (schemaElement == null) {
			// Something is seriously wrong, we have a schema
			return false;
		} else if ((schemaElement.getType() instanceof ISchemaComplexType) == false) {
			// Something is seriously wrong, we are a plugin parent
			return false;
		}
		// We have a schema complex type.  Either the target object has 
		// attributes or the element has children.
		// Generate the list of element proposals
		TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(schemaElement, (IDocumentElementNode) targetObject);
		// Determine whether we can paste the source elements as children of
		// the target object
		if (sourceObjects.length > 1) {
			IMonitorElement[] sourcePluginElements = new IMonitorElement[sourceObjects.length];
			System.arraycopy(sourceObjects, 0, sourcePluginElements, 0, sourceObjects.length);
			return canPasteSourceElements(sourcePluginElements, elementSet);
		}
		return canPasteSourceElement((IMonitorElement) sourceObjects[0], elementSet);
	}

	/**
	 * @param sourceElements
	 * @param targetElementSet
	 */
	private boolean canPasteSourceElements(IMonitorElement[] sourceElements, TreeSet targetElementSet) {
		// Performance optimization
		// HashSet of schema elements is not comparable for the source
		// objects (schema elements are transient)
		// Create a new HashSet with element names for comparison		
		HashSet targetElementNameSet = new HashSet();
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			targetElementNameSet.add(((ISchemaElement) iterator.next()).getName());
		}
		// Paste will be enabled only if all source objects can be pasted 
		// as children into the target element
		// Limitation:  Multiplicity checks will be compromised because we
		// are pasting multiple elements as a single transaction.  The 
		// mulitplicity check is computed on the current static state of the
		// target object with the assumption one new element will be added.
		// Obviously, adding more than one element can invalidate the check
		// due to choice, sequence multiplicity constraints.  Even if source
		// elements that are pasted violate multiplicity constraints the 
		// extensions builder will flag them with errors
		for (int i = 0; i < sourceElements.length; i++) {
			String sourceTagName = sourceElements[i].getName();
			if (targetElementNameSet.contains(sourceTagName) == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param sourceElement
	 * @param targetElementSet
	 */
	private boolean canPasteSourceElement(IMonitorElement sourceElement, TreeSet targetElementSet) {
		boolean canPaste = false;
		// Get the source element tag name
		String sourceTagName = sourceElement.getName();
		// Iterate over set of valid element proposals
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			// Get the proposal element tag name
			String targetTagName = ((ISchemaElement) iterator.next()).getName();
			// Only a source element that is found ithin the set of element 
			// proposals can be pasted
			if (sourceTagName.equals(targetTagName)) {
				canPaste = true;
				break;
			}
		}
		return canPaste;
	}

	private IMonitorModelBase getPluginModelBase() {
		// Note:  This method will work with fragments as long as a fragment.xml
		// is defined first.  Otherwise, paste will not work out of the box.
		// Get the model
		IMonitorModelBase model = (IMonitorModelBase) getPage().getModel();
		// Ensure the model is a bundle plugin model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return null;
		}
		// Get the extension model
		ISharedExtensionsModel extensionModel = ((IBundlePluginModelBase) model).getExtensionsModel();
		// Ensure the extension model is defined
		if ((extensionModel == null) || ((extensionModel instanceof IMonitorModelBase) == false)) {
			return null;
		}
		return ((IMonitorModelBase) extensionModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// By default, fragment.xml does not exist until the first extension
		// or extension point is created.  
		// Ensure the file exists before pasting because the model will be 
		// null and the paste will fail if it does not exist
		((ManifestEditor) getPage().getEditor()).ensurePluginContextPresence();
		// Note:  Multi-select in tree viewer is disabled; but, this function
		// can support multiple source objects
		// Get the model
		IMonitorModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		IMonitorBase pluginBase = model.getMonitorBase();
		try {
			// Paste all source objects into the target object
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];

				if ((sourceObject instanceof IMonitorExtension) && (pluginBase instanceof IDocumentElementNode)) {
					// Extension object
					IDocumentElementNode extension = (IDocumentElementNode) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					extension.reconnect((IDocumentElementNode) pluginBase, model);
					// Add the extension to the plugin parent (plugin)
					pluginBase.add((IMonitorExtension) extension);

				} else if ((sourceObject instanceof IMonitorElement) && (targetObject instanceof IMonitorParent) && (targetObject instanceof IDocumentElementNode)) {
					// Element object
					IDocumentElementNode element = (IDocumentElementNode) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					element.reconnect((IDocumentElementNode) targetObject, model);
					// Add the element to the plugin parent (extension or
					// element)
					((IMonitorParent) targetObject).add((IMonitorElement) element);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void handleMove(boolean up) {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
		IMonitorObject object = (IMonitorObject) sel.getFirstElement();
		if (object instanceof IMonitorElement) {
			IMonitorParent parent = (IMonitorParent) object.getParent();
			IMonitorObject[] children = parent.getChildren();
			int index = parent.getIndexOf(object);
			int newIndex = up ? index - 1 : index + 1;
			IMonitorObject child2 = children[newIndex];
			try {
				parent.swap(object, child2);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		} else if (object instanceof IMonitorExtension) {
			IMonitorExtension extension = (IMonitorExtension) object;
			IMonitorBase plugin = extension.getMonitorBase();
			IMonitorExtension[] extensions = plugin.getExtensions();
			int index = plugin.getIndexOf(extension);
			int newIndex = up ? index - 1 : index + 1;
			IMonitorExtension e2 = extensions[newIndex];
			try {
				plugin.swap(extension, e2);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	private void updateButtons(Object item) {
		if (getPage().getModel().isEditable() == false)
			return;
		boolean sorted = fSortAction != null && fSortAction.isChecked();
		if (sorted) {
			getTreePart().setButtonEnabled(BUTTON_MOVE_UP, false);
			getTreePart().setButtonEnabled(BUTTON_MOVE_DOWN, false);
			return;
		}

		boolean filtered = fFilteredTree.isFiltered();
		boolean addEnabled = true;
		boolean removeEnabled = false;
		boolean upEnabled = false;
		boolean downEnabled = false;

		if (item != null) {
			removeEnabled = true;
		}
		if (filtered) {
			// Fix for bug 194529 and bug 194828
			addEnabled = false;
			upEnabled = false;
			downEnabled = false;
		} else {
			if (item instanceof IStructuredSelection) {
				if (((IStructuredSelection) item).size() == 1) {
					Object selected = ((IStructuredSelection) item).getFirstElement();
					if (selected instanceof IMonitorElement) {
						IMonitorElement element = (IMonitorElement) selected;
						IMonitorParent parent = (IMonitorParent) element.getParent();
						// check up
						int index = parent.getIndexOf(element);
						if (index > 0)
							upEnabled = true;
						if (index < parent.getChildCount() - 1)
							downEnabled = true;
					} else if (selected instanceof IMonitorExtension) {
						IMonitorExtension extension = (IMonitorExtension) selected;
						IExtensions extensions = (IExtensions) extension.getParent();
						int index = extensions.getIndexOf(extension);
						int size = extensions.getExtensions().length;
						if (index > 0)
							upEnabled = true;
						if (index < size - 1)
							downEnabled = true;
					}
				}
			}
		}
		getTreePart().setButtonEnabled(BUTTON_ADD, addEnabled);
		getTreePart().setButtonEnabled(BUTTON_REMOVE, removeEnabled);
		getTreePart().setButtonEnabled(BUTTON_MOVE_UP, upEnabled);
		getTreePart().setButtonEnabled(BUTTON_MOVE_DOWN, downEnabled);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fFilteredTree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			StructuredViewer viewer = getStructuredViewerPart().getViewer();
			IStructuredSelection ssel = (IStructuredSelection) viewer.getSelection();
			updateButtons(ssel);
		}
	}

	protected void selectExtensionElement(ISelection selection) {
		fExtensionTree.setSelection(selection, true);
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
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		} else if (fFilteredTree.isFiltered()) {
			return false;
		} else if (isTreeViewerSorted()) {
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
		if ((targetObject instanceof IMonitorParent) == false) {
			return false;
		} else if ((targetObject instanceof IDocumentElementNode) == false) {
			return false;
		}
		// Validate source objects
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
		} else if ((sourceObjects[0] instanceof IDocumentElementNode) == false) {
			// Must be the right type
			return false;
		} else if ((sourceObjects[0] instanceof IMonitorParent) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

	/**
	 * @param sourcePluginObject
	 * @param targetPluginObject
	 */
	private boolean validateDropMoveModel(IMonitorParent sourcePluginObject, IMonitorParent targetPluginObject) {
		// Objects have to be from the same model
		ISharedMonitorModel sourceModel = sourcePluginObject.getModel();
		ISharedMonitorModel targetModel = targetPluginObject.getModel();
		if (sourceModel.equals(targetModel)) {
			return true;
		}
		return false;
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
		IMonitorParent sourcePluginObject = (IMonitorParent) sourceObjects[0];
		IMonitorParent targetPluginObject = (IMonitorParent) targetObject;
		// Validate model
		if (validateDropMoveModel(sourcePluginObject, targetPluginObject) == false) {
			return false;
		}
		// Validate move
		if (sourcePluginObject instanceof IMonitorExtension) {
			IMonitorExtension sourceExtensionObject = (IMonitorExtension) sourcePluginObject;
			if (targetPluginObject instanceof IMonitorExtension) {
				// Source:  Extension
				// Target:  Extension
				IMonitorExtension targetExtensionObject = (IMonitorExtension) targetPluginObject;
				return canDropMove(targetExtensionObject, sourceExtensionObject, targetLocation);
			} else if (targetPluginObject instanceof IMonitorElement) {
				// Source:  Extension
				// Target:  Element
				return false;
			}
		} else if (sourcePluginObject instanceof IMonitorElement) {
			IMonitorElement sourceElementObject = (IMonitorElement) sourcePluginObject;
			if (targetPluginObject instanceof IMonitorExtension) {
				// Source:  Element
				// Target:  Extension
				IMonitorExtension targetExtensionObject = (IMonitorExtension) targetPluginObject;
				return canDropMove(targetExtensionObject, sourceElementObject, targetLocation);
			} else if (targetPluginObject instanceof IMonitorElement) {
				// Source:  Element
				// Target:  Element
				IMonitorElement targetElementObject = (IMonitorElement) targetPluginObject;
				return canDropMove(targetElementObject, sourceElementObject, targetLocation);
			}
		}
		return false;
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IMonitorElement targetElementObject, IMonitorElement sourceElementObject, int targetLocation) {

		// Verify that the source is not the parent of the target
		if (validateDropMoveParent(targetElementObject, sourceElementObject) == false) {
			return false;
		}

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = ((IDocumentElementNode) targetElementObject).getPreviousSibling();
			if (sourceElementObject.equals(previousNode)) {
				return false;
			}
			IMonitorObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IMonitorParent) == false) {
				return false;
			}
			// Paste element as a sibling of the other element (before)
			return validateDropMoveSchema((IMonitorParent) targetParentObject, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = ((IDocumentElementNode) sourceElementObject).getPreviousSibling();
			if (targetElementObject.equals(nextNode)) {
				return false;
			}
			IMonitorObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IMonitorParent) == false) {
				return false;
			}
			// Paste element as a sibling of the other element (after)
			return validateDropMoveSchema((IMonitorParent) targetParentObject, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetElementObject;
			int childCount = targetExtensionNode.getChildCount();
			if (childCount != 0) {
				IDocumentElementNode lastNode = targetExtensionNode.getChildAt(childCount - 1);
				if (sourceElementObject.equals(lastNode)) {
					return false;
				}
			}
			// Paste element as the last child of the element
			return validateDropMoveSchema(targetElementObject, sourceElementObject);
		}
		return false;
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 */
	private boolean validateDropMoveParent(IMonitorElement targetElementObject, IMonitorElement sourceElementObject) {

		IMonitorObject currentParent = targetElementObject.getParent();
		while (true) {
			if (currentParent == null) {
				return true;
			} else if ((currentParent instanceof IMonitorElement) == false) {
				return true;
			} else if (sourceElementObject.equals(currentParent)) {
				return false;
			}
			currentParent = currentParent.getParent();
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IMonitorExtension targetExtensionObject, IMonitorElement sourceElementObject, int targetLocation) {

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
			int childCount = targetExtensionNode.getChildCount();
			if (childCount != 0) {
				IDocumentElementNode lastNode = targetExtensionNode.getChildAt(childCount - 1);
				if (sourceElementObject.equals(lastNode)) {
					return false;
				}
			}
			// Paste element as the last child of the extension
			return validateDropMoveSchema(targetExtensionObject, sourceElementObject);
		}
		return false;
	}

	/**
	 * @param targetPluginObject
	 * @param sourcePluginObject
	 */
	private boolean validateDropMoveSchema(IMonitorParent targetPluginObject, IMonitorParent sourcePluginObject) {
		IDocumentElementNode targetPluginNode = (IDocumentElementNode) targetPluginObject;
		// If the target is the source's parent, then the move is always 
		// valid.  No need to check the schema.  Order does not matter
		if (targetPluginObject.equals(sourcePluginObject.getParent())) {
			return true;
		}
		// Retrieve the schema corresponding to the target object		
		ISchema schema = getSchema(targetPluginObject);
		// If there is no schema, then a source object can be pasted as a 
		// child of any target object
		if (schema == null) {
			return true;
		}
		// Determine the element name of the target object
		String targetNodeTagName = targetPluginNode.getXMLTagName();
		// Retrieve the element schema for the target object
		ISchemaElement schemaElement = schema.findElement(targetNodeTagName);
		// Ensure we found a schema element and it is a schema complex type
		if (schemaElement == null) {
			// Something is seriously wrong, we have a schema
			return false;
		} else if ((schemaElement.getType() instanceof ISchemaComplexType) == false) {
			// Something is seriously wrong, we are a plugin parent
			return false;
		}
		// We have a schema complex type.  Either the target object has 
		// attributes or the element has children.
		// Generate the list of element proposals
		TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(schemaElement, targetPluginNode);
		// Iterate over set of valid element proposals
		Iterator iterator = elementSet.iterator();
		while (iterator.hasNext()) {
			// Get the proposal element tag name
			String targetTagName = ((ISchemaElement) iterator.next()).getName();
			// Only a source element that is found ithin the set of element 
			// proposals can be pasted
			String sourceNodeTagName = ((IDocumentElementNode) sourcePluginObject).getXMLTagName();
			if (sourceNodeTagName.equals(targetTagName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceExtensionObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IMonitorExtension targetExtensionObject, IMonitorExtension sourceExtensionObject, int targetLocation) {

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = ((IDocumentElementNode) targetExtensionObject).getPreviousSibling();
			if (sourceExtensionObject.equals(previousNode)) {
				return false;
			}
			// Paste extension as sibling of extension (before)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = ((IDocumentElementNode) sourceExtensionObject).getPreviousSibling();
			if (targetExtensionObject.equals(nextNode)) {
				return false;
			}
			// Paste extension as sibling of extension (after)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			return false;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		IMonitorParent pluginParentObject = (IMonitorParent) sourceObjects[0];
		// Remove the object from the model
		try {
			if (pluginParentObject instanceof IMonitorExtension) {
				IMonitorExtension extension = (IMonitorExtension) pluginParentObject;
				IMonitorBase pluginBase = pluginParentObject.getMonitorBase();
				if (pluginBase != null) {
					pluginBase.remove(extension);
				}
			} else if (pluginParentObject instanceof IMonitorElement) {
				IMonitorElement element = (IMonitorElement) pluginParentObject;
				IMonitorObject object = element.getParent();
				if (object instanceof IMonitorParent) {
					((IMonitorParent) object).remove(element);
				}
			}
			// Applicable for move operations
			// Flush the text edit operations associated with the move operation
			// to the source page
			// Move involves add new cloned object x and remove of original object
			// x 
			// Without flushing, multiple move operations up and down cause the
			// text edit operations to get completely screwed up (e.g. mark-up
			// in wrong position or getting lost)
			// TODO: MP: Undo: What are the implications of this?
			((MDEFormEditor) getPage().getEditor()).getContextManager().getPrimaryContext().flushEditorInput();
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
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
		IMonitorParent sourcePluginObject = (IMonitorParent) sourceObjects[0];
		IMonitorParent targetPluginObject = (IMonitorParent) targetObject;
		// Validate move
		try {
			if (sourcePluginObject instanceof IMonitorExtension) {
				IMonitorExtension sourceExtensionObject = (IMonitorExtension) sourcePluginObject;
				if (targetPluginObject instanceof IMonitorExtension) {
					// Source:  Extension
					// Target:  Extension
					IMonitorExtension targetExtensionObject = (IMonitorExtension) targetPluginObject;
					doDropMove(targetExtensionObject, sourceExtensionObject, targetLocation);
				} else if (targetPluginObject instanceof IMonitorElement) {
					// Source:  Extension
					// Target:  Element
					return;
				}
			} else if (sourcePluginObject instanceof IMonitorElement) {
				IMonitorElement sourceElementObject = (IMonitorElement) sourcePluginObject;
				if (targetPluginObject instanceof IMonitorExtension) {
					// Source:  Element
					// Target:  Extension
					IMonitorExtension targetExtensionObject = (IMonitorExtension) targetPluginObject;
					doDropMove(targetExtensionObject, sourceElementObject, targetLocation);
				} else if (targetPluginObject instanceof IMonitorElement) {
					// Source:  Element
					// Target:  Element
					IMonitorElement targetElementObject = (IMonitorElement) targetPluginObject;
					doDropMove(targetElementObject, sourceElementObject, targetLocation);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceExtensionObject
	 * @param targetLocation
	 */
	private void doDropMove(IMonitorExtension targetExtensionObject, IMonitorExtension sourceExtensionObject, int targetLocation) throws CoreException {
		// Get the model
		IMonitorModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Get the plugin base
		IMonitorBase pluginBase = model.getMonitorBase();
		// Ensure the plugin base is a document node
		if ((pluginBase instanceof IDocumentElementNode) == false) {
			return;
		} else if ((pluginBase instanceof MonitorBaseNode) == false) {
			return;
		}
		// Plug-in base node
		IDocumentElementNode pluginBaseNode = (IDocumentElementNode) pluginBase;
		// Source extension node
		IDocumentElementNode sourceExtensionNode = (IDocumentElementNode) sourceExtensionObject;
		// Target extension node
		IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceExtensionNode.reconnect(pluginBaseNode, model);
			// Get index of target extension
			int index = (pluginBaseNode.indexOf(targetExtensionNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste extension as sibling of extension (before)
			((MonitorBaseNode) pluginBaseNode).add(sourceExtensionObject, index);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceExtensionNode.reconnect(pluginBaseNode, model);
			// Get index of target extension
			int index = (pluginBaseNode.indexOf(targetExtensionNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste extension as sibling of extension (after)
			((MonitorBaseNode) pluginBaseNode).add(sourceExtensionObject, index + 1);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// NO-OP
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private void doDropMove(IMonitorExtension targetExtensionObject, IMonitorElement sourceElementObject, int targetLocation) throws CoreException {
		// Get the model
		IMonitorModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Target extension node
		IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
		// Source extension node
		IDocumentElementNode sourceElementNode = (IDocumentElementNode) sourceElementObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// NO-OP
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// NO-OP
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetExtensionNode, model);
			// Paste element as the last child of the extension
			targetExtensionObject.add(sourceElementObject);
		}
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private void doDropMove(IMonitorElement targetElementObject, IMonitorElement sourceElementObject, int targetLocation) throws CoreException {
		// Get the model
		IMonitorModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Target extension node
		IDocumentElementNode targetElementNode = (IDocumentElementNode) targetElementObject;
		// Source extension node
		IDocumentElementNode sourceElementNode = (IDocumentElementNode) sourceElementObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the target's parent
			IMonitorObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IMonitorParent) == false) {
				return;
			} else if ((targetParentObject instanceof IDocumentElementNode) == false) {
				return;
			}
			IDocumentElementNode targetParentNode = (IDocumentElementNode) targetParentObject;
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetParentNode, model);
			// Get index of target element
			int index = (targetParentNode.indexOf(targetElementNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste element as a sibling of the other element (before)
			((IMonitorParent) targetParentObject).add(index, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the target's parent
			IMonitorObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IMonitorParent) == false) {
				return;
			} else if ((targetParentObject instanceof IDocumentElementNode) == false) {
				return;
			}
			IDocumentElementNode targetParentNode = (IDocumentElementNode) targetParentObject;
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetParentNode, model);
			// Get index of target element
			int index = (targetParentNode.indexOf(targetElementNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste element as a sibling of the other element (after)
			((IMonitorParent) targetParentObject).add(index + 1, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetElementNode, model);
			// Paste element as the last child of the element
			targetElementObject.add(sourceElementObject);
		}

	}

	private boolean isTreeViewerSorted() {
		if (fSortAction == null) {
			return false;
		}
		return fSortAction.isChecked();
	}

	private boolean isSingleSelection() {
		IStructuredSelection selection = (IStructuredSelection) fExtensionTree.getSelection();
		return selection.size() == 1;
	}

	private void reportMissingExtensionPointSchemas(IMonitorBase pluginBase) {
		IMonitorExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IMonitorExtension ext = extensions[i];
			if (ext.getSchema() == null)
				reportMissingExtensionPointSchema(ext.getPoint());
		}
	}

	private void reportMissingExtensionPointSchema(String point) {
		getManagedForm().getMessageManager().addMessage(point, NLS.bind(MDEUIMessages.ManifestEditor_DetailExtension_missingExtPointSchema, point), null, IMessageProvider.WARNING);
	}
}
