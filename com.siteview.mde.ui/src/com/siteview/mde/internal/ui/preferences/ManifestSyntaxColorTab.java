/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.preferences;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.context.ManifestDocumentSetupParticipant;
import com.siteview.mde.internal.ui.editor.text.*;

public class ManifestSyntaxColorTab extends SyntaxColorTab {

	private static final String[][] COLOR_STRINGS = new String[][] { {MDEUIMessages.ManifestSyntaxColorTab_reservedOSGi, IPDEColorConstants.P_HEADER_OSGI}, {MDEUIMessages.ManifestSyntaxColorTab_keys, IPDEColorConstants.P_HEADER_KEY}, {MDEUIMessages.ManifestSyntaxColorTab_assignment, IPDEColorConstants.P_HEADER_ASSIGNMENT}, {MDEUIMessages.ManifestSyntaxColorTab_values, IPDEColorConstants.P_HEADER_VALUE}, {MDEUIMessages.ManifestSyntaxColorTab_attributes, IPDEColorConstants.P_HEADER_ATTRIBUTES}};

	public ManifestSyntaxColorTab(IColorManager manager) {
		super(manager);
	}

	protected IDocument getDocument() {
		StringBuffer buffer = new StringBuffer();
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer.append("Manifest-Version: 1.0"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Bundle-Name: %name"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Bundle-SymbolicName: com.example.xyz"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Require-Bundle:"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" org.eclipse.core.runtime;bundle-version=\"3.0.0\","); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" org.eclipse.ui;resolution:=optional"); //$NON-NLS-1$
		IDocument document = new Document(buffer.toString());
		new ManifestDocumentSetupParticipant().setup(document);
		return document;
	}

	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return new ManifestConfiguration(fColorManager);
	}

	protected String[][] getColorStrings() {
		return COLOR_STRINGS;
	}

}
