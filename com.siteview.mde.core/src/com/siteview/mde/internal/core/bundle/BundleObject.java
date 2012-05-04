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
package com.siteview.mde.internal.core.bundle;

import java.io.PrintWriter;
import java.io.Serializable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.siteview.mde.core.IModelChangeProvider;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.IWritable;
import com.siteview.mde.core.ModelChangedEvent;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ibundle.IBundleModel;
import com.siteview.mde.internal.core.monitor.IWritableDelimiter;

public class BundleObject implements Serializable, IWritable, IWritableDelimiter {
	private static final long serialVersionUID = 1L;

	private transient IBundleModel model;

	public BundleObject() {
	}

	public IBundleModel getModel() {
		return model;
	}

	public void setModel(IBundleModel newModel) {
		model = newModel;
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status = new Status(IStatus.ERROR, MDECore.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

	protected void fireStructureChanged(BundleObject[] children, int changeType) {
		IModelChangedEvent e = new ModelChangedEvent(model, changeType, children, null);
		fireModelChanged(e);
	}

	protected void fireStructureChanged(BundleObject child, int changeType) {
		IModelChangedEvent e = new ModelChangedEvent(model, changeType, new Object[] {child}, null);
		fireModelChanged(e);
	}

	protected void fireModelChanged(IModelChangedEvent e) {
		IModelChangeProvider provider = model;
		provider.fireModelChanged(e);
	}

	protected void firePropertyChanged(BundleObject object, String property, Object oldValue, Object newValue) {
		IModelChangeProvider provider = model;
		provider.fireModelObjectChanged(object, property, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print(toString());
	}

	/**
	 * @param model
	 */
	public void reconnect(IBundleModel model) {
		// Transient Field:  Model
		this.model = model;
	}

	/* (non-Javadoc)
	 * @see com.siteview.mde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		writer.println(',');
		writer.print(' ');
	}

}
