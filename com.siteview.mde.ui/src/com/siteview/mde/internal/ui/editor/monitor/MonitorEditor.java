/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.core.IIdentifiable;
import com.siteview.mde.core.build.*;
import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.build.IBuildObject;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelProvider;
import com.siteview.mde.internal.core.monitor.*;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.core.util.CoreUtility;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.build.*;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.editor.context.InputContextManager;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.osgi.service.prefs.BackingStoreException;

public class MonitorEditor extends MDELauncherFormEditor implements IShowEditorInput {

	private static int BUILD_INDEX = 5;
	private static boolean SHOW_SOURCE;
	private boolean fEquinox = true;
	private boolean fShowExtensions = true;
	private IEclipsePreferences fPrefs;
	private PluginExportAction fExportAction;
	private ILauncherFormPageHelper fLauncherHelper;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.MDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IMDEUIConstants.MONITOR_EDITOR_ID;
	}

	public static IEditorPart openMonitorEditor(String id) {
		return openMonitorEditor(MonitorRegistry.findModel(id));
	}

	public static IEditorPart openPluginEditor(BundleDescription bd) {
		return openMonitorEditor(MonitorRegistry.findModel(bd));
	}

	public static IEditorPart openMonitorEditor(IMonitorModelBase model) {
		if (model == null) {
			Display.getDefault().beep();
			return null;
		}
		return openMonitorEditor(model, false);
	}

	public static IEditorPart openMonitorEditor(IMonitorModelBase model, boolean source) {
		return open(model.getMonitorBase(), source);
	}

	public static IEditorPart open(Object object, boolean source) {
		SHOW_SOURCE = source;
		if (object instanceof IMonitorObject) {
			ISharedMonitorModel model = ((IMonitorObject) object).getModel();
			if (model instanceof IBundlePluginModelProvider)
				model = ((IBundlePluginModelProvider) model).getBundlePluginModel();
			if (model instanceof IMonitorModelBase) {
				String filename = ((IMonitorModelBase) model).isFragmentModel() ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
				if (!(object instanceof IMonitorExtension) && !(object instanceof IMonitorExtensionPoint)) {
					String installLocation = model.getInstallLocation();
					if (installLocation == null)
						return null;
					File file = new File(installLocation);
					if (file.isFile()) {
						if (CoreUtility.jarContainsResource(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR, false)) {
							filename = ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR;
						}
					} else if (new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR).exists()) {
						filename = ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR;
					}
				}
				IResource resource = model.getUnderlyingResource();
				if (resource == null)
					return openExternalPlugin(new File(model.getInstallLocation()), filename);
				return openWorkspacePlugin(resource.getProject().getFile(filename));
			}
		}
		if (object instanceof BaseDescription) {
			BundleDescription desc = ((BaseDescription) object).getSupplier();
			String id = desc.getSymbolicName();
			String version = desc.getVersion().toString();

			ModelEntry entry = MonitorRegistry.findEntry(id);
			IMonitorModelBase[] models = entry.getActiveModels();
			for (int i = 0; i < models.length; i++) {
				IMonitorModelBase model = models[i];
				if (version.equals(model.getMonitorBase().getVersion()))
					return open(model.getMonitorBase(), true);
			}
		}
		return null;
	}

	private static IEditorPart openWorkspacePlugin(IFile pluginFile) {
		return openEditor(new FileEditorInput(pluginFile));
	}

	private static IEditorPart openExternalPlugin(File location, String filename) {
		IEditorInput input = null;
		if (location.isFile()) {
			try {
				ZipFile zipFile = new ZipFile(location);
				if (zipFile.getEntry(filename) != null)
					input = new JarEntryEditorInput(new JarEntryFile(zipFile, filename));
			} catch (IOException e) {
			}
		} else {
			File file = new File(location, filename);
			if (file.exists()) {
				IFileStore store;
				try {
					store = EFS.getStore(file.toURI());
					input = new FileStoreEditorInput(store);
				} catch (CoreException e) {
				}
			}
		}
		return openEditor(input);
	}

	public static IEditorPart openEditor(IEditorInput input) {
		if (input != null) {
			try {
				return MDEPlugin.getActivePage().openEditor(input, IMDEUIConstants.MONITOR_EDITOR_ID);
			} catch (PartInitException e) {
				MDEPlugin.logException(e);
			}
		}
		return null;
	}

	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();
		IContainer container = file.getParent();

		IFile monitorFile = null;
		IFile buildFile = null;
		IFile pluginFile = null;
		boolean fragment = false;

		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.endsWith(ICoreConstants.MONIOTOR_FILENAME_EXE_LOWER_CASE)) {
			if (container instanceof IFolder)
				container = container.getParent();
			monitorFile = file;
			buildFile = container.getFile(ICoreConstants.BUILD_PROPERTIES_PATH);
			pluginFile = createPluginFile(container);
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) || name.equalsIgnoreCase(ICoreConstants.MANIFEST_FILENAME)) {
			pluginFile = file;
			fragment = name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			buildFile = container.getFile(ICoreConstants.BUILD_PROPERTIES_PATH);
			monitorFile = container.getFile(ICoreConstants.MANIFEST_PATH);
		}
		if (monitorFile.exists()) {
			IEditorInput in = new FileEditorInput(monitorFile);
			manager.putContext(in, new BundleInputContext(this, in, file == monitorFile));
		}
		if (pluginFile.exists()) {
			FileEditorInput in = new FileEditorInput(pluginFile);
			manager.putContext(in, new MonitorInputContext(this, in, file == pluginFile, fragment));
		}
		if (buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in, false));
		}
		manager.monitorFile(monitorFile);
		manager.monitorFile(pluginFile);
		manager.monitorFile(buildFile);

		fPrefs = new ProjectScope(container.getProject()).getNode(MDECore.PLUGIN_ID);
		if (fPrefs != null) {
			fShowExtensions = fPrefs.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true);
			fEquinox = fPrefs.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true);
		}
	}

	protected InputContextManager createInputContextManager() {
		PluginInputContextManager manager = new PluginInputContextManager(this);
		manager.setUndoManager(new MonitorUndoManager(this));
		return manager;
	}

	public void monitoredFileAdded(IFile file) {
		if (fInputContextManager == null)
			return;
		String name = file.getName();
		if (name.equalsIgnoreCase(ICoreConstants.MANIFEST_FILENAME)) {
			if (!fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BundleInputContext(this, in, false));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(MonitorInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new MonitorInputContext(this, in, false, false));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(MonitorInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new MonitorInputContext(this, in, false, true));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}

	public void ensurePluginContextPresence() {
		if (fInputContextManager.hasContext(MonitorInputContext.CONTEXT_ID))
			return;
		IProject project = fInputContextManager.getCommonProject();
		WorkspaceMonitorModelBase model = null;
		IFile file = null;
		if (fInputContextManager.getAggregateModel() instanceof IFragmentModel) {
			file = PDEProject.getFragmentXml(project);
			model = new WorkspaceFragmentModel(file, false);
		} else {
			file = PDEProject.getPluginXml(project);
			model = new WorkspaceMonitorModel(file, false);
		}

		IMonitorBase pluginBase = model.getMonitorBase(true);
		try {
			pluginBase.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		} catch (CoreException e) {
		}
		model.save();
		IEditorInput in = new FileEditorInput(file);
		fInputContextManager.putContext(in, new MonitorInputContext(this, in, false, false));

		updateBuildProperties(file.getName());
	}

	private void updateBuildProperties(String filename) {
		try {
			InputContext context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
			if (context != null) {
				IBuildModel buildModel = (IBuildModel) context.getModel();
				IBuild build = buildModel.getBuild();
				IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
				if (entry == null) {
					entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
					build.add(entry);
				}
				if (!entry.contains(filename))
					entry.addToken(filename);
			}
		} catch (CoreException e) {
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}

	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
		try {
			if (context.getId().equals(BuildInputContext.CONTEXT_ID))
				addPage(BUILD_INDEX, new BuildPage(this));
			else {
				updateFirstThreePages();
			}
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
	}

	public void contextRemoved(InputContext context) {
		close(true);
	}

	private void updateFirstThreePages() {
		try {
			int index = getActivePage();
			removePage(0);
//			removePage(0);
//			removePage(0);
			addPage(0, new RuntimePage(this));
			addPage(0, new DependenciesPage(this));
			addPage(0, new OverviewPage(this));
			setActivePage(index);
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
	}

	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		File file = new File(input.getURI());
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			manifestFile = file;
			File dir = file.getParentFile().getParentFile();
			buildFile = new File(dir, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
			pluginFile = createPluginFile(dir);
		} else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			buildFile = file;
			File dir = file.getParentFile();
			pluginFile = createPluginFile(dir);
			manifestFile = new File(dir, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			pluginFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
			manifestFile = new File(dir, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		}
		try {
			if (manifestFile.exists()) {
				IFileStore store = EFS.getStore(manifestFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new BundleInputContext(this, in, file == manifestFile));
			}
			if (pluginFile.exists()) {
				IFileStore store = EFS.getStore(pluginFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new MonitorInputContext(this, in, file == pluginFile, name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)));
			}
			if (buildFile.exists()) {
				IFileStore store = EFS.getStore(buildFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new BuildInputContext(this, in, file == buildFile));
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private File createPluginFile(File dir) {
		File pluginFile = new File(dir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		if (!pluginFile.exists())
			pluginFile = new File(dir, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		return pluginFile;
	}

	private IFile createPluginFile(IContainer container) {
		IFile pluginFile = container.getFile(ICoreConstants.PLUGIN_PATH);
		if (!pluginFile.exists())
			pluginFile = container.getFile(ICoreConstants.FRAGMENT_PATH);
		return pluginFile;
	}

	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		if (input instanceof JarEntryEditorInput) {
			createJarEntryContexts(manager, (JarEntryEditorInput) input);
			return;
		}

		String name = input.getName().toLowerCase(Locale.ENGLISH);
		if (name.startsWith(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			manager.putContext(input, new BundleInputContext(this, input, true));
		} else if (name.startsWith(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new MonitorInputContext(this, input, true, false));
		} else if (name.startsWith(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new MonitorInputContext(this, input, true, true));
		}
	}

	protected void createJarEntryContexts(InputContextManager manager, JarEntryEditorInput input) {
		IStorage storage = input.getStorage();
		ZipFile zip = (ZipFile) storage.getAdapter(ZipFile.class);
		try {
			if (zip == null)
				return;

			if (zip.getEntry(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR));
				manager.putContext(input, new BundleInputContext(this, input, storage.getName().equals(ICoreConstants.MANIFEST_FILENAME)));
			}

			if (zip.getEntry(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR));
				manager.putContext(input, new MonitorInputContext(this, input, storage.getName().equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), false));
			} else if (zip.getEntry(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
				manager.putContext(input, new MonitorInputContext(this, input, storage.getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR), true));
			}

			if (zip.getEntry(ICoreConstants.BUILD_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.BUILD_FILENAME_DESCRIPTOR));
				manager.putContext(input, new BuildInputContext(this, input, storage.getName().equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)));
			}
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
			}
		}
	}

	protected void addEditorPages() {
		try {
			addPage(new OverviewPage(this));
			addPage(new DependenciesPage(this));
			addPage(new RuntimePage(this));
			if (showExtensionTabs()) {
				addExtensionTabs();
			}
			if (fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				addPage(new BuildPage(this));
			}
		} catch (PartInitException e) {
			MDEPlugin.logException(e);
		}
		addSourcePage(BundleInputContext.CONTEXT_ID);
		addSourcePage(MonitorInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

	private boolean isSourcePageID(String pageID) {
		// Determine whether the page ID is a source page ID
		if (pageID == null) {
			return false;
		} else if (pageID.equals(BuildInputContext.CONTEXT_ID)) {
			// build.properites
			return true;
		} else if (pageID.equals(MonitorInputContext.CONTEXT_ID)) {
			// plugin.xml
			return true;
		} else if (pageID.equals(BundleInputContext.CONTEXT_ID)) {
			// MANIFEST.MF
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#computeInitialPageId()
	 */
	protected String computeInitialPageId() {
		// Used by plug-in search view to open query results, etc.
		if (SHOW_SOURCE) {
			SHOW_SOURCE = false;
			return getPrimarySourceInputContextID();
		}
		// Retrieve the initial page
		String firstPageId = super.computeInitialPageId();
		// If none is defined, return the default
		// If the initial page is a source page ID (e.g. build.properties,
		// MANIFEST.MF, plugin.xml), then return the page ID belonging to the
		// input context or file used to open this editor
		if (firstPageId == null) {
			return OverviewPage.PAGE_ID;
		} else if (isSourcePageID(firstPageId)) {
			return getPrimarySourceInputContextID();
		}

		return firstPageId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getPropertyEditorPageKey(org.eclipse.ui.IFileEditorInput)
	 */
	protected String getPropertyEditorPageKey(IFileEditorInput input) {
		// We are using the project itself to persist the editor page key property
		// The value persists even after the editor is closed
		// The project is used rather than the file in this case because the
		// manifest editor has 3 input files and only one build.properties,
		// one MANIFEST.MF and one plugin.xml should exist for each project.
		// We also want the last editor page to be shared between the two
		// input contexts.  The build.properties file has its own editor.
		IFile file = input.getFile();
		IProject project = file.getProject();
		// Ensure the project is defined
		if (project == null) {
			// Check the file for the key
			return super.getPropertyEditorPageKey(input);
		}
		// Get the persistent editor page key from the project 
		try {
			return project.getPersistentProperty(IMDEUIConstants.PROPERTY_MANIFEST_EDITOR_PAGE_KEY);
		} catch (CoreException e) {
			// Check the file for the key
			return super.getPropertyEditorPageKey(input);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#setPropertyEditorPageKey(org.eclipse.ui.IFileEditorInput, java.lang.String)
	 */
	protected void setPropertyEditorPageKey(IFileEditorInput input, String pageId) {
		// We are using the project itself to persist the editor page key property
		// The value persists even after the editor is closed
		// The project is used rather than the file in this case because the
		// manifest editor has 3 input files and only one build.properties,
		// one MANIFEST.MF and one plugin.xml should exist for each project.
		// We also want the last editor page to be shared between the two
		// input contexts.  The build.properties file has its own editor.
		IFile file = input.getFile();
		IProject project = file.getProject();
		// Ensure the project is defined
		if (project == null) {
			// Set the key on the file
			super.setPropertyEditorPageKey(input, pageId);
			return;
		}
		// Set the editor page ID as a persistent property on the project
		try {
			project.setPersistentProperty(IMDEUIConstants.PROPERTY_MANIFEST_EDITOR_PAGE_KEY, pageId);
		} catch (CoreException e) {
			// Set the key on the file
			super.setPropertyEditorPageKey(input, pageId);
			return;
		}
	}

	private String getPrimarySourceInputContextID() {
		// Get the input context used to open this editor
		InputContext primary = fInputContextManager.getPrimaryContext();
		// Ensure it is defined
		if (primary == null) {
			return null;
		}
		// Return its ID
		return primary.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected MDESourcePage createSourcePage(MDEFormEditor editor, String title, String name, String contextId) {
		if (contextId.equals(MonitorInputContext.CONTEXT_ID))
			return new MonitorSourcePage(editor, title, name);
		if (contextId.equals(BuildInputContext.CONTEXT_ID))
			return new BuildSourcePage(editor, title, name);
		if (contextId.equals(BundleInputContext.CONTEXT_ID))
			return new BundleSourcePage(editor, title, name);
		return super.createSourcePage(editor, title, name, contextId);
	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new ManifestOutlinePage(this);
	}

	public Object getAdapter(Class key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	public String getTitle() {
		IMonitorModelBase model = (IMonitorModelBase) getAggregateModel();
		if (model == null || !model.isValid())
			return super.getTitle();
		String text = getTitleText(model.getMonitorBase());
		if (text == null)
			return super.getTitle();
		return model.getResourceString(text);
	}

	public String getTitleProperty() {
		IPreferenceStore store = MDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return IMonitorObject.P_NAME;
		return IIdentifiable.P_ID;
	}

	private String getTitleText(IMonitorBase pluginBase) {
		IPreferenceStore store = MDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return pluginBase.getName();
		return pluginBase.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IFile) {
			String name = ((IFile) object).getName();
			if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR))
				context = fInputContextManager.findContext(MonitorInputContext.CONTEXT_ID);
			else if (name.equals(ICoreConstants.MANIFEST_FILENAME))
				context = fInputContextManager.findContext(BundleInputContext.CONTEXT_ID);
			else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR))
				context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IBuildObject) {
			context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IMonitorExtensionPoint || object instanceof IMonitorExtension) {
			context = fInputContextManager.findContext(MonitorInputContext.CONTEXT_ID);
		} else {
			context = fInputContextManager.findContext(BundleInputContext.CONTEXT_ID);
			if (context == null)
				context = fInputContextManager.findContext(MonitorInputContext.CONTEXT_ID);
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IShowEditorInput#showEditorInput(org.eclipse.ui.IEditorInput)
	 */
	public void showEditorInput(IEditorInput editorInput) {
		String name = editorInput.getName();
		String id = getActivePageInstance().getId();
		if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!BuildInputContext.CONTEXT_ID.equals(id))
				setActivePage(SHOW_SOURCE ? BuildInputContext.CONTEXT_ID : BuildPage.PAGE_ID);
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			if (!MonitorInputContext.CONTEXT_ID.equals(id)) {
				if (SHOW_SOURCE) {
					setActivePage(MonitorInputContext.CONTEXT_ID);
				} else if (fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
					setActivePage(ExtensionsPage.PAGE_ID);
				} else {
					setActivePage(OverviewPage.PAGE_ID);
				}
			}
		} else if (!BundleInputContext.CONTEXT_ID.equals(id)) {
			setActivePage(SHOW_SOURCE ? BundleInputContext.CONTEXT_ID : OverviewPage.PAGE_ID);
		}
	}

	public boolean showExtensionTabs() {
		if (fInputContextManager.hasContext(MonitorInputContext.CONTEXT_ID))
			return true;
		IBaseModel model = getAggregateModel();
		return fShowExtensions && model != null && model.isEditable();
	}

	public boolean isEquinox() {
		return fEquinox;
	}

	protected void addExtensionTabs() throws PartInitException {
		addPage(3, new ExtensionPointsPage(this));
		addPage(3, new ExtensionsPage(this));
	}

	protected void setShowExtensions(boolean show) throws BackingStoreException {
		if (fPrefs != null) {
			fPrefs.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, show);
			fPrefs.flush();
		}
		fShowExtensions = show;
	}

	public void contributeToToolbar(IToolBarManager manager) {
		contributeLaunchersToToolbar(manager);
		manager.add(getExportAction());
	}

	private PluginExportAction getExportAction() {
		if (fExportAction == null) {
			fExportAction = new PluginExportAction(this);
			fExportAction.setToolTipText(MDEUIMessages.PluginEditor_exportTooltip);
			fExportAction.setImageDescriptor(MDEPluginImages.DESC_EXPORT_PLUGIN_TOOL);
		}
		return fExportAction;
	}

	protected ILauncherFormPageHelper getLauncherHelper() {
		if (fLauncherHelper == null)
			fLauncherHelper = new MonitorLauncherFormPageHelper(this);
		return fLauncherHelper;
	}
}
