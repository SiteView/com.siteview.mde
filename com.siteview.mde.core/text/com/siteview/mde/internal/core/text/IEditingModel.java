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

import org.eclipse.jface.text.IDocument;
import com.siteview.mde.core.IEditable;
import com.siteview.mde.core.IModel;
import com.siteview.mde.core.IModelChangeProvider;

public interface IEditingModel extends IModel, IModelChangeProvider, IReconcilingParticipant, IEditable {

	public IDocument getDocument();

	public void setStale(boolean stale);

	public boolean isStale();

	public String getCharset();

	public void setCharset(String charset);

}
