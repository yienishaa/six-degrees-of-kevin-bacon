package ca.yorku.eecs;


import static org.neo4j.driver.v1.Values.parameters;


import java.io.IOException;
import java.io.OutputStream; //output stream
import java.nio.charset.StandardCharsets;
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
				getActor(r);

			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * If actor doesn't already exist, adds it to the database
	 * @param r
	 * @throws IOException
	 * @throws JSONException
	 */
	public void addActor(HttpExchange r) throws IOException, JSONException {

		String body = DBConnect.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		
		int statusCode = 0;
		String name;
		String actorId;

		/* Gets the name, actorId from the http request body(JSON), extracts them to variables */
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

		/* if the actor is in the DB return 400, if not add to the DB */
		if (!Utils.hasActorInDB(actorId)) {
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

	/**
	 * Finds an actor from the DB
	 * @param r
	 * @throws IOException
	 * @throws JSONException
	 */
	public void getActor(HttpExchange r) throws IOException, JSONException {
		
		String actorId = "";
		String response;
		int statusCode = 0;
		
		/* if the actorId is sent in URL parameters*/
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			statusCode = 0;
			actorId = params.get("actorId");
		}
		else
		{
			/* if the actorId is sent in body of the request */
			
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			statusCode = 0;
			actorId = deserialized.optString("actorId", "");
		}

		JSONObject obj = new JSONObject();

		try (Session session = DBConnect.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				String query = "MATCH (a:Actor {actorId:'" + actorId
						+ "'}) WITH a,[(a)-[:ACTED_IN]->(b:Movie) | b.movieId] AS movieIDs "
						+ "SET a.resume = movieIDs "
						+ "RETURN a.actorId as actorId, a.name as name, a.resume as movies";

				
				StatementResult results = tx.run(query);

				if (results.hasNext()) {
					Record record = results.next();
					statusCode = 200;

					obj.put("actorId", record.get("actorId").asString());
					obj.put("name", record.get("name").asString());
					obj.put("movies", record.get("movies").asList());
					
					
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

		String responseString = obj.toString();
		r.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		r.sendResponseHeaders(statusCode, responseString.getBytes(StandardCharsets.UTF_8).length);

		try (OutputStream os = r.getResponseBody()) {
			os.write(responseString.getBytes(StandardCharsets.UTF_8));
		}

	}

	

}