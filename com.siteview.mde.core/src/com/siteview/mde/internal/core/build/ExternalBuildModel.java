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
package com.siteview.mde.internal.core.build;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.MDECore;

public class ExternalBuildModel extends BuildModel {

	private static final long serialVersionUID = 1L;
	private String fInstallLocation;

	public ExternalBuildModel(String installLocation) {
		fInstallLocation = installLocation;
	}

	public String getInstallLocation() {
		return fInstallLocation;
	}

	public boolean isEditable() {
		return false;
	}

	public void load() {
		InputStream stream = null;
		try {
			URL url = null;
			File file = new File(getInstallLocation());
			if (file.isFile() && file.getName().endsWith(".jar")) { //$NON-NLS-1$
				url = new URL("jar:file:" + file.getAbsolutePath() + "!/build.properties"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				url = new URL("file:" + file.getAbsolutePath() + IPath.SEPARATOR + ICoreConstants.BUILD_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			}
			stream = url.openStream();
			load(stream, false);
		} catch (IOException e) {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				MDECore.logException(e);
			}
		}
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		return (file.isFile()) ? file : new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
	}

	public boolean isInSync() {
		return true;
	}
}
