/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.poulpe.web.controller;

import javax.annotation.Nonnull;

import org.jtalks.common.model.entity.Entity;

/**
 * This class represents state of currently selected in view entity.
 * 
 * @author Vyacheslav Zhivaev
 * 
 */
public class SelectedEntity<E extends Entity> {

    private E entity = null;

    /**
     * Gets currently selected entity.
     * 
     * @return the selected entity, can return {@code null} if nothing was selected
     */
    public E getEntity() {
        return entity;
    }

    /**
     * Sets entity wich currently selected in UI.
     * 
     * @param entity
     *            the new instance entity to set
     */
    public void setEntity(@Nonnull E entity) {
        this.entity = entity;
    }

}
