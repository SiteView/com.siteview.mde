/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction.java;

import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class SearchRepositoriesForIUProposal implements IJavaCompletionProposal {

	private String fPackageName;

	public SearchRepositoriesForIUProposal(String packageName) {
		fPackageName = packageName;
	}

	public int getRelevance() {
		return 0;
	}

	public void apply(IDocument document) {
		try {
			IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
			ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
			Command command = commandService.getCommand("com.siteview.mde.ui.searchTargetRepositories"); //$NON-NLS-1$
			IParameter parameter = command.getParameter("com.siteview.mde.ui.searchTargetRepositories.term"); //$NON-NLS-1$
			Parameterization parameterization = new Parameterization(parameter, fPackageName);
			ParameterizedCommand pc = new ParameterizedCommand(command, new Parameterization[] {parameterization});
			handlerService.executeCommand(pc, null);
		} catch (ExecutionException e) {
			MDEPlugin.log(e);
		} catch (NotDefinedException e) {
			MDEPlugin.log(e);
		} catch (NotEnabledException e) {
			MDEPlugin.log(e);
		} catch (NotHandledException e) {
			MDEPlugin.log(e);
		}
	}

	public String getAdditionalProposalInfo() {
		return NLS.bind(MDEUIMessages.SearchRepositoriesForIUProposal_description, fPackageName);
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		return NLS.bind(MDEUIMessages.SearchRepositoriesForIUProposal_message, fPackageName);
	}

	public Image getImage() {
		return MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_SITE_OBJ);
	}

	public Point getSelection(IDocument document) {
		return null;
	}

}
