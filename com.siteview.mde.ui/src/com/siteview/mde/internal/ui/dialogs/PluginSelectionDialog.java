/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.ui.dialogs;

import com.siteview.mde.core.monitor.*;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ibundle.IBundleModel;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.text.bundle.ImportPackageHeader;
import com.siteview.mde.internal.core.text.bundle.ImportPackageObject;
import com.siteview.mde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.osgi.framework.Constants;

public class PluginSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "com.siteview.mde.ui.dialogs.PluginSelectionDialog"; //$NON-NLS-1$
	private IMonitorModelBase[] fModels;

	private class PluginSearchItemsFilter extends ItemsFilter {

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean matchItem(Object item) {
			String id = null;
			if (item instanceof IMonitorModelBase) {
				IMonitorModelBase model = (IMonitorModelBase) item;
				id = model.getMonitorBase().getId();
			}

			return (matches(id));
		}

		protected boolean matches(String text) {
			String pattern = patternMatcher.getPattern();
			if (pattern.indexOf("*") != 0 & pattern.indexOf("?") != 0 & pattern.indexOf(".") != 0) {//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pattern = "*" + pattern; //$NON-NLS-1$
				patternMatcher.setPattern(pattern);
			}
			return patternMatcher.matches(text);
		}
	}

	private class PluginSearchComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			int id1 = getId(o1);
			int id2 = getId(o2);

			if (id1 != id2)
				return id1 - id2;
			return compareSimilarObjects(o1, o2);
		}

		private int getId(Object element) {
			if (element instanceof IMonitorModelBase) {
				return 100;
			}
			return 0;
		}

		private int compareSimilarObjects(Object o1, Object o2) {
			if (o1 instanceof IMonitorModelBase && o2 instanceof IMonitorModelBase) {
				IMonitorModelBase ipmb1 = (IMonitorModelBase) o1;
				IMonitorModelBase ipmb2 = (IMonitorModelBase) o2;
				return comparePlugins(ipmb1.getMonitorBase(), ipmb2.getMonitorBase());
			}
			return 0;
		}

		private int comparePlugins(IMonitorBase ipmb1, IMonitorBase ipmb2) {
			return ipmb1.getId().compareTo(ipmb2.getId());
		}

	}

	public PluginSelectionDialog(Shell parentShell, boolean includeFragments, boolean multipleSelection) {
		this(parentShell, getElements(includeFragments), multipleSelection);
	}

	public PluginSelectionDialog(Shell parentShell, IMonitorModelBase[] models, boolean multipleSelection) {
		super(parentShell, multipleSelection);
		fModels = models;
		setTitle(MDEUIMessages.PluginSelectionDialog_title);
		setMessage(MDEUIMessages.PluginSelectionDialog_message);
		MDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(MDEPlugin.getDefault().getLabelProvider());
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.PLUGIN_SELECTION);
	}

	public boolean close() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	private static IMonitorModelBase[] getElements(boolean includeFragments) {
		return MonitorRegistry.getActiveModels(includeFragments);
	}

	public static HashSet getExistingImports(IMonitorModelBase model, boolean includeImportPkg) {
		HashSet existingImports = new HashSet();
		addSelfAndDirectImports(existingImports, model);
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IMonitorModelBase host = MonitorRegistry.findModel(fragment.getPluginId());
			if (host instanceof IMonitorModel) {
				addSelfAndDirectImports(existingImports, host);
			}
		}
		if (includeImportPkg && model instanceof IBundlePluginModelBase) {
			addImportedPackages((IBundlePluginModelBase) model, existingImports);
		}
		return existingImports;
	}

	private static void addSelfAndDirectImports(HashSet set, IMonitorModelBase model) {
		set.add(model.getMonitorBase().getId());
		IMonitorImport[] imports = model.getMonitorBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			String id = imports[i].getId();
			if (set.add(id)) {
				addReexportedImport(set, id);
			}
		}
	}

	private static void addReexportedImport(HashSet set, String id) {
		IMonitorModelBase model = MonitorRegistry.findModel(id);
		if (model != null) {
			IMonitorImport[] imports = model.getMonitorBase().getImports();
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isReexported() && set.add(imports[i].getId())) {
					addReexportedImport(set, imports[i].getId());
				}
			}
		}
	}

	private static void addImportedPackages(IBundlePluginModelBase base, HashSet existingImports) {
		HashMap map = getImportPackages(base);
		if (map == null)
			return;

		ExportPackageDescription exported[] = MDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		for (int i = 0; i < exported.length; i++) {
			// iterate through all the exported packages
			ImportPackageObject ipo = (ImportPackageObject) map.get(exported[i].getName());
			// if we find an exported package that matches a pkg in the map, then the exported package matches a package on our import-package statement
			if (ipo != null) {
				// check version to make sure we only add bundles from valid packages
				String version = ipo.getVersion();
				if (version != null)
					try {
						if (!new VersionRange(version).isIncluded(exported[i].getVersion()))
							continue;
						// NFE if ImportPackageObject's version is improperly formatted - ignore any matching imported packages since version is invalid
					} catch (NumberFormatException e) {
						continue;
					}
				existingImports.add(exported[i].getSupplier().getSymbolicName());
			}
		}
	}

	// returns null instead of empty map so we know not to iterate through exported packages
	private static HashMap getImportPackages(IBundlePluginModelBase base) {
		IBundleModel bmodel = base.getBundleModel();
		if (bmodel != null) {
			ImportPackageHeader header = (ImportPackageHeader) bmodel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header != null) {
				// create a map of all the packages we import
				HashMap map = new HashMap();
				ImportPackageObject[] packages = header.getPackages();
				for (int i = 0; i < packages.length; i++)
					map.put(packages[i].getName(), packages[i]);
				return map;
			}
		}
		return null;
	}

	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	protected ItemsFilter createFilter() {
		return new PluginSearchItemsFilter();
	}

	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		for (int i = 0; i < fModels.length; i++) {
			contentProvider.add(fModels[i], itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = MDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = MDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	public String getElementName(Object item) {
		if (item instanceof IMonitorModelBase) {
			IMonitorModelBase model = (IMonitorModelBase) item;
			return model.getMonitorBase().getId();
		}
		return null;
	}

	protected Comparator getItemsComparator() {
		return new PluginSearchComparator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, IMDEUIConstants.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
	}

}
