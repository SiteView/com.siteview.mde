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
package com.siteview.mde.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

public class ExternalJavaSearchClasspathContainer implements IClasspathContainer {
	private IClasspathEntry[] fEntries;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (fEntries == null) {
			try {
				SearchablePluginsManager manager = MDECore.getDefault().getSearchablePluginsManager();
				fEntries = manager.computeContainerClasspathEntries();
			} catch (CoreException e) {
				MDECore.logException(e);
			}
		}
		return fEntries;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return MDECoreMessages.ExternalJavaSearchClasspathContainer_description;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return MDECore.JAVA_SEARCH_CONTAINER_PATH;
	}

}
