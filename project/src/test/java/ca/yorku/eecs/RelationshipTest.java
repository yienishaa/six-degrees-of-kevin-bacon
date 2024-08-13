package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import junit.framework.TestCase;

public class RelationshipTest extends TestCase {

	public RelationshipTest(String name) {
		super(name);
	}

	public void testaddRelationshipPass() throws IOException, JSONException {
		
		String actorId = "nm663";
		String movieId = "nm9172";
		
		URL url = new URL("http://localhost:8080/api/v1/addRelationship/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("actorId", actorId);
        jsonBody.put("movieId", movieId);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(200, status);
        
	}
	
	public void testaddRelationshipFail() throws IOException, JSONException {
		
		//Attempting to add the same actor will fail
		String actorId = "nm663";
		String movieId = "nm9172";
		
		URL url = new URL("http://localhost:8080/api/v1/addRelationship/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("actorId", actorId);
        jsonBody.put("movieId", movieId);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status == 404, true);
        
	}
	
	public void testhasRelationshipPass() throws IOException, JSONException {
		
		//String actor = "nm1001288";
		
		String actorId = "nm0000102";
		String movieId = "nm1111891";
		
		URL url = new URL("http://localhost:8080/api/v1/hasRelationship/?actorId="+actorId+"&movieId="+movieId);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(200, status);
        
        if (status == HttpURLConnection.HTTP_OK) 
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) 
            {
                response.append(inputLine);
            }
            in.close();

            String jsonResponse = response.toString();
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            actorId = jsonObject.getString("actorId");
            movieId = jsonObject.getString("movieId");
            boolean hasRelationship = jsonObject.getBoolean("hasRelationship");
            
            
            
            //////////////////////////////////////////////////////////
            
            try (Session session = DBConnect.driver.session()) 
            {
    			try (Transaction tx = session.beginTransaction()) 
    			{
    				String query = "MATCH (a:Actor)-[r:ACTED_IN]->(m:Movie) " + "WHERE a.actorId = '" + actorId
    						+ "' AND m.movieId = '" + movieId + "' RETURN COUNT(a) as result";

    				StatementResult results = tx.run(query);

    				if (results.hasNext()) 
    				{
    					Record record = results.next();
    					
    					assertEquals(1, record.get("result").asInt());
    					
    					
    				}
    			}
    		}
            
        }
	}
	
	public void testhasRelationshipFail() throws IOException, JSONException {
		
		String actorId = "nm0000";
		String movieId = "AAKKKKKKKA";
		
		URL url = new URL("http://localhost:8080/api/v1/hasRelationship/?actorId="+actorId+"&movieId="+movieId);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status == 404, true);
        
        
	}

}
