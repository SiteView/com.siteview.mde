/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.search;

import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;

import com.siteview.mde.internal.core.text.monitor.MonitorObjectNode;

import com.siteview.mde.core.monitor.*;

import org.eclipse.jface.text.*;
import org.eclipse.osgi.service.resolver.BaseDescription;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.osgi.framework.Constants;

// TODO this needs a rewrite
public class ManifestEditorOpener {

	public static IEditorPart open(Match match, boolean activate) throws PartInitException {
		IEditorPart editorPart = null;
		editorPart = ManifestEditor.open(match.getElement(), true);
		if (editorPart != null && editorPart instanceof ManifestEditor) {
			ManifestEditor editor = (ManifestEditor) editorPart;
			IDocument doc = editor.getDocument(match);
			if (doc != null) {
				Match exact = findExactMatch(doc, match, editor);
				editor.openToSourcePage(match.getElement(), exact.getOffset(), exact.getLength());
			}
		}
		return editorPart;
	}

	public static Match findExactMatch(IDocument document, Match match, IEditorPart editor) {
		if (match.getOffset() == -1 && match.getBaseUnit() == Match.UNIT_LINE)
			return new Match(match.getElement(), Match.UNIT_CHARACTER, 0, 0);
		IMonitorObject element = (IMonitorObject) match.getElement();
		String name = null;
		String value = null;
		IRegion region = null;
		// since Extension and Extension point matches don't contain line #'s, we need handle them differently (by trying to find matches in UI model)
		if (editor instanceof ManifestEditor && (element instanceof IMonitorExtension || element instanceof IMonitorExtensionPoint)) {
			region = getAttributeMatch((ManifestEditor) editor, element, document);
		} else {
			if (element instanceof IMonitorImport) {
				name = "plugin"; //$NON-NLS-1$
				value = ((IMonitorImport) element).getId();
			} else if (element instanceof IMonitor) {
				name = "id"; //$NON-NLS-1$
				value = ((IMonitor) element).getId();
			} else if (element instanceof IFragment) {
				name = "id"; //$NON-NLS-1$
				value = ((IFragment) element).getId();
			}

			region = getAttributeRegionForLine(document, name, value, match.getOffset());
		}
		if (region != null) {
			return new Match(element, Match.UNIT_CHARACTER, region.getOffset(), region.getLength());
		}
		return match;
	}

	private static IRegion getAttributeRegionForLine(IDocument document, String name, String value, int line) {
		try {
			int offset = document.getLineOffset(line) + document.getLineLength(line);
			return getAttributeRegion(document, name, value, offset);
		} catch (BadLocationException e) {
		}
		return null;
	}

	private static IRegion getAttributeRegion(IDocument document, String name, String value, int offset) {
		try {
			FindReplaceDocumentAdapter findReplaceAdapter = new FindReplaceDocumentAdapter(document);
			IRegion nameRegion = findReplaceAdapter.find(offset, name + "\\s*=\\s*\"" + value, false, false, false, true); //$NON-NLS-1$
			if (nameRegion != null) {
				if (document.get(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length()).equals(value))
					return new Region(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length());
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

	private static IRegion getAttributeRegion(IDocument document, String value, int offset) {
		try {
			FindReplaceDocumentAdapter findReplaceAdapter = new FindReplaceDocumentAdapter(document);
			IRegion nameRegion = findReplaceAdapter.find(offset, value, true, true, false, false);
			if (nameRegion != null) {
				if (document.get(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length()).equals(value))
					return new Region(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length());
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

	public static IRegion getAttributeMatch(ManifestEditor editor, Object object, IDocument document) {
		if (object instanceof IMonitorObject)
			return getAttributeMatch(editor, (IMonitorObject) object, document);

		// assume we have a base description
		String value = ((BaseDescription) object).getName();
		IManifestHeader header = ((IBundlePluginModelBase) editor.getAggregateModel()).getBundleModel().getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
		return getAttributeRegion(document, value, header.getOffset());
	}

	public static IRegion getAttributeMatch(ManifestEditor editor, String value, IDocument document) {
		return getAttributeRegion(document, value, 0);
	}

	// Try to find a match for an Extension or Extension point by looking through the extensions/extension points in UI model for match.
	private static IRegion getAttributeMatch(ManifestEditor editor, IMonitorObject object, IDocument document) {
		IMonitorObject[] elements = null;

		// find equivalent models in UI text model
		if (object instanceof IMonitorExtension)
			elements = ((IMonitorModelBase) editor.getAggregateModel()).getMonitorBase().getExtensions();
		else
			elements = ((IMonitorModelBase) editor.getAggregateModel()).getMonitorBase().getExtensionPoints();

		// iterate through the UI text models to find a match for a Search object.
		for (int i = 0; i < elements.length; i++) {
			IMonitorObject element = elements[i];
			if (element != null && object.equals(element)) {
				int offset = ((MonitorObjectNode) element).getOffset();
				offset += ((MonitorObjectNode) element).getLength();
				String name = (object instanceof IMonitorExtension) ? "point" : "id"; //$NON-NLS-1$ //$NON-NLS-2$
				String value = (object instanceof IMonitorExtension) ? ((IMonitorExtension) object).getPoint() : ((IMonitorExtensionPoint) object).getId();
				return getAttributeRegion(document, name, value, offset);
			}
		}
		return null;
	}
}
