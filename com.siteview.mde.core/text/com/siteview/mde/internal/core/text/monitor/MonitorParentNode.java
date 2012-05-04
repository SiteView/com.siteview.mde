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
package com.siteview.mde.internal.core.text.monitor;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.monitor.IMonitorObject;
import com.siteview.mde.core.monitor.IMonitorParent;
import com.siteview.mde.internal.core.text.IDocumentElementNode;

public class MonitorParentNode extends MonitorObjectNode implements IMonitorParent {

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#add(int, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void add(int index, IMonitorObject child) throws CoreException {
		addChildNode((IDocumentElementNode) child, index);
		fireStructureChanged(child, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#add(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void add(IMonitorObject child) throws CoreException {
		add(getChildCount(), child);
		child.setInTheModel(true);
		((MonitorObjectNode) child).setModel(getModel());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getChildCount()
	 */
	public int getChildCount() {
		return getChildNodes().length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getIndexOf(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public int getIndexOf(IMonitorObject child) {
		return indexOf((IDocumentElementNode) child);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#swap(org.eclipse.pde.core.plugin.IPluginObject, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void swap(IMonitorObject child1, IMonitorObject child2) throws CoreException {
		swap((IDocumentElementNode) child1, (IDocumentElementNode) child2);
		firePropertyChanged(this, P_SIBLING_ORDER, child1, child2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#getChildren()
	 */
	public IMonitorObject[] getChildren() {
		ArrayList result = new ArrayList();
		IDocumentElementNode[] nodes = getChildNodes();
		for (int i = 0; i < nodes.length; i++)
			result.add(nodes[i]);

		return (IMonitorObject[]) result.toArray(new IMonitorObject[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginParent#remove(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void remove(IMonitorObject child) throws CoreException {
		removeChildNode((IDocumentElementNode) child);
		child.setInTheModel(false);
		fireStructureChanged(child, IModelChangedEvent.REMOVE);
	}
}
