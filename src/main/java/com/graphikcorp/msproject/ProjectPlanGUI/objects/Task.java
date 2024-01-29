package com.graphikcorp.msproject.ProjectPlanGUI.objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.sqlite.util.StringUtils;

import java.util.List;

public class Task extends BasicTask {
	@JsonIgnore
	private List<BasicTask> predecessors;
	@JsonIgnore
	private List<BasicTask> successors;

	public List<BasicTask> getPredecessors() {
		return predecessors;
	}

	public void setPredecessors(List<BasicTask> predecessors) {
		this.predecessors = predecessors;
	}

	public List<BasicTask> getSuccessors() {
		return successors;
	}

	public void setSuccessors(List<BasicTask> successors) {
		this.successors = successors;
	}

	@Override
	public String toString() {
		return StringUtils.join(getValuesListInString(),",");
	}
}