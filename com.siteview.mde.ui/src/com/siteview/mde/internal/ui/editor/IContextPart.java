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
package com.siteview.mde.internal.ui.editor;

import com.siteview.mde.core.IModelChangedListener;

public interface IContextPart extends IModelChangedListener {
	boolean isEditable();

	MDEFormPage getPage();

	String getContextId();

	void fireSaveNeeded();

	void cancelEdit();
}
