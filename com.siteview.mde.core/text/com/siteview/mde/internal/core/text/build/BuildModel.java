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
package com.siteview.mde.internal.core.text.build;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import com.siteview.mde.core.build.IBuild;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.core.build.IBuildModelFactory;
import com.siteview.mde.internal.core.NLResourceHelper;
import com.siteview.mde.internal.core.text.AbstractEditingModel;

public class BuildModel extends AbstractEditingModel implements IBuildModel {

	//private Properties fProperties;
	private BuildModelFactory fFactory;
	private Build fBuild;

	/**
	 * @param document
	 * @param isReconciling
	 */
	public BuildModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fLoaded = true;
			((Build) getBuild()).load(source);
		} catch (IOException e) {
			fLoaded = false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#adjustOffsets(org.eclipse.jface.text.IDocument)
	 */
	public void adjustOffsets(IDocument document) {
		((Build) getBuild()).adjustOffsets(document);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getBuild()
	 */
	public IBuild getBuild() {
		if (fBuild == null)
			fBuild = new Build(this);
		return fBuild;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getFactory()
	 */
	public IBuildModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BuildModelFactory(this);
		return fFactory;
	}
}