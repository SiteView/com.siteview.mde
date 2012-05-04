/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;

import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.util.CoreUtility;

/**
 * This class manages the ability of external plug-ins in the model manager to
 * take part in the Java search. It manages a proxy Java projects and for each
 * external plug-in added to Java search, it adds its Java libraries as external
 * JARs to the proxy project. This makes the libraries visible to the Java
 * model, and they can take part in various Java searches.
 */
public class SearchablePluginsManager implements IFileAdapterFactory, IMonitorModelListener, ISaveParticipant {

	private static final String PROXY_FILE_NAME = ".searchable"; //$NON-NLS-1$
	public static final String PROXY_PROJECT_NAME = "External Plug-in Libraries"; //$NON-NLS-1$
	private static final String KEY = "searchablePlugins"; //$NON-NLS-1$

	private Listener fElementListener;
	private Set fPluginIdSet;
	private ArrayList fListeners;

	class Listener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent e) {
			if (e.getType() == ElementChangedEvent.POST_CHANGE) {
				handleDelta(e.getDelta());
			}
		}

		private boolean handleDelta(IJavaElementDelta delta) {
			Object element = delta.getElement();
			if (element instanceof IJavaModel) {
				IJavaElementDelta[] projectDeltas = delta.getAffectedChildren();
				for (int i = 0; i < projectDeltas.length; i++) {
					if (handleDelta(projectDeltas[i]))
						break;
				}
				return true;
			}
			if (delta.getElement() instanceof IJavaProject) {
				IJavaProject project = (IJavaProject) delta.getElement();
				if (project.getElementName().equals(PROXY_PROJECT_NAME)) {
					if (delta.getKind() == IJavaElementDelta.REMOVED) {
						fPluginIdSet.clear();
					} else if (delta.getKind() == IJavaElementDelta.ADDED) {
						// We may be getting a queued delta from when the manager was initialized, ignore unless we don't already have data
						if (fPluginIdSet == null || fPluginIdSet.size() == 0) {
							// Something other than the manager created the project, check if it has a .searchable file to load from
							initializeStates();
						}
					}
				}
				return true;
			}
			return false;
		}
	}

	public SearchablePluginsManager() {
		initializeStates();
		fElementListener = new Listener();
		JavaCore.addElementChangedListener(fElementListener);
		MDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	private void initializeStates() {
		fPluginIdSet = new TreeSet();
		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		try {
			if (project.exists() && project.isOpen()) {
				IFile proxyFile = project.getFile(PROXY_FILE_NAME);
				if (proxyFile.exists()) {
					Properties properties = new Properties();
					InputStream stream = proxyFile.getContents(true);
					properties.load(stream);
					stream.close();
					String value = properties.getProperty(KEY);
					if (value != null) {
						StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
						while (stok.hasMoreTokens())
							fPluginIdSet.add(stok.nextToken());
					}
				}
			}
		} catch (IOException e) {
		} catch (CoreException e) {
		}
	}

	public IJavaProject getProxyProject() {
		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		try {
			if (project.exists() && project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
				return JavaCore.create(project);
			}

		} catch (CoreException e) {
		}
		return null;
	}

	public void shutdown() {
		// remove listener
		JavaCore.removeElementChangedListener(fElementListener);
		MDECore.getDefault().getModelManager().removePluginModelListener(this);
		if (fListeners != null)
			fListeners.clear();
	}

	public IClasspathEntry[] computeContainerClasspathEntries() throws CoreException {
		ArrayList result = new ArrayList();

		IMonitorModelBase[] wModels = MonitorRegistry.getWorkspaceModels();
		for (int i = 0; i < wModels.length; i++) {
			IProject project = wModels[i].getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				result.add(JavaCore.newProjectEntry(project.getFullPath()));
			}
		}
		Iterator iter = fPluginIdSet.iterator();
		while (iter.hasNext()) {
			ModelEntry entry = MonitorRegistry.findEntry(iter.next().toString());
			if (entry != null) {
				boolean addModel = true;
				wModels = entry.getWorkspaceModels();
				for (int i = 0; i < wModels.length; i++) {
					IProject project = wModels[i].getUnderlyingResource().getProject();
					if (project.hasNature(JavaCore.NATURE_ID))
						addModel = false;
				}
				if (!addModel)
					continue;
				IMonitorModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					if (models[i].isEnabled())
						ClasspathUtilCore.addLibraries(models[i], result);
				}
			}
		}

		if (result.size() > 1) {
			// sort
			Map map = new TreeMap();
			for (int i = 0; i < result.size(); i++) {
				IClasspathEntry entry = (IClasspathEntry) result.get(i);
				String key = entry.getPath().lastSegment().toString();
				if (map.containsKey(key)) {
					key += System.currentTimeMillis();
				}
				map.put(key, entry);
			}
			return (IClasspathEntry[]) map.values().toArray(new IClasspathEntry[map.size()]);
		}
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}

	public Object createAdapterChild(FileAdapter parent, File file) {
		if (!file.isDirectory()) {
			String name = file.getName().toLowerCase(Locale.ENGLISH);
			if (name.endsWith(".jar")) { //$NON-NLS-1$
				IPackageFragmentRoot root = findPackageFragmentRoot(new Path(file.getAbsolutePath()));
				if (root != null)
					return root;
			}
		}
		return new FileAdapter(parent, file, this);
	}

	private IPackageFragmentRoot findPackageFragmentRoot(IPath jarPath) {
		IJavaProject jProject = getProxyProject();
		if (jProject != null) {
			try {
				IPackageFragmentRoot[] roots = jProject.getAllPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					IPackageFragmentRoot root = roots[i];
					IPath path = root.getPath();
					if (path.equals(jarPath))
						return root;

				}
			} catch (JavaModelException e) {
			}
		}

		// Find in other plug-in (and fragments) projects dependencies
		IMonitorModelBase[] pluginModels = MonitorRegistry.getWorkspaceModels();
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource().getProject();
			IJavaProject javaProject = JavaCore.create(project);
			try {
				IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
				for (int j = 0; j < roots.length; j++) {
					IPackageFragmentRoot root = roots[j];
					IPath path = root.getPath();
					if (path.equals(jarPath))
						return root;
				}
			} catch (JavaModelException e) {
			}
		}

		return null;
	}

	private void checkForProxyProject() {
		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		try {
			IProject project = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
			if (!project.exists())
				createProxyProject(new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}

	public void addToJavaSearch(IMonitorModelBase[] models) {
		checkForProxyProject();
		MonitorModelDelta delta = new MonitorModelDelta();
		int size = fPluginIdSet.size();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getMonitorBase().getId();
			if (fPluginIdSet.add(id)) {
				ModelEntry entry = MonitorRegistry.findEntry(id);
				if (entry != null)
					delta.addEntry(entry, MonitorModelDelta.CHANGED);
			}
		}
		if (fPluginIdSet.size() > size) {
			resetContainer();
			fireDelta(delta);
		}
	}

	public void removeFromJavaSearch(IMonitorModelBase[] models) {
		MonitorModelDelta delta = new MonitorModelDelta();
		int size = fPluginIdSet.size();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getMonitorBase().getId();
			if (fPluginIdSet.remove(id)) {
				ModelEntry entry = MonitorRegistry.findEntry(id);
				if (entry != null) {
					delta.addEntry(entry, MonitorModelDelta.CHANGED);
				}
			}
		}
		if (fPluginIdSet.size() < size) {
			resetContainer();
			fireDelta(delta);
		}
	}

	public void removeAllFromJavaSearch() {
		if (fPluginIdSet.size() > 0) {
			MonitorModelDelta delta = new MonitorModelDelta();
			for (Iterator iterator = fPluginIdSet.iterator(); iterator.hasNext();) {
				String id = (String) iterator.next();
				ModelEntry entry = MonitorRegistry.findEntry(id);
				if (entry != null) {
					delta.addEntry(entry, MonitorModelDelta.CHANGED);
				}
			}
			fPluginIdSet.clear();
			resetContainer();
			fireDelta(delta);
		}
	}

	public boolean isInJavaSearch(String symbolicName) {
		return fPluginIdSet.contains(symbolicName);
	}

	private void resetContainer() {
		IJavaProject jProject = getProxyProject();
		try {
			if (jProject != null) {
				JavaCore.setClasspathContainer(MDECore.JAVA_SEARCH_CONTAINER_PATH, new IJavaProject[] {jProject}, new IClasspathContainer[] {new ExternalJavaSearchClasspathContainer()}, null);
			}
		} catch (JavaModelException e) {
		}
	}

	public void modelsChanged(MonitorModelDelta delta) {
		ModelEntry[] entries = delta.getRemovedEntries();
		for (int i = 0; i < entries.length; i++) {
			if (fPluginIdSet.contains(entries[i].getId())) {
				fPluginIdSet.remove(entries[i].getId());
			}
		}
		resetContainer();
	}

	private void fireDelta(MonitorModelDelta delta) {
		if (fListeners != null) {
			for (int i = 0; i < fListeners.size(); i++) {
				((IMonitorModelListener) fListeners.get(i)).modelsChanged(delta);
			}
		}
	}

	public void addPluginModelListener(IMonitorModelListener listener) {
		if (fListeners == null)
			fListeners = new ArrayList();
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}

	public void removePluginModelListener(IMonitorModelListener listener) {
		if (fListeners != null)
			fListeners.remove(listener);
	}

	public void doneSaving(ISaveContext context) {
		// nothing is required here
	}

	public void prepareToSave(ISaveContext context) {
		// no need for preparation
	}

	public void rollback(ISaveContext context) {
		// do nothing.  not the end of the world.
	}

	public void saving(ISaveContext context) throws CoreException {
		if (context.getKind() != ISaveContext.FULL_SAVE)
			return;

		// persist state
		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		if (project.exists() && project.isOpen()) {
			IFile file = project.getFile(PROXY_FILE_NAME);
			Properties properties = new Properties();
			StringBuffer buffer = new StringBuffer();
			Iterator iter = fPluginIdSet.iterator();
			while (iter.hasNext()) {
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append(iter.next().toString());
			}
			properties.setProperty(KEY, buffer.toString());
			try {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				properties.store(outStream, ""); //$NON-NLS-1$
				outStream.flush();
				outStream.close();
				ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
				if (file.exists())
					file.setContents(inStream, true, false, new NullProgressMonitor());
				else
					file.create(inStream, true, new NullProgressMonitor());
				inStream.close();
			} catch (IOException e) {
				MDECore.log(e);
			}
		}
	}

	public IProject createProxyProject(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		IProject project = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
		if (project.exists()) {
			if (!project.isOpen()) {
				project.open(monitor);
			}
			return project;
		}

		monitor.beginTask(NLS.bind(MDECoreMessages.SearchablePluginsManager_createProjectTaskName, SearchablePluginsManager.PROXY_PROJECT_NAME), 5);
		project.create(new SubProgressMonitor(monitor, 1));
		project.open(new SubProgressMonitor(monitor, 1));
		CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, new SubProgressMonitor(monitor, 1));
		IJavaProject jProject = JavaCore.create(project);
		jProject.setOutputLocation(project.getFullPath(), new SubProgressMonitor(monitor, 1));
		computeClasspath(jProject, new SubProgressMonitor(monitor, 1));
		return project;
	}

	private void computeClasspath(IJavaProject project, IProgressMonitor monitor) {
		IClasspathEntry[] classpath = new IClasspathEntry[2];
		classpath[0] = JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath());
		classpath[1] = JavaCore.newContainerEntry(MDECore.JAVA_SEARCH_CONTAINER_PATH);
		try {
			project.setRawClasspath(classpath, monitor);
		} catch (JavaModelException e) {
		}
	}

}
