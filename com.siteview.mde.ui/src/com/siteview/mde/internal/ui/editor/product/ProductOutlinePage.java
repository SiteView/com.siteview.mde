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
package com.siteview.mde.internal.ui.editor.product;

import com.siteview.mde.internal.core.iproduct.*;
import com.siteview.mde.internal.ui.editor.FormOutlinePage;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;

public class ProductOutlinePage extends FormOutlinePage {

	public ProductOutlinePage(MDEFormEditor editor) {
		super(editor);
	}

	public void sort(boolean sorting) {
	}

	protected Object[] getChildren(Object parent) {
		if (parent instanceof DependenciesPage) {
			DependenciesPage page = (DependenciesPage) parent;
			IProduct product = ((IProductModel) page.getModel()).getProduct();
			if (product.useFeatures())
				return product.getFeatures();
			return product.getPlugins();
		}
		return new Object[0];
	}

	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin)
			return DependenciesPage.PLUGIN_ID;
		if (item instanceof IProductFeature)
			return DependenciesPage.FEATURE_ID;
		return super.getParentPageId(item);
	}

}
