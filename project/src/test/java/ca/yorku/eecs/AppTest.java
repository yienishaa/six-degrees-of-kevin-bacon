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
/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
	private static List<String> addedActorIDs = new ArrayList<String>();
	private static List<String> addedMovieIDs = new ArrayList<String>();
	private static int count;
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
        assertTrue( true );
    }
    
    public void test_addActorPass() throws IOException, JSONException {
		
		String actorId = "nm"+(int) (Math.random() * 10000); //always change this
		this.addedActorIDs.add(actorId);
		
		int nameIndex = (int) (Math.random() * ACTOR_NAMES.length);
		
		
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
    
    public void test_addActorFail() throws IOException, JSONException {
		
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
    
    public void test_getActorPass() throws IOException, JSONException {
		
		
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
	
	public void test_getActorFail() throws IOException, JSONException {
		
		String actor = "ABBB";
		
		URL url = new URL("http://localhost:8080/api/v1/getActor/?actorId="+actor);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 404 || status == 404, true);
        
        
	}
	
	public void test_addMoviePass() throws IOException, JSONException {
		
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
        System.out.println(jsonBody);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(200, status);
        
	}
	
	public void test_addMovieFail() throws IOException, JSONException {
		
		String movieId = addedMovieIDs.get(addedMovieIDs.size()-1);
		
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
        System.out.println(jsonBody);
        
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int status = con.getResponseCode();
        assertEquals(400, status);
        
	}

	public void test_getMoviePass() throws IOException, JSONException {
		
		
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

	
	public void test_getMovieFail() throws IOException, JSONException {
		
		
		String movie = "ABCD";
		
		URL url = new URL("http://localhost:8080/api/v1/getMovie/?movieId="+movie);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status== 404, true);
        
        
	}
	
	
	public void test_addRelationshipPass() throws IOException, JSONException {
		
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
	
	public void test_addRelationshipFail() throws IOException, JSONException {
		
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
	
	public void test_hasRelationshipPass() throws IOException, JSONException {
		
		
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
	
	public void test_hasRelationshipFail() throws IOException, JSONException {
		
		String actorId = "nm0000";
		String movieId = "AAKKKKKKKA";
		
		URL url = new URL("http://localhost:8080/api/v1/hasRelationship/?actorId="+actorId+"&movieId="+movieId);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status == 404, true);
        
        
	}
	
	public void test_computeBaconNumberPass() throws IOException, JSONException {
		
		
		String actor = addedActorIDs.get(addedActorIDs.size()-1);
		
		
		URL url = new URL("http://localhost:8080/api/v1/computeBaconNumber/?actorId="+actor);
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
            
            int num = jsonObject.getInt("baconNumber");
            
            
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
    					assertEquals(num, record.get("baconNumber").asInt());
    					
    					
    				}
    			}
    		}
            
        }
	}
	
	public void test_computeBaconNumberFail() throws IOException, JSONException {
		
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
		
		
		URL url = new URL("http://localhost:8080/api/v1/computeBaconNumber/?actorId="+actor);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        
        int status = con.getResponseCode();
        assertEquals(status == 400 || status ==404, true);
        
        
            
        
	}
	
	public void test_computeBaconPathPass() throws IOException, JSONException {
		
		
		String actor = "RW42098";
		
		
		URL url = new URL("http://localhost:8080/api/v1/computeBaconNumber/?actorId="+actor);
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
            
            JSONArray jsonPath = jsonObject.getJSONArray("baconPath");
            
            List<String> pathList = new ArrayList<String>();
            
            for (int i = 0; i < jsonPath.length(); i++) 
            {
                pathList.add(jsonPath.getString(i));
            }
            
            try (Session session = DBConnect.driver.session()) 
            {
    			try (Transaction tx = session.beginTransaction()) 
    			{
    				StatementResult results = tx.run(
                            "MATCH (a:Actor {actorId:$actorId}), (kb:Actor {actorId:$kevinBaconId}), "
                                    + "p=shortestPath((a)-[:ACTED_IN*]-(kb)) "
                                    + "RETURN nodes(p) AS path",
                            parameters("actorId", actor, "kevinBaconId", kevinBaconId));

    				if (results.hasNext()) 
    				{
    					Record record = results.next();
    					
    					assertEquals(pathList, record.get("actors").asList());
    					
    				}
    			}
    		}
            
        }
	}
    
}
