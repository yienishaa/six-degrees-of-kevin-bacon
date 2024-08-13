package ca.yorku.eecs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class BaconNumber implements HttpHandler {

    public BaconNumber() {
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGet(r);
            } else {
                r.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the Bacon number between actorId and Kevin Bacon
     * @param r
     * @throws IOException
     * @throws JSONException
     */
    public void handleGet(HttpExchange r) throws IOException, JSONException {
    	
    	
        
        final String kevinBaconId = "nm0000102";  // Kevin Bacon's actorId as specified
    	String actorId = "";
		//String response;
		int statusCode = 0;
		
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			
			actorId = params.get("actorId");
		}
		else
		{
			String body = DBConnect.convert(r.getRequestBody());
	        JSONObject deserialized = new JSONObject(body);
	        if (deserialized.has("actorId")) {
	            actorId = deserialized.getString("actorId");
	        }
		}
    	
        //System.out.println("INPUT: actorId: " + actorId);

        if (actorId.isEmpty()) {
            sendErrorResponse(r, 400, "Invalid request: 'actorId' is required.");
            return;
        }

        try (Session session = DBConnect.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Check if the actor exists
                StatementResult result = tx.run("MATCH (a:Actor {actorId:$actorId}) RETURN a.actorId AS actorId",
                        parameters("actorId", actorId));

                if (!result.hasNext()) {
                    sendErrorResponse(r, 404, "Actor not found.");
                } else {
                    // Compute the Bacon Number using a shortest path query
                    StatementResult pathResult = tx.run(
                            "MATCH (a:Actor {actorId:$actorId}), (kb:Actor {actorId:$kevinBaconId}), "
                                    + "p=shortestPath((a)-[:ACTED_IN*]-(kb)) "
                                    + "RETURN length(p)/2 AS baconNumber",
                            parameters("actorId", actorId, "kevinBaconId", kevinBaconId));

                    if (pathResult.hasNext()) {
                        Record record = pathResult.next();
                        int baconNumber = record.get("baconNumber").asInt();
                        JSONObject response = new JSONObject();
                        response.put("baconNumber", baconNumber);
                        sendSuccessResponse(r, response);
                    } else {
                        sendErrorResponse(r, 404, "No connection to Kevin Bacon found.");
                    }
                }
                tx.success();
            }
        } catch (Exception e) {
            System.err.println("Caught Exception: " + e.getMessage());
            sendErrorResponse(r, 500, "Internal server error.");
        }
    }

    private void sendErrorResponse(HttpExchange r, int statusCode, String message) throws IOException {
        JSONObject response = new JSONObject();
        try {
            response.put("error", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsonResponse = response.toString();
        r.sendResponseHeaders(statusCode, jsonResponse.length());
        r.getResponseBody().write(jsonResponse.getBytes());
        r.getResponseBody().close();
    }

    private void sendSuccessResponse(HttpExchange r, JSONObject response) throws IOException {
        String jsonResponse = response.toString();
        r.sendResponseHeaders(200, jsonResponse.length());
        r.getResponseBody().write(jsonResponse.getBytes());
        r.getResponseBody().close();
    }
    
    
}
