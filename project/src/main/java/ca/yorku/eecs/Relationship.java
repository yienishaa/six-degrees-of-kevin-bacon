package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
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

public class Relationship implements HttpHandler {

	public Relationship() {

	}

	@Override
	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("PUT")) {
				addRelationship(r);
			} else if (r.getRequestMethod().equals("GET")) {
				hasRelationship(r);
			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds relationship between Actor - ACTED_IN - Movie if the relationship doesn't already exist
	 * @param r
	 * @throws IOException
	 * @throws JSONException
	 */
	public void addRelationship(HttpExchange r) throws IOException, JSONException {

		String body = DBConnect.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String actorId;
		String movieId;

		if (deserialized.has("actorId")) {
			actorId = deserialized.getString("actorId");
		} else {
			actorId = "";
		}
		if (deserialized.has("movieId")) {
			movieId = deserialized.getString("movieId");
		} else {
			movieId = "";
		}

		if (Utils.hasMovieAndActor(movieId, actorId) == true && Utils.hasRelationshipAlready(movieId,actorId) == false) 
		{
			try (Session session = DBConnect.driver.session()) {

				String query = "MATCH (a: Actor), (m: Movie) " + "WHERE a.actorId = '" + actorId + "' "
						+ "AND m.movieId = '" + movieId + "' " + "CREATE (a)-[r:ACTED_IN]->(m)";
				
				
				session.run(query);

				statusCode = 200;
			}

			catch (Exception e) {
				System.err.println("Caught Exception: " + e.getMessage());
				statusCode = 500;
			}
		} 
		else {
			statusCode = 400;
		}

		r.sendResponseHeaders(statusCode, -1);
	}

	public void hasRelationship(HttpExchange r) throws IOException, JSONException {
		
		int statusCode = 0;
		String response = null;
		String movieId = "";
		String actorId = "";
		
		/* if the movieId and actorId is sent over Http parameters */
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			
			statusCode = 0;
			response = null;
			movieId = params.get("movieId");
			actorId = params.get("actorId");
		}
		else
		{
			/* if the movieId and actorId is sent over http body as a Json object*/
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			statusCode = 0;
			actorId = deserialized.optString("actorId", "");
			movieId = deserialized.optString("movieId", "");
		}

		
		JSONObject obj = new JSONObject();

		try (Session session = DBConnect.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				String query = "MATCH (a:Actor)-[r:ACTED_IN]->(m:Movie) " + "WHERE a.actorId = '" + actorId
						+ "' AND m.movieId = '" + movieId + "' RETURN COUNT(r) as count";
				System.out.println(query);

				StatementResult results = tx.run(query);

				if (results.hasNext()) {
					Record record = results.next();
					
					if(record.get("count").asInt() > 0)
					{
						statusCode = 200;
						obj.put("actorId", actorId);
						obj.put("movieId", movieId);
						obj.put("hasRelationship", true);
					}
					else
					{
						statusCode = 404;
					}
					
				} 
				else 
				{
					statusCode = 400;
					//obj.put("error", "NOT FOUND");
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

		String responseString = obj.toString();
		r.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		r.sendResponseHeaders(statusCode, responseString.getBytes(StandardCharsets.UTF_8).length);

		try (OutputStream os = r.getResponseBody()) {
			os.write(responseString.getBytes(StandardCharsets.UTF_8));
		}

	}

	
}