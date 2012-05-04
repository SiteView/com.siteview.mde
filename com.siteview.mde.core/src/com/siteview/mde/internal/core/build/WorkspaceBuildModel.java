/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package com.siteview.mde.internal.core.build;

import java.io.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IEditableModel;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.MDECore;

public class WorkspaceBuildModel extends BuildModel implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private IFile fUnderlyingResource;
	private boolean fDirty;
	private boolean fEditable = true;

	public WorkspaceBuildModel(IFile file) {
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
			writer.close();
		} catch (IOException e) {
			MDECore.logException(e);
		}
		return swriter.toString();
	}

	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	public boolean isDirty() {
		return fDirty;
	}

	public boolean isEditable() {
		return fEditable;
	}

	public void load() {
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
		} else {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
	}

	public boolean isInSync() {
		return true;
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
			stream = new ByteArrayInputStream(contents.getBytes("8859_1")); //$NON-NLS-1$
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
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
		getBuild().write("", writer); //$NON-NLS-1$
		fDirty = false;
	}

	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().toOSString();
	}
}
