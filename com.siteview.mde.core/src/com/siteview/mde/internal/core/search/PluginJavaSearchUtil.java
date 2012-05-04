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
package com.siteview.mde.internal.core.search;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import com.siteview.mde.core.monitor.IFragmentModel;
import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorImport;
import com.siteview.mde.core.monitor.IMonitorLibrary;
import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;
import com.siteview.mde.internal.core.ClasspathUtilCore;
import com.siteview.mde.internal.core.MDEManager;

public class PluginJavaSearchUtil {

	public static IMonitorModelBase[] getPluginImports(IMonitorImport dep) {
		return getPluginImports(dep.getId());
	}

	public static IMonitorModelBase[] getPluginImports(String pluginImportID) {
		HashSet set = new HashSet();
		collectAllPrerequisites(MonitorRegistry.findModel(pluginImportID), set);
		return (IMonitorModelBase[]) set.toArray(new IMonitorModelBase[set.size()]);
	}

	public static void collectAllPrerequisites(IMonitorModelBase model, HashSet set) {
		if (model == null || !set.add(model))
			return;
		IMonitorImport[] imports = model.getMonitorBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isReexported()) {
				IMonitorModelBase child = MonitorRegistry.findModel(imports[i].getId());
				if (child != null)
					collectAllPrerequisites(child, set);
			}
		}
	}

	public static IPackageFragment[] collectPackageFragments(IMonitorModelBase[] models, IJavaProject parentProject, boolean filterEmptyPackages) throws JavaModelException {
		ArrayList result = new ArrayList();
		IPackageFragmentRoot[] roots = parentProject.getAllPackageFragmentRoots();

		for (int i = 0; i < models.length; i++) {
			IMonitorModelBase model = models[i];
			IResource resource = model.getUnderlyingResource();
			if (resource == null) {
				ArrayList libraryPaths = new ArrayList();
				addLibraryPaths(model, libraryPaths);
				for (int j = 0; j < roots.length; j++) {
					if (libraryPaths.contains(roots[j].getPath())) {
						extractPackageFragments(roots[j], result, filterEmptyPackages);
					}
				}
			} else {
				IProject project = resource.getProject();
				for (int j = 0; j < roots.length; j++) {
					IJavaProject jProject = (IJavaProject) roots[j].getParent();
					if (jProject.getProject().equals(project)) {
						extractPackageFragments(roots[j], result, filterEmptyPackages);
					}
				}
			}
		}
		return (IPackageFragment[]) result.toArray(new IPackageFragment[result.size()]);
	}

	private static void extractPackageFragments(IPackageFragmentRoot root, ArrayList result, boolean filterEmpty) {
		try {
			IJavaElement[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				IPackageFragment fragment = (IPackageFragment) children[i];
				if (!filterEmpty || fragment.hasChildren())
					result.add(fragment);
			}
		} catch (JavaModelException e) {
		}
	}

	private static void addLibraryPaths(IMonitorModelBase model, ArrayList libraryPaths) {
		IMonitorBase plugin = model.getMonitorBase();

		IFragmentModel[] fragments = new IFragmentModel[0];
		if (plugin instanceof IMonitor)
			fragments = MDEManager.findFragmentsFor(model);

		File file = new File(model.getInstallLocation());
		if (file.isFile()) {
			libraryPaths.add(new Path(file.getAbsolutePath()));
		} else {
			IMonitorLibrary[] libraries = plugin.getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				String libraryName = ClasspathUtilCore.expandLibraryName(libraries[i].getName());
				String path = plugin.getModel().getInstallLocation() + IPath.SEPARATOR + libraryName;
				if (new File(path).exists()) {
					libraryPaths.add(new Path(path));
				} else {
					findLibraryInFragments(fragments, libraryName, libraryPaths);
				}
			}
		}
		if (ClasspathUtilCore.hasExtensibleAPI(model)) {
			for (int i = 0; i < fragments.length; i++) {
				addLibraryPaths(fragments[i], libraryPaths);
			}
		}
	}

	private static void findLibraryInFragments(IFragmentModel[] fragments, String libraryName, ArrayList libraryPaths) {
		for (int i = 0; i < fragments.length; i++) {
			String path = fragments[i].getInstallLocation() + IPath.SEPARATOR + libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
				break;
			}
		}
	}

	public static IJavaSearchScope createSeachScope(IJavaProject jProject) throws JavaModelException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		ArrayList filteredRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getResource() != null && roots[i].getResource().getProject().equals(jProject.getProject())) {
				filteredRoots.add(roots[i]);
			}
		}
		return SearchEngine.createJavaSearchScope((IJavaElement[]) filteredRoots.toArray(new IJavaElement[filteredRoots.size()]));
	}

}
