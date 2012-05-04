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
package com.siteview.mde.internal.core.site;

import com.siteview.mde.internal.core.isite.ISite;
import com.siteview.mde.internal.core.isite.ISiteArchive;
import com.siteview.mde.internal.core.isite.ISiteCategory;
import com.siteview.mde.internal.core.isite.ISiteCategoryDefinition;
import com.siteview.mde.internal.core.isite.ISiteDescription;
import com.siteview.mde.internal.core.isite.ISiteFeature;
import com.siteview.mde.internal.core.isite.ISiteModel;
import com.siteview.mde.internal.core.isite.ISiteModelFactory;
import com.siteview.mde.internal.core.isite.ISiteObject;

public class SiteModelFactory implements ISiteModelFactory {
	private ISiteModel model;

	public SiteModelFactory(ISiteModel model) {
		this.model = model;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createSite()
	 */
	public ISite createSite() {
		Site site = new Site();
		site.model = model;
		site.parent = null;
		return site;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createFeature()
	 */
	public ISiteFeature createFeature() {
		SiteFeature feature = new SiteFeature();
		feature.model = model;
		feature.parent = model.getSite();
		return feature;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createCategory()
	 */
	public ISiteCategory createCategory(ISiteFeature feature) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = feature;
		return category;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createCategoryDefinition()
	 */
	public ISiteCategoryDefinition createCategoryDefinition() {
		SiteCategoryDefinition def = new SiteCategoryDefinition();
		def.model = model;
		def.parent = model.getSite();
		return def;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createDescription()
	 */
	public ISiteDescription createDescription(ISiteObject parent) {
		SiteDescription desc = new SiteDescription();
		desc.model = model;
		desc.parent = parent;
		return desc;
	}

	/**
	 * @see com.siteview.mde.internal.core.isite.ISiteModelFactory#createArchive()
	 */
	public ISiteArchive createArchive() {
		SiteArchive archive = new SiteArchive();
		archive.model = model;
		archive.parent = model.getSite();
		return archive;
	}

}
