/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech> - bug 241503
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.core.builders.DependencyLoop;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class LoopDialog extends TrayDialog {
	private DependencyLoop[] fLoops;
	private TreeViewer fLoopViewer;
	private Image fLoopImage;

	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof DependencyLoop)
				return ((DependencyLoop) parentElement).getMembers();
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			return element instanceof DependencyLoop;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fLoops;
		}

	}

	class LoopLabelProvider extends LabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof DependencyLoop)
				return fLoopImage;
			return MDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return MDEPlugin.getDefault().getLabelProvider().getText(element);
		}
	}

	public LoopDialog(Shell parentShell, DependencyLoop[] loops) {
		super(parentShell);
		fLoops = loops;
		MDELabelProvider provider = MDEPlugin.getDefault().getLabelProvider();
		fLoopImage = provider.get(MDEPluginImages.DESC_LOOP_OBJ);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.LOOP_DIALOG);
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 9;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 300;
		container.setLayoutData(gd);

		fLoopViewer = new TreeViewer(container);
		fLoopViewer.setContentProvider(new ContentProvider());
		fLoopViewer.setLabelProvider(new LoopLabelProvider());
		Tree tree = fLoopViewer.getTree();
		gd = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(gd);
		fLoopViewer.setInput(MDEPlugin.getDefault());
		fLoopViewer.expandAll();
		getShell().setText(MDEUIMessages.LoopDialog_title);
		return container;
	}

	/**
	 * @since 3.5
	 */
	protected boolean isResizable() {
		return true;
	}
}
