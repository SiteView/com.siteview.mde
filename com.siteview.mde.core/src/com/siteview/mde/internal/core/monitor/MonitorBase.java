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
package com.siteview.mde.internal.core.monitor;

import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.monitor.IMatchRules;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorImport;
import com.siteview.mde.core.monitor.IMonitorLibrary;
import com.siteview.mde.internal.core.MDECoreMessages;
import com.siteview.mde.internal.core.MDEState;
import com.siteview.mde.internal.core.MDEStateHelper;
import org.osgi.framework.Version;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class MonitorBase extends AbstractExtensions implements IMonitorBase {
	private static final long serialVersionUID = 1L;

	private static final Version maxVersion = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private ArrayList fLibraries = new ArrayList();
	private ArrayList fImports = new ArrayList();
	private String fProviderName;
	private String fId;
	private String fVersion;
	private boolean fHasBundleStructure;
	private String fBundleSourceEntry;

	public MonitorBase(boolean readOnly) {
		super(readOnly);
	}

	public void add(IMonitorLibrary library) throws CoreException {
		ensureModelEditable();
		fLibraries.add(library);
		((MonitorLibrary) library).setInTheModel(true);
		((MonitorLibrary) library).setParent(this);
		fireStructureChanged(library, IModelChangedEvent.INSERT);
	}

	public void add(IMonitorImport iimport) throws CoreException {
		ensureModelEditable();
		((MonitorImport) iimport).setInTheModel(true);
		((MonitorImport) iimport).setParent(this);
		fImports.add(iimport);
		fireStructureChanged(iimport, IModelChangedEvent.INSERT);
	}

	public void add(IMonitorImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < iimports.length; i++) {
			((MonitorImport) iimports[i]).setInTheModel(true);
			((MonitorImport) iimports[i]).setParent(this);
			fImports.add(iimports[i]);
		}
		fireStructureChanged(iimports, IModelChangedEvent.INSERT);
	}

	public IMonitorLibrary[] getLibraries() {
		return (IMonitorLibrary[]) fLibraries.toArray(new IMonitorLibrary[fLibraries.size()]);
	}

	public IMonitorImport[] getImports() {
		return (IMonitorImport[]) fImports.toArray(new IMonitorImport[fImports.size()]);
	}

	public IMonitorBase getMonitorBase() {
		return this;
	}

	public String getProviderName() {
		return fProviderName;
	}

	public String getVersion() {
		return fVersion;
	}

	public String getId() {
		return fId;
	}

	void load(BundleDescription bundleDesc, MDEState state) {
		fId = bundleDesc.getSymbolicName();
		fVersion = bundleDesc.getVersion().toString();
		fName = state.getPluginName(bundleDesc.getBundleId());
		fProviderName = state.getProviderName(bundleDesc.getBundleId());
		fHasBundleStructure = state.hasBundleStructure(bundleDesc.getBundleId());
		fBundleSourceEntry = state.getBundleSourceEntry(bundleDesc.getBundleId());
		loadRuntime(bundleDesc, state);
		loadImports(bundleDesc);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_PROVIDER)) {
			setProviderName(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_LIBRARY_ORDER)) {
			swap((IMonitorLibrary) oldValue, (IMonitorLibrary) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	void load(Node node, String schemaVersion) {
		if (node == null)
			return;
		fSchemaVersion = schemaVersion;
		fId = getNodeAttribute(node, "id"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fProviderName = getNodeAttribute(node, "provider-name"); //$NON-NLS-1$
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child);
			}
		}
	}

	void loadRuntime(BundleDescription description, MDEState state) {
		String[] libraryNames = state.getLibraryNames(description.getBundleId());
		for (int i = 0; i < libraryNames.length; i++) {
			MonitorLibrary library = new MonitorLibrary();
			library.setModel(getModel());
			library.setInTheModel(true);
			library.setParent(this);
			library.load(libraryNames[i]);
			fLibraries.add(library);
		}
	}

	void loadRuntime(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().toLowerCase(Locale.ENGLISH).equals("library")) { //$NON-NLS-1$
				MonitorLibrary library = new MonitorLibrary();
				library.setModel(getModel());
				library.setInTheModel(true);
				library.setParent(this);
				fLibraries.add(library);
				library.load(child);
			}
		}
	}

	void loadImports(BundleDescription description) {
		BundleSpecification[] required = description.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			MonitorImport importElement = new MonitorImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			fImports.add(importElement);
			importElement.load(required[i]);
		}
		BundleDescription[] imported = MDEStateHelper.getImportedBundles(description);
		for (int i = 0; i < imported.length; i++) {
			MonitorImport importElement = new MonitorImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			fImports.add(importElement);
			importElement.load(imported[i]);
		}
	}

	void loadImports(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().toLowerCase(Locale.ENGLISH).equals("import")) { //$NON-NLS-1$
				MonitorImport importElement = new MonitorImport();
				importElement.setModel(getModel());
				importElement.setInTheModel(true);
				importElement.setParent(this);
				fImports.add(importElement);
				importElement.load(child);
			}
		}
	}

	protected void processChild(Node child) {
		String name = child.getNodeName().toLowerCase(Locale.ENGLISH);
		if (name.equals("runtime")) { //$NON-NLS-1$
			loadRuntime(child);
		} else if (name.equals("requires")) { //$NON-NLS-1$
			loadImports(child);

			// check to see if this model is a workspace model.  If so, don't load extensions/extension points through Node.
			// Instead, the extensions/extension points will be control by the extension registry.
			// One instance of where we want to load an external model's extensions/extension points from a Node is the convertSchemaToHTML ANT task.
		} else if (getModel().getUnderlyingResource() == null) {
			super.processChild(child);
		}
	}

	public void remove(IMonitorLibrary library) throws CoreException {
		ensureModelEditable();
		fLibraries.remove(library);
		((MonitorLibrary) library).setInTheModel(false);
		fireStructureChanged(library, IModelChangedEvent.REMOVE);
	}

	public void remove(IMonitorImport iimport) throws CoreException {
		ensureModelEditable();
		fImports.remove(iimport);
		((MonitorImport) iimport).setInTheModel(false);
		fireStructureChanged(iimport, IModelChangedEvent.REMOVE);
	}

	public void remove(IMonitorImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < iimports.length; i++) {
			fImports.remove(iimports[i]);
			((MonitorImport) iimports[i]).setInTheModel(false);
		}
		fireStructureChanged(iimports, IModelChangedEvent.REMOVE);
	}

	public void reset() {
		fLibraries = new ArrayList();
		fImports = new ArrayList();
		fProviderName = null;
		fSchemaVersion = null;
		fVersion = ""; //$NON-NLS-1$
		fName = ""; //$NON-NLS-1$
		fId = ""; //$NON-NLS-1$
		if (getModel() != null && getModel().getUnderlyingResource() != null) {
			fId = getModel().getUnderlyingResource().getProject().getName();
			fName = fId;
			fVersion = "0.0.0"; //$NON-NLS-1$
		}
		super.reset();
	}

	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		String oldValue = fProviderName;
		fProviderName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, fProviderName);
	}

	public void setVersion(String newVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = fVersion;
		fVersion = newVersion;
		firePropertyChanged(P_VERSION, oldValue, fVersion);
	}

	public void setId(String newId) throws CoreException {
		ensureModelEditable();
		String oldValue = fId;
		fId = newId;
		firePropertyChanged(P_ID, oldValue, fId);
	}

	public void internalSetVersion(String newVersion) {
		fVersion = newVersion;
	}

	public void swap(IMonitorLibrary l1, IMonitorLibrary l2) throws CoreException {
		ensureModelEditable();
		int index1 = fLibraries.indexOf(l1);
		int index2 = fLibraries.indexOf(l2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(MDECoreMessages.PluginBase_librariesNotFoundException);
		fLibraries.set(index2, l1);
		fLibraries.set(index1, l2);
		firePropertyChanged(this, P_LIBRARY_ORDER, l1, l2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IMonitorImport import1, IMonitorImport import2) throws CoreException {
		ensureModelEditable();
		int index1 = fImports.indexOf(import1);
		int index2 = fImports.indexOf(import2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(MDECoreMessages.PluginBase_importsNotFoundException);
		fImports.set(index2, import1);
		fImports.set(index1, import2);
		firePropertyChanged(this, P_IMPORT_ORDER, import1, import2);
	}

	public boolean isValid() {
		return hasRequiredAttributes();
	}

	protected boolean hasRequiredAttributes() {
		if (fName == null)
			return false;
		if (fId == null)
			return false;
		if (fVersion == null)
			return false;

		// validate libraries
		for (int i = 0; i < fLibraries.size(); i++) {
			IMonitorLibrary library = (IMonitorLibrary) fLibraries.get(i);
			if (!library.isValid())
				return false;
		}
		// validate imports
		for (int i = 0; i < fImports.size(); i++) {
			IMonitorImport iimport = (IMonitorImport) fImports.get(i);
			if (!iimport.isValid())
				return false;
		}
		return super.hasRequiredAttributes();
	}

	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		return SAXParserFactory.newInstance().newSAXParser();
	}

	public static int getMatchRule(VersionRange versionRange) {
		if (versionRange == null || versionRange.getMinimum() == null)
			return IMatchRules.NONE;

		Version minimum = versionRange.getMinimum();
		Version maximum = versionRange.getMaximum() == null ? maxVersion : versionRange.getMaximum();

		if (maximum.compareTo(maxVersion) >= 0)
			return IMatchRules.GREATER_OR_EQUAL;
		else if (minimum.equals(maximum))
			return IMatchRules.PERFECT;
		else if (!versionRange.isIncluded(minimum) || versionRange.isIncluded(maximum))
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMajor() == maximum.getMajor() - 1)
			return IMatchRules.COMPATIBLE;
		else if (minimum.getMajor() != maximum.getMajor())
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMinor() == maximum.getMinor() - 1)
			return IMatchRules.EQUIVALENT;
		else if (minimum.getMinor() != maximum.getMinor())
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMicro() == maximum.getMicro() - 1)
			return IMatchRules.PERFECT; // this is as close as we got

		return IMatchRules.NONE; // no real match rule for this
	}

	public boolean hasBundleStructure() {
		return fHasBundleStructure;
	}

	/**
	 * @return The bundle source entry from the manifest for this plugin or <code>null</code> if no entry exists.
	 */
	public String getBundleSourceEntry() {
		return fBundleSourceEntry;
	}

}
