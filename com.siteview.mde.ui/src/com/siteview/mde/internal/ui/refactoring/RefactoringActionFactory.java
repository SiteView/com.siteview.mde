/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.refactoring;

import com.siteview.mde.core.monitor.IPluginExtensionPoint;
import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class RefactoringActionFactory {

	public static PDERefactoringAction createRefactorPluginIdAction() {
		return createRefactorPluginIdAction(MDEUIMessages.RenamePluginAction_label);
	}

	public static PDERefactoringAction createRefactorPluginIdAction(String label) {
		return new PDERefactoringAction(label, new RefactoringPluginInfo()) {

			public RefactoringProcessor getRefactoringProcessor(RefactoringInfo info) {
				return new RenamePluginProcessor(info);
			}

			public RefactoringWizard getRefactoringWizard(PDERefactor refactor, RefactoringInfo info) {
				return new RenamePluginWizard(refactor, info);
			}

		};
	}

	public static PDERefactoringAction createRefactorExtPointAction(String label) {
		return new PDERefactoringAction(label, getExtensionPointInfo()) {

			public RefactoringProcessor getRefactoringProcessor(RefactoringInfo info) {
				return new RenameExtensionPointProcessor(info);
			}

			public RefactoringWizard getRefactoringWizard(PDERefactor refactor, RefactoringInfo info) {
				return new RenameExtensionPointWizard(refactor, info);
			}

		};
	}

	private static RefactoringInfo getExtensionPointInfo() {
		return new RefactoringInfo() {

			public IMonitorModelBase getBase() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint) fSelection).getMonitorModel();
				}
				return null;
			}

			public String getCurrentValue() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint) fSelection).getId();
				}
				return null;
			}

		};
	}

}
