package pt.airc.bpm.carmo.tiago.bonita;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTextField;
import javax.xml.bind.JAXBException;
import org.apache.activemq.artemis.utils.json.JSONArray;
import org.apache.activemq.artemis.utils.json.JSONException;
import org.apache.activemq.artemis.utils.json.JSONObject;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.airc.bpm.carmo.tiago.pojos.ActivityVariable;
import pt.airc.bpm.carmo.tiago.pojos.ActorMember;
import pt.airc.bpm.carmo.tiago.pojos.BusinessData;
import pt.airc.bpm.carmo.tiago.pojos.Case;
import pt.airc.bpm.carmo.tiago.pojos.CaseVariable;
import pt.airc.bpm.carmo.tiago.pojos.Contract;
import pt.airc.bpm.carmo.tiago.pojos.Design;
import pt.airc.bpm.carmo.tiago.pojos.Document;
import pt.airc.bpm.carmo.tiago.pojos.Input;
import pt.airc.bpm.carmo.tiago.pojos.Process;
import pt.airc.bpm.carmo.tiago.pojos.Task;
import pt.airc.bpm.carmo.tiago.pojos.TimerEvent;
import pt.airc.bpm.carmo.tiago.pojos.User;

/**
 * Connections class.
 * 
 * @author Tiago Carmo
 *
 */
public class Connections {
	private static final Logger LOGGER = LoggerFactory.getLogger(Connections.class);
	/**
	 * String to find milestones.
	 */
	private static final String MILESTONE = "Milestone";
	/**
	 * Users list.
	 */
	private List<String> users = new ArrayList<String>();
	/**
	 * http success code.
	 */
	private static final int SUCCESS_CODE = 200;
	/**
	 * Cookie string.
	 */
	private static final String COOKIE = "Cookie";
	/**
	 * The IP of the bonita server.
	 */
	private String ipServidor, userName;
	/**
	 * Request property for http requests.
	 */
	private static final String REQUEST_PROPERTY = "application/json";
	/**
	 * Method get to call rest api.
	 */
	private static final String GET_METHOD = "GET";
	/**
	 * Method post to call rest api.
	 */
	private static final String POST_METHOD = "POST";
	/**
	 * Method put to call rest api.
	 */
	private static final String PUT_METHOD = "PUT";
	/**
	 * Method delete to call rest api.
	 */
	private static final String DELETE_METHOD = "DELETE";
	/**
	 * cookie value (jsession).
	 */
	private String jsession;
	/**
	 * unmarshaller for the received json.
	 */
	private static final UnmarshallerC UNMARSH = new UnmarshallerC();
	/**
	 * array with simple variables.
	 */
	private List<String> noSubs;
	/**
	 * hashmap with complex variables.
	 */
	private Map<String, Input[]> subs;

	// ***************** (methods present in manual) ****************
	// *************************** MANUAL ***************************
	// ********************* (Primary methods) **********************

	/**
	 * Login function.
	 *
	 * @param username
	 *            of person logging in
	 * @param password
	 *            of person logging in
	 * @param ipServidor1
	 *            of bonita server
	 * @return string with error or success code
	 */
	public final String of_configuracao(final String username, final char[] password, final String ipServidor1) {
		if (username.equals("") || password.length == 0 || ipServidor1.equals("")) {
			return "-5";
		}
		setIpServidor("http://" + ipServidor1 + "/bonita");
		setUserName(Character.toUpperCase(username.charAt(0)) + username.substring(1, username.indexOf(".")));
		URL url = null;
		try {
			url = new URL("http://" + ipServidor1 + "/bonita/loginservice?username=" + username + "&password="
					+ String.valueOf(password) + "&redirect=false");
		} catch (MalformedURLException e) {
			return "-6";
		}
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(POST_METHOD);
		} catch (IOException e) {
			return "-101";
		}
		String cookieValue = null;
		try {
			if (connection.getResponseCode() != SUCCESS_CODE) {
				jsession = null;
				return "-2";
			} else {
				final List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
				for (String cookie : cookies) {
					if (cookie.contains("JSESSIONID")) {
						cookieValue = cookie;
					}
				}
				setJsession(cookieValue.substring(0, cookieValue.indexOf(";")));
				return "1";
			}
		} catch (IOException e) {
			return "-1";
		}
	}

	/**
	 * Create an instance of a process.
	 *
	 * @param processName
	 *            the name of the process from which to start an instance
	 * @param processID
	 *            the id of the process
	 * @param processVersion
	 *            the version of the process
	 * @return string with error or success code
	 */
	public final String of_invokeCreateWorkflow(final String processName, final String processID,
			final String processVersion) {
		if (processName.equals("") || !processID.matches(".*\\d+.*") || processID.equals("")
				|| processVersion.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/portal/resource/process/" + processName.replaceAll(" ", "%20") + "/"
					+ processVersion + "/content/?id=" + processID);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error starting case. Exception: " + e1);
			return "-6";
		}
		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
			desktop = Desktop.getDesktop();
		} else {
			desktop = null;
		}
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(url.toURI());
				LOGGER.info("Browser Opened in instantiation form");
				return "1";
			} catch (URISyntaxException | IOException e) {
				LOGGER.error("Unexpected error while executing method. Exception: " + e);
				return "-101";
			}
		} else {
			LOGGER.error("Unexpected error while executing method. No exception");
			return "-101";
		}
	}

	/**
	 * Run a given task from a given process instance.
	 *
	 * @param processName
	 *            name of process the task belongs to
	 * @param processVersion
	 *            version of process the task belongs to
	 * @param taskName
	 *            the name of the task to be run
	 * @param taskID
	 *            the id of the task to be run
	 * @return a string with error or success codes
	 */
	public final String of_invokeDispatchStep(final String processName, final String processVersion,
			final String taskName, final String taskID) {
		if (processName.equals("") || processVersion.equals("") || taskName.equals("") || !taskID.matches(".*\\d+.*")
				|| taskID.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/portal/resource/taskInstance/" + processName.replace(" ", "%20") + "/"
					+ processVersion + "/" + taskName.replace(" ", "%20") + "/content/?id=" + taskID);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error opening browser on task. Exception: " + e1);
			return "-6";
		}
		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
			desktop = Desktop.getDesktop();
		} else {
			desktop = null;
		}
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(url.toURI());
				LOGGER.info("Browser opened on task");
				return "1";
			} catch (URISyntaxException | IOException e) {
				LOGGER.error("Unexpected error while executing method. Exception: " + e);
				return "-101";
			}
		} else {
			LOGGER.error("Unexpected error while executing method");
			return "-101";
		}
	}

	/**
	 * Get the candidates for a given task.
	 *
	 * @param taskID
	 *            from which to know the candidate
	 * @return an array with the candidates name or an error code
	 */
	public final String[] of_invokeGetCandidates(final String taskID) {
		URL url = null;
		String error[] = new String[1];
		if (!taskID.matches(".*\\d+.*") || taskID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		try {
			url = new URL(getIpServidor() + "/API/bpm/activity/" + taskID);
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining candidate. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		Task task = null;
		try {
			task = UNMARSH.task(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.info("Error parsing JSON. Exception: " + e);
			return error;
		}
		String actorID = null;
		if (task.getAssigned_id().equals("")) {
			try {
				actorID = task.getActorId();
			} catch (Exception e) {
				error[0] = "-6";
				LOGGER.error("Error obtaining candidate. Exception: " + e);
				return error;
			}
		} else {
			error[0] = getActorName(task.getAssigned_id());
			return error;
		}
		LOGGER.info("Candidate obtained");
		StringBuilder actores = new StringBuilder();
		String[] actors = getActors(actorID);
		for(int i = 0; i<actors.length;i++){
			actores.append(actors[i] + "#");
		}
		String[] finalArray = new String[1];
		finalArray[0] = actores.toString().substring(0,actores.toString().length()-1);
		return finalArray;
	}

	/**
	 * Retrieve all the workflow steps up to the current point.
	 *
	 * @param caseID
	 *            from which to extract the steps
	 * @return an array with the steps or an error code
	 */
	public final String[] of_invokeGetFlowNodesExec(final String caseID) {
		String[] error = new String[1];
		if (!caseID.matches(".*\\d+.*") || caseID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/archivedFlowNode?p=0&f=caseId=" + caseID + "&o=archivedDate");
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtianing process flowNodes. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		List<Task> taskList = null;
		try {
			taskList = UNMARSH.taskList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return error;
		}
		String[] archivedTasksName = new String[taskList.size()];
		for (int i = 0; i < taskList.size(); i++) {
			archivedTasksName[i] = "Name: " + taskList.get(i).getName() + "\nState: " + taskList.get(i).getState()
					+ "\nReached Date: " + taskList.get(i).getLast_update_date() + "\nID: " + taskList.get(i).getId();
		}
		String[] currentTasksName = null;
		try {
			currentTasksName = of_invokeGetFlowNodesExecNow(caseID);
		} catch (IOException e) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return error;
		}
		String[] flowNodes = ArrayUtils.addAll(archivedTasksName, currentTasksName);
		if (flowNodes.length == 0) {
			error[0] = "-1";
			LOGGER.info("No tasks found");
			return error;
		} else {
			LOGGER.info("Archived tasks obtained");
			return flowNodes;
		}
	}

	/**
	 * Retrieve the current flowNodes.
	 *
	 * @param caseID
	 *            from which to extract the flowNodes
	 * @return an array with an error code or the flowNodes details
	 */
	public final String[] of_invokeGetStepActual(final String caseID) {
		String[] error = new String[1];
		if (!caseID.matches(".*\\d+.*") || caseID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/flowNode?p=0&c=50&f=caseId=" + caseID);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error obtaining current tasks. Exception: " + e1);
			error[0] = "-6";
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error while executing method. Exception: " + e1);
			return error;
		}
		List<Task> taskList = null;
		try {
			taskList = UNMARSH.taskList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return error;
		}
		String[] tasksName = new String[taskList.size()];
		for (int i = 0; i < taskList.size(); i++) {
			tasksName[i] = "Name: " + taskList.get(i).getName() + "\nID: " + taskList.get(i).getId()
					+ "\nAssigned User: " + taskList.get(i).getAssigned_id() + "\nProcess ID: "
					+ taskList.get(i).getProcessId() + "\nExpected End Date: " + taskList.get(i).getDueDate()
					+ "\nDescription: " + taskList.get(i).getDescription() + "\nState: " + taskList.get(i).getState();
		}
		if (tasksName.length == 0) {
			LOGGER.info("No tasks");
			error[0] = "-1";
			return error;
		} else {
			LOGGER.info("Tasks obtained");
			return tasksName;
		}
	}

	/**
	 * Retrieve the task variables.
	 *
	 * @param taskID
	 *            from which to extract the variables
	 * @return an array with error code or the variables details
	 */
	public final String[] of_invokeGetStepResponseItems(final String taskID) {
		String[] error = new String[1];
		if (!taskID.matches(".*\\d+.*") || taskID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/userTask/" + taskID + "/contract");
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining process step items. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		Contract contract = null;
		try {
			contract = UNMARSH.unContract(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.info("Error parsing JSON. Exception: " + e);
			return error;
		}
		final StringBuilder sb = new StringBuilder();
		final Input[] inputs = contract.getInputs();
		String[] responseItems = new String[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].getInputs().length != 0) {
				for (int j = 0; j < inputs[i].getInputs().length; j++) {
					responseItems[i] = sb.append("Name: " + inputs[i].getInputs()[j].getName() + "\nType: "
							+ inputs[i].getInputs()[j].getType() + "\nDescription: "
							+ inputs[i].getInputs()[j].getDescription()).toString();
				}
			} else {
				responseItems[i] = "Name: " + inputs[i].getName() + "\nType: " + inputs[i].getType() + "\nDescription: "
						+ inputs[i].getDescription();
			}
		}
		if (responseItems.length == 0) {
			LOGGER.info("No items found");
			error[0] = "-1";
			return error;
		} else {
			LOGGER.info("Step items retrieved");
			return responseItems;
		}
	}

	/**
	 * Retrieve the process inputs.
	 *
	 * @param processID
	 *            from which to retrieve the inputs
	 * @return the process inputs or an error code
	 */
	public final String[] of_invokeGetProcessDataFields(final String processID) {
		String[] error = new String[1];
		if (!processID.matches(".*\\d+.*") || processID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		Input[] inputs = null;
		inputs = of_invokeGetProcessContract(processID).getInputs();
		final StringBuilder sb = new StringBuilder();
		String[] inputDetails = new String[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].getInputs().length != 0) {
				for (int j = 0; j < inputs[i].getInputs().length; j++) {
					inputDetails[i] = sb.append("Name: " + inputs[i].getInputs()[j].getName() + "\nType: "
							+ inputs[i].getInputs()[j].getType() + "\nDescription: "
							+ inputs[i].getInputs()[j].getDescription() + "\n\n").toString().trim();
				}
			} else {
				inputDetails[i] = sb.append("Name: " + inputs[i].getName() + "\nType: " + inputs[i].getType()
						+ "\nDescription: " + inputs[i].getDescription() + "\n\n").toString().trim();
			}
		}
		if (inputDetails.length == 0) {
			LOGGER.info("No fields found");
			error[0] = "-1";
			return error;
		} else {
			LOGGER.info("Process data fields retrieved");
			return inputDetails;
		}
	}

	/**
	 * Retrieve the task description.
	 *
	 * @param taskID
	 *            from which to retrieve the description
	 * @return a String with the task description or an error code
	 */
	public final String of_invokeGetStepInstructions(final String taskID) {
		if (!taskID.matches(".*\\d+.*") || taskID.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/task/" + taskID);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error obtaining task instructions. Exception: " + e1);
			return "-6";
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			LOGGER.error("Unexpected error while executing method. Exception: " + e1);
			return "-101";
		}
		Task task = null;
		try {
			task = UNMARSH.task(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return "-8";
		}
		if (task.getDescription().equals("")) {
			LOGGER.info("Task has no description");
			return "-1";
		} else {
			LOGGER.info("Description obtained");
			return task.getDescription();
		}
	}

	/**
	 * Function to obtain the case variables.
	 *
	 * @param caseID
	 *            from which to obtain the variables
	 * @return an array with the variables info or an error code
	 */
	public final String[] of_invokeGetProcessVariables(final String caseID) {
		String[] error = new String[1];
		if (!caseID.matches(".*\\d+.*") || caseID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/caseVariable?p=0&c=100&f=case_id=" + caseID);
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining process variables. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error while executing the method. Exception: " + e1);
			return error;
		}
		List<CaseVariable> variableList = null;
		try {
			variableList = UNMARSH.variableList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return error;
		}
		String[] caseVariablesNames = new String[variableList.size()];
		for (int i = 0; i < variableList.size(); i++) {
			caseVariablesNames[i] = "Name: " + variableList.get(i).getName() + "\n" + "Type: "
					+ variableList.get(i).getType() + "\n" + "Value: " + variableList.get(i).getValue() + "\n";
		}
		String[] bdmVariables = null;
		try {
			bdmVariables = getProcessVariablesBDM(caseID);
		} catch (IOException e) {
			error[0] = "-6";
			LOGGER.error("Error obtaining process variables. Exception: " + e);
			return error;
		}
		String[] variableListFinal = ArrayUtils.addAll(caseVariablesNames, bdmVariables);
		if (variableListFinal.length == 0) {
			error[0] = "-1";
			LOGGER.info("No variables");
			return error;
		} else {
			LOGGER.info("Process variables retrieved");
			return ArrayUtils.addAll(caseVariablesNames, bdmVariables);
		}
	}

	/**
	 * Get the task variable details.
	 *
	 * @param taskId
	 *            from which to retrieve the variables
	 * @param variableName
	 *            from which to know the details
	 * @return a string with the variable details or an error code
	 */
	public final String of_invokeGetActivityVariable(final String taskId, final String variableName) {
		if (!taskId.matches(".*\\d+.*") || taskId.equals("") || variableName.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/activityVariable/" + taskId + "/" + variableName);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error obtaining variable details. Exception: " + e1);
			return "-6";
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return "-101";
		}
		ActivityVariable variable = null;
		try {
			variable = UNMARSH.activityVariable(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.info("Error parsing JSON. Exception: " + e);
			return "-8";
		}
		String var = "ID: " + variable.getId() + "\nName: " + variable.getName() + "\nDescription: "
				+ variable.getDescription() + "\nValue: " + variable.getValue();
		if (var.equals("")) {
			LOGGER.info("No such variable");
			return "-1";
		} else {
			LOGGER.info("Variable details retrieved");
			return var;
		}
	}

	/**
	 * Function to obtain exposed processes.
	 *
	 * @return array containing the process info
	 */
	public final String[] of_invokeGetWorkClasses() {
		String[] error = new String[1];
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/process?p=0&c=100");
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining exposed workflows. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		List<Process> processList = null;
		try {
			processList = UNMARSH.processList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Error parsing JSON while retrieving processes. Exception: " + e);
			return error;
		}
		String[] processArray = new String[processList.size()];
		for (int i = 0; i < processList.size(); i++) {
			processArray[i] = "Name: " + processList.get(i).getName() + "\n" + "ID: " + processList.get(i).getId()
					+ "\nVersion: " + processList.get(i).getVersion();
		}
		if (processArray.length == 0) {
			error[0] = "-1";
			LOGGER.info("No Processes");
			return error;
		} else {
			LOGGER.info("Processes retrieved");
			return processArray;
		}
	}

	/**
	 * First the task is unassigned (empty user). Then, another method is called
	 * to assign the task to the user.
	 *
	 * @param taskID
	 *            from which to change the actor
	 * @param userName
	 *            of the actor to reassign to the task
	 * @return a string with success or error code
	 */
	public final String of_invokeReassignTaskCandidate(final String taskID, final String userName) {
		if (!taskID.matches(".*\\d+.*") || taskID.equals("") || userName.equals("")) {
			LOGGER.info("Missing parameters");
			return "-5";
		}
		URL url = null;
		String payload = "{\"assigned_id\":\"" + "" + "\"}";
		try {
			url = new URL(getIpServidor() + "/API/bpm/userTask/" + taskID);
		} catch (MalformedURLException e) {
			LOGGER.error("Error defining new candidate. Exception: " + e);
			return "-6";
		}
		HttpURLConnection connection = null;
		try {
			connection = executePutRequest(url, payload);
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
		int code = 0;
		try {
			code = connection.getResponseCode();
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return "-101";
		}
		if (code != SUCCESS_CODE) {
			LOGGER.error("Error defining new candidate. Response code: " + code);
			return "-6";
		} else {
			try {
				if (assignTask(taskID, userName).equals("1")) {
					LOGGER.info("Task assigned to user " + userName);
					return "1";
				} else {
					LOGGER.error("Error defining new candidate. No exception");
					return "-6";
				}
			} catch (IOException e) {
				LOGGER.error("Unexpected error executing method. Exception: " + e);
				return "-101";
			}
		}
	}

	/**
	 * Get process bdm variables details.
	 *
	 * @param bdmType
	 *            type of bdm variable
	 * @param bdmID
	 *            id of bdm variable
	 * @return the bdm variable details
	 */
	public final String of_invokeGetProcessBDMVariable(final String bdmType, final String bdmID) {
		if (bdmType.equals("") || !bdmID.matches(".*\\d+.*") || bdmID.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bdm/businessData/" + bdmType + "/findByIds?ids=" + bdmID);
		} catch (MalformedURLException e) {
			LOGGER.error("Error obtiaing the variable details. Exception: " + e);
			return "-6";
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e) {
			LOGGER.error("Unexpected error while executing method. Exception: " + e);
			return "-101";
		}
		if (sbuilder.equals("")) {
			LOGGER.error("No such BDM variable");
			return "-1";
		} else {
			LOGGER.info("BDM Variable details obtained");
			return sbuilder.toString().replaceAll(",", "\n").replaceAll("\"", "").replace("[", "").replace("]", "")
					.replace("{", "").replace("}", "").replaceAll(":", ": ");
		}
	}

	/**
	 * List of exposed cases.
	 *
	 * @return an array with the case list or an error code
	 */
	public final String[] of_invokeGetProcessInstances() {
		URL url;
		String[] error = new String[1];
		try {
			url = new URL(getIpServidor() + "/API/bpm/case?p=0&c=50");
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining process instances. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error while executing method. Exception: " + e1);
			return error;
		}
		List<Case> caseList = null;
		try {
			caseList = UNMARSH.caseList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Erro parsing JSON. Exception: " + e);
			return error;
		}
		String[] casesID = new String[caseList.size()];
		for (int i = 0; i < caseList.size(); i++) {
			casesID[i] = "Process ID: " + caseList.get(i).getProcessDefinitionId() + "\nCase ID: "
					+ caseList.get(i).getId() + "\nState: " + caseList.get(i).getState() + "\nRoot CaseID: "
					+ caseList.get(i).getRootCaseId();
		}

		if (casesID.length == 0) {
			LOGGER.info("No Cases");
			error[0] = "-1";
			return error;
		} else {
			LOGGER.info("Cases retrieved");
			return casesID;
		}
	}

	/**
	 * Logout function.
	 *
	 * @return a string with a success code or error code
	 */
	public final String of_logout() {
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/logoutservice?redirect=false");
		} catch (MalformedURLException e) {
			LOGGER.error("Error logging out. Exception: " + e);
			return "-6";
		}
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(GET_METHOD);
			connection.connect();
		} catch (IOException e) {
			LOGGER.error("Unexpected error logging out. Exception " + e);
			return "-101";
		}
		LOGGER.info("Logout successfull. Exit app");
		return "1";
	}

	/**
	 * Set process variables values.
	 *
	 * @param caseID
	 *            form which to extract the variables
	 * @param variableName
	 *            from which to set the value
	 * @param variableType
	 *            from which to set the value
	 * @param value
	 *            to give to the variable
	 * @return the string containing the success or error code
	 */
	public final String of_invokeSetProcessVariables(final String caseID, final String variableName,
			final String variableType, final String value) {
		if (!caseID.matches(".*\\d+.*") || caseID.equals("") || variableName.equals("") || variableType.equals("")
				|| value.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		final String variableNameFinal = variableName.replace(" ", "%20");
		try {
			url = new URL(getIpServidor() + "/API/bpm/caseVariable/" + caseID + "/" + variableNameFinal);
		} catch (MalformedURLException e) {
			LOGGER.error("Error obtaining variable. Exception: " + e);
			return "-6";
		}
		final String payload = "{\"type\":\"" + variableType + "\",\"value\":\"" + value + "\"}";
		HttpURLConnection connection = null;
		try {
			connection = executePutRequest(url, payload);
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
		try {
			if (connection.getResponseCode() == SUCCESS_CODE) {
				LOGGER.info("Variable updated");
				return "1";
			} else {
				LOGGER.error("Unexpected error executing method. Response code: " + connection.getResponseCode());
				return "-101";
			}
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
	}

	/**
	 * Function to set given activity variables.
	 * 
	 * @param taskID
	 *            the task with the variable
	 * @param variableName
	 *            the name of the variable to be updated
	 * @param variableValue
	 *            the value to be set
	 * @return a String with success or error code
	 */
	public final String of_invokeSetActivityVariable(final String taskID, final String variableName,
			final String variableValue) {
		if (!taskID.matches(".*\\d+.*") || taskID.equals("") || variableName.equals("") || variableValue.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/activity/" + taskID);
		} catch (MalformedURLException e) {
			LOGGER.error("Error obtaining activity variable. Exception: " + e);
			return "-6";
		}
		final String payload = "{\"variables\":[{\"name\":\"" + variableName + "\",\"value\":\"" + variableValue
				+ "\"}]}";
		HttpURLConnection connection = null;
		try {
			connection = executePutRequest(url, payload);
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
		try {
			if (connection.getResponseCode() == SUCCESS_CODE) {
				LOGGER.info("Variable updated");
				return "1";
			} else {
				LOGGER.error("Unexpected error executing method. Response code: " + connection.getResponseCode());
				return "-101";
			}
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
	}

	/**
	 * Get the passed and not passed milestones in the case.
	 * 
	 * @param caseID
	 *            case from which to know the passed milestones
	 * @param processID
	 *            process from which to retrieve all the milestones
	 * @return a string with the passed and not passed milestones or an error
	 *         code
	 */
	public final String of_invokeGetMilestones(final String caseID, final String processID) {
		if (!caseID.matches(".*\\d+.*") || caseID.equals("") || !processID.matches(".*\\d+.*")
				|| processID.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		// todas as milestones passadas
		ArrayList<String> milestonesName;
		try {
			milestonesName = of_invokeMilestonesNames(caseID);
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		} catch (JAXBException e) {
			LOGGER.info("Error parsing JSON. Exception: " + e);
			return "-8";
		}
		// todas as milestones
		final ArrayList<String> everyMilestone = new ArrayList<String>();
		final String[] everyStep = of_invokeGetEveryStep(processID);
		if (everyStep[0].equals("-6")) {
			LOGGER.error("Error obtaining milestones");
			return "-6";
		} else if (everyStep[0].equals("-101")) {
			LOGGER.error("Unexpected error executing method");
			return "-101";
		} else if (everyStep[0].equals("-8")) {
			LOGGER.info("Error parsing JSON");
			return "-8";
		} else {
			int i = 0;
			for (final String task : everyStep) {
				if (task.contains(MILESTONE)) {
					everyMilestone.add(task);
					i++;
				}
			}
			if (i == 0) {
				LOGGER.info("Haven't reached any milestones or there are no milestones");
				return "-1";
			}
		}
		// milestones nao passadas
		for (int j = 0; j < milestonesName.size(); j++) {
			if (everyMilestone.contains(milestonesName.get(j))) {
				everyMilestone.remove(milestonesName.get(j));
			}
		}
		final Set<String> set = new HashSet<String>(milestonesName);
		final StringBuilder sb = new StringBuilder();
		sb.append("Milestones ultrapassadas: \n");
		for (final String s : set) {
			sb.append(s);
			sb.append("\n");
		}
		sb.append("\nMilestones não ultrapassadas: \n");
		for (final String s : everyMilestone) {
			sb.append(s);
			sb.append("\n");
		}
		LOGGER.info("Milestones retrieved");
		return sb.toString();
	}

	/**
	 * Get every step in workflow.
	 * 
	 * @param processID
	 *            from which to extract the steps
	 * @return an array with the steps or an error code
	 */
	public final String[] of_invokeGetEveryStep(final String processID) {
		String[] error = new String[1];
		if (!processID.matches(".*\\d+.*") || processID.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing parameters");
			return error;
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/process/" + processID + "/design");
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error retrieving steps. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.info("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		Design design = null;
		try {
			design = UNMARSH.design(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.info("Error parsing JSON. Exception: " + e);
			return error;
		}
		final int length = design.getFlowElementContainer().getActivities().length;
		final String[] activities = new String[length];
		for (int i = 0; i < length; i++) {
			activities[i] = design.getFlowElementContainer().getActivities()[i].getName();
		}
		LOGGER.info("Steps retrieved");
		return activities;
	}

	/**
	 * Add document to case.
	 * 
	 * @param caseId
	 *            to which to add document to
	 * @param fileUrl
	 *            location of file
	 * @param name
	 *            name to give to file
	 * @return string with document details or error code
	 */
	public final String of_invokeCreateDocument(final String caseId, String fileUrl, final String name) {
		String payload = null;
		if (fileUrl.contains("http://")) {
			payload = "{\"caseId\":\"" + caseId + "\",\"url\":\"" + fileUrl + "\",\"name\":\"" + name + "\"}";
		} else {
			fileUrl = fileUrl.replace("\\", "\\\\");
			payload = "{\"caseId\":\"" + caseId + "\",\"file\":\"" + fileUrl + "\",\"name\":\"" + name + "\"}";
		}
		if (!caseId.matches(".*\\d+.*") || caseId.equals("") || fileUrl.equals("") || name.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/caseDocument");
		} catch (MalformedURLException e1) {
			LOGGER.error("Error posting document. Exception: " + e1);
			return "-6";
		}
		final StringBuilder sbuilder = new StringBuilder();
		String line = null;
		try {
			HttpURLConnection connection = executePostRequest(url, payload);
			if (connection.getResponseCode() == 403) {
				LOGGER.error("Unauthorized access to file");
				return "-1";
			}
			final BufferedReader breader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while ((line = breader.readLine()) != null) {
				sbuilder.append(line + '\n');
			}
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return "-101";
		}
		Document document = null;
		try {
			document = UNMARSH.document(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return "-8";
		}
		LOGGER.info("Document created");
		return "Name: " + document.getName() + "\nAuthor: " + getActorName(document.getSubmittedBy())
				+ "\nCreation Date: " + document.getCreationDate() + "\nID: " + document.getId() + "\nURL: "
				+ document.getUrl() + "\n";
	}

	/**
	 * Open the process overview page.
	 * 
	 * @param processName
	 *            the name of the parent process
	 * @param caseID
	 *            from which to open the overview
	 * @param processVersion
	 *            the version of the parent process
	 * @return success or error code
	 */
	public final String of_invokeGetOverview(final String processName, final String caseID,
			final String processVersion) {
		if (processName.equals("") || !caseID.matches(".*\\d+.*") || caseID.equals("") || processVersion.equals("")
				|| !processVersion.matches(".*\\d+.*")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/portal/resource/processInstance/" + processName.replaceAll(" ", "%20")
					+ "/" + processVersion + "/content/?id=" + caseID);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error opening overview page. Exception: " + e1);
			return "-6";
		}
		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
			desktop = Desktop.getDesktop();
		} else {
			desktop = null;
		}
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(url.toURI());
				LOGGER.info("Browser opened in overview page");
				return "1";
			} catch (URISyntaxException | IOException e) {
				LOGGER.error("Unexpected error while executing method. Exception: " + e);
				return "-101";
			}
		} else {
			LOGGER.error("Unexpected error while executing method. No exception");
			return "-101";
		}
	}

	/**
	 * Retrieve timer in the process instance.
	 * 
	 * @param caseId
	 *            from which to retrieve the timers
	 * @return the timer list or an error code
	 */
	public String[] of_invokeTimerEvents(String caseId) {
		String[] error = new String[1];
		if (!caseId.matches(".*\\d+.*") || caseId.equals("")) {
			error[0] = "-5";
			LOGGER.error("Missing paramters");
			return error;
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/timerEventTrigger?p=0&c=100&caseId=" + caseId);
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			LOGGER.error("Error obtaining Timer events. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return error;
		}
		List<TimerEvent> timerEvent = null;
		try {
			timerEvent = UNMARSH.timerEvent(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return error;
		}
		String[] timers = new String[timerEvent.size()];
		for (int i = 0; i < timerEvent.size(); i++) {
			String data = timerEvent.get(i).getExecutionDate();
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			long milliSeconds = Long.parseLong(data);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(milliSeconds);
			String dataFinal = formatter.format(calendar.getTime());
			timers[i] = "Name: " + timerEvent.get(i).getEventInstanceName() + "\nId: " + timerEvent.get(i).getId()
					+ "\nExecutionDate: " + dataFinal;
		}
		if (timers.length == 0) {
			LOGGER.info("No Timers found");
			error[0] = "-1";
			return error;
		} else {
			LOGGER.info("Timer events obtained");
			return timers;
		}
	}

	/**
	 * Cancel (delete) a process instance.
	 * 
	 * @param caseId
	 *            the id of the case to be deleted
	 * @return a success or error code
	 */
	public String of_invokeCancelInstance(String caseId) {
		if (!caseId.matches(".*\\d+.*") || caseId.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/bpm/case/" + caseId);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error deleting process instance. Exception: " + e1);
			return "-6";
		}
		String line;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			final StringBuilder sbuilder = new StringBuilder();
			connection.setRequestMethod(DELETE_METHOD);
			connection.setRequestProperty(COOKIE, getJsession());
			connection.connect();
			final BufferedReader breader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while ((line = breader.readLine()) != null) {
				sbuilder.append(line + '\n');
			}
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return "-101";
		}
		LOGGER.info("Case deleted");
		return "1";
	}

	// ************************* TODO *******************************
	// ******************** (not in manual) *************************
	// ************************ OTHERS ******************************
	// ****************** (Auxiliary methods) ***********************

	/**
	 * Method used for every get request.
	 * 
	 * @param url
	 *            to connect
	 * @param connection
	 *            to open
	 * @return the response
	 * @throws IOException
	 *             in case something goes wrong
	 */
	private StringBuilder executeGetRequest(URL url) throws IOException {
		String line;
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		final StringBuilder sbuilder = new StringBuilder();
		connection.setRequestMethod(GET_METHOD);
		connection.setRequestProperty(COOKIE, getJsession());
		connection.connect();
		final BufferedReader breader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while ((line = breader.readLine()) != null) {
			sbuilder.append(line + '\n');
		}
		return sbuilder;
	}

	/**
	 * Method used for every put request.
	 * 
	 * @param url
	 *            to connect
	 * @param connection
	 *            to open
	 * @param payload
	 *            to send
	 * @return the connection
	 * @throws IOException
	 *             in case something goes wrong
	 */
	private HttpURLConnection executePutRequest(URL url, String payload) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(PUT_METHOD);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", REQUEST_PROPERTY);
		connection.setRequestProperty("Accept", REQUEST_PROPERTY);
		connection.setRequestProperty(COOKIE, getJsession());
		final OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
		osw.write(String.format(payload));
		osw.flush();
		osw.close();
		return connection;
	}

	/**
	 * Method used for every post request.
	 * 
	 * @param url
	 *            to connect
	 * @param payload
	 *            to send
	 * @return the connection
	 * @throws IOException
	 *             in case something goes wrong
	 */
	private HttpURLConnection executePostRequest(final URL url, final String payload) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(POST_METHOD);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", REQUEST_PROPERTY);
		connection.setRequestProperty("Accept", REQUEST_PROPERTY);
		connection.setRequestProperty(COOKIE, getJsession());
		final OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
		if (!payload.equals(null)) {
			osw.write(payload);
			osw.flush();
			osw.close();
		}
		return connection;
	}

	/**
	 * Get the process BDM variables.
	 *
	 * @param caseID
	 *            from which to extract the variables
	 * @return an array with the variables or null
	 * @throws IOException
	 *             in case something goes wrong
	 */
	private String[] getProcessVariablesBDM(final String caseID) throws IOException {
		URL url;
		url = new URL(getIpServidor() + "/API/bdm/businessDataReference" + "?f=caseId=" + caseID + "&p=0&c=10");
		StringBuilder sbuilder = new StringBuilder();
		sbuilder = executeGetRequest(url);
		List<BusinessData> businessDataList = null;
		try {
			businessDataList = UNMARSH.businessDataList(sbuilder.toString());
		} catch (JAXBException e) {
		}
		if (businessDataList.size() != 0) {
			String[] businessDataName = new String[businessDataList.size()];
			for (int i = 0; i < businessDataList.size(); i++) {
				businessDataName[i] = "ID: " + businessDataList.get(i).getStorageId() + "\nType: "
						+ businessDataList.get(i).getType() + "\n" + "Info: BDM variable\n";
			}
			return businessDataName;
		} else {
			return null;
		}
	}

	/**
	 * Retrieve the current flowNodes.
	 *
	 * @param caseID
	 *            to search the current task
	 * @return the current task or step
	 * @throws IOException
	 *             in case something goes wrong
	 */
	private String[] of_invokeGetFlowNodesExecNow(final String caseID) throws IOException {
		final URL url = new URL(getIpServidor() + "/API/bpm/flowNode?p=0&f=caseId=" + caseID + "&o=lastUpdateDate");
		StringBuilder sbuilder = new StringBuilder();
		sbuilder = executeGetRequest(url);
		List<Task> taskList = null;
		try {
			taskList = UNMARSH.taskList(sbuilder.toString());
		} catch (JAXBException e) {
		}
		String[] tasks = new String[taskList.size()];
		for (int i = 0; i < taskList.size(); i++) {
			tasks[i] = "Name: " + taskList.get(i).getName() + "\nState: " + taskList.get(i).getState()
					+ "\nReached Date: " + taskList.get(i).getLast_update_date() + "\nID: " + taskList.get(i).getId()
					+ "\n";
		}
		return tasks;
	}

	/**
	 * Reassign a user to a task (second function).
	 *
	 * @param taskID
	 *            id of task to be assigned
	 * @param userID
	 *            id of user to be assigned to the task
	 * @throws IOException
	 *             in case there is a problem assigning the task
	 * @return a string with a success or error code
	 */
	private String assignTask(final String taskID, final String userName) throws IOException {
		String userID;
		try {
			userID = getUserID(userName);
		} catch (JAXBException e) {
			return "-5";
		}
		String payload = "{\"assigned_id\":\"" + userID + "\"}";
		final URL url = new URL(getIpServidor() + "/API/bpm/userTask/" + taskID);
		HttpURLConnection connection = executePutRequest(url, payload);
		final int code = connection.getResponseCode();
		if (code == SUCCESS_CODE) {
			return "1";
		} else {
			return "-6";
		}
	}

	/**
	 * Retrieve the ID of a certain user.
	 * 
	 * @param userName
	 *            from which to retrieve the ID
	 * @return the user ID
	 * @throws IOException
	 *             in case something goes wrong
	 * @throws JAXBException
	 *             in case the unmarshalling finds an error
	 */
	private String getUserID(final String userName) throws IOException, JAXBException {
		URL url = null;
		url = new URL(getIpServidor() + "/API/identity/user?f=userName=" + userName);
		StringBuilder sbuilder = new StringBuilder();
		sbuilder = executeGetRequest(url);
		List<User> userList = null;
		userList = UNMARSH.userList(sbuilder.toString());
		String userID = null;
		for (final User user : userList) {
			if (user.getUserName().equals(userName)) {
				userID = user.getId();
			}
		}
		return userID;
	}

	/**
	 * Execute a task (wrong way).
	 *
	 * @param taskID
	 *            to execute
	 * @param approved
	 *            whether it will be approved or not
	 * @return a string with error or success code
	 */
	public final String of_invokeDispatchStepP(final String taskID, final String approved) {
		final String payload = "{\"isApproved\":\"" + approved + "\"}";
		URL url = null;
		if (!taskID.matches(".*\\d+.*") || taskID.equals("")) {
			LOGGER.error("Missing parameters");
			return "-5";
		}
		try {
			url = new URL(getIpServidor() + "/API/bpm/userTask/" + taskID + "/execution");
		} catch (MalformedURLException e) {
			LOGGER.error("Error executing task. Exception: " + e);
			return "-6";
		}
		HttpURLConnection connection = null;
		try {
			connection = executePostRequest(url, payload);
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
		try {
			if (connection.getResponseCode() == 204) {
				LOGGER.info("Task executed");
				return "1";
			} else {
				LOGGER.error("Unexpected error executing method. Response code: " + connection.getResponseCode());
				return "-101";
			}
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
	}

	/**
	 * Retrieve the actor name (auxiliary method).
	 *
	 * @param actorID
	 *            the id from which to find the name
	 * @return the actor name
	 */
	@SuppressWarnings("unchecked")
	private String[] getActors(final String actorID) {
		URL url = null;
		String error[] = new String[1];
		if (!actorID.matches(".*\\d+.*") || actorID.equals("")) {
			error[0] = "-5";
			return error;
		}
		try {
			url = new URL(getIpServidor() + "/API/bpm/actorMember?p=0&c=150&f=actor_id=" + actorID);
		} catch (MalformedURLException e1) {
			error[0] = "-6";
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error[0] = "-101";
			return error;
		}
		List<ActorMember> actorList = null;
		try {
			actorList = UNMARSH.actorList(sbuilder.toString());
		} catch (JAXBException e) {
			error[0] = "-8";
			return error;
		}
		String[] actorNames = new String[actorList.size()];
		String[] stockArr = null;
		for (int i = 0; i < actorList.size(); i++) {
			final ActorMember actor = actorList.get(i);
			if (!actor.getUser_id().equals("-1") && actor.getGroup_id().equals("-1")) {
				actorNames[i] = getActorName(actor.getUser_id());
			} else if (actor.getUser_id().equals("-1") && !actor.getGroup_id().equals("-1")) {
				users = ListUtils.union(users, getActorsInGroup(actor.getGroup_id()));
			} else if (!actor.getUser_id().equals("-1") && !actor.getGroup_id().equals("-1")) {
				actorNames[i] = getActorName(actor.getUser_id());
				users = ListUtils.union(users, getActorsInGroup(actor.getGroup_id()));
			}
		}
		for (int i = 0; i < actorNames.length; i++) {
			if (actorNames[i] != null) {
				users.add(actorNames[i]);
			}
		}
		final Set<String> hs = new HashSet<>();
		hs.addAll(users);
		users.clear();
		users.addAll(hs);
		stockArr = new String[users.size()];
		stockArr = users.toArray(stockArr);
		users.clear();
		return stockArr;
	}

	/**
	 * Get the process contract (auxiliary method).
	 *
	 * @param processID
	 *            name of process from which to extract the contract
	 * @return the contract
	 * @throws IOException
	 *             in case something goes wrong
	 */
	public final Contract of_invokeGetProcessContract(final String processID) {
		URL url = null;
		if (!processID.matches(".*\\d+.*") || processID.equals("")) {
			LOGGER.error("Missing/Wrong parameters");
			return null;
		}
		try {
			url = new URL(getIpServidor() + "/API/bpm/process/" + processID + "/contract");
		} catch (MalformedURLException e1) {
			LOGGER.error("Error retrieving process contract. Exception: " + e1);
			return null;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return null;
		}
		Contract contract = null;
		try {
			contract = UNMARSH.unContract(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.error("Error parsing JSON. Exception:" + e);
			return null;
		}
		final Map<String, Input[]> inputList = new HashMap<String, Input[]>();
		final Input[] inputs = contract.getInputs();
		for (final Input input : inputs) {
			if (input.getInputs().length == 0) {
				inputList.put(input.getName(), null);
			} else {
				inputList.put(input.getName(), input.getInputs());
			}
		}
		setNoSubs(new ArrayList<String>());
		setSubs(new HashMap<String, Input[]>());
		for (final Map.Entry<String, Input[]> entry : inputList.entrySet()) {
			if (entry.getValue() == null) {
				getNoSubs().add(entry.getKey());
			} else {
				for (int i = 0; i < entry.getValue().length; i++) {
					getSubs().put(entry.getKey(), entry.getValue());
				}
			}
		}
		LOGGER.info("Contract retrieved");
		return contract;
	}

	/**
	 * Function to obtain the ids of the actors in a group.
	 * 
	 * @param group_id
	 *            the id of the group from which to know the actors
	 * @return an arraylist with all the actors
	 */
	private ArrayList<String> getActorsInGroup(String group_id) {
		URL url = null;
		final ArrayList<String> error = new ArrayList<String>();
		if (!group_id.matches(".*\\d+.*") || group_id.equals("")) {
			error.add("-5");
			LOGGER.error("Missing parameters");
			return error;
		}
		try {
			url = new URL(getIpServidor() + "/API/identity/user?p=0&c=150&f=group_id=" + group_id);
		} catch (MalformedURLException e1) {
			error.add("-6");
			LOGGER.error("Error obtaining actors id. Exception: " + e1);
			return error;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			error.add("-101");
			LOGGER.error("Unexpected error exedcuting method. Exception: " + e1);
			return error;
		}
		List<User> userList = null;
		try {
			userList = UNMARSH.userList(sbuilder.toString());
		} catch (JAXBException e) {
			error.add("-8");
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return error;
		}
		final ArrayList<String> userIDs = new ArrayList<String>();
		for (int i = 0; i < userList.size(); i++) {
			userIDs.add(userList.get(i).getUserName());
		}
		LOGGER.info("User ids obtained");
		return userIDs;
	}

	/**
	 * start a process (other way..wrong).
	 *
	 * @param inputs
	 *            map with all the inputs required in contract
	 * @param processID
	 *            id of process from which to start the case
	 * @return the json response
	 */
	public final String startProcessHere(final Map<Integer, JTextField> inputs, final String processID) {
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/process/" + processID + "/instantiation");
		} catch (MalformedURLException e) {
			LOGGER.error("Error starting process. Exception: " + e);
			return "-6";
		}
		HttpURLConnection connection = null;
		OutputStreamWriter osw = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(POST_METHOD);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", REQUEST_PROPERTY);
			connection.setRequestProperty("Accept", REQUEST_PROPERTY);
			connection.setRequestProperty(COOKIE, getJsession());
			osw = new OutputStreamWriter(connection.getOutputStream());
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing metrhod. Exception: " + e);
			return "-101";
		}
		final JSONObject json = new JSONObject();
		final JSONObject jsonFinal = new JSONObject();
		JSONArray jsonArray = null;
		for (final Map.Entry<Integer, JTextField> entry : inputs.entrySet()) {
			for (final Map.Entry<String, Input[]> entry2 : getSubs().entrySet()) {
				for (int i = 0; i < entry2.getValue().length; i++) {
					if (entry.getValue().getName().equals(entry2.getValue()[i].getName())) {
						jsonArray = new JSONArray();
						try {
							jsonArray.put(json.append(entry.getValue().getName(), entry.getValue().getText()));
						} catch (JSONException e) {
							LOGGER.error("Error parsing JSON. Exception: " + e);
							return "-8";
						}
					}
					try {
						jsonFinal.put(entry2.getKey(), jsonArray);
					} catch (JSONException e) {
						LOGGER.error("Error parsing JSON. Exception: " + e);
						return "-8";
					}
				}
			}
			for (int j = 0; j < getNoSubs().size(); j++) {
				if (entry.getValue().getName().equals(getNoSubs().get(j))) {
					try {
						jsonFinal.append(entry.getValue().getName(), entry.getValue().getText());
					} catch (JSONException e) {
						LOGGER.error("Error parsing JSON. Exception: " + e);
						return "-8";
					}
				}
			}
		}
		try {
			osw.write(jsonFinal.toString().replace("[", "").replace("]", ""));
			osw.flush();
			osw.close();
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing metrhod. Exception: " + e);
			return "-101";
		}
		int code;
		try {
			code = connection.getResponseCode();
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing method. Exception: " + e);
			return "-101";
		}
		if (code != SUCCESS_CODE) {
			LOGGER.error("Unexpected error executing method. Response code: " + code);
			return "-101";
		}
		InputStream stream = null;
		try {
			stream = connection.getInputStream();
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing metrhod. Exception: " + e);
			return "-101";
		}
		final BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String response = "";
		String line;
		try {
			while ((line = br.readLine()) != null) {
				response += line;
			}
		} catch (IOException e) {
			LOGGER.error("Unexpected error executing metrhod. Exception: " + e);
			return "-101";
		}
		LOGGER.info("Case started");
		return response.replace("{", "").replace("}", "").replace("\"", "").replace(":", ": ");
	}

	/**
	 * Function to obtain the actors name.
	 * 
	 * @param assigned_id
	 *            from which to obtain the name
	 * @return the name of the actor or an error code
	 */
	private String getActorName(final String assigned_id) {
		URL url;
		try {
			url = new URL(getIpServidor() + "/API/identity/user/" + assigned_id);
		} catch (MalformedURLException e) {
			LOGGER.error("Error obtaining actor name. Exception: " + e);
			return "-6";
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e) {
			LOGGER.error("Unexpected error running method. Exception: " + e);
			return "-101";
		}
		User user = null;
		try {
			user = UNMARSH.user(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.error("Error parsing JSON. Exception: " + e);
			return "-8";
		}
		LOGGER.info("Actor name obtained");
		return user.getUserName();
	}

	/**
	 * Function to obtain the passed milestones name.
	 * 
	 * @param caseID
	 *            from which to extract the milestones
	 * @return an arraylist with the milestones
	 * @throws IOException
	 *             in case something goes wrong
	 * @throws JAXBException
	 *             in case there is a problem with JSON
	 */
	private ArrayList<String> of_invokeMilestonesNames(final String caseID) throws IOException, JAXBException {
		URL url = null;
		url = new URL(getIpServidor() + "/API/bpm/archivedFlowNode?p=0&f=caseId=" + caseID + "&o=archivedDate");
		StringBuilder sbuilder = new StringBuilder();
		sbuilder = executeGetRequest(url);
		List<Task> taskList = null;
		taskList = UNMARSH.taskList(sbuilder.toString());
		final ArrayList<String> milestones = new ArrayList<String>();
		for (final Task task : taskList) {
			if (task.getName().contains(MILESTONE)) {
				milestones.add(task.getName());
				milestones.add("true");
				milestones.add(task.getReached_state_date());
				milestones.add("3");
			}
		}
		LOGGER.info("Milestones details obtained");
		return milestones;
	}
	
	public String[] createWorkFlowDafuq(){
		
		return null;
	}

	/**
	 *
	 * @return the arrayList with variables with no subvariables
	 */
	public final List<String> getNoSubs() {
		return noSubs;
	}

	/**
	 *
	 * @param noSubs1
	 *            the arraylist to be set
	 */
	public final void setNoSubs(final List<String> noSubs1) {
		this.noSubs = noSubs1;
	}

	/**
	 *
	 * @return the hashmap with complex variables
	 */
	public final Map<String, Input[]> getSubs() {
		return subs;
	}

	/**
	 *
	 * @param subs1
	 *            the hashmap to be set
	 */
	public final void setSubs(final Map<String, Input[]> subs1) {
		this.subs = subs1;
	}

	/**
	 *
	 * @return the jsession cookie
	 */
	public final String getJsession() {
		return jsession;
	}

	/**
	 *
	 * @param jsession1
	 *            to be set
	 */
	public final void setJsession(final String jsession1) {
		this.jsession = jsession1;
	}

	/**
	 *
	 * @return the server ip address
	 */
	public final String getIpServidor() {
		return ipServidor;
	}

	/**
	 *
	 * @param ipServidor1
	 *            to be set
	 */
	public final void setIpServidor(final String ipServidor1) {
		this.ipServidor = ipServidor1;
	}

	/**
	 *
	 * @return the users
	 */
	public List<String> getUsers() {
		return users;
	}

	/**
	 *
	 * @param users
	 *            to be set
	 */
	public void setUsers(final List<String> users) {
		this.users = users;
	}

	/**
	 *
	 * @return the username
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 *
	 * @param userName1
	 *            to be set
	 */
	public void setUserName(String userName1) {
		this.userName = userName1;
	}
	
	// ***********************testes************************ //

	public final String createWorkflow(final String processName) {
		URL url = null;
		try {
			url = new URL(getIpServidor() + "/API/bpm/process?s=" + processName);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error retrieving process contract. Exception: " + e1);
			return null;
		}
		StringBuilder sbuilder = new StringBuilder();
		try {
			sbuilder = executeGetRequest(url);
		} catch (IOException e1) {
			LOGGER.error("Unexpected error executing method. Exception: " + e1);
			return null;
		}
		Process process = null;
		try {
			process = UNMARSH.process(sbuilder.toString());
		} catch (JAXBException e) {
			LOGGER.error("Error parsing JSON. Exception:" + e);
			return null;
		}
		
		String processID = process.getId();
		URL url2;
		try {
			url2 = new URL(getIpServidor() + "/API/bpm/case");
		} catch (MalformedURLException e1) {
			LOGGER.error("Error retrieving process contract. Exception: " + e1);
			return null;
		}
		String line = null;
		try {
			HttpURLConnection con = executePostRequest(url2, "");
			if(con.getResponseCode() == 200){
				final BufferedReader breader = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while ((line = breader.readLine()) != null) {
					sbuilder.append(line + '\n');
				}
			};
		} catch (IOException e) {
			LOGGER.error("Error: " + e);
			return null;
		}
		return null;
	}
	
}
