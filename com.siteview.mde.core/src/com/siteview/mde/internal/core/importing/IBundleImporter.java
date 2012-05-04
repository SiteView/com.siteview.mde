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

import com.siteview.mde.internal.core.importing.provisional.IBundleImporterDelegate;

/**
 * A bundle importer represents an instance of a bundle importer extension.
 * Clients contributing a bundle importer extension contribute an implementation
 * of {@link IBundleImporterDelegate} rather than this interface.
 * <p>
 * Clients contributing a bundle importer extension are intended to implement
 * {@link IBundleImporterDelegate}.
 * </p>
 * @since 3.6
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBundleImporter extends IBundleImporterDelegate {

	/**
	 * Returns this impoter's unique identifier.
	 * 
	 * @return identifier
	 */
	public String getId();

	/**
	 * Returns a short description of this importer, or <code>null</code> if unspecified.
	 * 
	 * @return description or <code>null</code>
	 */
	public String getDescription();

	/**
	 * Returns a human readable name for this importer.
	 * 
	 * @return name
	 */
	public String getName();
}
