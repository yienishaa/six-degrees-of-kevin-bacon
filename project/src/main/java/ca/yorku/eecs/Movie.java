package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Movie implements HttpHandler {

	public Movie() {
	}
	
	public static void createMovieAll(String movieId, String name, int year, double rating, long profit) 
	{
		if (!Utils.hasMovieInDB(movieId)) {
	        try (Session session = DBConnect.driver.session()) {
	            session.run("CREATE (a:Movie {name:$name, movieId:$movieId, year:$year, rating:$rating, profit:$profit});",
	                    parameters("name", name, "movieId", movieId, "year", year, "rating", rating, "profit", profit));
	            
	            
	        } catch (Exception e) {
	            System.err.println("Caught Exception: " + e.getMessage());
	            
	        }
	    } else {
	        System.out.println("Movie already exsist");
	    }
	}
	
	public static void createMovie(String movieId, String name) 
	{
		if (!Utils.hasMovieInDB(movieId)) {
	        try (Session session = DBConnect.driver.session()) {
	            session.run("CREATE (a:Movie {name:$name, movieId:$movieId});",
	                    parameters("name", name, "movieId", movieId));
	            
	            
	        } catch (Exception e) {
	            System.err.println("Caught Exception: " + e.getMessage());
	            
	        }
	    } else {
	        System.out.println("Movie already exsist");
	    }
	}

	@Override
	public void handle(HttpExchange r) {
		String path = r.getRequestURI().getPath();
		//System.out.println("Request Path: " + path); // Debugging line

		try {
			if (r.getRequestMethod().equals("PUT")) {
				addMovie(r);
			} else if (r.getRequestMethod().equals("GET")) {
				if (path.equals("/api/v1/moviesByYear/")) {
					handleMoviesByYear(r);
				} else if (path.equals("/api/v1/moviesByPopularity/")) {
					handleMoviesByPopularity(r);
				} else if (path.equals("/api/v1/boxOfficePerformance/")) {
					handleBoxOfficePerformance(r);
				} else {
					getMovie(r);
					System.out.println("get movie basic");
				}
			} else {
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Adds a movie to the DB if it doesn't already exist
	 * @param r
	 * @throws IOException
	 * @throws JSONException
	 */
	public void addMovie(HttpExchange r) throws IOException, JSONException {
	    String body = DBConnect.convert(r.getRequestBody());
	    JSONObject deserialized = new JSONObject(body);
	    int statusCode = 0;

	    String name = deserialized.optString("name", "");
	    String movieId = deserialized.optString("movieId", "");
	    int year = deserialized.optInt("year", 0);
	    double rating = deserialized.optDouble("rating", 0.0);
	    long profit = deserialized.optLong("profit", 0);

	    if (!Utils.hasMovieInDB(movieId)) {
	        try (Session session = DBConnect.driver.session()) {
	            session.run("CREATE (a:Movie {name:$name, movieId:$movieId, year:$year, rating:$rating, profit:$profit});",
	                    parameters("name", name, "movieId", movieId, "year", year, "rating", rating, "profit", profit));
	            System.out.println("The Neo4j transaction ran");
	            statusCode = 200;
	        } catch (Exception e) {
	            System.err.println("Caught Exception: " + e.getMessage());
	            statusCode = 500;
	        }
	    } else {
	        statusCode = 400;
	    }

	    r.sendResponseHeaders(statusCode, -1);
	}

	/**
	 * Getting movie by only the movieId
	 * @param r
	 * @throws IOException
	 * @throws JSONException
	 */
	public void getMovie(HttpExchange r) throws IOException, JSONException {

		int statusCode = 0;
		String response = null;
		String movieId = "";
		
		/* if movieId sent in the http request parameters*/
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			
			statusCode = 0;
			response = null;
			movieId = params.get("movieId");
		}
		else
		{
			/* if the movieId is sent in body of the request */
			
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			statusCode = 0;
			movieId = deserialized.optString("movieId", "");
		}
		
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


		String responseString = obj.toString();
		r.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		r.sendResponseHeaders(statusCode, responseString.getBytes(StandardCharsets.UTF_8).length);

		try (OutputStream os = r.getResponseBody()) {
			os.write(responseString.getBytes(StandardCharsets.UTF_8));
		}

	}

	/**
	 * Handles the retrieval of movies based on the year of release.
	 * <p>
	 * This method processes a GET request to fetch movies released in a specific
	 * year. The year is provided in the request body as a JSON object. The method
	 * verifies the validity of the year and retrieves all movies released in that
	 * year. If no movies are found, it returns a 404 status. The results are
	 * returned as a JSON array.
	 * </p>
	 *
	 * @param r the HttpExchange object representing the request and response.
	 * @throws IOException   if an I/O error occurs.
	 * @throws JSONException if an error occurs while parsing the JSON request body.
	 */
	public void handleMoviesByYear(HttpExchange r) throws IOException, JSONException {
		
		
		int statusCode = 0;
		
		int year = 0;
		
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			
			statusCode = 0;
			
			year = Integer.parseInt(params.get("year"));
		}
		else
		{
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			statusCode = 0;
			year = deserialized.optInt("year", 0);
		}
		
		
		//System.out.println("INPUT: year: " + year);

		try (Session session = DBConnect.driver.session()) {
			if (year == 0) {
				statusCode = 400; // BAD REQUEST
			} else {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run(
							"MATCH (m:Movie) WHERE m.year = $year RETURN m.movieId AS movieId, m.name AS name, m.rating AS rating",
							parameters("year", year));
					List<JSONObject> moviesList = new ArrayList<>();
					while (result.hasNext()) {
						Record record = result.next();
						JSONObject movie = new JSONObject();
						movie.put("movieId", record.get("movieId").asString());
						movie.put("name", record.get("name").asString());
						movie.put("rating", record.get("rating").asDouble());
						movie.put("year", year);
						moviesList.add(movie);
					}

					if (moviesList.isEmpty()) {
						statusCode = 404; // NOT FOUND
					} else {
						JSONArray response = new JSONArray(moviesList);
						String jsonResponse = response.toString();
						r.sendResponseHeaders(200, jsonResponse.length());
						r.getResponseBody().write(jsonResponse.getBytes());
						r.getResponseBody().close();
						return;
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

	/**
	 * Handles the retrieval of movies based on their popularity (rating).
	 * <p>
	 * This method processes a GET request to fetch movies with a specific rating.
	 * The rating is provided in the request body as a JSON object. The method
	 * retrieves all movies with the exact specified rating, ensuring that only
	 * those with the precise rating value are returned. If no movies match the
	 * rating, it returns a 404 status. The results are returned as a JSON array.
	 * </p>
	 *
	 * @param r the HttpExchange object representing the request and response.
	 * @throws IOException   if an I/O error occurs.
	 * @throws JSONException if an error occurs while parsing the JSON request body.
	 */
	public void handleMoviesByPopularity(HttpExchange r) throws IOException, JSONException {
		
		int statusCode = 0;
		double rating = 0.0;
		
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			rating = Double.parseDouble(params.get("rating"));
		}
		else
		{
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			rating = deserialized.optDouble("rating", -1); // Default to -1 if rating isn't provided
		}
		
		
		

		System.out.println("INPUT: rating: " + rating);

		try (Session session = DBConnect.driver.session()) {
			if (rating < 0 || rating > 10) { // Rating is between 1-10. 10 being the better rating.
				statusCode = 400; // BAD REQUEST if rating is invalid or not provided
			} else {
				try (Transaction tx = session.beginTransaction()) {
					// Adjust the query to match only the exact rating
					StatementResult result = tx.run(
							"MATCH (m:Movie) WHERE m.rating = $rating RETURN m.movieId AS movieId, m.name AS name, m.rating AS rating ORDER BY m.name",
							parameters("rating", rating));

					List<JSONObject> moviesList = new ArrayList<>();
					while (result.hasNext()) {
						Record record = result.next();
						JSONObject movie = new JSONObject();
						movie.put("movieId", record.get("movieId").asString());
						movie.put("name", record.get("name").asString());
						movie.put("rating", record.get("rating").asDouble());
						moviesList.add(movie);
					}

					if (moviesList.isEmpty()) {
						statusCode = 404; // NOT FOUND if no movies match the exact rating
					} else {
						JSONArray response = new JSONArray(moviesList);
						String jsonResponse = response.toString();
						r.sendResponseHeaders(200, jsonResponse.length());
						r.getResponseBody().write(jsonResponse.getBytes());
						r.getResponseBody().close();
						return;
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

	/**
	 * Handles the retrieval of movies based on their box office performance
	 * (profit).
	 * <p>
	 * This method processes a GET request to fetch movies with a specific profit.
	 * The profit is provided in the request body as a formatted string (e.g., "1M"
	 * for million). The method converts this formatted string into a numeric value
	 * and retrieves all movies whose profits fall within a specified margin (e.g.,
	 * 10%) around the input value. The movies are sorted by how close their profit
	 * is to the requested profit, from closest to least closest. The results are
	 * returned as a JSON array.
	 * </p>
	 *
	 * @param r the HttpExchange object representing the request and response.
	 * @throws IOException   if an I/O error occurs.
	 * @throws JSONException if an error occurs while parsing the JSON request body.
	 */
	public void handleBoxOfficePerformance(HttpExchange r) throws IOException, JSONException {
		
		int statusCode = 0;
		String profitString = "";
		
		if(r.getRequestURI().getRawQuery() != null)
		{
			Map<String, String> params = Utils.queryToMap(r.getRequestURI().getRawQuery()); 
			profitString = params.get("profit");
		}
		else
		{
			String body = DBConnect.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			profitString = deserialized.optString("profit", ""); // Expect a string like "1M" or "500K"
		}
		
		

		// Convert the formatted string into a numeric value
		double profit = parseFormattedProfit(profitString);

		System.out.println("INPUT: profit (numeric): " + profit);

		try (Session session = DBConnect.driver.session()) {
			if (profit < 0) {
				statusCode = 400; // BAD REQUEST if profit is invalid or not provided
			} else {
				try (Transaction tx = session.beginTransaction()) {
					// Define a margin (e.g., 80% of the input profit)
					double margin = profit * 0.8;

					// Query to find movies within the margin, ordered by proximity to the input
					// profit
					StatementResult result = tx.run(
							"MATCH (m:Movie) " + "WHERE m.profit >= $minProfit AND m.profit <= $maxProfit "
									+ "RETURN m.movieId AS movieId, m.name AS name, m.profit AS profit "
									+ "ORDER BY abs(m.profit - $profit)",
							parameters("minProfit", profit - margin, "maxProfit", profit + margin, "profit", profit));

					List<JSONObject> moviesList = new ArrayList<>();
					while (result.hasNext()) {
						Record record = result.next();
						JSONObject movie = new JSONObject();
						movie.put("movieId", record.get("movieId").asString());
						movie.put("name", record.get("name").asString());
						movie.put("profit", formatProfit(record.get("profit").asDouble())); // Format the output
						moviesList.add(movie);
					}

					if (moviesList.isEmpty()) {
						statusCode = 404; // NOT FOUND if no movies match within the margin
					} else {
						JSONArray response = new JSONArray(moviesList);
						String jsonResponse = response.toString();
						r.sendResponseHeaders(200, jsonResponse.length());
						r.getResponseBody().write(jsonResponse.getBytes());
						r.getResponseBody().close();
						return;
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

	/**
	 * Converts a formatted profit string (e.g., "1M" for million) into a numeric
	 * value.
	 * <p>
	 * This helper method interprets strings with "M", "B", or "K" suffixes to
	 * represent million, billion, and thousand, respectively. It returns the
	 * corresponding numeric value.
	 * </p>
	 *
	 * @param profitString the profit string to be converted (e.g., "2.5B").
	 * @return the numeric value of the profit (e.g., 2,500,000,000).
	 */
	private double parseFormattedProfit(String profitString) { // Helper function to parse formatted profit strings
		if (profitString.endsWith("M")) {
			return Double.parseDouble(profitString.replace("M", "")) * 1_000_000;
		} else if (profitString.endsWith("B")) {
			return Double.parseDouble(profitString.replace("B", "")) * 1_000_000_000;
		} else if (profitString.endsWith("K")) {
			return Double.parseDouble(profitString.replace("K", "")) * 1_000;
		} else {
			return Double.parseDouble(profitString); // Assume it's a plain number
		}
	}

	/**
	 * Formats a numeric profit value into a human-readable string with "M", "B", or
	 * "K" suffixes.
	 * <p>
	 * This helper method converts large numeric values into a more readable format,
	 * using suffixes to denote million, billion, and thousand. This is used when
	 * returning results to the client.
	 * </p>
	 *
	 * @param profit the numeric profit value to be formatted.
	 * @return the formatted profit string (e.g., "2.5B").
	 */
	private String formatProfit(double profit) {// Helper function to format profit numbers as strings with M/B/K
												// suffixes
		if (profit >= 1_000_000_000) {
			return String.format("%.2fB", profit / 1_000_000_000);
		} else if (profit >= 1_000_000) {
			return String.format("%.2fM", profit / 1_000_000);
		} else if (profit >= 1_000) {
			return String.format("%.2fK", profit / 1_000);
		} else {
			return String.format("%.2f", profit); // Just show the number if it's less than 1,000
		}
		
		
	}
	
	
}