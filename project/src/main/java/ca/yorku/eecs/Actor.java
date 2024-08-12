package ca.yorku.eecs;


import static org.neo4j.driver.v1.Values.parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream; //output stream
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Actor implements HttpHandler {

	public Actor() {

	}

	@Override
	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("PUT")) {
				addActor(r);
			} else if (r.getRequestMethod().equals("GET")) {
				System.out.println("get actor");
				getActor(r);

			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addActor(HttpExchange r) throws IOException, JSONException {

		String body = DBConnect.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String name;
		String actorId;

		if (deserialized.has("name")) {
			name = deserialized.getString("name");
		} else {
			name = "";
		}
		if (deserialized.has("actorId")) {
			actorId = deserialized.getString("actorId");
		} else {
			actorId = "";
		}

		System.out.println("Running addActor: name: " + name + " actorId: " + actorId);

		if (!hasActorInDB(actorId)) {
			try (Session session = DBConnect.driver.session()) {

				session.run("CREATE (a:Actor {name:$name, actorId:$actorId});",
						parameters("name", name, "actorId", actorId));
				statusCode = 200;
			}

			catch (Exception e) {
				System.err.println("Caught Exception: " + e.getMessage());
				statusCode = 500;
			}
		} else {
			statusCode = 400;
		}

		r.sendResponseHeaders(statusCode, -1);
	}

	public void getActor(HttpExchange r) throws IOException, JSONException {
		
		Map<String, String> params = queryToMap(r.getRequestURI().getRawQuery()); 

		//String body = DBConnect.convert(r.getRequestBody());
		//JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String response = null;
		String actorId = params.get("actorId");

		/*if (deserialized.has("actorId")) {
			actorId = deserialized.getString("actorId");
		} else {
			actorId = "";
		}*/

		System.out.println("Running getActor: actorId: " + actorId);
		String sendToWrite = "Running getActor: actorId: " + actorId;
		fileWrite(sendToWrite);

		JSONObject obj = new JSONObject();

		try (Session session = DBConnect.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				String query = "MATCH (a:Actor {actorId:'" + actorId
						+ "'}) WITH a,[(a)-[:ACTED_IN]->(b:Movie) | b.movieId] AS movieIDs "
						+ "SET a.resume = movieIDs "
						+ "RETURN a.actorId as actorId, a.name as name, a.resume as movies";

				System.out.println(query);
				
				fileWrite(query);

				StatementResult results = tx.run(query);

				if (results.hasNext()) {
					Record record = results.next();
					statusCode = 200;

					obj.put("actorId", record.get("actorId").asString());
					obj.put("name", record.get("name").asString());
					obj.put("movies", record.get("movies").asList());
					
					fileWrite(record.get("actorId").asString());
					fileWrite(record.get("name").asString());
					fileWrite(record.get("movies").asList().toString());

				} else {
					statusCode = 404;
					obj.put("error", "NOT FOUND");
				}

			} catch (Exception e) {
				System.err.println("Caught Exception: " + e.getMessage());
				response = e.getMessage();
				statusCode = 500;
				session.close();
			}
		} catch (Exception e) {
			System.err.println("Caught Exception: " + e.getMessage());
			response = e.getMessage();
			statusCode = 500;
		}

		System.out.println(obj);

		String responseString = obj.toString();
		r.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		r.sendResponseHeaders(statusCode, responseString.getBytes(StandardCharsets.UTF_8).length);

		try (OutputStream os = r.getResponseBody()) {
			os.write(responseString.getBytes(StandardCharsets.UTF_8));
		}

	}

	boolean hasActorInDB(String actorId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		StatementResult results = tx
				.run("MATCH (a:Actor) WHERE a.actorId = '" + actorId + "' RETURN COUNT (a.actorId) as count");
		Record record = results.next();
		response = record.get("count").asInt();

		if (response > 0) {
			return true;
		}

		return false;
	}
	
	public void fileWrite(String sendToWrite)
	{
		try 
		{
			      File myObj = new File("filename.txt");
			      
			      if (myObj.createNewFile()) 
			      {
				        System.out.println("File created: " + myObj.getName());
				        
				        try 
				        {
				            FileWriter myWriter = new FileWriter("filename.txt");
				            myWriter.write(sendToWrite);
				            myWriter.close();
				            System.out.println("Successfully wrote to the file.");
				        }
				        catch (IOException e) 
				        {
				            System.out.println("An error occurred.");
				            e.printStackTrace();
				        }
			      } 
			      else 
			      {
			        System.out.println("File already exists.");
			      }
		} 
		catch (IOException e) 
		{
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		}
	}
	
	public Map<String, String> queryToMap(String query) {
	    if(query == null) {
	        return null;
	    }
	    
	    Map<String, String> result = new HashMap<>();
	    for (String param : query.split("&")) 
	    {
	        String[] entry = param.split("=");
	        if (entry.length > 1) 
	        {
	            result.put(entry[0], entry[1]);
	        }
	        else{
	            result.put(entry[0], "");
	        }
	    }
	    return result;
	}

}