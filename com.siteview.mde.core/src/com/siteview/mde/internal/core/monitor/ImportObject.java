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

import java.io.PrintWriter;
import java.io.Serializable;

import com.siteview.mde.core.ISourceObject;
import com.siteview.mde.core.IWritable;
import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorImport;
import com.siteview.mde.core.monitor.IMonitorModelBase;

public class ImportObject extends MonitorReference implements IWritable, Serializable, IWritableDelimiter {

	private static final long serialVersionUID = 1L;
	private IMonitorImport iimport;

	public ImportObject() {
		super();
	}

	public ImportObject(IMonitorImport iimport) {
		super(iimport.getId());
		this.iimport = iimport;
	}

	public ImportObject(IMonitorImport iimport, IMonitor plugin) {
		super(plugin);
		this.iimport = iimport;
	}

	public IMonitorImport getImport() {
		return iimport;
	}

	public boolean equals(Object object) {
		if (object instanceof ImportObject) {
			ImportObject io = (ImportObject) object;
			if (iimport.equals(io.getImport()))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		iimport.write(indent, writer);
	}

	public Object getAdapter(Class key) {
		if (key.equals(ISourceObject.class)) {
			if (iimport instanceof ISourceObject)
				return iimport;
		}
		return super.getAdapter(key);
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.plugin.PluginReference#reconnect(org.eclipse.pde.core.plugin.IPlugin)
	 */
	public void reconnect(IMonitorModelBase model) {
		super.reconnect(model);
		// Field that has transient fields:  Import
		IMonitorBase parent = model.getMonitorBase();
		// Note:  Cannot make into a 'IDocument*' interface.  The functionality
		// is usually done by the '*Node' classes; but, it is the opposite here
		if (iimport instanceof MonitorImport) {
			((MonitorImport) iimport).reconnect(model, parent);
		}
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// Note:  Cannot make into a 'IDocument*' interface.  The functionality
		// is usually done by the '*Node' classes; but, it is the opposite here
		if (iimport instanceof MonitorImport) {
			((MonitorImport) iimport).writeDelimeter(writer);
		}
	}

}
