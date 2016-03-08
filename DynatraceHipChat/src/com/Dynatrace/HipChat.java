
 /**
  * This template file was generated by dynaTrace client.
  * The dynaTrace community portal can be found here: http://community.compuwareapm.com/
  * For information how to publish a plugin please visit http://community.compuwareapm.com/plugins/contribute/
  **/ 

package com.Dynatrace;

import com.dynatrace.diagnostics.pdk.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;
import org.json.simple.JSONObject;


public class HipChat implements ActionV2 {

	private static final Logger log = Logger.getLogger(HipChat.class.getName());


	/**
	 * Initializes the Plugin. This method is called in the following cases:
	 * <ul>
	 * <li>before <tt>execute</tt> is called the first time for this
	 * scheduled Plugin</li>
	 * <li>before the next <tt>execute</tt> if <tt>teardown</tt> was called
	 * after the last execution</li>
	 * </ul>
	 * 
	 * <p>
	 * If the returned status is <tt>null</tt> or the status code is a
	 * non-success code then {@link #teardown(ActionEnvironment)} will be called
	 * next.
	 * 
	 * <p>
	 * Resources like sockets or files can be opened in this method.
	 * Resources like sockets or files can be opened in this method.
	 * @param env
	 *            the configured <tt>ActionEnvironment</tt> for this Plugin
	 * @see #teardown(ActionEnvironment)
	 * @return a <tt>Status</tt> object that describes the result of the
	 *         method call
	 */
	@Override
	public Status setup(ActionEnvironment env) throws Exception {
		// TODO
		return new Status(Status.StatusCode.Success);
	}

	/**
	 * Executes the Action Plugin to process incidents.
	 * 
	 * <p>
	 * This method may be called at the scheduled intervals, but only if incidents
	 * occurred in the meantime. If the Plugin execution takes longer than the
	 * schedule interval, subsequent calls to
	 * {@link #execute(ActionEnvironment)} will be skipped until this method
	 * returns. After the execution duration exceeds the schedule timeout,
	 * {@link ActionEnvironment#isStopped()} will return <tt>true</tt>. In this
	 * case execution should be stopped as soon as possible. If the Plugin
	 * ignores {@link ActionEnvironment#isStopped()} or fails to stop execution in
	 * a reasonable timeframe, the execution thread will be stopped ungracefully
	 * which might lead to resource leaks!
	 * 
	 * @param env
	 *            a <tt>ActionEnvironment</tt> object that contains the Plugin
	 *            configuration and incidents
	 * @return a <tt>Status</tt> object that describes the result of the
	 *         method call
	 */
	@Override
	public Status execute(ActionEnvironment env) throws Exception {
		/*
		// this sample shows how to receive and act on incidents
		Collection<Incident> incidents = env.getIncidents();
		for (Incident incident : incidents) {
			String message = incident.getMessage();
			log.info("Incident " + message + " triggered.");
			for (Violation violation : incident.getViolations()) {
				log.info("Measure " + violation.getViolatedMeasure().getName() + " violoated threshold.");
			}
		}
		*/
            
                //MAP ALL INCIDENTS A COLLECTION
		Collection<Incident> incidents = env.getIncidents();
		
		//FOR EACH INCIDENT
		for (Incident incident : incidents) {
			
			//LOG INCIDENT MESSAGE
			String message = incident.getMessage();
			log.fine("Incident " + message + " triggered.");
			
			//SET URL FROM USER INPUT FIELD
			URL url = env.getConfigUrl("url");
			
			//Allow for HipChat notifications
			boolean notify = env.getConfigBoolean("notify");
                        
                        //OPEN URL CONNECTION AND SET TIMEOUTS - USES CONNECTION METHOD 'POST'
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setConnectTimeout(5000);
			con.setReadTimeout(20000);
			
			//SET VARIABLES
			OutputStream out;
			InputStream in;
			int responseCode;
			String responseBody;
                        
                        //JSON CREATION
			JSONObject jsonObj = new JSONObject();
                        
                        // Compose string chat_message => This message will be sent to the HipChat channel
                        String chat_message = null;
                        chat_message = "<strong>Dynatrace incident triggered:</strong> " + incident.getIncidentRule().getName();
                        chat_message = chat_message + " <ul>";
                        chat_message = chat_message + "<li><strong>Incident UUID:</strong> " + incident.getKey().getUUID() + "</li>";
                        
                        chat_message = chat_message + "<li><strong>Incident start:</strong> " + incident.getStartTime() + " </li>";
                        chat_message = chat_message + "<li><strong>Incident end:</strong> " + incident.getEndTime() + " </li>";
                        
                        if (incident.isOpen()){
                            chat_message = chat_message + "<li><strong>Status:</strong> Open </li>";
			}
                        else if (incident.isClosed()){
                            chat_message = chat_message + "<li><strong>Status:</strong> Closed </li>";
                        }
                        else {
                            chat_message = chat_message + "<li><strong>Status:</strong> Unknown status </li>";
                        }
                        
//                        chat_message = chat_message + "<li><strong>Status state code:</strong> " + incident.getState() + "</li>";
                        
                        chat_message = chat_message + "<li><strong>Severity:</strong> " + incident.getSeverity().toString() + "</li>";
                        chat_message = chat_message + "<li><strong>Message: </strong>: " + message + "</li>";
                        
                        for (Violation violation : incident.getViolations()) {
                            chat_message = chat_message + "<li><strong>Violated Measure: </strong>: " + violation.getViolatedMeasure().getName() + " <strong> - Threshold: </strong>" + violation.getViolatedThreshold().getValue() + "</li>";
			}
                        
                        chat_message = chat_message + "</ul>";
			
                        /*
                         * Create JSON Object => Will be sent to HipChat via HTTP POST
                         */
			jsonObj.put("color", "green");
			jsonObj.put("message", chat_message);
			jsonObj.put("notify", notify);
			jsonObj.put("message_format", "html");
                        
                        
                        //JSON TO STRING
			String jsonString = jsonObj.toJSONString();
                        
			//LOG JSON STRING
			log.fine("JSON String is: " + jsonString);
			
			//JSON STRING TO BYTES
			byte[] payload = jsonString.getBytes();
			
			//SET CONNECTION OUTPUT
			con.setFixedLengthStreamingMode(payload.length);
                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setDoOutput(true);
			
			//LOG PROGRESS
			log.fine("Trying to get output stream...");
			
			//TRY TO GET OUTPUT STREAM
			try{
                            out = con.getOutputStream();
			}
			
			//CATCH EXCEPTION, LOG IT THEN SEND RESPONSE ERROR CODE
			catch (IOException e){
                            log.severe("Exception thrown whilst getting output stream...");
                            log.severe(e.toString());
                            con.disconnect();
                            return new Status (Status.StatusCode.ErrorInternalException);
			}
			
			//LOG PROGRESS
			log.fine("Trying to write to output stream");
			
			//TRY TO SEND PAYLOAD
			try{
                            out.write(payload);
                            out.close();
			}
			
			//CATCH EXCEPTION, LOG IT THEN SEND RESPONSE ERROR CODE
			catch (IOException e) {
                            log.severe("Exception thrown whilst writing to output stream...");
                            log.severe(e.toString());
                            con.disconnect();
                            return new Status (Status.StatusCode.ErrorInternalException);
			}
			
			//LOG PROGRESS
			log.fine("Trying to connect...");
                        
                        			
			//TRY TO GET RESPONSE CODE
			try{
                            responseCode = con.getResponseCode();
                            log.fine("Response Code : " + responseCode);
			}
			
			//CATCH EXCEPTION, LOG IT THEN SEND RESPONSE ERROR CODE
			catch (IOException e) {
                            log.severe("Exception thrown whilst writing to output stream...");
                            log.severe(e.toString());
                            con.disconnect();
                            return new Status (Status.StatusCode.ErrorInternalException);
			}

//			//TRY TO GET INPUT STREAM
//			try{
//                            if(responseCode == 200){
//                            	in = con.getInputStream();
//                            }
//                            else{
//				in = con.getErrorStream();
//                            }
//				
//                            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(in));
//			    responseBody = bufferReader.readLine();
//			    bufferReader.close();
//			    if(responseCode != 200){
//			    	log.warning("Response code was: " + responseCode);
//			    	log.warning("Error received from PagerDuty: " + responseBody);
//			    }
//			}
//			
//			//CATCH EXCEPTION, LOG IT THEN SEND RESPONSE ERROR CODE
//			catch (IOException e) {
//				log.severe("Exception thrown whilst reading from input stream...");
//				log.severe(e.toString());
//				return new Status (Status.StatusCode.ErrorInternalException);
//			}
			
			//DISCONNECT
			finally{
				con.disconnect();
			}
                }
                
		return new Status(Status.StatusCode.Success);
	}

	/**
	 * Shuts the Plugin down and frees resources. This method is called in the
	 * following cases:
	 * <ul>
	 * <li>the <tt>setup</tt> method failed</li>
	 * <li>the Plugin configuration has changed</li>
	 * <li>the execution duration of the Plugin exceeded the schedule timeout</li>
	 * <li>the schedule associated with this Plugin was removed</li>
	 * </ul>
	 * <p>
	 * The Plugin methods <tt>setup</tt>, <tt>execute</tt> and
	 * <tt>teardown</tt> are called on different threads, but they are called
	 * sequentially. This means that the execution of these methods does not
	 * overlap, they are executed one after the other.
	 * 
	 * <p>
	 * Examples:
	 * <ul>
	 * <li><tt>setup</tt> (failed) -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, configuration changes, <tt>execute</tt>
	 * ends -&gt; <tt>teardown</tt><br>
	 * on next schedule interval: <tt>setup</tt> -&gt; <tt>execute</tt> ...</li>
	 * <li><tt>execute</tt> starts, execution duration timeout,
	 * <tt>execute</tt> stops -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, <tt>execute</tt> ends, schedule is
	 * removed -&gt; <tt>teardown</tt></li>
	 * </ul>
	 * Failed means that either an unhandled exception is thrown or the status
	 * returned by the method contains a non-success code.
	 * 
	 * <p>
	 * All by the Plugin allocated resources should be freed in this method.
	 * Examples are opened sockets or files.
	 * 
	 * @see #setup(ActionEnvironment)
	 */
	@Override
	public void teardown(ActionEnvironment env) throws Exception {
		// TODO
	}
}
