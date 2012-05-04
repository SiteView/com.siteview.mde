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

package com.siteview.mde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.ui.*;

/**
 * This action toggles whether the Outline page links its selection to the
 * active editor.
 * 
 * @since 3.0
 */
public class ToggleLinkWithEditorAction extends Action {

	MDEFormEditor fEditor;

	public ToggleLinkWithEditorAction(MDEFormEditor editor) {
		super(MDEUIMessages.ToggleLinkWithEditorAction_label);
		boolean isLinkingEnabled = MDEPlugin.getDefault().getPreferenceStore().getBoolean("ToggleLinkWithEditorAction.isChecked"); //$NON-NLS-1$
		setChecked(isLinkingEnabled);
		fEditor = editor;
		setToolTipText(MDEUIMessages.ToggleLinkWithEditorAction_toolTip);
		setDescription(MDEUIMessages.ToggleLinkWithEditorAction_description);
		setImageDescriptor(MDEPluginImages.DESC_LINK_WITH_EDITOR);
		setDisabledImageDescriptor(MDEPluginImages.DESC_LINK_WITH_EDITOR_DISABLED);
	}

	public void run() {
		MDEPlugin.getDefault().getPreferenceStore().setValue("ToggleLinkWithEditorAction.isChecked", isChecked()); //$NON-NLS-1$
		if (isChecked())
			fEditor.synchronizeOutlinePage();
	}
}
