package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

public class MovieTest extends TestCase {

	public MovieTest(String name) throws IOException, JSONException {
		super(name);
		
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	

	public void testMovie() throws IOException, JSONException {
		testGetMovie("nm7001453");
		
	}

	public void testHandle() {
		fail("Not yet implemented");
	}

	public void testAddMovie() throws IOException, JSONException {
		
		String movieId = "nm1111000";
		String name = "name";
		
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

	public void testGetMovie(String movie) throws IOException, JSONException {
		
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
            System.out.println("movieId: " + movieId);
            System.out.println("name: " + name);
            System.out.println("actors: " + actors.toString());
            
            //////////////////////////////////////////////////////////
            
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

	public void testHasMovieInDB() {
		fail("Not yet implemented");
	}

}
