/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.importing;

import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.importing.provisional.BundleImportDescription;
import com.siteview.mde.internal.core.importing.provisional.IBundleImporterDelegate;

/**
 * A bundle importer extension.
 * 
 * @since 3.6
 */
public class BundleImporterExtension implements IBundleImporter {

	private IBundleImporterDelegate delegate;
	private IConfigurationElement element;

	/**
	 * Constructs a bundle importer extension on the given element.
	 * 
	 * @param element contribution
	 */
	public BundleImporterExtension(IConfigurationElement element) {
		this.element = element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporterDelegate#validateImport(java.util.Map[])
	 */
	public BundleImportDescription[] validateImport(Map[] manifests) {
		try {
			return getDelegate().validateImport(manifests);
		} catch (CoreException e) {
			MDECore.log(e);
			return null;
		}
	}

	/**
	 * Returns underlying delegate.
	 * 
	 * @return delegate
	 * @exception CoreException if unable to instantiate delegate
	 */
	private synchronized IBundleImporterDelegate getDelegate() throws CoreException {
		if (delegate == null) {
			delegate = (IBundleImporterDelegate) element.createExecutableExtension("class"); //$NON-NLS-1$
		}
		return delegate;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.importing.IBundleImporterDelegate#performImport(org.eclipse.pde.core.importing.BundleImportDescription[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IProject[] performImport(BundleImportDescription[] descriptions, IProgressMonitor monitor) throws CoreException {
		return getDelegate().performImport(descriptions, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getId()
	 */
	public String getId() {
		return element.getAttribute("id"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getDescription()
	 */
	public String getDescription() {
		return element.getAttribute("description"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getName()
	 */
	public String getName() {
		return element.getAttribute("name"); //$NON-NLS-1$
	}

}
