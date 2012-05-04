/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import org.eclipse.jface.text.IRegion;
import com.siteview.mde.internal.ui.editor.text.AbstractHyperlink;
import com.siteview.mde.internal.ui.search.ShowDescriptionAction;

public class ExtensionHyperLink extends AbstractHyperlink {

	public ExtensionHyperLink(IRegion region, String pointID) {
		super(region, pointID);
	}

	public void open() {
		new ShowDescriptionAction(fElement).run();
	}

}
