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
package com.siteview.mde.internal.core.monitor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import com.siteview.mde.core.IEditableModel;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelProvider;
import org.osgi.framework.Constants;

public class WorkspaceExtensionsModel extends AbstractExtensionsModel implements IEditableModel, IBundlePluginModelProvider {
	private static final long serialVersionUID = 1L;
	private IFile fUnderlyingResource;
	private boolean fDirty;
	private boolean fEditable = true;
	private transient IBundlePluginModelBase fBundleModel;

	protected NLResourceHelper createNLResourceHelper() {
		return new NLResourceHelper(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME, getNLLookupLocations());
	}

	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public WorkspaceExtensionsModel(IFile file) {
		fUnderlyingResource = file;
	}

	public void fireModelChanged(IModelChangedEvent event) {
		fDirty = true;
		super.fireModelChanged(event);
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		save(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
			MDECore.logException(e);
		}
		return swriter.toString();
	}

	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().removeLastSegments(1).addTrailingSeparator().toOSString();
	}

	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	public boolean isInSync() {
		if (fUnderlyingResource == null)
			return true;
		IPath path = fUnderlyingResource.getLocation();
		if (path == null)
			return false;
		return super.isInSync(path.toFile());
	}

	public boolean isDirty() {
		return fDirty;
	}

	public boolean isEditable() {
		return fEditable;
	}

	public void load() {
		if (fUnderlyingResource == null)
			return;
		getExtensions(true);
	}

	protected void updateTimeStamp() {
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	public void save() {
		if (fUnderlyingResource == null)
			return;
		ByteArrayInputStream stream = null;
		try {
			String contents = getContents();
			stream = new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
		} catch (CoreException e) {
			MDECore.logException(e);
		} catch (IOException e) {
			MDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				MDECore.logException(e);
			}
		}
	}

	public void save(PrintWriter writer) {
		if (isLoaded()) {
			fExtensions.write("", writer); //$NON-NLS-1$
		}
		fDirty = false;
	}

	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.plugin.AbstractExtensionsModel#createExtensions()
	 */
	protected Extensions createExtensions() {
		Extensions extensions = super.createExtensions();
		extensions.setIsFragment(fUnderlyingResource.getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
		return extensions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fUnderlyingResource.getName();
	}

	public void setBundleModel(IBundlePluginModelBase model) {
		fBundleModel = model;
	}

	public IBundlePluginModelBase getBundlePluginModel() {
		return fBundleModel;
	}

}
