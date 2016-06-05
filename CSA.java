import java.io.*;
import java.util.*;

class Connection {
    int departure_station, arrival_station;
    int departure_timestamp, arrival_timestamp;

    // Connection constructor
    Connection(String line) {
        line.trim();
        String[] tokens = line.split(" ");

        departure_station = Integer.parseInt(tokens[0]);
        arrival_station = Integer.parseInt(tokens[1]);
        departure_timestamp = Integer.parseInt(tokens[2]);
        arrival_timestamp = Integer.parseInt(tokens[3]);
    }
};

class Timetable {
    List<Connection> connections;

    // Timetable constructor: reads all the connections from stdin
    Timetable(BufferedReader in) {
        connections = new ArrayList<Connection>();
        String line;
        try {
            line = in.readLine();

            while (!line.isEmpty()) {
                connections.add( new Connection(line) );
                line = in.readLine();
            }
        } catch( Exception e) {
            System.out.println("Something went wrong while reading the data: " + e.getMessage());
        }
    }
};

class ParetoOptima {
	protected List<List<Integer>> paretoOptima;
	
	public ParetoOptima() {
		this.paretoOptima = new ArrayList<List<Integer>>();
	}
	
	public void addCandidate(List<Integer> candidate) {
		// check whether candidate is dominated by last entry
		if (!this.paretoOptima.isEmpty()) {
//			int lastEntryIndex = this.paretoOptima.size()-1;
//			List<Integer> lastEntry = this.paretoOptima.get(lastEntryIndex);
			// check whether candidate is dominated by last entry
			// no condition because only one 
			if(true) {
				// TODO check for equality
				this.paretoOptima.add(candidate);
			}
		}
		else {
			// Append candidate to the list
			this.paretoOptima.add(candidate);
		}
	}
	/**
	 * @override
	 */
	public String toString() {
		String result = "";
		for(List<Integer> optima : this.paretoOptima) {
			result += "departure time : " + optima.get(0) + "; arrival time : " + optima.get(1) + "\n";
		}
		return result;
	}
}

public class CSA {
    public static final int MAX_STATIONS  = 100000;

    Timetable timetable;
    Connection in_connection[];
    int earliest_arrival[];
    static String criteria;
    // station_id => profile
    Map<Integer, ParetoOptima> profiles;
    
    CSA(BufferedReader in) {
        timetable = new Timetable(in);
    }

    void main_loop(int arrival_station) {
        int earliest = Integer.MAX_VALUE;
        for (Connection connection: timetable.connections) {
            if (connection.departure_timestamp >= earliest_arrival[connection.departure_station] &&
                    connection.arrival_timestamp < earliest_arrival[connection.arrival_station]) {
                earliest_arrival[connection.arrival_station] = connection.arrival_timestamp;
                in_connection[connection.arrival_station] = connection;

                if(connection.arrival_station == arrival_station) {
                    earliest = Math.min(earliest, connection.arrival_timestamp);
                }
            } else if(connection.arrival_timestamp > earliest) {
                return;
            }
        }
    }

    void print_result(int arrival_station) {
        if(in_connection[arrival_station] == null) {
            System.out.println("NO_SOLUTION");
        } else {
            List<Connection> route = new ArrayList<Connection>();
            // We have to rebuild the route from the arrival station 
            Connection last_connection = in_connection[arrival_station];
            while (last_connection != null) {
                route.add(last_connection);
                last_connection = in_connection[last_connection.departure_station];
            }

            // And now print it out in the right direction
            Collections.reverse(route);
            for (Connection connection : route) {
                System.out.println(connection.departure_station + " " + connection.arrival_station + " " +
                        connection.departure_timestamp + " " + connection.arrival_timestamp);
            }
        }
        System.out.println("");
        System.out.flush();
    }

    void compute(int departure_station, int arrival_station, int departure_time) {
        in_connection = new Connection[MAX_STATIONS];
        earliest_arrival = new int[MAX_STATIONS];
        for(int i = 0; i < MAX_STATIONS; ++i) {
            in_connection[i] = null;
            earliest_arrival[i] = Integer.MAX_VALUE;
        }
        earliest_arrival[departure_station] = departure_time;

        if (departure_station <= MAX_STATIONS && arrival_station <= MAX_STATIONS) {
            main_loop(arrival_station);
        }
        print_result(arrival_station);
    }
    
    void computeWithProfiles(int departureStation, int arrivalStation, int departureTime) {
    	in_connection = new Connection[MAX_STATIONS];
    	// Initialize profiles
    	this.profiles = new HashMap<Integer, ParetoOptima>();
    	// Generate profiles
    	for(int connectionIndex = this.timetable.connections.size() - 1; connectionIndex >= 0; connectionIndex--) {
    		// TODO : check if necessary to reinitialize earliest_arrival at each profile update
            earliest_arrival = new int[MAX_STATIONS];
            in_connection = new Connection[MAX_STATIONS];
            for(int i = 0; i < MAX_STATIONS; ++i) {
                in_connection[i] = null;
                earliest_arrival[i] = Integer.MAX_VALUE;
            }
            //System.out.println(connectionIndex);
//            earliest_arrival[this.timetable.connections.get(connectionIndex).arrival_station] = this.timetable.connections.get(connectionIndex).arrival_timestamp;
            earliest_arrival[this.timetable.connections.get(connectionIndex).departure_station] = this.timetable.connections.get(connectionIndex).departure_timestamp;
    		System.out.println("test init : " + earliest_arrival[this.timetable.connections.get(connectionIndex).departure_station]);
    		System.out.println("test init index : " + this.timetable.connections.get(connectionIndex).departure_station);
            this.updateProfile(connectionIndex, arrivalStation);
    		// retrieve the result
    		int connectionDepartureStation = this.timetable.connections.get(connectionIndex).departure_station;
    		boolean profileSetUp = this.profiles.containsKey(connectionDepartureStation);
    		System.out.println("Arrival station : " + arrivalStation);
//    		for(int i = 0; i < 4; i++) {
//    			System.out.println(in_connection[i]);
//    		}
    		if(in_connection[arrivalStation] != null) {
    			System.out.println(in_connection[arrivalStation]);
        		List<Integer> candidate = new ArrayList<Integer>();
        		System.out.println("passage");
        		// departure timestamp from connection departure
        		candidate.add(this.timetable.connections.get(connectionIndex).departure_timestamp);
        		// earliest arrival at target station
        		candidate.add(this.earliest_arrival[arrivalStation]);
        		System.out.println(candidate);
        		if(!profileSetUp) {
        			ParetoOptima profile = new ParetoOptima();
        			profile.addCandidate(candidate);
        			this.profiles.put(connectionDepartureStation, profile);
        		}
        		else {
        			this.profiles.get(connectionDepartureStation).addCandidate(candidate);
        		}	
    		}
    		// TODO
    	}
    	this.displayTimetable();
    	this.displayProfiles();
    }
    
    /**
     * 
     */
    void updateProfile(int connectionIndex, int arrivalStation) {
    	int earliest = Integer.MAX_VALUE;
    	// update the profile with the basic main loop, with a subset of connection
    	for(int i = connectionIndex; i < timetable.connections.size(); i ++) {
    		System.out.println("i : " + i);
    		Connection connection = this.timetable.connections.get(i);
//    		System.out.println(connection.departure_timestamp);
//    		System.out.println(earliest_arrival[connection.departure_station]);
    		if(!(connection.departure_timestamp >= earliest_arrival[connection.departure_station])) {
    			System.out.println("first");
    		}
    		if(!(connection.arrival_timestamp < earliest_arrival[connection.arrival_station])) {
    			System.out.println("second");
    			System.out.println("earliest_arrival[connection.arrival_station]) : " + earliest_arrival[connection.arrival_station]);
    			System.out.println("connection.arrival_timestamp : " + connection.arrival_timestamp);
    		}
            if (connection.departure_timestamp >= earliest_arrival[connection.departure_station] &&
                    connection.arrival_timestamp < earliest_arrival[connection.arrival_station]) {
            	System.out.println("should work");
                earliest_arrival[connection.arrival_station] = connection.arrival_timestamp;
                in_connection[connection.arrival_station] = connection;

                if(connection.arrival_station == arrivalStation) {
                    earliest = Math.min(earliest, connection.arrival_timestamp);
                }
            } else if(connection.arrival_timestamp > earliest) {
                return;
            }
    	}
    }
    
    protected void displayProfiles() {
    	for (Map.Entry<Integer, ParetoOptima> entry : this.profiles.entrySet()) {
    	    Integer stationId = entry.getKey();
    	    ParetoOptima profile = entry.getValue();
    	    System.out.println("Profile for " + stationId + " : \n" + profile);
    	    // ...
    	}
    }
    protected void displayTimetable() {
    	for(Connection c : timetable.connections) {
    		System.out.println(c.departure_station + " : " + c.departure_timestamp + "; " + c.arrival_station + " : " + c.arrival_timestamp + ".");
    	}
    }
    public static void main(String[] args) {
    	if(args.length > 0) {
    		// earliest arrival is the default choice
    		CSA.criteria = args[0].equals("quickest") ? "quickest" : "earliest";
    	}
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        CSA csa = new CSA(in);

        String line;
        try {
            line = in.readLine();

            while (!line.isEmpty()) {
                String[] tokens = line.split(" ");
                csa.computeWithProfiles(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                line = in.readLine();
            }
        } catch( Exception e) {
        	e.printStackTrace();
            System.out.println("Something went wrong while reading the parameters: " + e.getMessage());
        }
    }
}
