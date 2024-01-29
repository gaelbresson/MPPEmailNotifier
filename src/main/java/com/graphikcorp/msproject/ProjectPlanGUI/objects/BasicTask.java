package com.graphikcorp.msproject.ProjectPlanGUI.objects;

import org.sqlite.util.StringUtils;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BasicTask implements Comparable<BasicTask> {
    private String ID;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String duration;
    private List<String> resource;
    private boolean active;
    private boolean canStart=false;
    private boolean isCritical=false;
    private Number percentage;
    private String fullText;
    private List<String> parent;
    private String parentsLI;
    private List<Object> valuesList;
    private List<String> dependencies;
    private List<String> dependenciesActors;
    private double delayWithBL =0;
    private double delayComparedToFinishDate=0;

    private String comments;

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public List<String> getDependenciesActors() {
        return dependenciesActors;
    }

    public void setDependenciesActors(List<String> dependenciesActors) {
        this.dependenciesActors = dependenciesActors;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public double getDelayComparedToFinishDate() {
        return delayComparedToFinishDate;
    }

    public void setDelayComparedToFinishDate(double delayComparedToFinishDate) {
        this.delayComparedToFinishDate = delayComparedToFinishDate;
    }

    public double getDelayWithBL() {
        return delayWithBL;
    }

    public void setDelayWithBL(double delayWithBL) {
        this.delayWithBL = delayWithBL;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public void setCanStart(boolean canStart) {
        this.canStart = canStart;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getParentsLI() {
        return parentsLI;
    }

    public void setParentsLI(String parentsLI) {
        this.parentsLI = parentsLI;
    }

    public List<String> getParent() {
        return parent;
    }

    public void setParent(List<String> parent) {
        this.parent = parent;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public List<String> getResource() {
        return resource;
    }

    public void setResource(List<String> resource) {
        this.resource = resource;
    }

    @Override
    public int compareTo(BasicTask o) {
        if (getEndDate() == null || o.getEndDate() == null) {
            return 0;
        }
        return getEndDate().compareTo(o.getEndDate());
    }

    public Number getPercentage() {
        return percentage;
    }

    public void setPercentage(Number percentage) {
        this.percentage = percentage;
    }

    public void setValuesList(List<Object> valuesList) {
        this.valuesList = valuesList;
    }

    public List<Object> getValuesList(){
        if(valuesList == null) {
            List<Object> result = new ArrayList<Object>();
            result.add(""+ID);
            result.add(name);
            SimpleDateFormat df = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
            result.add(""+df.format(startDate));
            result.add(""+df.format(endDate));
            result.add(null);
            result.add(percentage);
            result.add(String.join(",", dependencies));

            setValuesList(result);
        }
        return valuesList;
    }


    public List<String> getValuesListInString(){
        List<String> result = new ArrayList<String>();
        result.add("['"+ID+"'");
        result.add("'"+name+"'");
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
        result.add(""+df.format(startDate));
        result.add(""+df.format(endDate));
        result.add(""+Duration.between(startDate, endDate).get(ChronoUnit.NANOS));
        result.add(""+percentage.intValue());
        result.add("'"+String.join(",", dependencies)+"']");

        return result;
    }

    @Override
    public String toString() {
        return StringUtils.join(getValuesListInString(),",");
    }

}
