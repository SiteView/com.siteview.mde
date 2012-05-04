/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.bundle;

import com.siteview.mde.core.monitor.IFragment;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.internal.core.ibundle.IBundleFragmentModel;

public class BundleFragmentModel extends BundlePluginModelBase implements IBundleFragmentModel {

	private static final long serialVersionUID = 1L;

	public IMonitorBase createMonitorBase() {
		BundleFragment bfragment = new BundleFragment();
		bfragment.setModel(this);
		return bfragment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragmentModel#getFragment()
	 */
	public IFragment getFragment() {
		return (IFragment) getMonitorBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return true;
	}
}
