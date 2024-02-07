package com.graphikcorp.msproject.ProjectPlanGUI.controller;

import com.graphikcorp.msproject.ProjectPlanGUI.objects.BasicTask;
import com.graphikcorp.msproject.ProjectPlanGUI.objects.Task;
import com.graphikcorp.msproject.ProjectPlanGUI.objects.TasksReminder;
import net.sf.mpxj.*;
import net.sf.mpxj.reader.UniversalProjectReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.unbescape.html.HtmlEscape;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Controller
public class TaskController {
	// This task reminder list will contains all reminder email we have to send
	private List<TasksReminder> taskList2 = new ArrayList<>();
	Logger logger = LogManager.getLogger(LoggingController.class);

	//this specific task reminder will be used for each mail to send and populate the email templates
	private TasksReminder taskToRemind = new TasksReminder();

	// Here are the configuration values that may be overloaded by the configuration file (application.properties)
	@Value("${notif.email.FROM:John.Doe@mail.com}")
	private String notificationEMailFROM;
	@Value("${notif.email.BCC:John.Doe@mail.com,Emily.Doe@mail.com}")
	private String notificationEMailBCC;
	@Value("${notif.email.TEST:John.Doe@mail.com,Emily.Doe@mail.com}")
	private String notificationEMailTestDest;
	@Value("${mail.smtp.host:smtp.mail.com}")
	private String MAIL_SMTP_HOST;
	@Value("${mail.smtp.port:25}")
	private String MAIL_SMTP_PORT;
	@Value("${mppFilePath:C:\\Users\\johndoe\\myMPPFile.mpp}")
	private String PROJECT_FILE_PATH;
	@Value("${outlineFilter}")
	private String OUTLINE_FILTER;
	@Value("${web.context:}")
	private String WEB_CONTEXT;
	@Autowired
	Environment environment;

	// Endpoint for the root URL
	@GetMapping("/")
	public String showIndexPage(Model model) {
		// by accessing this page, the task list is build a first time with current parameters established
		taskList2 = listTasksPerParent();
		model.addAttribute("PROJECT_FILE_PATH", PROJECT_FILE_PATH);
		model.addAttribute("OUTLINE_FILTER", OUTLINE_FILTER);
		model.addAttribute("TEST_DEST_EMAIL", notificationEMailTestDest);
		return "index"; // Return the name of the view template to render
	}

	// Endpoint to display the list of tasks based on the resource
	@GetMapping("/reminderContent")
	public String getReminderContent(Model model) {

		model.addAttribute("taskToRemind", taskToRemind);
		return "reminderContent"; // Return the name of the view template to render
	}

	//This methode is used to build the HTML Email to send base on the
	private String getReminderContent(boolean late, boolean current, boolean next, int nbDays, boolean test){

		String result = "Sorry but we faced trouble to send you the list of Task for the cut over, please contact back the cut over team about that";
		try {
			HttpClient client = HttpClient.newHttpClient();
			String contentURI = "http://"+environment.getProperty("server.host")+":"+environment.getProperty("server.port")+"/reminderContent?";
			if(late)
				contentURI=contentURI+"late=true";
			if(current)
				if(late)
					contentURI = contentURI+"&current=true";
				else
					contentURI = contentURI+"current=true";
			if(next)
				if(late || current)
					contentURI = contentURI+"&next=true";
				else
					contentURI = contentURI+"next=true";
			if(nbDays > 0)
				if(late || current || next)
					contentURI = contentURI+"&nbDays="+nbDays;
				else
					contentURI = contentURI+"nbDays="+nbDays;
			if(test)
				if(late || current || next || nbDays > 0)
					contentURI = contentURI+"&test=true";
				else
					contentURI = contentURI+"test=true";

			logger.info("URL called for the email Content:"+contentURI);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(contentURI))
					.build();
			HttpResponse<String> response = client.send(request,
					HttpResponse.BodyHandlers.ofString());

			result = response.body();
		}catch(IOException | InterruptedException ioe){
			ioe.printStackTrace();
		}
		return result;
	}

	private List<TasksReminder>listTasksPerParent() {

		List<TasksReminder> tasksReminder = new ArrayList<TasksReminder>();

		try{
			// Specify the path to the Microsoft Project file
			String filePath = PROJECT_FILE_PATH;

			// Load the project file

			UniversalProjectReader reader = new UniversalProjectReader();
			ProjectFile project;
			logger.info("Reading file : "+filePath);
			project = reader.read(filePath);

			LocalDate ld_planEnd = LocalDate.now().plusDays(2000);
			// Iterate over tasks
			for (net.sf.mpxj.Task task : project.getTasks()) {

//				if (task.getResourceAssignments().size() > 0 && taskStartDate != null && taskStartDate.after(startDate) && taskStartDate.before(endDate)) {

				//Get All active tasks with any start date defined and filter on outline level.
				if (task.getActive() && task.getParentTask() != null
//						&& task.getResourceAssignments().size() > 0
						&& (OUTLINE_FILTER == null || OUTLINE_FILTER.isEmpty() || task.getOutlineNumber().startsWith(OUTLINE_FILTER) || task.getOutlineNumber().equals(OUTLINE_FILTER))
						&& !task.getExternalTask()
						&& (task.getPlannedStart() != null || task.getBaselineStart() != null || task.getStart()!=null)
//						&& ((task.getPercentageComplete() == null || task.getPercentageComplete().doubleValue() < 100.0)
//								&& planEnd.after(task.getFinish()))
				){
					//If the current task contains Subtask, then group subtasks of it în Next, Current or late tasks.
					List<net.sf.mpxj.Task> subTasks = task.getChildTasks();
					if(subTasks != null && subTasks.size()>0){
						List<BasicTask> next = new ArrayList<BasicTask>();
						List<BasicTask> current = new ArrayList<BasicTask>();
						List<BasicTask> late = new ArrayList<BasicTask>();
						List<BasicTask> closed = new ArrayList<BasicTask>();

						List<String> assignees = new ArrayList<String>();

						for (net.sf.mpxj.Task subTask : subTasks) {
							if (subTask.getActive()) {
								//Take the later available Start
								Date latestStart = subTask.getPlannedStart();
								if (latestStart == null) {
									latestStart = subTask.getStart();
								}
								if (latestStart == null) {
									latestStart = subTask.getBaselineStart();
								}
								//This case should happen only if the planned start is not null and before the Start
								if (subTask.getStart() != null && subTask.getStart().after(latestStart)) {
									latestStart = subTask.getStart();
								}
								//This case should happen only if the planned start or the start is not null and before the Baseline Start
								if (subTask.getBaselineStart() != null && subTask.getBaselineStart().after(latestStart)) {
									latestStart = subTask.getBaselineStart();
								}

								Date latestFinish = subTask.getPlannedFinish();
								if (latestFinish == null) {
									latestFinish = subTask.getFinish();
								}
								if (latestFinish == null) {
									latestFinish = subTask.getBaselineFinish();
								}
								//This case should happen only if the planned finish is not null and before the Finish
								if (subTask.getFinish() != null && subTask.getFinish().after(latestFinish)) {
									latestFinish = subTask.getFinish();
								}
								//This case should happen only if the planned finish or the finish is not null and before the Baseline finish
								if (subTask.getBaselineFinish() != null && subTask.getBaselineFinish().after(latestFinish)) {
									latestFinish = subTask.getBaselineFinish();
								}

								//Sort task on the right list dependeing on the latest Start Date and the completion
								// Tasks done finishing before now
								if ((subTask.getPercentageComplete() != null && subTask.getPercentageComplete().doubleValue() > 99.0)) {
									//Done tasks
									BasicTask bt = getBasicTaskFromMPPTask(subTask, latestStart, latestFinish);
									closed.add(bt);
									if (bt.getResource() != null)
										for (int j = 0; j < bt.getResource().size(); j++) {
											String resource = bt.getResource().get(j);
											if (!assignees.contains(resource.replace(';', ',')))
												assignees.add(resource);
										}
								} else if (latestStart.after(new Date())) {
									//Next tasks not finished
									BasicTask bt = getBasicTaskFromMPPTask(subTask, latestStart, latestFinish);
									next.add(bt);
									if (bt.getResource() != null)
										for (int j = 0; j < bt.getResource().size(); j++) {
											String resource = bt.getResource().get(j);

											if (!assignees.contains(resource.replace(';', ',')))
												assignees.add(resource);
										}
								} else if (latestFinish.before(new Date())) {
									//Late tasks not finished
									BasicTask bt = getBasicTaskFromMPPTask(subTask, latestStart, latestFinish);
									late.add(bt);
									if (bt.getResource() != null)
										for (int j = 0; j < bt.getResource().size(); j++) {
											String resource = bt.getResource().get(j);
											if (!assignees.contains(resource.replace(';', ',')))
												assignees.add(resource);
										}
								} else {
									BasicTask bt = getBasicTaskFromMPPTask(subTask, latestStart, latestFinish);
									current.add(bt);
									if (bt.getResource() != null)
										for (int j = 0; j < bt.getResource().size(); j++) {
											String resource = bt.getResource().get(j);
											if (!assignees.contains(resource.replace(';', ',')))
												assignees.add(resource);
										}
								}
							}
						}

						Date latestPStart = task.getPlannedStart();
						if(latestPStart == null)
							latestPStart=task.getStart();
						if(latestPStart == null)
							latestPStart=task.getBaselineStart();
						if(task.getStart() != null && task.getStart().after(latestPStart))
							latestPStart=task.getStart();
						if(task.getBaselineStart() != null && task.getBaselineStart().after(latestPStart))
							latestPStart=task.getBaselineStart();

						TasksReminder tr = new TasksReminder();

						StringBuilder parentFullPath = new StringBuilder(task.getName());
						net.sf.mpxj.Task ptask = task.getParentTask();
						while (ptask != null && ptask.getParentTask() != null){
							parentFullPath.insert(0, ptask.getName() + " > ");
							ptask=ptask.getParentTask();
						}

						tr.setParentFullPath(parentFullPath.toString());
						current.sort(Comparator.comparing(BasicTask::getEndDate));
						tr.setTasksCurrent(current);
						closed.sort(Comparator.comparing(BasicTask::getEndDate));
						tr.setTasksClosed(closed);
						next.sort(Comparator.comparing(BasicTask::getEndDate));
						tr.setTasksNext(next);
						late.sort(Comparator.comparing(BasicTask::getEndDate));
						tr.setTasksLate(late);
						tr.setParentName(task.getName());
						tr.setParentId(task.getID());
						tr.setParentOwner(getTaskFromMPPTask(task).getResource());
						tr.setAssignees(assignees);
						tasksReminder.add(tr);
						taskToRemind = tr;
					}
					// Else Do nothing as it's a leaf
				}
			}
		}
		catch(MPXJException mpxje){
			mpxje.printStackTrace();
		}

		return tasksReminder;
	}

	private Task getTaskFromMPPTask(net.sf.mpxj.Task task) {
		Task aTask = new Task();
		aTask.setDuration(task.getDurationText());
		aTask.setID(task.getID().toString());
		aTask.setName(task.getName());
		List<String> resources = new ArrayList<>();
		if(task.getResourceAssignments() != null && !task.getResourceAssignments().isEmpty()) {
			List<ResourceAssignment> SuccessorResources = task.getResourceAssignments();
			for (ResourceAssignment resourceAssignment : SuccessorResources) {
				String resource;
				resource = extractSubstring(resourceAssignment.toString(), " resource=", " start=");
				if (resource!=null && !resources.contains(resource.replaceAll("\\[.*?\\]", "")))
					resources.add(resource.replaceAll("\\[.*?\\]", ""));
			}
		}
		aTask.setResource(resources);

		return aTask;
	}

	/**
	 * Method extracting basic data from a MPP task
	 * @param task the task extracted from the MPP file
	 * @param latestStart used to define the start date of the task depending on previous analysis done the Baseline and actual start date
	 * @param latestFinish used to define the finish date of the task depending on previous analysis done the Baseline and actual finish date
	 * @return a {@link BasicTask} that will be used in the reminders Emails
	 */
	private BasicTask getBasicTaskFromMPPTask(net.sf.mpxj.Task task, Date latestStart, Date latestFinish) {

		//Build the task to build based on the MPP task
		BasicTask aTask = new BasicTask();
		aTask.setDuration(task.getDurationText());
		aTask.setID(task.getID().toString());
		aTask.setName(task.getName());
		aTask.setComments(HtmlEscape.escapeHtml4Xml( task.getNotes() ).replace( System.getProperty("line.separator"), "<br />" ));
		//Build the resources list defined for the task.
		List<String> resources = new ArrayList<>();
		if(task.getResourceAssignments() != null && !task.getResourceAssignments().isEmpty()) {
			List<ResourceAssignment> SuccessorResources = task.getResourceAssignments();
			for (ResourceAssignment resourceAssignment : SuccessorResources) {
				String resource;
				resource = extractSubstring(resourceAssignment.toString(), " resource=", " start=");
				if (!resources.contains(resource.replaceAll("\\[.*?\\]", "")))
					resources.add(resource.replaceAll("\\[.*?\\]", ""));
			}
		}
		aTask.setResource(resources);

		aTask.setStartDate(latestStart.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime());
		aTask.setEndDate(latestFinish.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime());

		boolean predecessorIsDone = true;
		for (int i = 0; predecessorIsDone && i < task.getPredecessors().size(); i++) {
			Relation relation =  task.getPredecessors().get(i);
			BasicTask currentPredecessor = toBasicTask(relation.getTargetTask());
			if(currentPredecessor.isActive() && (currentPredecessor.getPercentage() == null || currentPredecessor.getPercentage().doubleValue() < 99)){
				predecessorIsDone = false;
			}
		}

		aTask.setCanStart(predecessorIsDone);
		aTask.setCritical(task.getCritical());

		aTask.setPercentage(task.getPercentageComplete());
		if(task.getDuration().getDuration()<1) {
			Duration duration = Duration.getInstance(task.getDuration().getDuration(), TimeUnit.HOURS);
			if (duration.getDuration() < 1){
				duration = Duration.getInstance(duration.getDuration(), TimeUnit.MINUTES);
			}

			aTask.setDuration(duration.toString());
		}else{
			aTask.setDuration(task.getDuration().toString());
		}
		//aTask.setFullText();
		// Load all fields in the class (private included)
		List<String> reversed = new ArrayList<String>();
		//Iterate on parents to load the parents lists
		net.sf.mpxj.Task taskIt = task.getParentTask();
		while(taskIt != null){
			reversed.add(taskIt.getName());
			taskIt = taskIt.getParentTask();
		}

		StringBuilder parentsLI;
		parentsLI = new StringBuilder();

		for (int i = 0, j = reversed.size() - 1; i < j; i++) {
			parentsLI = new StringBuilder("<UL><LI>" + reversed.get(i) + parentsLI + "</li></ul>");
		}
		aTask.setParentsLI(parentsLI.toString());

		List<String> parents = new ArrayList<String>();
		for (int i = 0, j = reversed.size() - 1; reversed.size()>0; i++) {
			parents.add(i, reversed.remove(j));
			j--;
		}
		aTask.setParent(parents);

		List<String> dependencies = new ArrayList<String>();
		for (int i = 0; task.getPredecessors() != null && i < task.getPredecessors().size(); i++) {
			Relation relation = task.getPredecessors().get(i);
			dependencies.add(relation.getTargetTask().getID().toString());
		}
		aTask.setDependencies(dependencies);

		//Define delay in case of a baseline End and an end date are both defined
		if(task.getBaselineFinish() != null){
			float diffMS=new Date().getTime()-task.getBaselineFinish().getTime();
			aTask.setDelayWithBL(diffMS/(3600000.0*24));
		}

		//Define delay in number of days
		if(task.getFinish() != null){
			long diffMS=new Date().getTime()-task.getFinish().getTime();
			aTask.setDelayComparedToFinishDate(diffMS/(3600000.0*24));
		}

		List<String> dependenciesActors = new ArrayList<String>();
		for (int i = 0; task.getSuccessors() != null && i < task.getSuccessors().size(); i++) {
			Relation relation = task.getSuccessors().get(i);
			dependencies.add(relation.getTargetTask().getID().toString());
			if(relation.getTargetTask().getResourceAssignments() != null && relation.getTargetTask().getResourceAssignments().size()>0 && relation.getTargetTask().getResourceAssignments().get(0) != null ) {
				List<ResourceAssignment> SuccessorResources = relation.getTargetTask().getResourceAssignments();
				for (ResourceAssignment resourceAssignment : SuccessorResources) {
					String resource = extractSubstring(resourceAssignment.toString(), " resource=", " start=");
					if (!dependenciesActors.contains(resource))
						dependenciesActors.add(resource);
				}
			}
		}

		aTask.setDependenciesActors(dependenciesActors);

		return aTask;
	}

	/**
	 * Methode used to identify if all predecessors was completed or not and to check successor actors to indicate in the reminder emails.
	 * @param task The {@link net.sf.mpxj.Task} extracted from the MPP
	 * @return a @{@link BasicTask}
	 */
	public BasicTask toBasicTask(net.sf.mpxj.Task task){
		BasicTask aTask = new BasicTask();
		aTask.setDuration(task.getDurationText());
		aTask.setID(task.getID().toString());
		aTask.setName(task.getName());
		if(task.getResourceAssignments() != null && task.getResourceAssignments().size()>0 && task.getResourceAssignments().get(0) != null ) {
			List<String> resources = new ArrayList<>();
			if(task.getResourceAssignments() != null && !task.getResourceAssignments().isEmpty()) {
				List<ResourceAssignment> SuccessorResources = task.getResourceAssignments();
				for (ResourceAssignment resourceAssignment : SuccessorResources) {
					String resource = extractSubstring(resourceAssignment.toString(), " resource=", " start=");
					if (!resources.contains(resource.replaceAll("\\[.*?\\]", "")))
						resources.add(resource.replaceAll("\\[.*?\\]", ""));
				}
			}
			aTask.setResource(resources);
		}

		aTask.setStartDate(task.getStart().toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime());
		aTask.setEndDate(task.getFinish().toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime());

		aTask.setPercentage(task.getPercentageComplete());
		aTask.setDuration(task.getDuration().toString());
		// Load all fields in the class (private included)
		List<String> reversed = new ArrayList<String>();
		//Iterate on parents to load the parents lists
		net.sf.mpxj.Task taskIt = task.getParentTask();
		while(taskIt != null){
			reversed.add(taskIt.getName());
			taskIt = taskIt.getParentTask();
		}

		StringBuilder parentsLI = new StringBuilder();

		for (int i = 0, j = reversed.size() - 1; i < j; i++) {
			parentsLI = new StringBuilder("<UL><LI>" + reversed.get(i) + parentsLI + "</li></ul>");
		}
		aTask.setParentsLI(parentsLI.toString());

		List<String> parents = new ArrayList<String>();
		for (int i = 0, j = reversed.size() - 1; reversed.size()>0; i++) {
			parents.add(i, reversed.remove(j));
			j--;
		}
		aTask.setParent(parents);

		List<String> dependencies = new ArrayList<String>();
		List<String> dependenciesActors = new ArrayList<String>();
		for (int i = 0; task.getSuccessors() != null && i < task.getSuccessors().size(); i++) {
			Relation relation = task.getSuccessors().get(i);
			dependencies.add(relation.getTargetTask().getID().toString());
			if(relation.getTargetTask().getResourceAssignments() != null && relation.getTargetTask().getResourceAssignments().size()>0 && relation.getTargetTask().getResourceAssignments().get(0) != null ) {
				String resources = extractSubstring(relation.getTargetTask().getResourceAssignments().get(0).toString(), " resource=", " start=");
				dependenciesActors.add(resources);
			}
		}

		aTask.setDependenciesActors(dependenciesActors);
		aTask.setDependencies(dependencies);

		aTask.setActive(task.getActive());

		return aTask;
	}

	/**
	 * Extract from the MPP data the resources assigned to the MPP task
	 * @param input Take the resource assignment string from the {@link net.sf.mpxj.Task}
	 * @param startMarker the string that allow us to identify the beginning of the string we have to extract
	 * @param endMarker the string that allow us to identify the end of the string we have to extract
	 * @return The extracted string
	 */
	private String extractSubstring(String input, String startMarker, String endMarker) {
		String result = null;
		int startIndex = input.indexOf(startMarker);
		// Start marker not found
		if (startIndex != -1) {
			int endIndex = input.indexOf(endMarker, startIndex + startMarker.length());// End marker not found
			if (endIndex != -1) {
				result = input.substring(startIndex + startMarker.length(), endIndex);
			}
		}

		return result;
	}

	/**
	 * This page allow to reload the MPP based on the values put in the index form.
	 * @param mppPath consider the MPP file fullpath
	 * @param outlineFilter the outline filter if needed
	 * @return to the index page
	 */
	@GetMapping("/updateMPP")
	private RedirectView updateMpp(@RequestParam("mppPath") Optional<String> mppPath, @RequestParam("outlineFilter") Optional<String> outlineFilter){
		mppPath.ifPresent(s -> PROJECT_FILE_PATH = s);
		outlineFilter.ifPresent(s -> OUTLINE_FILTER = s);
		logger.info("Mpp path setup :"+PROJECT_FILE_PATH);
		logger.info("Outline filter :"+OUTLINE_FILTER);
		return new RedirectView(WEB_CONTEXT+"/");
	}

	/**
	 * This page is called to build the reminder emails
	 * @param bLate Define if we should send or not the late tasks information
	 * @param bCurrent Define if we should send or not the current tasks information
	 * @param bNext Define if we should send or not the Next tasks information
	 * @param nbDays Define How many days we should consider to list the next tasks
	 * @param btest  Define if we should send emails to the Test recipient or to the real people involved.
	 * @param testDestEmail Define the test email address to use for test purpose
	 * @return redirect to the index page
	 */
	@GetMapping("/sendMail")
	private RedirectView sendAllReminder(@RequestParam("late") Optional<Boolean> bLate,@RequestParam("current") Optional<Boolean> bCurrent,@RequestParam("next") Optional<Boolean> bNext,@RequestParam("nbDays") Integer nbDays,@RequestParam("test") Optional<Boolean> btest,@RequestParam("testDestEmail") Optional<String> testDestEmail){

		testDestEmail.ifPresent(s -> notificationEMailTestDest = s);

		for (TasksReminder tasksReminder : taskList2) {
			if (
					(tasksReminder.getTasksCurrent() != null && tasksReminder.getTasksCurrent().size() > 0) ||
							(tasksReminder.getTasksNext() != null && tasksReminder.getTasksNext().size() > 0) ||
							(tasksReminder.getTasksLate() != null && tasksReminder.getTasksLate().size() > 0)
			) {
				//Filter next tasks per starting time filter
				for (int j = 0; j < tasksReminder.getTasksNext().size(); j++) {
					BasicTask basicTask = tasksReminder.getTasksNext().get(j);
					if (LocalDateTime.now().plusDays(nbDays).isBefore(basicTask.getStartDate())) {
						logger.info("Task " + tasksReminder.getTasksNext().get(j).getName() + " filtered on date start");
						tasksReminder.getTasksNext().remove(j);
						j--;
					}
				}

				if (bLate.isEmpty() || !bLate.get())
					tasksReminder.setTasksLate(new ArrayList<BasicTask>());
				if (bNext.isEmpty() || !bNext.get())
					tasksReminder.setTasksNext(new ArrayList<BasicTask>());
				if (bCurrent.isEmpty() || !bCurrent.get())
					tasksReminder.setTasksCurrent(new ArrayList<BasicTask>());

				tasksReminder.refreshAssignees();

				if ((tasksReminder.getTasksCurrent() != null && tasksReminder.getTasksCurrent().size() > 0) ||
								(tasksReminder.getTasksNext() != null && tasksReminder.getTasksNext().size() > 0) ||
								(tasksReminder.getTasksLate() != null && tasksReminder.getTasksLate().size() > 0)
				) {
					sendReminderEmail(tasksReminder, bLate.isPresent() && bLate.get(), bCurrent.isPresent() && bCurrent.get(), bNext.isPresent() && bNext.get(), nbDays, btest.isPresent() && btest.get());
				}
			}
		}

		return new RedirectView(WEB_CONTEXT+"/");
	}

	/**
	 * Send Email of the current {@link TasksReminder}
	 * @param tasksReminder the {@link TasksReminder} to send
	 * @param late define if we should or not send the late tasks
	 * @param current define if we should or not send the current tasks
	 * @param next define if we should or not send the next tasks
	 * @param nbDays define how many days we should consider to send for the next tasks
	 * @param test Define if we should send it for test purpose or not (content change maybe)
	 */
	private void sendReminderEmail(TasksReminder tasksReminder, Boolean late, Boolean current, Boolean next, Integer nbDays, Boolean test){
		// Sender's email ID needs to be mentioned
		String from = notificationEMailFROM;

		Properties props = new Properties();
		props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.host", MAIL_SMTP_HOST);
		props.put("mail.smtp.port", MAIL_SMTP_PORT);

		// Get the Session object.
		Session session = Session.getInstance(props);

		//create message using session
		MimeMessage message = new MimeMessage(session);

		MimeBodyPart part = new MimeBodyPart();
		try {
			//define the task to remind in the model
			taskToRemind=tasksReminder;

			String content = getReminderContent(late, current,next,nbDays, test);
			part.setContent(content, "text/html; charset=utf-8");
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(part);

			message.setContent(mp,"text/html; charset=utf-8");
			message.setFrom(new InternetAddress(from));
			if(tasksReminder.getAssignees() != null && tasksReminder.getAssignees().size()>0) {
				if (test) {
					logger.info("Mail sent in test mode to : " + notificationEMailTestDest);
					message.addRecipients(Message.RecipientType.TO, notificationEMailTestDest);
					message.addRecipients(Message.RecipientType.CC, notificationEMailTestDest);
					message.addRecipients(Message.RecipientType.BCC, notificationEMailTestDest);
				} else {
					logger.info("Mail sent in PRODUCTION mode to : " + notificationEMailTestDest);
					message.addRecipients(Message.RecipientType.TO, String.join(",", tasksReminder.getAssignees()));
					message.addRecipients(Message.RecipientType.CC, String.join(",", tasksReminder.getParentOwner()));
					message.addRecipients(Message.RecipientType.BCC, notificationEMailBCC);
				}
				String emailSubject = "ATLAS R2 - cut over tasks - " + tasksReminder.getParentFullPath();
				if (test)
					emailSubject = " TEST - " + emailSubject;

				message.setSubject(emailSubject);

				if (tasksReminder.getTasksLate() != null && tasksReminder.getTasksLate().size() > 0) {
					message.setHeader("X-Priority", "1");
					message.setHeader("x-msmail-priority", "high");
				}
				logger.info("Mail Sending for parent task:" + tasksReminder.getParentName() +"- To : "+tasksReminder.getAssignees());
				//sending message
				Transport.send(message);
				logger.info("Mail Sent for parent task:" + tasksReminder.getParentName());
			}else{
				logger.info("Mail not sent because of missing assignee : "+tasksReminder.getAssignees()+" for parent task:" + tasksReminder.getParentName());
			}
		} catch (MessagingException e) {
			System.err.println("An error happened by sending the Email:"+e.getMessage());
			e.printStackTrace();
		}
	}

}