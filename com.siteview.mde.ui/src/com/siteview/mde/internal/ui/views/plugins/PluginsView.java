/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 191365
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.views.plugins;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.core.monitor.*;

import java.io.*;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.JarEntryEditorInput;
import com.siteview.mde.internal.ui.refactoring.PDERefactoringAction;
import com.siteview.mde.internal.ui.refactoring.RefactoringActionFactory;
import com.siteview.mde.internal.ui.util.SourcePluginFilter;
import com.siteview.mde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import com.siteview.mde.internal.ui.views.dependencies.OpenPluginReferencesAction;
import com.siteview.mde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.*;
import org.eclipse.ui.progress.*;

public class PluginsView extends ViewPart implements IMonitorModelListener {

	private static final String DEFAULT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private static final String HIDE_WRKSPC = "hideWorkspace"; //$NON-NLS-1$
	private static final String HIDE_EXENABLED = "hideEnabledExternal"; //$NON-NLS-1$
	private static final String SHOW_EXDISABLED = "showDisabledExternal"; //$NON-NLS-1$
	private TreeViewer fTreeViewer;
	private DrillDownAdapter fDrillDownAdapter;
	private IPropertyChangeListener fPropertyListener;
	private Action fOpenAction;
	private Action fHideExtDisabledFilterAction;
	private Action fHideExtEnabledFilterAction;
	private Action fHideWorkspaceFilterAction;
	private Action fOpenManifestAction;
	private Action fOpenSchemaAction;
	private Action fOpenSystemEditorAction;
	private Action fOpenClassFileAction;
	private Action fOpenTextEditorAction;
	private Action fSelectDependentAction;
	private Action fSelectInJavaSearchAction;
	private Action fSelectAllAction;
	private PDERefactoringAction fRefactorAction;
	private CollapseAllAction fCollapseAllAction;
	private DisabledFilter fHideExtEnabledFilter = new DisabledFilter(true);
	private DisabledFilter fHideExtDisabledFilter = new DisabledFilter(false);
	private WorkspaceFilter fHideWorkspaceFilter = new WorkspaceFilter();
	private SourcePluginFilter fSourcePluginFilter = new SourcePluginFilter();
	private JavaFilter fJavaFilter = new JavaFilter();
	private CopyToClipboardAction fCopyAction;
	private Clipboard fClipboard;
	private Object fRoot = null;

	class DisabledFilter extends ViewerFilter {

		boolean fEnabled;

		DisabledFilter(boolean enabled) {
			fEnabled = enabled;
		}

		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IMonitorModelBase) {
				IMonitorModelBase model = (IMonitorModelBase) element;
				return model.getUnderlyingResource() != null || model.isEnabled() != fEnabled;
			}
			return true;
		}
	}

	class WorkspaceFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IMonitorModelBase) {
				IMonitorModelBase model = (IMonitorModelBase) element;
				return model.getUnderlyingResource() == null;
			}
			return true;
		}
	}

	class JavaFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IPackageFragment) {
				IPackageFragment packageFragment = (IPackageFragment) element;
				try {
					return packageFragment.hasChildren();
				} catch (JavaModelException e) {
					return false;
				}
			}
			return true;
		}
	}

	class CollapseAllAction extends Action {
		public CollapseAllAction() {
			setText(MDEUIMessages.PluginsView_CollapseAllAction_label);
			setDescription(MDEUIMessages.PluginsView_CollapseAllAction_description);
			setToolTipText(MDEUIMessages.PluginsView_CollapseAllAction_tooltip);
			setImageDescriptor(MDEPluginImages.DESC_COLLAPSE_ALL);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			fTreeViewer.collapseAll();
		}
	}

	/**
	 * Constructor for PluginsView.
	 */
	public PluginsView() {
		fPropertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
					fTreeViewer.refresh();
				}
			}
		};
	}

	public void dispose() {
		MDECore.getDefault().getModelManager().removePluginModelListener(this);
		MDECore.getDefault().getSearchablePluginsManager().removePluginModelListener(this);
		MDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		fDrillDownAdapter = new DrillDownAdapter(fTreeViewer);
		fTreeViewer.setContentProvider(new PluginsContentProvider(this));
		fTreeViewer.setLabelProvider(new PluginsLabelProvider());
		// need custom comparator so that way PendingUpdateAdapter is at the top.  Using regular PluginComparator the PendingUpdateAdapter
		// will be sorted to the bottom.  When it is removed after the table is initialized, the focus will go to the last item in the table (bug 216339)
		fTreeViewer.setComparator(new ListUtil.PluginComparator() {

			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof PendingUpdateAdapter)
					return -1;
				else if (e2 instanceof PendingUpdateAdapter)
					return 1;
				return super.compare(viewer, e1, e2);
			}

		});
		initDragAndDrop();
		makeActions();
		initFilters();
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);
		registerGlobalActions(actionBars);
		hookContextMenu();
		hookDoubleClickAction();
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());
			}
		});
		MDECore.getDefault().getSearchablePluginsManager().addPluginModelListener(this);
		fTreeViewer.setInput(fRoot = getDeferredTreeRoot());

		MDECore.getDefault().getModelManager().addPluginModelListener(this);
		MDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getViewSite().setSelectionProvider(fTreeViewer);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(fTreeViewer.getControl(), IHelpContextIds.PLUGINS_VIEW);
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		manager.add(fHideWorkspaceFilterAction);
		manager.add(fHideExtEnabledFilterAction);
		manager.add(fHideExtDisabledFilterAction);
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		manager.add(fCollapseAllAction);
	}

	private void registerGlobalActions(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
		actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
				if (selection.size() == 1) {
					Object element = selection.getFirstElement();
					if (element instanceof IMonitorModelBase) {
						fRefactorAction.setSelection(element);
						fRefactorAction.run();
						return;
					}
				}
				Display.getDefault().beep();
			}
		});
	}

	private void makeActions() {
		fClipboard = new Clipboard(fTreeViewer.getTree().getDisplay());
		fOpenAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(MDEUIMessages.PluginsView_open);

		fHideExtDisabledFilterAction = new Action() {
			public void run() {
				boolean checked = fHideExtDisabledFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideExtDisabledFilter);
				else
					fTreeViewer.addFilter(fHideExtDisabledFilter);
				getSettings().put(SHOW_EXDISABLED, checked);
				updateContentDescription();
			}
		};
		fHideExtDisabledFilterAction.setText(MDEUIMessages.PluginsView_showDisabled);

		fHideExtEnabledFilterAction = new Action() {
			public void run() {
				boolean checked = fHideExtEnabledFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideExtEnabledFilter);
				else
					fTreeViewer.addFilter(fHideExtEnabledFilter);
				getSettings().put(HIDE_EXENABLED, !checked);
				updateContentDescription();
			}
		};
		fHideExtEnabledFilterAction.setText(MDEUIMessages.PluginsView_showEnabled);

		fHideWorkspaceFilterAction = new Action() {
			public void run() {
				boolean checked = fHideWorkspaceFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideWorkspaceFilter);
				else
					fTreeViewer.addFilter(fHideWorkspaceFilter);
				getSettings().put(HIDE_WRKSPC, !checked);
				updateContentDescription();
			}
		};
		fHideWorkspaceFilterAction.setText(MDEUIMessages.PluginsView_showWorkspace);

		fOpenTextEditorAction = new Action() {
			public void run() {
				handleOpenTextEditor(getSelectedFile(), null);
			}
		};
		fOpenTextEditorAction.setText(MDEUIMessages.PluginsView_textEditor);
		fOpenTextEditorAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		fOpenSystemEditorAction = new Action() {
			public void run() {
				handleOpenSystemEditor(getSelectedFile());
			}
		};
		fOpenSystemEditorAction.setText(MDEUIMessages.PluginsView_systemEditor);

		fOpenManifestAction = new Action() {
			public void run() {
				handleOpenManifestEditor(getSelectedFile());
			}
		};
		fOpenManifestAction.setText(MDEUIMessages.PluginsView_manifestEditor);

		fOpenSchemaAction = new Action() {
			public void run() {
				handleOpenSchemaEditor(getSelectedFile());
			}
		};
		fOpenSchemaAction.setText(MDEUIMessages.PluginsView_schemaEditor);

		fCopyAction = new CopyToClipboardAction(fClipboard);
		fCopyAction.setText(MDEUIMessages.PluginsView_copy);

		fSelectDependentAction = new Action() {
			public void run() {
				handleSelectDependencies();
			}
		};
		fSelectDependentAction.setText(MDEUIMessages.PluginsView_dependentPlugins);

		fSelectInJavaSearchAction = new Action() {
			public void run() {
				handleSelectInJavaSearch();
			}
		};
		fSelectInJavaSearchAction.setText(MDEUIMessages.PluginsView_pluginsInJavaSearch);

		fSelectAllAction = new Action() {
			public void run() {
				super.run();
				fTreeViewer.getTree().selectAll();
			}
		};
		fSelectAllAction.setText(MDEUIMessages.PluginsView_SelectAllAction_label);

		fCollapseAllAction = new CollapseAllAction();

		fOpenClassFileAction = new OpenAction(getViewSite());

		fRefactorAction = RefactoringActionFactory.createRefactorPluginIdAction();
	}

	private FileAdapter getSelectedFile() {
		Object obj = getSelectedObject();
		if (obj instanceof FileAdapter)
			return (FileAdapter) obj;
		return null;
	}

	private IMonitorModelBase getEnclosingModel() {
		Object obj = getSelectedObject();
		if (obj == null)
			return null;
		if (obj instanceof IMonitorModelBase)
			return (IMonitorModelBase) obj;
		if (obj instanceof FileAdapter) {
			FileAdapter file = (FileAdapter) obj;
			if (file.isManifest()) {
				FileAdapter parent = file.getParent();
				if (parent instanceof ModelFileAdapter)
					return ((ModelFileAdapter) parent).getModel();
			}
		}
		return null;
	}

	private Object getSelectedObject() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		return selection.getFirstElement();
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();

		boolean allowRefactoring = false;
		if (selection.size() == 1) {
			Object sobj = selection.getFirstElement();
			boolean addSeparator = false;
			if (sobj instanceof IMonitorModelBase) {
				IMonitorModelBase model = (IMonitorModelBase) sobj;
				File file = new File(model.getInstallLocation());
				if (file.isFile() || model.getUnderlyingResource() != null) {
					manager.add(fOpenAction);
				}
				if (model.getUnderlyingResource() != null)
					allowRefactoring = true;
			}
			if (sobj instanceof FileAdapter && ((FileAdapter) sobj).isDirectory() == false) {
				manager.add(fOpenAction);
				MenuManager openWithMenu = new MenuManager(MDEUIMessages.PluginsView_openWith);
				fillOpenWithMenu(openWithMenu, sobj);
				manager.add(openWithMenu);
				addSeparator = true;
			}
			if (isOpenableStorage(sobj)) {
				manager.add(fOpenAction);
				addSeparator = true;
			}
			if (sobj instanceof IClassFile) {
				manager.add(fOpenClassFileAction);
				addSeparator = true;
			}
			IMonitorModelBase entry = getEnclosingModel();
			if (entry != null) {
				Action action = new OpenPluginDependenciesAction(entry);
				action.setText(MDEUIMessages.PluginsView_openDependencies);
				action.setImageDescriptor(MDEPluginImages.DESC_CALLEES);
				manager.add(action);
				manager.add(new Separator());

				action = new OpenPluginReferencesAction(entry);
				action.setText(MDEUIMessages.SearchAction_references);
				action.setImageDescriptor(MDEPluginImages.DESC_CALLERS);
				manager.add(action);
				addSeparator = true;
			}
			if (addSeparator)
				manager.add(new Separator());
		}
		if (selection.size() > 0) {
			if (isShowInApplicable()) {
				String showInLabel = MDEUIMessages.PluginsView_showIn;
				IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
				if (bindingService != null) {
					String keyBinding = bindingService.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
					if (keyBinding != null) {
						showInLabel += '\t' + keyBinding;
					}
				}
				IMenuManager showInMenu = new MenuManager(showInLabel);
				showInMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(getViewSite().getWorkbenchWindow()));

				manager.add(showInMenu);
				manager.add(new Separator());
			}
			if (ImportActionGroup.canImport(selection)) {
				ImportActionGroup actionGroup = new ImportActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}

			JavaSearchActionGroup actionGroup = new JavaSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		}
		fCopyAction.setSelection(selection);
		manager.add(fCopyAction);
		IMenuManager selectionMenu = new MenuManager(MDEUIMessages.PluginsView_select);
		manager.add(selectionMenu);
		if (selection.size() > 0)
			selectionMenu.add(fSelectDependentAction);
		selectionMenu.add(fSelectInJavaSearchAction);
		selectionMenu.add(fSelectAllAction);
		manager.add(new Separator());
		if (allowRefactoring) {
			fRefactorAction.setSelection(selection.getFirstElement());
			manager.add(fRefactorAction);
			manager.add(new Separator());
		}
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public boolean isShowInApplicable() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (selection.isEmpty())
			return false;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof IMonitorModelBase))
				return false;
			if (((IMonitorModelBase) obj).getUnderlyingResource() == null)
				return false;
		}
		return true;
	}

	private void fillOpenWithMenu(IMenuManager manager, Object obj) {
		FileAdapter adapter = (FileAdapter) obj;
		String editorId = adapter.getEditorId();

		String fileName = adapter.getFile().getName();
		String lcFileName = fileName.toLowerCase(Locale.ENGLISH);
		ImageDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(fileName);
		if (lcFileName.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || lcFileName.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) || lcFileName.equals(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			fOpenManifestAction.setImageDescriptor(desc);
			manager.add(fOpenManifestAction);
			manager.add(new Separator());
			fOpenManifestAction.setChecked(editorId != null && editorId.equals(IMDEUIConstants.MANIFEST_EDITOR_ID));
		}
		if (lcFileName.endsWith(".mxsd") || lcFileName.endsWith(".exsd")) { //$NON-NLS-1$ //$NON-NLS-2$
			fOpenSchemaAction.setImageDescriptor(desc);
			manager.add(fOpenSchemaAction);
			manager.add(new Separator());
			fOpenSchemaAction.setChecked(editorId != null && editorId.equals(IMDEUIConstants.SCHEMA_EDITOR_ID));
		}
		manager.add(fOpenTextEditorAction);
		fOpenTextEditorAction.setChecked(editorId == null || editorId.equals(DEFAULT_EDITOR_ID));
		fOpenSystemEditorAction.setImageDescriptor(desc);
		fOpenSystemEditorAction.setChecked(editorId != null && editorId.equals("@system")); //$NON-NLS-1$
		manager.add(fOpenSystemEditorAction);
	}

	protected void initDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {FileTransfer.getInstance()};
		fTreeViewer.addDragSupport(ops, transfers, new PluginsDragAdapter(fTreeViewer));
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = MDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("pluginsView"); //$NON-NLS-1$
		if (section == null) {
			section = master.addNewSection("pluginsView"); //$NON-NLS-1$
		}
		return section;
	}

	private void initFilters() {
		IDialogSettings settings = getSettings();
		fTreeViewer.addFilter(fJavaFilter);
		boolean hideWorkspace = settings.getBoolean(HIDE_WRKSPC);
		boolean hideEnabledExternal = settings.getBoolean(HIDE_EXENABLED);
		boolean hideDisabledExternal = !settings.getBoolean(SHOW_EXDISABLED);
		if (hideWorkspace)
			fTreeViewer.addFilter(fHideWorkspaceFilter);
		if (hideEnabledExternal)
			fTreeViewer.addFilter(fHideExtEnabledFilter);
		if (hideDisabledExternal)
			fTreeViewer.addFilter(fHideExtDisabledFilter);
		fHideWorkspaceFilterAction.setChecked(!hideWorkspace);
		fHideExtEnabledFilterAction.setChecked(!hideEnabledExternal);
		fHideExtDisabledFilterAction.setChecked(!hideDisabledExternal);
		fTreeViewer.addFilter(fSourcePluginFilter);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PluginsView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fTreeViewer.getControl());
		fTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fTreeViewer);
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		if (obj instanceof IMonitorModelBase) {
			boolean expanded = false;
			// only expand target models
			if (((IMonitorModelBase) obj).getUnderlyingResource() == null) {
				expanded = fTreeViewer.getExpandedState(obj);
				fTreeViewer.setExpandedState(obj, !expanded);
			}
			if (fTreeViewer.getExpandedState(obj) == expanded) {
				// not expandable, open editor
				ManifestEditor.openPluginEditor((IMonitorModelBase) obj);
			}
		} else if (obj instanceof FileAdapter) {
			FileAdapter adapter = (FileAdapter) obj;
			if (adapter.isDirectory()) {
				fTreeViewer.setExpandedState(adapter, !fTreeViewer.getExpandedState(adapter));
				return;
			}
			String editorId = adapter.getEditorId();
			if (editorId != null && editorId.equals("@system")) //$NON-NLS-1$
				handleOpenSystemEditor(adapter);
			else
				handleOpenTextEditor(adapter, editorId);
		} else if (obj instanceof IClassFile) {
			fOpenClassFileAction.run();
		} else if (isOpenableStorage(obj)) {
			handleOpenStorage((IStorage) obj);
		}
	}

	private static boolean isOpenableStorage(Object storage) {
		if (storage instanceof IJarEntryResource)
			return ((IJarEntryResource) storage).isFile();
		return storage instanceof IStorage;
	}

	private void handleOpenStorage(IStorage obj) {
		IWorkbenchPage page = MDEPlugin.getActivePage();
		IEditorInput input = new JarEntryEditorInput(obj);
		try {
			page.openEditor(input, DEFAULT_EDITOR_ID);
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
	}

	private void handleSelectDependencies() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (selection.size() == 0)
			return;

		IMonitorModelBase[] models = new IMonitorModelBase[selection.size()];
		System.arraycopy(selection.toArray(), 0, models, 0, selection.size());
		// exclude "org.eclipse.ui.workbench.compatibility" - it is only needed for pre-3.0 bundles
		Set set = DependencyManager.getSelfandDependencies(models, new String[] {"org.eclipse.ui.workbench.compatibility"}); //$NON-NLS-1$
		Object[] symbolicNames = set.toArray();
		ArrayList result = new ArrayList(set.size());
		for (int i = 0; i < symbolicNames.length; i++) {
			IMonitorModelBase model = MonitorRegistry.findModel(symbolicNames[i].toString());
			if (model != null)
				result.add(model);
		}
		fTreeViewer.setSelection(new StructuredSelection(result.toArray()));
	}

	private void handleSelectInJavaSearch() {
		PluginsContentProvider provider = (PluginsContentProvider) fTreeViewer.getContentProvider();
		Object[] elements = provider.getElements(fTreeViewer.getInput());
		ArrayList result = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			if (element instanceof IMonitorModelBase) {
				String id = ((IMonitorModelBase) element).getMonitorBase().getId();
				if (MDECore.getDefault().getSearchablePluginsManager().isInJavaSearch(id))
					result.add(element);
			}
		}
		fTreeViewer.setSelection(new StructuredSelection(result.toArray()));
	}

	private void handleOpenTextEditor(FileAdapter adapter, String editorId) {
		if (adapter == null)
			return;
		IWorkbenchPage page = MDEPlugin.getActivePage();
		if (editorId == null) {
			if (adapter.isManifest())
				editorId = IMDEUIConstants.MANIFEST_EDITOR_ID;
			else if (adapter.isSchema())
				editorId = IMDEUIConstants.SCHEMA_EDITOR_ID;
		}
		try {
			if (editorId == null || editorId.equals("@system")) //$NON-NLS-1$
				editorId = DEFAULT_EDITOR_ID;
			IFileStore store = EFS.getStore(adapter.getFile().toURI());
			IEditorInput in = new FileStoreEditorInput(store);
			page.openEditor(in, editorId);
			adapter.setEditorId(editorId);
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void handleOpenManifestEditor(FileAdapter adapter) {
		handleOpenTextEditor(adapter, IMDEUIConstants.MANIFEST_EDITOR_ID);
	}

	private void handleOpenSchemaEditor(FileAdapter adapter) {
		handleOpenTextEditor(adapter, IMDEUIConstants.SCHEMA_EDITOR_ID);
	}

	private void handleOpenSystemEditor(FileAdapter adapter) {
		if (adapter == null)
			return;
		File localFile = null;

		try {
			localFile = getLocalCopy(adapter.getFile());
		} catch (IOException e) {
			MDEPlugin.logException(e);
			return;
		}
		// Start busy indicator.
		final File file = localFile;
		final boolean result[] = new boolean[1];
		BusyIndicator.showWhile(fTreeViewer.getTree().getDisplay(), new Runnable() {
			public void run() {
				// Open file using shell.
				String path = file.getAbsolutePath();
				result[0] = Program.launch(path);
			}
		});

		// ShellExecute returns whether call was successful
		if (!result[0]) {
			MDEPlugin.logException(new PartInitException(NLS.bind(MDEUIMessages.PluginsView_unableToOpen, file.getName())));
		} else {
			adapter.setEditorId("@system"); //$NON-NLS-1$
		}
	}

	private File getLocalCopy(File file) throws IOException {
		// create a tmp. copy of this file and make it
		// read-only. This is to ensure that the original
		// file belonging to the external plug-in directories
		// will not be modified. 
		String fileName = file.getName();
		String prefix;
		String suffix = null;
		int dotLoc = fileName.indexOf('.');
		if (dotLoc != -1) {
			prefix = fileName.substring(0, dotLoc);
			suffix = fileName.substring(dotLoc);
		} else {
			prefix = fileName;
		}

		File tmpFile = File.createTempFile(prefix, suffix);
		tmpFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(tmpFile);
		FileInputStream fis = new FileInputStream(file);
		byte[] cbuffer = new byte[1024];
		int read = 0;

		while (read != -1) {
			read = fis.read(cbuffer);
			if (read != -1)
				fos.write(cbuffer, 0, read);
		}
		fos.flush();
		fos.close();
		fis.close();
		tmpFile.setReadOnly();
		return tmpFile;
	}

	private void handleSelectionChanged(ISelection selection) {
		String text = ""; //$NON-NLS-1$
		Object obj = getSelectedObject();
		if (obj instanceof IMonitorModelBase) {
			text = ((IMonitorModelBase) obj).getInstallLocation();
		}
		if (obj instanceof FileAdapter) {
			text = ((FileAdapter) obj).getFile().getAbsolutePath();
		}
		getViewSite().getActionBars().getStatusLineManager().setMessage(text);
	}

	private void hookDoubleClickAction() {
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fTreeViewer.getTree().setFocus();
	}

	void updateTitle(Object newInput) {
		IConfigurationElement config = getConfigurationElement();
		if (config == null)
			return;

		if (newInput == null || newInput.equals(MDECore.getDefault().getModelManager())) {
			updateContentDescription();
			setTitleToolTip(getTitle());
		} else {
			setTitleToolTip(getInputPath(newInput));
		}
	}

	private String getInputPath(Object input) {
		if (input instanceof FileAdapter) {
			return "file: " + ((FileAdapter) input).getFile().getAbsolutePath(); //$NON-NLS-1$
		}
		if (input instanceof IMonitorModelBase) {
			IMonitorModelBase model = (IMonitorModelBase) input;
			return "plugin: " + model.getInstallLocation(); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	protected void updateContentDescription() {
		String total = null;
		String visible = null;

		MonitorModelManager manager = MDECore.getDefault().getModelManager();
		if (manager.isInitialized()) {
			// Only show the correct values if the PDE is already initialized
			// (N.B. PluginRegistry.getAllModels() would call the init if allowed to execute)
			total = Integer.toString(MonitorRegistry.getAllModels().length);
			visible = Integer.toString(fTreeViewer.getTree().getItemCount());
		} else {
			// defaults to be shown if the PDE isn't initialized
			total = MDEUIMessages.PluginsView_TotalPlugins_unknown;
			visible = "0"; //$NON-NLS-1$
		}
		setContentDescription(NLS.bind(MDEUIMessages.PluginsView_description, visible, total));
	}

	public void modelsChanged(final MonitorModelDelta delta) {
		if (fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			return;

		fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (fTreeViewer.getTree().isDisposed())
					return;
				if ((kind & MonitorModelDelta.CHANGED) != 0 || (kind & MonitorModelDelta.REMOVED) != 0) {
					// Don't know exactly what change - 
					// the safest way out is to refresh
					fTreeViewer.refresh();
				} else if ((kind & MonitorModelDelta.ADDED) != 0) {
					ModelEntry[] added = delta.getAddedEntries();
					for (int i = 0; i < added.length; i++) {
						IMonitorModelBase[] models = getModels(added[i]);
						for (int j = 0; j < models.length; j++) {
							if (isVisible(models[j]))
								fTreeViewer.add(fRoot, models[j]);
						}
					}
				}
				updateContentDescription();
			}
		});
	}

	private IMonitorModelBase[] getModels(ModelEntry entry) {
		return (entry.hasWorkspaceModels()) ? entry.getWorkspaceModels() : entry.getExternalModels();
	}

	private boolean isVisible(IMonitorModelBase entry) {
		ViewerFilter[] filters = fTreeViewer.getFilters();
		for (int i = 0; i < filters.length; i++) {
			if (!filters[i].select(fTreeViewer, fRoot, entry))
				return false;
		}
		return true;
	}

	public Object getAdapter(Class adapter) {
		if (isShowInApplicable()) {
			if (adapter == IShowInSource.class && isShowInApplicable()) {
				return getShowInSource();
			} else if (adapter == IShowInTargetList.class) {
				return getShowInTargetList();
			}
		}

		return super.getAdapter(adapter);
	}

	/**
	 * Returns the <code>IShowInSource</code> for this view.
	 * @return the <code>IShowInSource</code> 
	 */
	protected IShowInSource getShowInSource() {
		return new IShowInSource() {
			public ShowInContext getShowInContext() {
				ArrayList resourceList = new ArrayList();
				IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
				IStructuredSelection resources;
				if (selection.isEmpty()) {
					resources = null;
				} else {
					for (Iterator iter = selection.iterator(); iter.hasNext();) {
						Object obj = iter.next();
						if (obj instanceof IMonitorModelBase) {
							resourceList.add(((IMonitorModelBase) obj).getUnderlyingResource());
						}
					}
					resources = new StructuredSelection(resourceList);
				}

				return new ShowInContext(fTreeViewer.getInput(), resources);
			}
		};
	}

	/**
	 * Returns the <code>IShowInTargetList</code> for this view.
	 * @return the <code>IShowInTargetList</code> 
	 */
	protected IShowInTargetList getShowInTargetList() {
		return new IShowInTargetList() {
			public String[] getShowInTargetIds() {
				return new String[] {JavaUI.ID_PACKAGES, IPageLayout.ID_PROJECT_EXPLORER};
			}
		};
	}

	/*
	 * Returns an IDeferredWorkbenchAdapater which will be used to load this view in the background if the ModelManager is not fully initialized
	 */
	private IDeferredWorkbenchAdapter getDeferredTreeRoot() {
		return new IDeferredWorkbenchAdapter() {

			public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
				Object[] bases = getChildren(object);
				collector.add(bases, monitor);
				monitor.done();
			}

			public ISchedulingRule getRule(Object object) {
				return null;
			}

			public boolean isContainer() {
				return true;
			}

			public Object[] getChildren(Object o) {
				return MDECore.getDefault().getModelManager().getAllModels();
			}

			public ImageDescriptor getImageDescriptor(Object object) {
				return null;
			}

			public String getLabel(Object o) {
				return MDEUIMessages.PluginsView_deferredLabel0;
			}

			public Object getParent(Object o) {
				return null;
			}

		};
	}
}
