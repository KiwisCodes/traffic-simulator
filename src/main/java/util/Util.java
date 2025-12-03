package util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.tudresden.sumo.cmd.Simulation;
import it.polito.appeal.traci.SumoTraciConnection;

public class Util {
	public static List<String> parseStringToList(String input) {
        // Remove the leading "[" and trailing "]"
        String cleaned = input.substring(1, input.length() - 1);
        
        // Split by comma followed by any optional whitespace
        String[] elements = cleaned.split(",\\s*");
        
        return new ArrayList<>(Arrays.asList(elements));
    }
	public static List<String> getRandomElementsWithReplacement(List<String> sourceList, int N) {
        List<String> resultList = new ArrayList<>();
        Random rand = new Random();
        int listSize = sourceList.size();

        if (listSize == 0) {
            return resultList; // Handle empty source list case
        }
        
        for (int i = 0; i < N; i++) {
            // Generate a random index between 0 (inclusive) and listSize (exclusive)
            int randomIndex = rand.nextInt(listSize);
            
            // Add the element at that random index to the result list
            resultList.add(sourceList.get(randomIndex));
       }
       return resultList;     
	}
	public static int getDepartTime(SumoTraciConnection conn) throws Exception {
		int offset = 10;
		// Assume conn.do_job_get returns a 'Double' object or primitive 'double'
		Object timeObject = conn.do_job_get(Simulation.getTime());
		int currentTime;
		if (timeObject instanceof Double) {
		    // 1. Convert the Double object to a primitive double
		    double timeDouble = ((Double) timeObject).doubleValue();
		    // 2. Safely round and cast the double to an integer
		    // We use Math.round() for standard rounding to the nearest whole number.
		    // This returns a 'long', which we cast to 'int'.
		    currentTime = (int) Math.round(timeDouble); 
		} else if (timeObject instanceof Integer) {
		    // Handle the case where it might already be an Integer
		    currentTime = ((Integer) timeObject).intValue();
		} else {
		    // Handle unexpected types gracefully
		    throw new IllegalArgumentException("Expected Double or Integer for time value.");
		}
		return currentTime + offset;
	}
}
