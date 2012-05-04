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

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.monitor.IMonitorObject;
import com.siteview.mde.core.monitor.IMonitorElement;
import com.siteview.mde.core.monitor.IMonitorParent;
import com.siteview.mde.internal.core.MDECoreMessages;

public abstract class MonitorParent extends IdentifiableMonitorObject implements IMonitorParent {
	private static final long serialVersionUID = 1L;
	protected ArrayList fChildren = null;

	public MonitorParent() {
	}

	public void add(int index, IMonitorObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().add(index, child);
		postAdd(child);
	}

	public void add(IMonitorObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().add(child);
		postAdd(child);
	}

	void appendChild(IMonitorElement child) {
		getChildrenList().add(child);
	}

	protected void postAdd(IMonitorObject child) {
		((MonitorObject) child).setInTheModel(true);
		((MonitorObject) child).setParent(this);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	public int getChildCount() {
		return getChildrenList().size();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IMonitorParent) {
			IMonitorParent target = (IMonitorParent) obj;
			if (target.getChildCount() != getChildCount())
				return false;
			IMonitorObject[] tchildren = target.getChildren();
			for (int i = 0; i < tchildren.length; i++) {
				IMonitorObject tchild = tchildren[i];
				IMonitorObject child = (IMonitorObject) getChildrenList().get(i);
				if (child == null || child.equals(tchild) == false)
					return false;
			}
			return true;
		}
		return false;
	}

	public int getIndexOf(IMonitorObject child) {
		return getChildrenList().indexOf(child);
	}

	public void swap(IMonitorObject child1, IMonitorObject child2) throws CoreException {
		ensureModelEditable();
		int index1 = getChildrenList().indexOf(child1);
		int index2 = getChildrenList().indexOf(child2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(MDECoreMessages.PluginParent_siblingsNotFoundException);
		getChildrenList().set(index2, child1);
		getChildrenList().set(index1, child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	public IMonitorObject[] getChildren() {
		return (IMonitorObject[]) getChildrenList().toArray(new IMonitorObject[getChildrenList().size()]);
	}

	public void remove(IMonitorObject child) throws CoreException {
		ensureModelEditable();
		getChildrenList().remove(child);
		((MonitorObject) child).setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}

	protected ArrayList getChildrenList() {
		if (fChildren == null)
			fChildren = new ArrayList(1);
		return fChildren;
	}

}
