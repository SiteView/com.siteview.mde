/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.core.monitor;

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IIdentifiable;

/**
 * A model object that represents the content of a plug-in or
 * fragment manifest. This object contains data that is common
 * for both plug-ins and fragments.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IMonitorBase extends IExtensions, IIdentifiable {
	/**
	 * A property that will be used to notify that
	 * the provider name has changed.
	 */
	String P_PROVIDER = "provider-name"; //$NON-NLS-1$
	/**
	 * A property that will be used to notify
	 * that a version has changed.
	 */
	String P_VERSION = "version"; //$NON-NLS-1$

	/**
	 * A property that will be used to notify
	 * that library order in a plug-in has changed. 
	 */
	String P_LIBRARY_ORDER = "library_order"; //$NON-NLS-1$

	/**
	 * A property that will be used to notify
	 * that import order in a plug-in has changed. 
	 */
	String P_IMPORT_ORDER = "import_order"; //$NON-NLS-1$

	/**
	 * A property that will be used to notify
	 * that 3.0 release compatibility flag has been changed. 
	 */
	String P_SCHEMA_VERSION = "schema-version"; //$NON-NLS-1$

	/**
	 * Adds a new library to this plugin.
	 * This method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param library the new library
	 * @throws CoreException if the model is not editable
	 */
	void add(IMonitorLibrary library) throws CoreException;

	/**
	 * Adds a new plug-in import to this plugin.
	 * This method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param pluginImport the new import object
	 * @throws CoreException if the model is not editable
	 */
	void add(IMonitorImport pluginImport) throws CoreException;

	/**
	 * Removes an import from the plugin. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param pluginImport the import object
	 * @throws CoreException if the model is not editable
	 */
	void remove(IMonitorImport pluginImport) throws CoreException;

	/**
	 * Returns libraries referenced in this plug-in.
	 *
	 * @return an array of libraries
	 */
	IMonitorLibrary[] getLibraries();

	/**
	 * Returns imports defined in this plug-in.
	 *
	 * @return an array of import objects
	 */
	IMonitorImport[] getImports();

	/**
	 * Returns a name of the plug-in provider.
	 *
	 * @return plug-in provider name
	 */
	String getProviderName();

	/**
	 * Returns this plug-in's version
	 * @return the version of the plug-in
	 */
	String getVersion();

	/**
	 * Removes a library from the plugin. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param library the library object
	 * @throws CoreException if the model is not editable
	 */
	void remove(IMonitorLibrary library) throws CoreException;

	/**
	 * Sets the name of the plug-in provider.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param providerName the new provider name
	 * @throws CoreException if the model is not editable
	 */
	void setProviderName(String providerName) throws CoreException;

	/**
	 * Sets the version of the plug-in.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param version the new plug-in version
	 * @throws CoreException if the model is not editable
	 */
	void setVersion(String version) throws CoreException;

	/**
	 * Swaps the positions of the provided libraries
	 * in the list of libraries. Libraries are looked up
	 * by the class loader in the order of declaration.
	 * If two libraries contain classes with the same
	 * name, library order will determine which one is
	 * encountered first.
	 *
	 * @param l1 the first library object
	 * @param l2 the second library object
	 * @throws CoreException if the model is not editable
	 */
	void swap(IMonitorLibrary l1, IMonitorLibrary l2) throws CoreException;

	/**
	 * Swaps the positions of the plug-ins provided in
	 * in the dependency list. This order is the one used
	 * used by the classloader when loading classes.
	 *
	 * @param import1 the first import object
	 * @param import2 the second import object
	 * @throws CoreException if the model is not editable
	 */
	void swap(IMonitorImport import1, IMonitorImport import2) throws CoreException;

	/**
	 * Returns version of the manifest grammar
	 * @return version of the manifest grammar, or <samp>null</samp>
	 */
	String getSchemaVersion();

	/**
	 * Sets the R3.0 compatibility flag
	 * @param schemaVersion version of the manifest grammar
	 * @throws CoreException if the model is not editable
	 */
	void setSchemaVersion(String schemaVersion) throws CoreException;

}
