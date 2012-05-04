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
package com.siteview.mde.internal.core.bundle;

import java.io.*;
import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import com.siteview.mde.core.IEditableModel;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.converter.MonitorConverter;
import com.siteview.mde.internal.core.ibundle.IBundle;
import com.siteview.mde.internal.core.ibundle.IBundleModelFactory;
import com.siteview.mde.internal.core.text.bundle.BundleModelFactory;
import com.siteview.mde.internal.core.util.CoreUtility;

public class WorkspaceBundleModel extends BundleModel implements IEditableModel {
	private static final long serialVersionUID = 1L;

	private IFile fUnderlyingResource;

	private boolean fDirty;

	private boolean fEditable = true;

	private IBundleModelFactory fFactory;

	private static final String MANIFEST_VERSION = "Manifest-Version"; //$NON-NLS-1$

	public WorkspaceBundleModel(IFile file) {
		fUnderlyingResource = file;
	}

	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
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

	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	public String getInstallLocation() {
		// Ensure we have an underlying resource
		if (fUnderlyingResource == null) {
			return null;
		}
		IPath path = fUnderlyingResource.getLocation();
		if (path == null)
			return null;
		return path.removeLastSegments(2).addTrailingSeparator().toOSString();
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
		if (fUnderlyingResource.exists()) {
			InputStream stream = null;
			try {
				stream = fUnderlyingResource.getContents(true);
				load(stream, false);
			} catch (Exception e) {
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
	}

	public boolean isInSync() {
		// If we have no underlying resource, it probably got deleted from right
		// underneath us; thus, the model is not in sync
		if (fUnderlyingResource == null) {
			return false;
		} else if (fUnderlyingResource.getLocation() == null) {
			return false;
		}
		return isInSync(fUnderlyingResource.getLocation().toFile());
	}

	protected void updateTimeStamp() {
		// If we have no underlying resource, it probably got deleted from right
		// underneath us; thus, there is nothing to update the time stamp for
		if (fUnderlyingResource == null) {
			return;
		} else if (fUnderlyingResource.getLocation() == null) {
			return;
		}
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	public void save() {
		if (fUnderlyingResource == null)
			return;
		ByteArrayInputStream stream = null;
		try {
			String contents = getContents();
			stream = new ByteArrayInputStream(contents.getBytes("UTF-8")); //$NON-NLS-1$
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				// prevents Core Exception when META-INF folder does not exist
				IContainer parent = fUnderlyingResource.getParent();
				if (!parent.exists() && parent instanceof IFolder)
					CoreUtility.createFolder((IFolder) parent);
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
			setLoaded(true);
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
		IBundle bundle = getBundle();
		Map headers = ((Bundle) bundle).getHeaders();
		boolean addManifestVersion = headers.get(MANIFEST_VERSION) == null;
		if (addManifestVersion)
			headers.put(MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		try {
			MonitorConverter.getDefault().writeManifest(headers, writer);
		} catch (IOException e) {
			MDECore.logException(e);
		} finally {
			if (addManifestVersion)
				headers.remove(MANIFEST_VERSION);
		}
		fDirty = false;
	}

	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

	public IBundleModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BundleModelFactory(this);
		return fFactory;
	}
}
