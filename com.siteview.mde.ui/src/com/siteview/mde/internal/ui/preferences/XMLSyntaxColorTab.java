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
import com.siteview.mde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import com.siteview.mde.internal.ui.editor.text.*;

public class XMLSyntaxColorTab extends SyntaxColorTab {

	private static final String[][] COLOR_STRINGS = new String[][] {
	/*		{Display name, IPreferenceStore key}		*/
	{MDEUIMessages.EditorPreferencePage_text, IPDEColorConstants.P_DEFAULT}, {MDEUIMessages.EditorPreferencePage_proc, IPDEColorConstants.P_PROC_INSTR}, {MDEUIMessages.EditorPreferencePage_tag, IPDEColorConstants.P_TAG}, {MDEUIMessages.EditorPreferencePage_string, IPDEColorConstants.P_STRING}, {MDEUIMessages.XMLSyntaxColorTab_externalizedStrings, IPDEColorConstants.P_EXTERNALIZED_STRING}, {MDEUIMessages.EditorPreferencePage_comment, IPDEColorConstants.P_XML_COMMENT}};

	public XMLSyntaxColorTab(IColorManager manager) {
		super(manager);
	}

	protected IDocument getDocument() {
		StringBuffer buffer = new StringBuffer();
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("<plugin>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("<!-- Comment -->"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("   <extension point=\"some.id\">"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("      <tag name=\"%externalized\">body text</tag>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("   </extension>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("</plugin>"); //$NON-NLS-1$

		IDocument document = new Document(buffer.toString());
		new XMLDocumentSetupParticpant().setup(document);
		return document;
	}

	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return new XMLConfiguration(fColorManager);
	}

	protected String[][] getColorStrings() {
		return COLOR_STRINGS;
	}
}
