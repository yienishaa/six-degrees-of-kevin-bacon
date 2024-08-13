package ca.yorku.eecs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class BaconPath implements HttpHandler {

    public BaconPath() {
    }

    @Override
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

    public void handleGet(HttpExchange r) throws IOException, JSONException 
    {
    	
    	int statusCode = 0;
        String actorId = "";
        final String kevinBaconId = "nm0000102";  // Kevin Bacon's actorId
        
		
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = queryToMap(r.getRequestURI().getRawQuery()); 
			
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
    	
        System.out.println("INPUT: actorId: " + actorId);

        if (actorId.isEmpty()) {
            sendErrorResponse(r, 400, "Invalid request: 'actorId' is required.");
            return;
        }

        try (Session session = DBConnect.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Check if the actor exists
                
            	//StatementResult result = tx.run("MATCH (a:Actor {actorId:$actorId}) RETURN a.actorId AS actorId",
            	//parameters("actorId", actorId));
            	
            	boolean actorExsists = hasActor(actorId);

                if (!actorExsists) {
                    sendErrorResponse(r, 404, "Actor not found.");
                } else {
                    // Compute the shortest path to Kevin Bacon
                	
                	StatementResult pathResult = tx.run(
                            "MATCH (a:Actor {actorId:$actorId}), (kb:Actor {actorId:$kevinBaconId}), "
                                    + "p=shortestPath((a)-[:ACTED_IN*]-(kb)) "
                                    + "RETURN nodes(p) AS path",
                            parameters("actorId", actorId, "kevinBaconId", kevinBaconId));
                	
                	
                    List<String> baconPath = new ArrayList<>();

                    if (pathResult.hasNext()) {
                        Record record = pathResult.next();
                        
                        List<Node> pathNodes = record.get("path").asList(Value::asNode); // Convert each item to a Node
                        
                        
                        for(Node node : pathNodes) {
                        	if(node.containsKey("actorId"))
                        	{
                        		baconPath.add(node.get("actorId").asString());
                        	}
                        	if(node.containsKey("movieId"))
                        	{
                        		baconPath.add(node.get("movieId").asString());
                        	}
                            
                        }
                        
                        JSONArray response = new JSONArray(baconPath);
                        
                        sendSuccessResponse(r,response);
                    } else {
                        sendErrorResponse(r, 404, "No path to Kevin Bacon found.");
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

    private void sendSuccessResponse(HttpExchange r, JSONArray response) throws IOException, JSONException {
    	
    	JSONObject obj = new JSONObject();
    	obj.put("baconPath", response);
        String jsonResponse = obj.toString();
        r.sendResponseHeaders(200, jsonResponse.length());
        r.getResponseBody().write(jsonResponse.getBytes());
        r.getResponseBody().close();
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
    
    boolean hasActor(String actorId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		String queryActor = "MATCH (a:Actor) WHERE a.actorId = '" + actorId + "' RETURN COUNT (a.actorId) as count";
		
		StatementResult resultsActor = tx.run(queryActor);
		

		if (resultsActor.hasNext()) {
			
			Record recordA = resultsActor.next();
			
			if(recordA.get("count").asInt()==1)
			{
				
				
				return true;
			}
			else 
			{
				
				return false;
			}
		}

		
		return false;
	}
}
