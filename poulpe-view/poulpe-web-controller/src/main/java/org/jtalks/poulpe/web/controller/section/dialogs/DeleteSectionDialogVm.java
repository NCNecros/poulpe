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
package org.jtalks.poulpe.web.controller.section.dialogs;

import org.jtalks.poulpe.model.entity.PoulpeSection;
import org.jtalks.poulpe.service.ForumStructureService;
import org.jtalks.poulpe.web.controller.section.ForumStructureVm;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.NotifyChange;

/**
 * This VM is responsible for deleting the section: whether to move the content of the section to the other section or
 * should the section be deleted with whole its content, etc.
 *
 * @author stanislav bashkirtsev
 */
public class DeleteSectionDialogVm extends AbstractDialogVm {
    private static final String SHOW_DIALOG = "showDialog";
    private final ForumStructureVm forumStructureVm;
    private final ForumStructureService forumStructureService;

    public DeleteSectionDialogVm(ForumStructureVm forumStructureVm, ForumStructureService forumStructureService) {
        this.forumStructureVm = forumStructureVm;
        this.forumStructureService = forumStructureService;
    }

    @Command
    @NotifyChange(SHOW_DIALOG)
    public void confirmDeleteSectionWithContent() {
        PoulpeSection selectedSection = forumStructureVm.getSelectedItemInTree().getSectionItem();
        forumStructureService.deleteSectionWithBranches(selectedSection);
        forumStructureVm.removeSectionFromTree(selectedSection);
    }

    @GlobalCommand
    @NotifyChange(SHOW_DIALOG)
    public void deleteSection() {
        showDialog();
    }
}