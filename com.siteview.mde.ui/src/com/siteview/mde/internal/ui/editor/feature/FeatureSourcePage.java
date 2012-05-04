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
package com.siteview.mde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.ui.editor.*;

public class FeatureSourcePage extends XMLSourcePage {

	public FeatureSourcePage(MDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void updateSelection(SelectionChangedEvent e) {
		// NO-OP
	}

	protected ISortableContentOutlinePage createOutlinePage() {
		// TODO remove this method when the above three stubs
		// are implemented
		return new FeatureOutlinePage((MDEFormEditor) getEditor());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		// NO-OP
	}
}
