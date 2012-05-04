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
package com.siteview.mde.internal.core.text;

import com.siteview.mde.core.IWritable;

public interface IDocumentKey extends IWritable, IDocumentRange {
	void setName(String name);

	String getName();

	void setOffset(int offset);

	void setLength(int length);

	String write();

}
