/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Les Jones <lesojones@gamil.com> - bug 205361
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.tools;

import com.siteview.mde.core.monitor.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.core.build.IBuild;
import com.siteview.mde.core.build.IBuildEntry;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.build.WorkspaceBuildModel;
import com.siteview.mde.internal.core.bundle.WorkspaceBundlePluginModel;
import com.siteview.mde.internal.core.ibundle.IBundle;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.natures.MDE;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.core.util.CoreUtility;
import com.siteview.mde.internal.core.util.IdUtil;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

/**
 * Operation to convert a normal workspace project into a plug-in project. This
 * code has, in the main, been refactored (copied with little or no amendment)
 * from org.eclipse.pde.internal.ui.wizards.tool.ConvertedProjectsPage.
 */
public class ConvertProjectToPluginOperation extends WorkspaceModifyOperation {

	private IProject[] projectsToConvert;

	private String fLibraryName;
	private String[] fSrcEntries;
	private String[] fLibEntries;

	/**
	 * Workspace operation to convert the specified project into a plug-in
	 * project.
	 * 
	 * @param theProjectsToConvert
	 *            The project to be converted.
	 */
	public ConvertProjectToPluginOperation(IProject[] theProjectsToConvert) {

		this.projectsToConvert = theProjectsToConvert;
	}

	/**
	 * Convert a normal java project into a plug-in project.
	 * 
	 * @param monitor
	 *            Progress monitor
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		try {
			monitor.beginTask(MDEUIMessages.ConvertedProjectWizard_converting, projectsToConvert.length);

			for (int i = 0; i < projectsToConvert.length; i++) {
				IProject projectToConvert = projectsToConvert[i];

				convertProject(projectToConvert, monitor);
				monitor.worked(1);
			}

		} catch (CoreException e) {
			MDEPlugin.logException(e);
		} finally {
			monitor.done();
		}
	}

	private void convertProject(IProject projectToConvert, IProgressMonitor monitor) throws CoreException {

		// Do early checks to make sure we can get out fast if we're not setup
		// properly
		if (projectToConvert == null || !projectToConvert.exists()) {
			return;
		}

		// Nature check - do we need to do anything at all?
		if (projectToConvert.hasNature(MDE.PLUGIN_NATURE)) {
			return;
		}

		CoreUtility.addNatureToProject(projectToConvert, MDE.PLUGIN_NATURE, monitor);

		loadClasspathEntries(projectToConvert, monitor);
		loadLibraryName(projectToConvert);

		createManifestFile(PDEProject.getManifest(projectToConvert), monitor);

		IFile buildFile = PDEProject.getBuildProperties(projectToConvert);
		if (!buildFile.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildFile);
			IBuild build = model.getBuild(true);
			IBuildEntry entry = model.getFactory().createEntry(IBuildEntry.BIN_INCLUDES);
			if (PDEProject.getPluginXml(projectToConvert).exists())
				entry.addToken(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			if (PDEProject.getManifest(projectToConvert).exists())
				entry.addToken(ICoreConstants.MANIFEST_FOLDER_NAME);
			for (int i = 0; i < fLibEntries.length; i++) {
				entry.addToken(fLibEntries[i]);
			}

			if (fSrcEntries.length > 0) {
				entry.addToken(fLibraryName);
				IBuildEntry source = model.getFactory().createEntry(IBuildEntry.JAR_PREFIX + fLibraryName);
				for (int i = 0; i < fSrcEntries.length; i++) {
					source.addToken(fSrcEntries[i]);
				}
				build.add(source);
			}
			if (entry.getTokens().length > 0)
				build.add(entry);

			model.save();
		}
	}

	private void loadClasspathEntries(IProject project, IProgressMonitor monitor) {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] currentClassPath = new IClasspathEntry[0];
		ArrayList sources = new ArrayList();
		ArrayList libraries = new ArrayList();
		try {
			currentClassPath = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
		}
		for (int i = 0; i < currentClassPath.length; i++) {
			int contentType = currentClassPath[i].getEntryKind();
			if (contentType == IClasspathEntry.CPE_SOURCE) {
				String relativePath = getRelativePath(currentClassPath[i], project);
				if (relativePath.equals("")) { //$NON-NLS-1$
					sources.add("."); //$NON-NLS-1$
				} else {
					sources.add(relativePath + "/"); //$NON-NLS-1$
				}
			} else if (contentType == IClasspathEntry.CPE_LIBRARY) {
				String path = getRelativePath(currentClassPath[i], project);
				if (path.length() > 0)
					libraries.add(path);
				else
					libraries.add("."); //$NON-NLS-1$
			}
		}
		fSrcEntries = (String[]) sources.toArray(new String[sources.size()]);
		fLibEntries = (String[]) libraries.toArray(new String[libraries.size()]);

		IClasspathEntry[] classPath = new IClasspathEntry[currentClassPath.length + 1];
		System.arraycopy(currentClassPath, 0, classPath, 0, currentClassPath.length);
		classPath[classPath.length - 1] = ClasspathComputer.createContainerEntry();
		try {
			javaProject.setRawClasspath(classPath, monitor);
		} catch (JavaModelException e) {
		}
	}

	private String getRelativePath(IClasspathEntry cpe, IProject project) {
		IPath path = project.getFile(cpe.getPath()).getProjectRelativePath();
		return path.removeFirstSegments(1).toString();
	}

	private void loadLibraryName(IProject project) {
		if (isOldTarget() || (fLibEntries.length > 0 && fSrcEntries.length > 0)) {
			String libName = project.getName();
			int i = libName.lastIndexOf("."); //$NON-NLS-1$
			if (i != -1)
				libName = libName.substring(i + 1);
			fLibraryName = libName + ".jar"; //$NON-NLS-1$
		} else {
			fLibraryName = "."; //$NON-NLS-1$
		}
	}

	private void organizeExports(final IProject project) {
		PDEModelUtility.modifyModel(new ModelModification(PDEProject.getManifest(project)) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBundlePluginModelBase))
					return;
				OrganizeManifest.organizeExportPackages(((IBundlePluginModelBase) model).getBundleModel().getBundle(), project, true, true);
			}
		}, null);
	}

	private String createInitialName(String id) {
		int loc = id.lastIndexOf('.');
		if (loc == -1)
			return id;
		StringBuffer buf = new StringBuffer(id.substring(loc + 1));
		buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
		return buf.toString();
	}

	private void createManifestFile(IFile file, IProgressMonitor monitor) throws CoreException {
		WorkspaceBundlePluginModel model = new WorkspaceBundlePluginModel(file, null);
		model.load();
		IBundle pluginBundle = model.getBundleModel().getBundle();

		String pluginId = pluginBundle.getHeader(Constants.BUNDLE_SYMBOLICNAME);
		String pluginName = pluginBundle.getHeader(Constants.BUNDLE_NAME);
		String pluginVersion = pluginBundle.getHeader(Constants.BUNDLE_VERSION);

		boolean missingInfo = (pluginId == null || pluginName == null || pluginVersion == null);

		// If no ID exists, create one
		if (pluginId == null) {
			pluginId = IdUtil.getValidId(file.getProject().getName());
		}
		// At this point, the plug-in ID is not null

		// If no version number exists, create one
		if (pluginVersion == null) {
			pluginVersion = "1.0.0.qualifier"; //$NON-NLS-1$
		}

		// If no name exists, create one using the non-null pluginID
		if (pluginName == null) {
			pluginName = createInitialName(pluginId);
		}

		pluginBundle.setHeader(Constants.BUNDLE_SYMBOLICNAME, pluginId);
		pluginBundle.setHeader(Constants.BUNDLE_VERSION, pluginVersion);
		pluginBundle.setHeader(Constants.BUNDLE_NAME, pluginName);

		if (missingInfo) {
			IMonitorModelFactory factory = model.getMonitorFactory();
			IMonitorBase base = model.getMonitorBase();
			if (fLibraryName != null && !fLibraryName.equals(".")) { //$NON-NLS-1$
				IMonitorLibrary library = factory.createLibrary();
				library.setName(fLibraryName);
				library.setExported(true);
				base.add(library);
			}
			for (int i = 0; i < fLibEntries.length; i++) {
				IMonitorLibrary library = factory.createLibrary();
				library.setName(fLibEntries[i]);
				library.setExported(true);
				base.add(library);
			}
			if (TargetPlatformHelper.getTargetVersion() >= 3.1)
				pluginBundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		}

		model.save();
		monitor.done();
		organizeExports(file.getProject());
	}

	private boolean isOldTarget() {
		return TargetPlatformHelper.getTargetVersion() < 3.1;
	}

}
