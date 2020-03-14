package gr.university.thesis.controller;

import gr.university.thesis.entity.Item;
import gr.university.thesis.entity.Project;
import gr.university.thesis.entity.Sprint;
import gr.university.thesis.entity.User;
import gr.university.thesis.entity.enumeration.ItemPriority;
import gr.university.thesis.entity.enumeration.ItemType;
import gr.university.thesis.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * This is the project manager controller, here, a project manager can request actions such as viewing all projects
 * or creating one
 */
@Controller
@RequestMapping("/pm")
public class ProjectManagerController {

    ProjectService projectService;
    SessionService sessionService;
    ItemService itemService;
    SprintService sprintService;
    ItemSprintHistoryService itemSprintHistoryService;


    /**
     * constructor of this class, correct way to set the autowired attributes
     *
     * @param projectService: service that manages all the projects of the system
     * @param sessionService: service that manages the current session
     * @param itemService:    service that manages the items stored in the repository
     * @param sprintService:  service that manages the sprints stored in the repository
     */
    @Autowired
    public ProjectManagerController(ProjectService projectService, SessionService sessionService,
                                    ItemService itemService, SprintService sprintService,
                                    ItemSprintHistoryService itemSprintHistoryService) {
        this.projectService = projectService;
        this.sessionService = sessionService;
        this.itemService = itemService;
        this.sprintService = sprintService;
        this.itemSprintHistoryService = itemSprintHistoryService;
    }

    /**
     * this method calls the project service in order to create a new project and store it in the repository
     *
     * @param title:       input for the title of the new project
     * @param description: input for the description of the new project
     * @param session:     the current session, needed to find the creator of the project
     * @return: returns project panel template (redirects)
     */
    @PostMapping("/createProject")
    public String createProject(@RequestParam String title,
                                @RequestParam String description,
                                HttpSession session) {
        projectService.createProject(title, description, sessionService.getUserWithSessionId(session));
        return "redirect:/user/projectPanel";
    }

    /**
     * this method calls the project service in order to update an existing project and store it in the repository
     *
     * @param projectId:          the project id in order to find it on the repository
     * @param projectTitle:       the possibly updated title of the project
     * @param projectDescription: the possibly updated description of the project
     * @return: returns project panel template (redirects)
     */
    @RequestMapping(value = "/editProject", params = "action=update", method = RequestMethod.POST)
    public String updateProject(@RequestParam long projectId,
                                @RequestParam String projectTitle,
                                @RequestParam String projectDescription) {
        projectService.updateProject(projectId, projectTitle, projectDescription);
        return "redirect:/user/projectPanel";
    }

    /**
     * this method calls the project service in order to delete an existing project from the repository
     *
     * @param projectId: required id in order to delete the project from the repository
     * @return: returns project panel template (redirects)
     */
    @RequestMapping(value = "/editProject", params = "action=delete", method = RequestMethod.POST)
    public String deleteProject(@RequestParam long projectId) {
        projectService.deleteProject(projectId);
        return "redirect:/user/projectPanel";
    }

    /**
     * this method calls the project service in order to create a new item and store it in the repository
     *
     * @param title:       input for the title of the new project
     * @param description: input for the description of the new project
     * @param type:        input for the type of item
     * @param priority:    input for the priority of the item
     * @param effort:      input for the effort needed to complete the item
     * @param projectId:   project that this item belongs to
     * @param assigneeId:  id of the assigned user
     * @param session:     current session, needed to find the creator of this item
     * @return
     */
    @PostMapping("/createItem")
    public String createItem(@RequestParam String title,
                             @RequestParam String description,
                             @RequestParam String type,
                             @RequestParam String priority,
                             @RequestParam int effort,
                             @RequestParam long projectId,
                             @RequestParam long assigneeId,
                             @RequestParam long parentId,
                             HttpSession session) {
        itemService.createItem(title, description, ItemType.valueOf(type),
                ItemPriority.valueOf(priority), effort, new Project(projectId),
                new User(assigneeId), sessionService.getUserWithSessionId(session),
                new Item(parentId));
        return "redirect:/user/project/" + projectId;
    }

    /**
     * this method calls the item service in order to update an existing item and store it in the repository
     *
     * @param itemProjectId:   the id of the project, needed for the redirection
     * @param itemId:          id of the item, needed to find it on the repository
     * @param itemTitle:       title of the item
     * @param itemDescription: description of the item
     * @param itemType:        ItemType of the item
     * @param itemPriority:    ItemPriority of the item
     * @param itemEffort:      effort required to finish this item
     * @param itemAssigneeId:  the user that thisitem is assigned to
     * @return: returns a redirection to the current project backlog
     */
    @RequestMapping(value = "/editItem", params = "action=update", method = RequestMethod.POST)
    public String updateItem(@RequestParam long itemProjectId,
                             @RequestParam long itemId,
                             @RequestParam String itemTitle,
                             @RequestParam String itemDescription,
                             @RequestParam String itemType,
                             @RequestParam String itemPriority,
                             @RequestParam int itemEffort,
                             @RequestParam long itemAssigneeId,
                             @RequestParam long itemParentId,
                             @RequestParam long sprintId
    ) {

        //the associations need to be defined before the item is updated
        itemSprintHistoryService.manageItemSprintAssociation(new Item(itemId), new Sprint(sprintId), new Item(itemParentId));
        itemService.updateItem(itemId, itemTitle, itemDescription, itemType, itemPriority, itemEffort,
                new User(itemAssigneeId), new Item(itemParentId));
        return "redirect:/user/project/" + itemProjectId;
    }

    /**
     * this method calls the item service in order to delete an existing item from the repository
     *
     * @param itemId:        required id in order to delete the item from the repository
     * @param itemProjectId: required for the redirection to the project backlog
     * @return: returns a redirection to the current project backlog
     */
    @RequestMapping(value = "/editItem", params = "action=delete", method = RequestMethod.POST)
    public String deleteItem(@RequestParam long itemId,
                             @RequestParam long itemProjectId,
                             @RequestParam long sprintId) {
        //need to delete its associations first (if they exist)
        itemSprintHistoryService.removeItemToSprint(new Item(itemId), new Sprint(sprintId), null);
        itemService.deleteItem(itemId);
        return "redirect:/user/project/" + itemProjectId;
    }

    /**
     * this method calls the ItemSprintHistory service in order to move an item to a ready sprint
     *
     * @param itemId:        required id in order to know which item to move to the sprint
     * @param sprintId:      require id in order to know to which sprint it should move the item to
     * @param itemProjectId: required for the redirection to the project backlog
     * @return: returns a redirection to the current project backlog
     */
    @RequestMapping(value = "/editItem", params = "action=move", method = RequestMethod.POST)
    public String moveItemToSprint(@RequestParam long itemId,
                                   @RequestParam long sprintId,
                                   @RequestParam long itemProjectId) {
        itemSprintHistoryService.moveItemToSprint(new Item(itemId), new Sprint(sprintId), null);
        return "redirect:/user/project/" + itemProjectId;
    }

    /**
     * this method calls the ItemSprintHistory service in order to remove an item to a ready sprint
     *
     * @param itemId:        required id in order to know which item to move to the sprint
     * @param sprintId:      require id in order to know to which sprint it should move the item to
     * @param itemProjectId: required for the redirection to the project backlog
     * @return: returns a redirection to the current project backlog
     */
    @RequestMapping(value = "/editItem", params = "action=remove", method = RequestMethod.POST)
    public String removeItemFromSprint(@RequestParam long itemId,
                                       @RequestParam long sprintId,
                                       @RequestParam long itemProjectId) {
        System.out.println(itemId + " " + sprintId);
        itemSprintHistoryService.removeItemToSprint(new Item(itemId), new Sprint(sprintId), null);
        return "redirect:/user/project/" + itemProjectId;
    }

    /**
     * this method moves a ready sprint to the state of active
     *
     * @param sprintId:        the sprint that the user wants to start
     * @param sprintProjectId: the project that this sprint belongs to
     * @return: redirection to project page
     */
    @RequestMapping(value = "/editSprint", params = "action=start", method = RequestMethod.POST)
    public String startSprint(@RequestParam long sprintId,
                              @RequestParam long sprintProjectId,
                              @RequestParam String sprintGoal,
                              @RequestParam int sprintDuration) {
        sprintService.startSprint(sprintId, sprintGoal, sprintDuration);
        return "redirect:/user/project/" + sprintProjectId;
    }

    /**
     * this methods finishes a sprint and moves it to a finish state
     *
     * @param sprintId:        the sprint that the user wants to finish
     * @param sprintProjectId: the project that this sprint belongs to
     * @return: redirection to project page
     */
    @RequestMapping(value = "/editSprint", params = "action=finish", method = RequestMethod.POST)
    public String finishSprint(@RequestParam long sprintId,
                               @RequestParam long sprintProjectId) {
        sprintService.finishSprint(sprintId);
        return "redirect:/user/project/" + sprintProjectId;
    }
}
