package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

public class Utils {
	
	/**
	 * Gets the query result containing a map, puts it into a Java map
	 * @param query
	 * @return
	 */
	public static Map<String, String> queryToMap(String query) {
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
	
	/**
	 * This is used by the Actor.java class to find if an actor exist before adding one to the DB
	 * @param actorId
	 * @return
	 */
	public static boolean hasActorInDB(String actorId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		StatementResult results = tx
				.run("MATCH (a:Actor) WHERE a.actorId = '" + actorId + "' RETURN COUNT (a.actorId) as count");
		Record record = results.next();
		response = record.get("count").asInt();

		if (response > 0) {
			return true;
		}

		return false;
	}
	
	/**
	 * This is used by the Movie.java class to find of a movie already exist in the DB
	 * @param movieId
	 * @return
	 */
	public static boolean hasMovieInDB(String movieId) {
		try (Session session = DBConnect.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult results = tx.run(
						"MATCH (a:Movie) WHERE a.movieId = $movieId RETURN COUNT(a.movieId) as count",
						parameters("movieId", movieId));
				int count = results.next().get("count").asInt();
				tx.success();
				return count > 0;
			}
		}
	}
	
	/**
	 * This is used by the BaconPath.java file
	 * @param actorId
	 * @return
	 */
	public static boolean hasActor(String actorId) {
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

	/**
	 * This is used by Relationship.java
	 * @param movieId
	 * @param actorId
	 * @return
	 */
	public static boolean hasMovieAndActor(String movieId, String actorId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		String queryActor = "MATCH (a:Actor) WHERE a.actorId = '" + actorId + "' RETURN COUNT (a.actorId) as count";
		String queryMovie = "MATCH (m:Movie) WHERE m.movieId = '" + movieId + "' RETURN COUNT (m.movieId) as count";

		StatementResult resultsActor = tx.run(queryActor);
		StatementResult resultsMovie = tx.run(queryMovie);

		if (resultsActor.hasNext() && resultsMovie.hasNext()) {
			
			Record recordA = resultsActor.next();
			Record recordM = resultsMovie.next();
			
			if(recordA.get("count").asInt()==1 && recordM.get("count").asInt()==1)
			{
				
				System.out.println(true);
				return true;
			}
			else 
			{
				System.out.println(false);
				return false;
			}
		}

		System.out.println(false);
		return false;
	}
	
	/**
	 * This is used by Relationship.java
	 * @param movieId
	 * @param actorId
	 * @return
	 */
	public static boolean hasRelationshipAlready(String movieId, String actorId) {
		int response = 0;

		Session session = DBConnect.driver.session();
		Transaction tx = session.beginTransaction();
		String query = "MATCH (a:Actor)-[r:ACTED_IN]->(m:Movie) " + "WHERE a.actorId = '" + actorId
				+ "' AND m.movieId = '" + movieId + "' RETURN COUNT(r) as count";


		StatementResult results = tx.run(query);
		

		if (results.hasNext()) {
			
			Record recordA = results.next();
			
			
			if(recordA.get("count").asInt() > 0)
			{
				
				System.out.println(true);
				return true;
			}
			else 
			{
				System.out.println(false);
				return false;
			}
		}

		System.out.println(false);
		return false;
	}
}
