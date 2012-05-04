/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.dependencies;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;
import com.siteview.mde.internal.ui.refactoring.PDERefactoringAction;
import com.siteview.mde.internal.ui.refactoring.RefactoringActionFactory;
import com.siteview.mde.internal.ui.search.dependencies.DependencyExtentAction;
import com.siteview.mde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.Page;

public abstract class DependenciesViewPage extends Page {
	class FocusOnSelectionAction extends Action {
		public void run() {
			handleFocusOn(getSelectedObject());
		}

		public void update(Object object) {
			setEnabled(object != null);
			String name = ((LabelProvider) fViewer.getLabelProvider()).getText(object);
			setText(NLS.bind(MDEUIMessages.DependenciesViewPage_focusOnSelection, name));
			setImageDescriptor(MDEPluginImages.DESC_FOCUS_ON);
		}
	}

	private Action fFocusOnAction;

	private FocusOnSelectionAction fFocusOnSelectionAction;

	private Action fOpenAction;

	protected PDERefactoringAction fRefactorAction;

	private IPropertyChangeListener fPropertyListener;

	private DependenciesView fView;

	protected StructuredViewer fViewer;

	protected IContentProvider fContentProvider;

	private Action fHideFragmentFilterAction;

	protected Action fHideOptionalFilterAction;

	private FragmentFilter fHideFragmentFilter = new FragmentFilter();

	private static final String HIDE_FRAGMENTS = "hideFrags"; //$NON-NLS-1$

	private static final String HIDE_OPTIONAL = "hideOptional"; //$NON-NLS-1$

	class FragmentFilter extends ViewerFilter {

		public boolean select(Viewer v, Object parent, Object element) {
			BundleDescription desc = null;
			if (element instanceof BundleSpecification) {
				BaseDescription supplier = ((BundleSpecification) element).getSupplier();
				if (supplier instanceof BundleDescription)
					desc = (BundleDescription) supplier;
			} else if (element instanceof BundleDescription) {
				desc = (BundleDescription) element;
			} else if (element instanceof ImportPackageSpecification) {
				BaseDescription export = ((ImportPackageSpecification) element).getSupplier();
				desc = ((ExportPackageDescription) export).getExporter();
			}
			if (desc != null) {
				return desc.getHost() == null;
			}
			return true;
		}
	}

	/**
	 * 
	 */
	public DependenciesViewPage(DependenciesView view, IContentProvider contentProvider) {
		this.fView = view;
		this.fContentProvider = contentProvider;
		fPropertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
					fViewer.refresh();
				}
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fViewer = createViewer(parent);
		fViewer.setComparator(DependenciesViewComparator.getViewerComparator());
		MDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getSite().setSelectionProvider(fViewer);
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	public void dispose() {
		MDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		super.dispose();
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

		if (selection.size() == 1) {
			manager.add(fOpenAction);
			manager.add(new Separator());
		}
		fFocusOnSelectionAction.update(getSelectedObject());
		if (fFocusOnSelectionAction.isEnabled())
			manager.add(fFocusOnSelectionAction);
		manager.add(fFocusOnAction);
		Object selectionElement = selection.getFirstElement();

		manager.add(new Separator());
		// only show Find Dependency Extent when in Callees view
		if (selection.size() == 1 && !fView.isShowingCallers()) {
			String id = null;
			if (selectionElement instanceof BundleSpecification) {
				id = ((BundleSpecification) selectionElement).getName();
			} else if (selectionElement instanceof BundleDescription) {
				id = ((BundleDescription) selectionElement).getSymbolicName();
			}
			// don't include find dependency extent for unresolved imports or bundles
			if (id != null && MonitorRegistry.findModel(id) != null) {
				Object input = fViewer.getInput();
				if (input instanceof IMonitorBase)
					input = ((IMonitorBase) input).getModel();
				if (input instanceof IMonitorModelBase) {
					IMonitorModelBase base = (IMonitorModelBase) input;
					IResource res = (base == null) ? null : base.getUnderlyingResource();
					if (res != null)
						manager.add(new DependencyExtentAction(res.getProject(), id));
				}
			}
		}
		// Unused Dependencies Action, only for worskpace plug-ins
		ISharedMonitorModel model = null;
		if (selectionElement instanceof BundleSpecification) {
			model = MonitorRegistry.findModel(((BundleSpecification) selectionElement).getName());
		} else if (selectionElement instanceof BundleDescription) {
			model = MonitorRegistry.findModel((BundleDescription) selectionElement);
		} else if (selectionElement instanceof IMonitorBase) {
			// root
			model = ((IMonitorBase) selectionElement).getModel();
		}
		if (model != null && model.getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction((IMonitorModelBase) model, true));
		}
		if (enableRename(selection)) {
			manager.add(new Separator());
			manager.add(fRefactorAction);
		}

		manager.add(new Separator());
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return fViewer.getControl();
	}

	private Object getSelectedObject() {
		IStructuredSelection selection = getSelection();
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		return selection.getFirstElement();
	}

	protected IStructuredSelection getSelection() {
		return (IStructuredSelection) fViewer.getSelection();
	}

	protected void setSelection(IStructuredSelection selection) {
		if (selection != null && !selection.isEmpty())
			fViewer.setSelection(selection, true);
	}

	/**
	 * @return Returns the view.
	 */
	public DependenciesView getView() {
		return fView;
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		BundleDescription desc = null;
		if (obj instanceof BundleSpecification) {
			desc = (BundleDescription) ((BundleSpecification) obj).getSupplier();
		} else if (obj instanceof BundleDescription) {
			desc = (BundleDescription) obj;

		} else if (obj instanceof IMonitorBase) {
			// root object
			desc = ((IMonitorModelBase) ((IMonitorBase) obj).getModel()).getBundleDescription();
		} else if (obj instanceof ImportPackageSpecification) {
			BaseDescription export = ((ImportPackageSpecification) obj).getSupplier();
			desc = ((ExportPackageDescription) export).getExporter();
		}
		if (desc != null)
			ManifestEditor.openPluginEditor(desc);
	}

	private void handleFocusOn() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(fViewer.getControl().getShell(), true, false);
		dialog.create();
		if (dialog.open() == Window.OK) {
			handleFocusOn(dialog.getFirstResult());
		}
	}

	private void handleFocusOn(Object newFocus) {
		if (newFocus instanceof IMonitorModelBase) {
			fView.openTo(newFocus);
		}
		if (newFocus instanceof IMonitorBase) {
			fView.openTo(((IMonitorBase) newFocus).getModel());
		}
		if (newFocus instanceof IMonitorImport) {
			IMonitorImport pluginImport = ((IMonitorImport) newFocus);
			String id = pluginImport.getId();
			IMonitorModelBase model = MonitorRegistry.findModel(id);
			if (model != null) {
				fView.openTo(model);
			} else {
				fView.openTo(null);
			}
		}
		BundleDescription desc = null;
		if (newFocus instanceof BundleSpecification) {
			desc = (BundleDescription) ((BundleSpecification) newFocus).getSupplier();
			if (desc == null)
				fView.openTo(null);
		}
		if (newFocus instanceof BundleDescription) {
			desc = (BundleDescription) newFocus;
		}
		if (desc != null)
			fView.openTo(MonitorRegistry.findModel(desc.getSymbolicName()));
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DependenciesViewPage.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(fView.getSite().getId(), menuMgr, fViewer);
	}

	private void hookDoubleClickAction() {
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
	}

	private void makeActions() {
		fOpenAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(MDEUIMessages.DependenciesView_open);

		fFocusOnSelectionAction = new FocusOnSelectionAction();

		fFocusOnAction = new Action() {
			public void run() {
				handleFocusOn();
			}
		};
		fFocusOnAction.setText(MDEUIMessages.DependenciesViewPage_focusOn);
		fFocusOnAction.setImageDescriptor(MDEPluginImages.DESC_FOCUS_ON);

		fRefactorAction = RefactoringActionFactory.createRefactorPluginIdAction();

		fHideFragmentFilterAction = new Action() {
			public void run() {
				boolean checked = fHideFragmentFilterAction.isChecked();
				if (checked)
					fViewer.removeFilter(fHideFragmentFilter);
				else
					fViewer.addFilter(fHideFragmentFilter);
				getSettings().put(HIDE_FRAGMENTS, !checked);
			}
		};
		fHideFragmentFilterAction.setText(MDEUIMessages.DependenciesViewPage_showFragments);

		fHideOptionalFilterAction = new Action() {
			public void run() {
				boolean checked = isChecked();
				handleShowOptional(isChecked(), true);
				getSettings().put(HIDE_OPTIONAL, !checked);
			}
		};
		fHideOptionalFilterAction.setText(MDEUIMessages.DependenciesViewPage_showOptional);
	}

	protected abstract void handleShowOptional(boolean checked, boolean refreshIfNecessary);

	protected abstract boolean isShowingOptional();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.Page#makeContributions(org.eclipse.jface.action.IMenuManager,
	 *      org.eclipse.jface.action.IToolBarManager,
	 *      org.eclipse.jface.action.IStatusLineManager)
	 */
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars(getSite().getActionBars());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		if (fViewer != null) {
			Control c = fViewer.getControl();
			if (!c.isFocusControl()) {
				c.setFocus();
			}
		}
	}

	public void setInput(Object object) {
		if (object != fViewer.getInput())
			fViewer.setInput(object);
	}

	// returns true if Rename Action is valid.
	protected boolean enableRename(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object selectionElement = selection.getFirstElement();
			IMonitorModelBase base = null;
			if (selectionElement instanceof IMonitorImport) {
				String id = ((IMonitorImport) selectionElement).getId();
				base = MonitorRegistry.findModel(id);
			} else if (selectionElement instanceof IMonitorObject) {
				base = (IMonitorModelBase) ((IMonitorObject) selectionElement).getModel();
			} else if (selectionElement instanceof BundleSpecification) {
				BundleDescription desc = (BundleDescription) ((BundleSpecification) selectionElement).getSupplier();
				if (desc != null)
					base = MonitorRegistry.findModel(desc);
			} else if (selectionElement instanceof BundleDescription) {
				base = MonitorRegistry.findModel((BundleDescription) selectionElement);
			}
			if (base != null && base.getUnderlyingResource() != null) {
				fRefactorAction.setSelection(base);
				return true;
			}
		}
		return false;
	}

	public void setActive(boolean active) {
		if (active) {
			// update filter actions before updating filters because the filters depend on the state of the filter actions
			// update filter actions - both filter actions are specific to each Page instance
			if (fView.isShowingCallers()) {
				// deactive show optional on Callers view.  
				fHideOptionalFilterAction.setChecked(true);
				fHideOptionalFilterAction.setEnabled(false);
			} else {
				fHideOptionalFilterAction.setEnabled(true);
				fHideOptionalFilterAction.setChecked(!getSettings().getBoolean(HIDE_OPTIONAL));
			}
			fHideFragmentFilterAction.setChecked(!getSettings().getBoolean(HIDE_FRAGMENTS));

			// update viewer's fragment filter
			boolean showFragments = fHideFragmentFilterAction.isChecked();
			boolean containsFragments = true;
			ViewerFilter[] filters = fViewer.getFilters();
			for (int i = 0; i < filters.length; i++) {
				if (filters[i].equals(fHideFragmentFilter)) {
					containsFragments = false;
					break;
				}
			}
			if (showFragments != containsFragments)
				if (showFragments)
					fViewer.removeFilter(fHideFragmentFilter);
				else
					fViewer.addFilter(fHideFragmentFilter);

			// update viewer's optional filtering
			if (fHideOptionalFilterAction.isChecked() != isShowingOptional())
				handleShowOptional(fHideOptionalFilterAction.isChecked(), false);
		}

		if (fContentProvider instanceof DependenciesViewPageContentProvider) {
			if (active) {
				// when a page is activated, we need to have the content provider listen for changes and refresh the view to get current data
				((DependenciesViewPageContentProvider) fContentProvider).attachModelListener();
				fViewer.refresh();
			} else
				// when page is deactivated, we need to remove model listener from content manager.  Otherwise model changes will be sent to all 
				// DependenciesViewPageContentProvider (including inactive ones).  This will cause problems with the content provider's logic!!
				((DependenciesViewPageContentProvider) fContentProvider).removeModelListener();
		}
	}

	private void contributeToActionBars(IActionBars actionBars) {
		IToolBarManager manager = actionBars.getToolBarManager();
		manager.add(fFocusOnAction);
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		manager.add(fHideFragmentFilterAction);
		manager.add(fHideOptionalFilterAction);
		IDialogSettings settings = getSettings();
		boolean hideFragments = settings.getBoolean(HIDE_FRAGMENTS);
		boolean hideOptional = settings.getBoolean(HIDE_OPTIONAL);
		fHideFragmentFilterAction.setChecked(!hideFragments);
		fHideOptionalFilterAction.setChecked(!hideOptional);
		// The filtering will be executed in the setActive function when the viewer is displayed
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = MDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("dependenciesView"); //$NON-NLS-1$
		if (section == null) {
			section = master.addNewSection("dependenciesView"); //$NON-NLS-1$
		}
		return section;
	}

}
