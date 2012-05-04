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
package com.siteview.mde.internal.ui.editor.actions;

import com.siteview.mde.internal.ui.editor.monitor.ExtensionHyperLink;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.MDESourcePage;
import com.siteview.mde.internal.ui.editor.text.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.ITextEditor;

public class HyperlinkAction extends Action implements MouseListener, KeyListener {

	protected IHyperlinkDetector fDetector;
	protected StyledText fStyledText;
	protected IHyperlink fLink;

	public HyperlinkAction() {
		setImageDescriptor(MDEPluginImages.DESC_LINK_OBJ);
		setEnabled(false);
	}

	public void run() {
		if (fLink != null)
			fLink.open();
	}

	public IHyperlink getHyperLink() {
		return fLink;
	}

	protected void removeListeners() {
		if (!hasDetector() || isTextDisposed())
			return;
		fStyledText.removeMouseListener(this);
		fStyledText.removeKeyListener(this);
	}

	protected void addListeners() {
		if (!hasDetector() || isTextDisposed())
			return;
		fStyledText.addMouseListener(this);
		fStyledText.addKeyListener(this);
	}

	public boolean detectHyperlink() {
		fLink = null;
		if (!hasDetector() || isTextDisposed())
			return false;

		Point p = fStyledText.getSelection();
		IHyperlink[] links = fDetector.detectHyperlinks(null, new Region(p.x, p.y - p.x), false);

		if (links == null || links.length == 0)
			return false;

		fLink = links[0];
		return true;
	}

	public void setTextEditor(ITextEditor editor) {
		StyledText newText = editor instanceof MDESourcePage ? ((MDESourcePage) editor).getViewer().getTextWidget() : null;
		if (fStyledText != null && fStyledText.equals(newText))
			return;

		// remove the previous listeners if there were any
		removeListeners();
		fStyledText = newText;
		fDetector = editor instanceof MDESourcePage ? (IHyperlinkDetector) ((MDESourcePage) editor).getAdapter(IHyperlinkDetector.class) : null;
		// Add new listeners, if hyperlinks are present
		addListeners();

		setEnabled(detectHyperlink());
		generateActionText();
	}

	protected boolean hasDetector() {
		return fDetector != null;
	}

	private boolean isTextDisposed() {
		return fStyledText == null || fStyledText.isDisposed();
	}

	public void generateActionText() {
		String text = MDEUIMessages.HyperlinkActionNoLinksAvailable;
		if (fLink instanceof JavaHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenType;
		else if (fLink instanceof ExtensionHyperLink)
			text = MDEUIMessages.HyperlinkActionOpenDescription;
		else if (fLink instanceof BundleHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenBundle;
		else if (fLink instanceof PackageHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenPackage;
		else if (fLink instanceof ResourceHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenResource;
		else if (fLink instanceof SchemaHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenSchema;
		else if (fLink instanceof TranslationHyperlink)
			text = MDEUIMessages.HyperlinkActionOpenTranslation;
		setText(text);
		setToolTipText(text);
	}

	public void mouseDoubleClick(MouseEvent e) {
		// Ignore
	}

	public void mouseDown(MouseEvent e) {
		// Ignore
	}

	public void mouseUp(MouseEvent e) {
		setEnabled(detectHyperlink());
		generateActionText();
	}

	public void keyPressed(KeyEvent e) {
		setEnabled(detectHyperlink());
		generateActionText();
	}

	public void keyReleased(KeyEvent e) {
		// Ignore
	}

}
