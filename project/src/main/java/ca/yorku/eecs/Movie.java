package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

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

public class Movie implements HttpHandler {

	public Movie() {

	}

	@Override
	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("PUT")) {
				addMovie(r);
			} else if (r.getRequestMethod().equals("GET")) {
				getMovie(r);
			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addMovie(HttpExchange r) throws IOException, JSONException {

		String body = DBConnect.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String name;
		String movieId;

		if (deserialized.has("name")) {
			name = deserialized.getString("name");
		} else {
			name = "";
		}
		if (deserialized.has("movieId")) {
			movieId = deserialized.getString("movieId");
		} else {
			movieId = "";
		}

		System.out.println("INPUTS: name: " + name + " movieId: " + movieId);

		if (!hasMovieInDB(movieId)) {
			try (Session session = DBConnect.driver.session()) {

				session.run("CREATE (a:Movie {name:$name, movieId:$movieId});",
						parameters("name", name, "movieId", movieId));

				System.out.println("The Neo4j transaction ran");
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

	public void getMovie(HttpExchange r) throws IOException, JSONException {

		Map<String, String> params = queryToMap(r.getRequestURI().getRawQuery()); 
		
		//System.out.println("param A=" + r.getRequestURI().getRawQuery());
		
		//String body = DBConnect.convert(r.getRequestBody());
		//JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		String response = null;
		String movieId = params.get("movieId");

		/*if (deserialized.has("movieId")) {
			movieId = deserialized.getString("movieId");
		} else {
			movieId = "";
		}*/

		System.out.println("OUTPUTS: movieId: " + movieId);

		// MATCH (Actor)-[:ACTED_IN]->(Movie) WHERE Movie.movieId='nm7001453' return
		// Movie.movieId as movieId, Movie.name as name, Actor.name as actors

		JSONObject obj = new JSONObject();

		try (Session session = DBConnect.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				String query = "MATCH (m:Movie {movieId: '" + movieId
						+ "'}) WITH m,[(a:Actor)-[:ACTED_IN]->(m) | a.actorId] AS actorIDs "
						+ "SET m.resume = actorIDs "
						+ "RETURN m.movieId as movieId, m.name as name, m.resume as actors";

				StatementResult results = tx.run(query);

				if (results.hasNext()) {
					Record record = results.next();
					statusCode = 200;
					obj.put("movieId", record.get("movieId").asString());
					obj.put("name", record.get("name").asString());
					obj.put("actors", record.get("actors").asList());
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

		/*
		 * r.sendResponseHeaders(statusCode, response.length()); OutputStream os =
		 * r.getResponseBody(); os.write(response.getBytes()); os.close();
		 */

		System.out.println(obj);

		String responseString = obj.toString();
		r.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		r.sendResponseHeaders(statusCode, responseString.getBytes(StandardCharsets.UTF_8).length);

		try (OutputStream os = r.getResponseBody()) {
			os.write(responseString.getBytes(StandardCharsets.UTF_8));
		}

	}

	boolean hasMovieInDB(String movieId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		StatementResult results = tx
				.run("MATCH (a:Movie) WHERE a.movieId = '" + movieId + "' RETURN COUNT (a.movieId) as count");
		Record record = results.next();
		response = record.get("count").asInt();

		if (response > 0) {
			return true;
		}

		return false;
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
}