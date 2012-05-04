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

import com.siteview.mde.internal.core.text.monitor.MonitorModel;

import com.siteview.mde.core.monitor.IExtensions;
import com.siteview.mde.core.monitor.IMonitorExtension;

import java.util.*;
import org.eclipse.jface.text.*;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.core.text.IEditingModel;
import com.siteview.mde.internal.ui.editor.AbstractFoldingStructureProvider;
import com.siteview.mde.internal.ui.editor.MDESourcePage;

public class PluginFoldingStructureProvider extends AbstractFoldingStructureProvider {

	private Map fPositionToElement = new HashMap();

	public PluginFoldingStructureProvider(MDESourcePage editor, IEditingModel model) {
		super(editor, model);
	}

	public void addFoldingRegions(Set currentRegions, IEditingModel model) throws BadLocationException {
		IExtensions extensions = ((MonitorModel) model).getExtensions();
		IMonitorExtension[] pluginExtensions = extensions.getExtensions();

		addFoldingRegions(currentRegions, pluginExtensions, model.getDocument());
	}

	private void addFoldingRegions(Set regions, IMonitorExtension[] nodes, IDocument document) throws BadLocationException {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentElementNode element = (IDocumentElementNode) nodes[i];
			int startLine = document.getLineOfOffset(element.getOffset());
			int endLine = document.getLineOfOffset(element.getOffset() + element.getLength());
			if (startLine < endLine) {
				int start = document.getLineOffset(startLine);
				int end = document.getLineOffset(endLine) + document.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}
			IDocumentElementNode[] children = element.getChildNodes();
			if (children != null) {
				addFoldingRegions(regions, children, document);
			}
		}
	}

	private void addFoldingRegions(Set regions, IDocumentElementNode[] nodes, IDocument document) throws BadLocationException {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentElementNode element = nodes[i];
			int startLine = document.getLineOfOffset(element.getOffset());
			int endLine = document.getLineOfOffset(element.getOffset() + element.getLength());
			if (startLine < endLine) {
				int start = document.getLineOffset(startLine);
				int end = document.getLineOffset(endLine) + document.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}
			IDocumentElementNode[] children = element.getChildNodes();
			if (children != null) {
				addFoldingRegions(regions, children, document);
			}
		}
	}

}
