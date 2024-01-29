package com.graphikcorp.msproject.ProjectPlanGUI.objects;

import java.util.ArrayList;
import java.util.List;

public class TasksReminder {
    private int parentId;
    private String parentName;
    private String parentFullPath;
    private List<String> parentOwner;
    private List<String> assignees;
    private List<BasicTask> tasksNext;
    private List<BasicTask> tasksCurrent;
    private List<BasicTask> tasksLate;
    private List<BasicTask> tasksClosed;

    public List<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }

    public String getParentFullPath() {
        return parentFullPath;
    }

    public void setParentFullPath(String parentFullPath) {
        this.parentFullPath = parentFullPath;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<String> getParentOwner() {
        return parentOwner;
    }

    public void setParentOwner(List<String> parentOwner) {
        this.parentOwner = parentOwner;
    }

    public List<BasicTask> getTasksNext() {
        return tasksNext;
    }

    public void setTasksNext(List<BasicTask> tasksNext) {
        this.tasksNext = tasksNext;
    }

    public List<BasicTask> getTasksCurrent() {
        return tasksCurrent;
    }

    public void setTasksCurrent(List<BasicTask> tasksCurrent) {
        this.tasksCurrent = tasksCurrent;
    }

    public List<BasicTask> getTasksLate() {
        return tasksLate;
    }

    public void setTasksLate(List<BasicTask> tasksLate) {
        this.tasksLate = tasksLate;
    }

    public List<BasicTask> getTasksClosed() {
        return tasksClosed;
    }

    public void setTasksClosed(List<BasicTask> tasksClosed) {
        this.tasksClosed = tasksClosed;
    }

    public void refreshAssignees(){
        setAssignees(new ArrayList<String>());
        if(tasksLate != null && tasksLate.size()>0)
        {
            for (int i = 0; i < tasksLate.size(); i++) {
                BasicTask basicTask = tasksLate.get(i);
                for (int j = 0; basicTask.getResource() != null && j < basicTask.getResource().size(); j++) {
                    String resource = basicTask.getResource().get(j);
                    if(!assignees.contains(resource))
                        assignees.add(resource);
                }
            }
        }
        if(tasksNext != null && tasksNext.size()>0) {
            for (int i = 0; i < tasksNext.size(); i++) {
                BasicTask basicTask = tasksNext.get(i);
                for (int j = 0; basicTask.getResource() != null && j < basicTask.getResource().size(); j++) {
                    String resource = basicTask.getResource().get(j);
                    if(!assignees.contains(resource))
                        assignees.add(resource);
                }
            }
        }
        if(tasksCurrent != null && tasksCurrent.size()>0) {
            for (int i = 0; i < tasksCurrent.size(); i++) {
                BasicTask basicTask = tasksCurrent.get(i);
                for (int j = 0; basicTask.getResource() != null && j < basicTask.getResource().size(); j++) {
                    String resource = basicTask.getResource().get(j);
                    if(!assignees.contains(resource))
                        assignees.add(resource);
                }
            }
        }
    }
}
