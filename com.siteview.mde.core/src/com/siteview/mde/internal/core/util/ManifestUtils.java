/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.util;

import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import com.siteview.mde.core.build.IBuild;
import com.siteview.mde.core.build.IBuildEntry;
import com.siteview.mde.internal.core.build.WorkspaceBuildModel;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.project.PDEProject;

public class ManifestUtils {

	private ManifestUtils() {
	}

	public static IPackageFragmentRoot[] findPackageFragmentRoots(IManifestHeader header, IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		String[] libs;
		if (header == null || header.getValue() == null)
			libs = new String[] {"."}; //$NON-NLS-1$
		else
			libs = header.getValue().split(","); //$NON-NLS-1$

		IBuild build = getBuild(project);
		if (build == null) {
			try {
				return javaProject.getPackageFragmentRoots();
			} catch (JavaModelException e) {
				return new IPackageFragmentRoot[0];
			}
		}
		List pkgFragRoots = new LinkedList();
		for (int j = 0; j < libs.length; j++) {
			String lib = libs[j];
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=230469  			
			IPackageFragmentRoot root = null;
			if (!lib.equals(".")) { //$NON-NLS-1$
				try {
					root = javaProject.getPackageFragmentRoot(project.getFile(lib));
				} catch (IllegalArgumentException e) {
					return new IPackageFragmentRoot[0];
				}
			}
			if (root != null && root.exists()) {
				pkgFragRoots.add(root);
			} else {
				IBuildEntry entry = build.getEntry("source." + lib); //$NON-NLS-1$
				if (entry == null)
					continue;
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IResource resource = project.findMember(tokens[i]);
					if (resource == null)
						continue;
					root = javaProject.getPackageFragmentRoot(resource);
					if (root != null && root.exists())
						pkgFragRoots.add(root);
				}
			}
		}
		return (IPackageFragmentRoot[]) pkgFragRoots.toArray(new IPackageFragmentRoot[pkgFragRoots.size()]);
	}

	public final static IBuild getBuild(IProject project) {
		IFile buildProps = PDEProject.getBuildProperties(project);
		if (buildProps.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			if (model != null)
				return model.getBuild();
		}
		return null;
	}

	public static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE || (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}

}
