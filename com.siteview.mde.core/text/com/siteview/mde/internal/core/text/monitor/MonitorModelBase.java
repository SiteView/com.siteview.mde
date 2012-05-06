/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.text.monitor;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import com.siteview.mde.core.IModel;
import com.siteview.mde.core.IWritable;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.NLResourceHelper;
import com.siteview.mde.internal.core.MDEManager;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

public abstract class MonitorModelBase extends XMLEditingModel implements IMonitorModelBase, IDocumentListener {

	private MonitorBaseNode fPluginBase;
	private boolean fIsEnabled;
	private MonitorDocumentHandler fHandler;
	private IMonitorModelFactory fFactory;
	private String fLocalization;
	private boolean fHasTriedToCreateModel;

	public MonitorModelBase(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fFactory = new MonitorDocumentNodeFactory(this);
		document.addDocumentListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#createPluginBase()
	 */
	public IMonitorBase createMonitorBase(boolean isFragment) {
		if (isFragment) {
			fPluginBase = new FragmentNode();
			fPluginBase.setXMLTagName("fragment"); //$NON-NLS-1$
		} else {
			fPluginBase = new MonitorNode();
			fPluginBase.setXMLTagName("plugin"); //$NON-NLS-1$
		}
		fPluginBase.setInTheModel(true);
		fPluginBase.setModel(this);
		return fPluginBase;
	}

	protected IWritable getRoot() {
		return getMonitorBase();
	}

	public IMonitorBase createMonitorBase() {
		return createMonitorBase(isFragmentModel());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBuildModel()
	 */
	public IBuildModel getBuildModel() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase()
	 */
	public IMonitorBase getMonitorBase() {
		return getMonitorBase(true);
	}

	public IExtensions getExtensions() {
		return getMonitorBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase(boolean)
	 */
	public IMonitorBase getMonitorBase(boolean createIfMissing) {
		if (!fLoaded && !fHasTriedToCreateModel && createIfMissing) {
			try {
				createMonitorBase();
				load();
			} catch (CoreException e) {
			} finally {
				fHasTriedToCreateModel = true;
			}
		}
		return fPluginBase;
	}

	public IExtensions getExtensions(boolean createIfMissing) {
		return getMonitorBase(createIfMissing);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isEnabled()
	 */
	public boolean isEnabled() {
		return fIsEnabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		fIsEnabled = enabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginFactory()
	 */
	public IMonitorModelFactory getMonitorFactory() {
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getNLLookupLocation()
	 */
	public URL getNLLookupLocation() {
		try {
			String installLocation = getInstallLocation();
			return installLocation == null ? null : new URL("file:" + installLocation); //$NON-NLS-1$
		} catch (MalformedURLException e) {
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getFactory()
	 */
	public IExtensionsModelFactory getFactory() {
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		URL[] locations = MDEManager.getNLLookupLocations(this);
		return (locations.length == 0) ? null : new NLResourceHelper(fLocalization == null ? "plugin" : fLocalization, //$NON-NLS-1$
				locations);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.XMLEditingModel#createDocumentHandler(org.eclipse.pde.core.IModel)
	 */
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null)
			fHandler = new MonitorDocumentHandler(this, reconciling);
		return fHandler;
	}

	public IDocumentElementNode getLastErrorNode() {
		if (fHandler != null)
			return fHandler.getLastErrorNode();
		return null;
	}

	public void setLocalization(String localization) {
		fLocalization = localization;
	}

	/*
	 * @see com.siteview.mde.internal.core.text.AbstractEditingModel#dispose()
	 * @since 3.6
	 */
	public void dispose() {
		getDocument().removeDocumentListener(this);
		super.dispose();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	public void documentChanged(DocumentEvent event) {
		fHasTriedToCreateModel = false;
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

}
