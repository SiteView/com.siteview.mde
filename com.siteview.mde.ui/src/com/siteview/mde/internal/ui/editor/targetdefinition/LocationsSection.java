/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.targetdefinition;

import com.siteview.mde.internal.core.target.provisional.ITargetDefinition;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.editor.FormLayoutFactory;
import com.siteview.mde.internal.ui.shared.target.ITargetChangedListener;
import com.siteview.mde.internal.ui.shared.target.TargetLocationsGroup;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing the content of the target (bundle containers) in the target definition editor
 * @see DefinitionPage
 * @see TargetEditor
 */
public class LocationsSection extends SectionPart {

	private TargetLocationsGroup fContainerGroup;
	private TargetEditor fEditor;

	public LocationsSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 * 
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		sectionData.horizontalSpan = 2;
		section.setLayoutData(sectionData);
		section.setText(MDEUIMessages.LocationSection_0);

		section.setDescription(MDEUIMessages.TargetDefinitionContentPage_LocationDescription);
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		client.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		fContainerGroup = TargetLocationsGroup.createInForm(client, toolkit);
		fEditor.getTargetChangedListener().setLocationTree(fContainerGroup);
		fContainerGroup.addTargetChangedListener(fEditor.getTargetChangedListener());
		fContainerGroup.addTargetChangedListener(new ITargetChangedListener() {
			public void contentsChanged(ITargetDefinition definition, Object source, boolean resolve, boolean forceResolve) {
				markDirty();
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fContainerGroup.setInput(getTarget());
		super.refresh();
	}

}
