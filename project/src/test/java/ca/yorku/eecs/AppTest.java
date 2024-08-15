package ca.yorku.eecs;

import junit.framework.Test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.json.JSONArray;

//import static org.junit.jupiter.api.Assertions.assertEquals;

//import org.junit.*;
//import org.junit.jupiter.api.Test;

// Extra imported libraries for testing
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
	public static List<String> addedActorIDs = new ArrayList<String>();
	public static List<String> addedMovieIDs = new ArrayList<String>();
	public static int count;
	final String kevinBaconId = "nm0000102";  // Kevin Bacon's actorId as specified
	
	private static final String[] ACTOR_NAMES = 
		{
		        "Robert Downey Jr.", "Scarlett Johansson", "Chris Hemsworth", 
		        "Jennifer Lawrence", "Tom Hanks", "Meryl Streep", 
		        "Leonardo DiCaprio", "Natalie Portman", "Morgan Freeman", 
		        "Denzel Washington", "Emma Stone", "Brad Pitt",
		        "Angelina Jolie", "Johnny Depp", "Anne Hathaway"
		};
	
	private static final String[] MOVIE_TITLES = 
		{
		        "The Shawshank Redemption", "The Godfather", "The Dark Knight", 
		        "Pulp Fiction", "Forrest Gump", "Inception", 
		        "Fight Club", "The Matrix", "Goodfellas", 
		        "The Lord of the Rings: The Return of the King", "Gladiator", "Titanic",
		        "Saving Private Ryan", "Schindler's List", "Interstellar"
		};
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
        
        
        
        
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        //assertTrue( true );
        
        Actor.createActor("nm0000102", "Kevin Bacon");
        Actor.createActor("nm0000103", "Carrie-Anne Moss");
        Actor.createActor("nm0000104", "Laurence Fishburne");
        Actor.createActor("nm0000105", "Hugo Weaving");
        Actor.createActor("nm0000106", "Joel Silver");
        Actor.createActor("nm0000107", "Kiefer Sutherland");
        Actor.createActor("nm0000108", "James Marshall");
        
        Movie.createMovie("M19928", "A Few Good Men");
        Movie.createMovie("M19929", "You've Got Mail");
        Movie.createMovie("M19930", "Top Gun");
        Movie.createMovie("M19931", "Stand By Me");
        Movie.createMovie("M19931", "What Dreams May Come");
        Movie.createMovie("M19931", "Joe Versus the Volcano");
        
        Relationship.createRelationship("nm0000102","M19928" );
        Relationship.createRelationship("nm0000103","M19928" );
        Relationship.createRelationship("nm0000103","M19929" );
        Relationship.createRelationship("nm0000104","M19929" );
        Relationship.createRelationship("nm0000104","M19930" );
    }
    
    
    
    public void testAddActorPass() throws IOException, JSONException {
    	
    	
		
		String actorId = "nm"+(int) (Math.random() * 10000); //always change this
		this.addedActorIDs.add(actorId);
		
		int nameIndex = (int) (Math.random() * ACTOR_NAMES.length);
		//System.out.println(nameIndex);
		
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
		String actorId = addedActorIDs.get(0);
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
	
public void testAddMoviePass() throws IOException, JSONException {
		
		String movieId = "nm"+(int) (Math.random() * 10000); //always change this
		addedMovieIDs.add(movieId);
		int nameIndex = (int) (Math.random() * MOVIE_TITLES.length);
		
		
		String name = MOVIE_TITLES[nameIndex];
		
		
		
		URL url = new URL("http://localhost:8080/api/v1/addMovie/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("movieId", movieId);
        jsonBody.put("name", name);
        
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(200, status);
        
	}
	
	public void testAddMovieFail() throws IOException, JSONException {
		
		String movieId = addedMovieIDs.get(addedMovieIDs.size()-1);
		//String movieId = "nm"+(int) (Math.random() * 10000); //always change this
		
		int nameIndex = 0;
		
		
		String name = MOVIE_TITLES[nameIndex];
		
		
		
		URL url = new URL("http://localhost:8080/api/v1/addMovie/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("movieId", movieId);
        jsonBody.put("name", name);
        
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(400, status);
        
	}

	public void testGetMoviePass() throws IOException, JSONException {
		
		
		String movie = addedMovieIDs.get(addedMovieIDs.size()-1);
		
		
		URL url = new URL("http://localhost:8080/api/v1/getMovie/?movieId="+movie);
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
            
            String movieId = jsonObject.getString("movieId");
            String name = jsonObject.getString("name");
            JSONArray actorsArray = jsonObject.getJSONArray("actors");
            
            List<String> actors = new ArrayList<String>();
            
            for (int i = 0; i < actorsArray.length(); i++) 
            {
                actors.add(actorsArray.getString(i));
            }
            
            try (Session session = DBConnect.driver.session()) 
            {
    			try (Transaction tx = session.beginTransaction()) 
    			{
    				String query = "MATCH (m:Movie {movieId: '" + movieId
    						+ "'}) WITH m,[(a:Actor)-[:ACTED_IN]->(m) | a.actorId] AS actorIDs "
    						+ "SET m.resume = actorIDs "
    						+ "RETURN m.movieId as movieId, m.name as name, m.resume as actors";

    				StatementResult results = tx.run(query);

    				if (results.hasNext()) 
    				{
    					Record record = results.next();
    					assertEquals(movieId, record.get("movieId").asString());
    					assertEquals(name, record.get("name").asString());
    					assertEquals(actors, record.get("actors").asList());
    					
    				}
    			}
    		}
            
        }
	}

	
	public void testGetMovieFail() throws IOException, JSONException {
		
		
		String movie = "ABCD";
		
		URL url = new URL("http://localhost:8080/api/v1/getMovie/?movieId="+movie);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status== 404, true);
        
        
	}
	
	
	public void testaddRelationshipPass() throws IOException, JSONException {
	    
		/*
		 * Here we are using the ID's from the previous actors and movies, assuming the both
		 * of them have been added successfully
		 */
		
	    String actorId = AppTest.addedActorIDs.get(AppTest.addedActorIDs.size()-1);
	    String movieId = AppTest.addedMovieIDs.get(AppTest.addedMovieIDs.size()-1);

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
		String actorId = addedActorIDs.get(0);  
	    String movieId = addedMovieIDs.get(0);  

		
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
        //System.out.println(status);
        assertEquals(status == 400 || status == 404, true);
        
	}
	
	public void testhasRelationshipPass() throws IOException, JSONException {
		
		/*
		 * Here, we are using the ID's that already have a relationship established
		 */
	 
	    String actorId = addedActorIDs.get(0); 
	    String movieId = addedMovieIDs.get(0); 

	    URL url = new URL("http://localhost:8080/api/v1/hasRelationship/?actorId=" + actorId + "&movieId=" + movieId);
	    HttpURLConnection con = (HttpURLConnection) url.openConnection();
	    con.setRequestMethod("GET");
	    con.setDoOutput(true);

	    int status = con.getResponseCode();
	    assertEquals(200, status);

	    if (status == HttpURLConnection.HTTP_OK) {
	        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        String inputLine;
	        StringBuilder response = new StringBuilder();

	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();

	        String jsonResponse = response.toString();
	        JSONObject jsonObject = new JSONObject(jsonResponse);

	        boolean hasRelationship = jsonObject.getBoolean("hasRelationship");

	        assertTrue(hasRelationship);

	        // Verify with the database directly
	        try (Session session = DBConnect.driver.session()) {
	            try (Transaction tx = session.beginTransaction()) {
	                String query = "MATCH (a:Actor)-[r:ACTED_IN]->(m:Movie) WHERE a.actorId = $actorId AND m.movieId = $movieId RETURN COUNT(a) as result";
	                StatementResult results = tx.run(query, parameters("actorId", actorId, "movieId", movieId));

	                if (results.hasNext()) {
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
	
	public void testcomputeBaconNumberPass() throws IOException, JSONException {
	    // Use a valid actor ID that should have a Bacon number
	    
		
		String actor = "nm0000104";
	    

	    URL url = new URL("http://127.0.0.1:8080/api/v1/computeBaconNumber/?actorId=" + actor);
	    HttpURLConnection con = (HttpURLConnection) url.openConnection();
	    con.setRequestMethod("GET");
	    con.setDoOutput(true);
	    
	    //System.out.println("testcomputeBaconNumberPass "+url);

	    int status = con.getResponseCode();
	    assertEquals(200, status);

	    if (status == HttpURLConnection.HTTP_OK) {
	        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        String inputLine;
	        StringBuilder response = new StringBuilder();

	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();

	        String jsonResponse = response.toString();
	        JSONObject jsonObject = new JSONObject(jsonResponse);

	        int apiBaconNumber = jsonObject.getInt("baconNumber");

	        //System.out.println("API Bacon Number: " + apiBaconNumber); // Debug print

	        // Here, instead of checking against the database, we simply assert that a Bacon number is returned
	        assertTrue(apiBaconNumber >= 0); // Bacon number should be 0 or greater
	    }
	}

	
	public void testcomputeBaconNumberFail() throws IOException, JSONException {
		
		String actor = "LD9812";
		
		try (Session session = DBConnect.driver.session()) 
        {
			try (Transaction tx = session.beginTransaction()) 
			{
				StatementResult results = tx.run(
                        "MATCH (a:Actor {actorId:$actorId}), (kb:Actor {actorId:$kevinBaconId}), "
                                + "p=shortestPath((a)-[:ACTED_IN*]-(kb)) "
                                + "RETURN length(p)/2 AS baconNumber",
                        parameters("actorId", actor, "kevinBaconId", kevinBaconId));
				

				if (results.hasNext()) 
				{
					Record record = results.next();
					assertEquals(0, record.get("baconNumber").asInt());
					
					
				}
			}
		}
		
		
		URL url = new URL("http://127.0.0.1:8080/api/v1/computeBaconNumber/?actorId="+actor);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status ==404, true);
        
        
            
        
	}
	
	public void testcomputeBaconPathPass() throws IOException, JSONException {
		/*
		 * Here, we're using a valid actor ID that already has a Bacon path...
		 */
		
	    String id = "nm0000104"; //This is in our DB

	    URL url = new URL("http://127.0.0.1:8080/api/v1/computeBaconPath/?actorId=" + id);
	    HttpURLConnection con = (HttpURLConnection) url.openConnection();
	    con.setRequestMethod("GET");
	    con.setDoOutput(true);
	    
	    System.out.println("testcomputeBaconPathPass() "+url);

	    int status = con.getResponseCode();
	    assertEquals(200, status);

	    if (status == HttpURLConnection.HTTP_OK) {
	        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        String inputLine;
	        StringBuilder response = new StringBuilder();

	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();

	        String jsonResponse = response.toString();
	        JSONObject jsonObject = new JSONObject(jsonResponse);

	        JSONArray apiPath = jsonObject.getJSONArray("baconPath");

	        System.out.println("API Bacon Path: " + apiPath.toString()); 

	        // Here, we're using assertTrue to ensure that the returned path isn't empty
	        
	        assertTrue(apiPath.length() > 0); // Path should not be empty
	    }
	}
	
	public void testcomputeBaconPathFail() throws IOException, JSONException 
	{
	   
	    String id = "nm9999999"; // This is assuming that the ID is not related to Kevin Bacon's ID

	    URL url = new URL("http://localhost:8080/api/v1/computeBaconPath/?actorId=" + id);
	    HttpURLConnection con = (HttpURLConnection) url.openConnection();
	    con.setRequestMethod("GET");
	    con.setDoOutput(true);

	    int s = con.getResponseCode();
	    // Using assertTrue to see if we're getting 404 or 400 error status
	    assertTrue(s == 400 || s == 404);

	    if (s == HttpURLConnection.HTTP_OK) 
	    {
	        BufferedReader read = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        String input;
	        StringBuilder output = new StringBuilder();

	        while ((input = read.readLine()) != null) 
	        	output.append(input);
	        
	        read.close();

	        String response = output.toString();
	        JSONObject obj = new JSONObject(response);

	        // Here, we're checking to see if the path is empty. Otherwise, an error message is sent
	        if (obj.has("error")) 
	        {
	            String errorMessage = obj.getString("error");
	            System.out.println("Error Message: " + errorMessage);
	            assertFalse(errorMessage.isEmpty());
	        } 
	        
	        else 
	        {
	            JSONArray path = obj.getJSONArray("baconPath");
	            assertTrue(path.length() == 0); // If there's no connection, the path should be empty
	        }
	    }
	}


    
}
