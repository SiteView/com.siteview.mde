/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.siteview.mde.internal.core.text.monitor;

import com.siteview.mde.internal.core.text.DocumentElementNode;

/**
 * DocumentGenericNode
 *
 */
public class DocumentGenericNode extends DocumentElementNode {

	private static final long serialVersionUID = 1L;

	/**
	 * @param name
	 */
	public DocumentGenericNode(String name) {
		// NO-OP
		// Used just for generic element type
		setXMLTagName(name);
	}

}
