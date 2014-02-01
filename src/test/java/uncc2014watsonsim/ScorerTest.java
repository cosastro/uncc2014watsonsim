package uncc2014watsonsim;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class ScorerTest {

	@Test
	public void testAggregate() {
		// Setup possible inputs
		Engine yahoo = new Engine("Yahoo");
		yahoo.add(new ResultSet("Alligators", 0.5, true, 1));
		Engine bing = new Engine("Bing");
		bing.add(new ResultSet("Alligators", 0.5, true, 1));
		
		Engine output_set;
		AverageScorer ml = new AverageScorer();
		
		// Make an exact copy when there is 1 result
		output_set = ml.test(new Question("Animals", "Alligators", yahoo));
		assertEquals(output_set.get(0), yahoo.get(0));
		assertEquals(output_set.get(0).getScore(), yahoo.get(0).getScore(), 0.001);
		
		// Average two results
		output_set = ml.test(new Question("Animals", "Alligators", yahoo, bing));
		assertEquals(output_set.get(0), yahoo.get(0));
		assertEquals(0.5, output_set.get(0).getScore(), 0.001);
		
		// Sort unique results
		yahoo.add(new ResultSet("Eels", 0.38, false, 2));
		bing.add(new ResultSet("Elk", 0.19, false, 2));
		output_set = ml.test(new Question("Animals", "Alligators", yahoo, bing));
		assertEquals("Alligators", output_set.get(0).getTitle());
		assertEquals("Eels", output_set.get(1).getTitle());
		assertEquals("Elk", output_set.get(2).getTitle());
	}

}
