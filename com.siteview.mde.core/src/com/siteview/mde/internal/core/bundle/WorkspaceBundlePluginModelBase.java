/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.bundle;

import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IEditableModel;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.ISharedExtensionsModel;
import com.siteview.mde.internal.core.NLResourceHelper;
import com.siteview.mde.internal.core.MDEManager;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.monitor.WorkspaceExtensionsModel;
import com.siteview.mde.internal.core.monitor.WorkspaceMonitorModelBase;

import org.osgi.framework.Constants;

public abstract class WorkspaceBundlePluginModelBase extends WorkspaceMonitorModelBase implements IBundlePluginModelBase {

	private static final long serialVersionUID = 1L;
	private ISharedExtensionsModel fExtensionsModel = null;
	private IBundleModel fBundleModel = null;
	private IFile fPluginFile;

	public WorkspaceBundlePluginModelBase(IFile manifestFile, IFile pluginFile) {
		super(manifestFile, false);
		fPluginFile = pluginFile;
	}

	abstract public IMonitorBase createMonitorBase();

	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		if (fPluginBase == null)
			fPluginBase = createMonitorBase();

		if (fBundleModel == null) {
			fBundleModel = new WorkspaceBundleModel((IFile) getUnderlyingResource());
		}
		fBundleModel.load(stream, outOfSync);
	}

	public void save() {
		if (fExtensionsModel != null && fExtensionsModel.getExtensions().getExtensions().length > 0) {
			((BundlePluginBase) fPluginBase).updateSingleton(true);
			if (((IEditableModel) fExtensionsModel).isDirty())
				((IEditableModel) fExtensionsModel).save();
		}

		if (fBundleModel != null && ((IEditableModel) fBundleModel).isDirty())
			((IEditableModel) fBundleModel).save();
	}

	public String getContents() {
		if (fBundleModel != null && fBundleModel instanceof WorkspaceBundleModel)
			return ((WorkspaceBundleModel) fBundleModel).getContents();
		return null;
	}

	public String getBundleLocalization() {
		IBundle bundle = fBundleModel != null ? fBundleModel.getBundle() : null;
		return bundle != null ? bundle.getLocalization() : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

	protected NLResourceHelper createNLResourceHelper() {
		String localization = getBundleLocalization();
		return localization == null ? null : new NLResourceHelper(localization, MDEManager.getNLLookupLocations(this));
	}

	public IBundleModel getBundleModel() {
		if (fBundleModel == null) {
			IFile file = (IFile) getUnderlyingResource();
			fBundleModel = new WorkspaceBundleModel(file);
			if (file.exists()) {
				try {
					fBundleModel.load();
				} catch (CoreException e) {
				}
			}
		}
		return fBundleModel;
	}

	public ISharedExtensionsModel getExtensionsModel() {
		if (fExtensionsModel == null && fPluginFile != null) {
			fExtensionsModel = new WorkspaceExtensionsModel(fPluginFile);
			((WorkspaceExtensionsModel) fExtensionsModel).setBundleModel(this);
			((WorkspaceExtensionsModel) fExtensionsModel).setEditable(isEditable());
			if (fPluginFile.exists())
				try {
					fExtensionsModel.load();
				} catch (CoreException e) {
				}
		}
		return fExtensionsModel;
	}

	public void setBundleModel(IBundleModel bundleModel) {
		if (bundleModel instanceof IEditableModel)
			fBundleModel = bundleModel;
	}

	public void setExtensionsModel(ISharedExtensionsModel extensionsModel) {
		if (extensionsModel instanceof IEditableModel)
			fExtensionsModel = extensionsModel;
	}

}
