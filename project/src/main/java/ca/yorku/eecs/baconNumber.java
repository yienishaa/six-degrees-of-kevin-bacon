package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class baconNumber implements HttpHandler {

	public baconNumber() {
	}

	@Override
	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("GET")) {
				handleGet(r);
			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleGet(HttpExchange r) throws IOException, JSONException {
		String body = DBConnect.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String actorId;
		int baconNumber = -1;
		final String kevinBaconId = "nm0000102"; // Kevin Bacon's actorId as specified

		if (deserialized.has("actorId")) {
			actorId = deserialized.getString("actorId");
		} else {
			actorId = "";
		}

		System.out.println("INPUT: actorId: " + actorId);

		try (Session session = DBConnect.driver.session()) {
			if (actorId.isEmpty()) {
				statusCode = 400; // BAD REQUEST
			} else {
				try (Transaction tx = session.beginTransaction()) {
					// Check if the actor exists
					StatementResult result = tx.run("MATCH (a:Actor {actorId:$actorId}) RETURN a.actorId AS actorId",
							parameters("actorId", actorId));
					if (!result.hasNext()) {
						statusCode = 404; // NOT FOUND
					} else {
						// Compute the Bacon Number using a shortest path query
						StatementResult pathResult = tx.run(
								"MATCH (a:Actor {actorId:$actorId}), (kb:Actor {actorId:$kevinBaconId}), "
										+ "p=shortestPath((a)-[:ACTED_IN*]-(kb)) "
										+ "RETURN length(p)/2 AS baconNumber",
								parameters("actorId", actorId, "kevinBaconId", kevinBaconId));
						if (pathResult.hasNext()) {
							Record record = pathResult.next();
							baconNumber = record.get("baconNumber").asInt();
							JSONObject response = new JSONObject();
							response.put("baconNumber", baconNumber);
							String jsonResponse = response.toString();
							r.sendResponseHeaders(200, jsonResponse.length());
							r.getResponseBody().write(jsonResponse.getBytes());
							r.getResponseBody().close();
						} else {
							statusCode = 404; // NOT FOUND
						}
					}
					tx.success();
				}
			}
		} catch (Exception e) {
			System.err.println("Caught Exception: " + e.getMessage());
			statusCode = 500; // INTERNAL SERVER ERROR
		}
		r.sendResponseHeaders(statusCode, -1);
	}
}