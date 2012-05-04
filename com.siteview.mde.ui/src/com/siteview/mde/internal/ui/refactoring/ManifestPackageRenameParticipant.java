/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.refactoring;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.osgi.service.resolver.*;
import com.siteview.mde.internal.core.WorkspaceModelManager;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class ManifestPackageRenameParticipant extends PDERenameParticipant {

	protected boolean initialize(Object element) {
		try {
			if (element instanceof IPackageFragment) {
				IPackageFragment fragment = (IPackageFragment) element;
				if (!fragment.containsJavaResources())
					return false;
				IJavaProject javaProject = (IJavaProject) fragment.getAncestor(IJavaElement.JAVA_PROJECT);
				IProject project = javaProject.getProject();
				if (WorkspaceModelManager.isPluginProject(project)) {
					fProject = javaProject.getProject();
					fElements = new HashMap();
					fElements.put(fragment, getArguments().getNewName());
					return true;
				}
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	public String getName() {
		return MDEUIMessages.ManifestPackageRenameParticipant_packageRename;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		super.addBundleManifestChange(result, pm);
		IMonitorModelBase model = MonitorRegistry.findModel(fProject);
		if (model != null) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				BundleDescription[] dependents = desc.getDependents();
				for (int i = 0; i < dependents.length; i++) {
					if (isAffected(desc, dependents[i])) {
						IMonitorModelBase candidate = MonitorRegistry.findModel(dependents[i]);
						if (candidate instanceof IBundlePluginModelBase) {
							IFile file = (IFile) candidate.getUnderlyingResource();
							addBundleManifestChange(file, result, pm);
						}
					}
				}
			}
		}
	}

	private boolean isAffected(BundleDescription desc, BundleDescription dependent) {
		ImportPackageSpecification[] imports = dependent.getImportPackages();
		Iterator iter = fElements.keySet().iterator();
		while (iter.hasNext()) {
			String name = ((IJavaElement) iter.next()).getElementName();
			for (int i = 0; i < imports.length; i++) {
				if (name.equals(imports[i].getName())) {
					BaseDescription supplier = imports[i].getSupplier();
					if (supplier instanceof ExportPackageDescription) {
						if (desc.equals(((ExportPackageDescription) supplier).getExporter()))
							return true;
					}
				}
			}
		}
		return false;
	}

}
