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
package com.siteview.mde.internal.ui.commands;

import java.util.*;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.commands.ICommandService;

public class CommandTreeContentProvider implements ITreeContentProvider {

	protected final int F_CAT_CONTENT = 0; // category grouped content
	protected final int F_CON_CONTENT = 1; // context grouped content

	private ICommandService fComServ;
	private TreeMap fCatMap; // mapping of commands to category
	private TreeMap fConMap; // mapping of commands to context
	private Viewer fViewer;
	private int fCurContent = F_CAT_CONTENT;

	public CommandTreeContentProvider(ICommandService comServ) {
		fComServ = comServ;
		init();
	}

	private void init() {
		fCatMap = new TreeMap(new Comparator() {
			public int compare(Object arg0, Object arg1) {
				String comA = CommandList.getText(arg0);
				String comB = CommandList.getText(arg1);
				if (comA != null)
					return comA.compareTo(comB);
				return +1; // undefined ids should go last
			}
		});
		fConMap = new TreeMap();
		Command[] commands = fComServ.getDefinedCommands();
		for (int i = 0; i < commands.length; i++) {
			/*
			 * IWorkbenchRegistryConstants.AUTOGENERATED_PREFIX = "AUTOGEN:::"
			 */
			// skip commands with autogenerated id's
			if (commands[i].getId().startsWith("AUTOGEN:::")) //$NON-NLS-1$
				continue;
			// populate category map
			try {
				Category cat = commands[i].getCategory();
				ArrayList list = (ArrayList) fCatMap.get(cat);
				if (list == null)
					fCatMap.put(cat, list = new ArrayList());
				list.add(commands[i]);
			} catch (NotDefinedException e) {
				continue;
			}
			// TODO: populate context map
			// can we easily group commands by context?
		}
	}

	public Object getParent(Object element) {
		if (element instanceof Command)
			try {
				return ((Command) element).getCategory();
			} catch (NotDefinedException e) {
				// undefined category - should never hit this as these commands
				// will not be listed
			}
		return null;
	}

	public void dispose() {
		fCatMap.clear();
		fConMap.clear();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Category) {
			ArrayList list = (ArrayList) fCatMap.get(parentElement);
			if (list != null)
				return list.toArray(new Command[list.size()]);
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof Category) {
			ArrayList list = (ArrayList) fCatMap.get(element);
			if (list != null)
				return list.size() > 0;
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		switch (fCurContent) {
			case F_CAT_CONTENT :
				return fCatMap.keySet().toArray();
			case F_CON_CONTENT :
				return new Object[0];
		}
		return null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = viewer;
	}

	public void refreshWithCategoryContent() {
		fCurContent = F_CAT_CONTENT;
		if (fViewer != null)
			fViewer.refresh();
	}

	public void refreshWithContextContent() {
		fCurContent = F_CON_CONTENT;
		if (fViewer != null)
			fViewer.refresh();
	}

}