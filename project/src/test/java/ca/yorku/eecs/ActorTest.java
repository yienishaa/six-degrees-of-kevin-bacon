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

public class ActorTest extends TestCase {
	
	private static List<String> addedActorIDs = new ArrayList<String>();
	private static int count;
	
	private static final String[] ACTOR_NAMES = 
	{
	        "Robert Downey Jr.", "Scarlett Johansson", "Chris Hemsworth", 
	        "Jennifer Lawrence", "Tom Hanks", "Meryl Streep", 
	        "Leonardo DiCaprio", "Natalie Portman", "Morgan Freeman", 
	        "Denzel Washington", "Emma Stone", "Brad Pitt",
	        "Angelina Jolie", "Johnny Depp", "Anne Hathaway"
	};
	
	

	public ActorTest(String name) {
		super(name);
	}

	
	
	public void testAddActorPass() throws IOException, JSONException {
		
		String actorId = "nm"+(int) (Math.random() * 10000); //always change this
		this.addedActorIDs.add(actorId);
		
		int nameIndex = (int) (Math.random() * ACTOR_NAMES.length);
		System.out.println(nameIndex);
		
		String name = ACTOR_NAMES[nameIndex];

		
		URL url = new URL("http://localhost:8080/api/v1/addActor/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("actorId", actorId);
        jsonBody.put("name", name);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(200, status);
        
	}
	
	public void testAddActorFail() throws IOException, JSONException {
		
		//Attempting to add the same actor will fail
		String actorId = "nm121212";
		String name = "Kanye West";
		
		URL url = new URL("http://localhost:8080/api/v1/addActor/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("actorId", actorId);
        jsonBody.put("name", name);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(400, status);
        
	}
	
	public void testGetActorPass() throws IOException, JSONException {
		
		//String actor = "nm1001288";
		
		String actor = addedActorIDs.get(addedActorIDs.size()-1);
		
		URL url = new URL("http://localhost:8080/api/v1/getActor/?actorId="+actor);
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
            
            String actorId = jsonObject.getString("actorId");
            String name = jsonObject.getString("name");
            JSONArray movieArray = jsonObject.getJSONArray("movies");
            
            List<String> movies = new ArrayList<String>();
            
            for (int i = 0; i < movieArray.length(); i++) 
            {
            	movies.add(movieArray.getString(i));
            }
            System.out.println("movies: " + movies.toString());
            System.out.println("name: " + name);
            System.out.println("actorId: " + actorId);
            
            //////////////////////////////////////////////////////////
            
            try (Session session = DBConnect.driver.session()) 
            {
    			try (Transaction tx = session.beginTransaction()) 
    			{
    				String query = "MATCH (a:Actor {actorId:'" +actor
    						+ "'}) WITH a,[(a)-[:ACTED_IN]->(b:Movie) | b.movieId] AS movieIDs "
    						+ "SET a.resume = movieIDs "
    						+ "RETURN a.actorId as actorId, a.name as name, a.resume as movies";

    				StatementResult results = tx.run(query);

    				if (results.hasNext()) 
    				{
    					Record record = results.next();
    					assertEquals(actorId, record.get("actorId").asString());
    					assertEquals(name, record.get("name").asString());
    					assertEquals(movies, record.get("movies").asList());
    					
    				}
    			}
    		}
            
        }
	}
	
	public void testGetActorFail() throws IOException, JSONException {
		
		String actor = "ABBB";
		
		URL url = new URL("http://localhost:8080/api/v1/getActor/?actorId="+actor);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 404 || status == 404, true);
        
        
	}
	
	

}
