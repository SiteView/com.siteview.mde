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
package com.siteview.mde.internal.core.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.FactoryConfigurationError;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;

import com.siteview.mde.core.monitor.*;

import org.eclipse.pde.internal.build.IPDEBuildConstants;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.ifeature.IFeatureModel;
import com.siteview.mde.internal.core.ifeature.IFeaturePlugin;

public class CoreUtility {

	/**
	 * Read a file from an InputStream and write it to the file system.
	 *
	 * @param in InputStream from which to read.
	 * @param file output file to create.
	 * @exception IOException
	 */
	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		try {
			byte buffer[] = new byte[1024];
			int count;
			try {
				while ((count = in.read(buffer, 0, buffer.length)) > 0) {
					fos.write(buffer, 0, count);
				}
			} finally {
				fos.close();
				fos = null;
				in.close();
				in = null;
			}
		} catch (IOException e) {
			MDECore.logException(e);
			throw e;
		}
	}

	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}

	public static void createFolder(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			IContainer parent = folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder) parent);
			}
			folder.create(true, true, null);
		}
	}

	public static void createProject(IProject project, IPath location, IProgressMonitor monitor) throws CoreException {
		if (!Platform.getLocation().equals(location)) {
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			desc.setLocation(location);
			project.create(desc, monitor);
		} else
			project.create(monitor);
	}

	public static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		text = text.replaceAll("\\r|\\n|\\f|\\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
		return text.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static void deleteContent(File curr) {
		if (curr.exists()) {
			if (curr.isDirectory()) {
				File[] children = curr.listFiles();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						deleteContent(children[i]);
					}
				}
			}
			curr.delete();
		}
	}

	public static boolean guessUnpack(BundleDescription bundle) {
		if (bundle == null)
			return true;

		if (new File(bundle.getLocation()).isFile())
			return false;

		// at this point always make sure launcher fragments are flat; or else you will have launching problems
		HostSpecification host = bundle.getHost();
		if (host != null && host.getName().equals(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER)) {
			return true;
		}

		IWorkspaceRoot root = MDECore.getWorkspace().getRoot();
		IContainer container = root.getContainerForLocation(new Path(bundle.getLocation()));
		if (container == null)
			return true;

		if (container instanceof IProject) {
			try {
				if (!((IProject) container).hasNature(JavaCore.NATURE_ID))
					return true;
			} catch (CoreException e) {
				MDECore.logException(e);
			}
		}

		IMonitorModelBase model = MonitorRegistry.findModel(bundle);
		if (model == null)
			return true;

		// check bundle header
		if (model instanceof IBundlePluginModelBase) {
			IBundleModel bundleModel = ((IBundlePluginModelBase) model).getBundleModel();
			if (bundleModel != null) {
				IBundle b = bundleModel.getBundle();
				String header = b.getHeader(ICoreConstants.ECLIPSE_BUNDLE_SHAPE);
				if (header != null) {
					return ICoreConstants.SHAPE_DIR.equals(header);
				}
			}
		}

		// check features
		FeatureModelManager manager = MDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] models = manager.getModels();
		for (int i = 0; i < models.length; i++) {
			IFeatureModel featureModel = models[i];
			IFeaturePlugin[] plugins = featureModel.getFeature().getPlugins();
			for (int j = 0; j < plugins.length; j++) {
				IFeaturePlugin featurePlugin = plugins[j];
				if (featurePlugin.getId().equals(bundle.getSymbolicName())) {
					return featurePlugin.isUnpack();
				}
			}
		}

		IMonitorLibrary[] libraries = model.getMonitorBase().getLibraries();
		if (libraries.length == 0)
			return false;

		for (int i = 0; i < libraries.length; i++) {
			if (libraries[i].getName().equals(".")) //$NON-NLS-1$
				return false;
		}
		return true;
	}

	public static boolean jarContainsResource(File file, String resource, boolean directory) {
		ZipFile jarFile = null;
		try {
			jarFile = new ZipFile(file, ZipFile.OPEN_READ);
			ZipEntry resourceEntry = jarFile.getEntry(resource);
			if (resourceEntry != null)
				return directory ? resourceEntry.isDirectory() : true;
		} catch (IOException e) {
			MDECore.logException(e);
		} catch (FactoryConfigurationError e) {
			MDECore.logException(e);
		} finally {
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
				MDECore.logException(e2);
			}
		}
		return false;
	}

	public static void copyFile(IPath originPath, String name, File target) {
		File source = new File(originPath.toFile(), name);
		if (source.exists() == false)
			return;
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(target);
			byte[] buf = new byte[1024];
			int len = is.read(buf);
			while (len != -1) {
				os.write(buf, 0, len);
				len = is.read(buf);
			}
		} catch (IOException e) {
			MDECore.logException(e);
		} finally {
			try {
				if (is != null)
					is.close();
				if (os != null)
					os.close();
			} catch (IOException e) {
				MDECore.logException(e);
			}
		}
	}

	public static org.eclipse.jface.text.Document getTextDocument(File bundleLocation, String path) {
		ZipFile jarFile = null;
		InputStream stream = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if ("jar".equals(extension) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(path);
				if (manifestEntry != null) {
					stream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, path);
				if (file.exists())
					stream = new FileInputStream(file);
			}
			return getTextDocument(stream);
		} catch (IOException e) {
			MDECore.logException(e);
		} finally {
			try {
				if (jarFile != null)
					jarFile.close();
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				MDECore.logException(e);
			}
		}
		return null;
	}

	public static org.eclipse.jface.text.Document getTextDocument(InputStream in) {
		ByteArrayOutputStream output = null;
		String result = null;
		try {
			output = new ByteArrayOutputStream();

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				output.write(buffer, 0, count);
			}

			result = output.toString("UTF-8"); //$NON-NLS-1$
			output.close();
			output = null;
			in.close();
			in = null;
		} catch (IOException e) {
			// close open streams
			if (output != null) {
				try {
					output.close();
				} catch (IOException ee) {
					MDECore.logException(ee);
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					MDECore.logException(ee);
				}
			}
		}
		return result == null ? null : new org.eclipse.jface.text.Document(result);
	}

}
