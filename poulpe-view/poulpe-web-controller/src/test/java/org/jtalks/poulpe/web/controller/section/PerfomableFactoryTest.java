package org.jtalks.poulpe.web.controller.section;

import static org.jtalks.poulpe.web.controller.utils.ObjectsFactory.fakeSection;
import static org.jtalks.poulpe.web.controller.utils.ObjectsFactory.sectionWithBranches;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.jtalks.poulpe.model.dto.PermissionChanges;
import org.jtalks.poulpe.model.entity.Jcommune;
import org.jtalks.poulpe.model.entity.PoulpeBranch;
import org.jtalks.poulpe.model.entity.PoulpeSection;
import org.jtalks.poulpe.service.BranchService;
import org.jtalks.poulpe.service.ComponentService;
import org.jtalks.poulpe.service.SectionService;
import org.jtalks.poulpe.web.controller.DialogManager.Performable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Alexey Grigorev
 */
public class PerfomableFactoryTest {

    private PerfomableFactory perfomableFactory;
    
    @Mock SectionPresenter presenter;
    @Mock SectionService sectionService;
    @Mock ComponentService componentService;
    @Mock BranchService branchService;
    @Mock ZkSectionView view;
    @Mock ZkSectionTreeComponent currentSectionTreeComponent;
    @Mock Jcommune forum;

    private PoulpeSection section = sectionWithBranches();
    private PoulpeBranch branch = (PoulpeBranch) section.getBranches().get(0);
    
    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);

        perfomableFactory = new PerfomableFactory(presenter);
        perfomableFactory.setCurrentSectionTreeComponent(currentSectionTreeComponent);
        perfomableFactory.setSectionService(sectionService);
        perfomableFactory.setComponentService(componentService);
        perfomableFactory.setBranchService(branchService);
        perfomableFactory.setSectionView(view);
    }
    
    @Test
    public void testCreatePerformableCreateSection() {
        Performable perf = perfomableFactory.saveSection(section, forum);
        
        perf.execute();

        verify(forum).addSection(section);
        verify(componentService).saveComponent(forum);
        verify(view).addSection(section);
        verify(view).closeEditSectionDialog();
    }

    @Test
    public void testUpdatePerformableEditSection() {
        Performable perf = perfomableFactory.updateSection(section);
        
        perf.execute();

        verify(currentSectionTreeComponent).updateSectionInView(section);
        verify(view).closeEditSectionDialog();
        verify(sectionService).saveSection(section);
    }

    @Test
    public void testDeleteBranchPerformable() {
        Performable perf = perfomableFactory.deleteBranch(branch);
        perf.execute();
        verify(sectionService).saveSection(section);
        verify(branchService, times(3)).changeGrants(any(PoulpeBranch.class), any(PermissionChanges.class));
        verify(presenter).updateView();
    }

    @Test
    public void testDeleteSectionSaveBranchesPerformable() {
        PoulpeSection section1 = fakeSection(), section2 = fakeSection();
        Performable perf = perfomableFactory.deleteSection(section1, section2);

        perf.execute();

        verify(sectionService).deleteAndMoveBranchesTo(section1, section2);
    }

    @Test
    public void testDeleteSectionWithoutSaveBranchesPerformable() {
        Performable perf = perfomableFactory.deleteSection(section, null);

        perf.execute();
        verify(sectionService).deleteRecursively(section);
    }

}
