import java.io.*;
import java.util.*;

class Connection {
  int departureStation, arrival_station;
  int departureTimestamp, arrivalTimestamp;

  // Connection constructor
  Connection(String line) {
    line.trim();
    String[] tokens = line.split(" ");

    departureStation = Integer.parseInt(tokens[0]);
    arrival_station = Integer.parseInt(tokens[1]);
    departureTimestamp = Integer.parseInt(tokens[2]);
    arrivalTimestamp = Integer.parseInt(tokens[3]);
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
        connections.add(new Connection(line));
        line = in.readLine();
      }
    } catch (Exception e) {
      System.out.println("Something went wrong while reading the data: " + e.getMessage());
    }
  }
};

class PathSchedule {
  int departureTimestamp, arrivalTimestamp, duration;
  public PathSchedule(int departureTimestamp, int arrivalTimestamp) {
    this.departureTimestamp = departureTimestamp;
    this.arrivalTimestamp = arrivalTimestamp;
    this.duration = arrivalTimestamp - departureTimestamp;
  }
}

class ParetoOptima {
  protected List<PathSchedule> paretoOptima;

  public ParetoOptima() {
    this.paretoOptima = new ArrayList<PathSchedule>();
  }

  public void addCandidate(PathSchedule pathSchedule) {
    // If there is already a pareto optima in the profile
    System.out.println("add profile");
    if (!this.paretoOptima.isEmpty()) {
      int lastEntryIndex = this.paretoOptima.size() - 1;
      PathSchedule lastEntry = this.paretoOptima.get(lastEntryIndex);
      // Check whether candidate is dominated by last entry
      if (!(lastEntry.arrivalTimestamp == pathSchedule.arrivalTimestamp)) {
        // Append candidate to the list
        this.paretoOptima.add(pathSchedule);
      }
    } else {
      this.paretoOptima.add(pathSchedule);
    }
  }
  
  public int getDepartureTimeForQuickest() {
    int departureTime = Integer.MAX_VALUE;
    int result = 0;
    for(PathSchedule pathSchedule : this.paretoOptima) {
      if(pathSchedule.duration < departureTime) {
        departureTime = pathSchedule.duration;
        result = pathSchedule.departureTimestamp;
      }
    }
    return result;
  }

  /**
   * @override
   */
  public String toString() {
    String result = "";
    for (PathSchedule optima : this.paretoOptima) {
      result += "departure time : " + optima.departureTimestamp + "; arrival time : " + optima.arrivalTimestamp + "\n";
    }
    return result;
  }
}

/**
 *
 */
public class CSA {
  public static final int MAX_STATIONS = 100000;

  Timetable timetable;
  Connection inConnection[];
  int earliestArrival[];
  static boolean interactive;
  int departureStation;
  int arrivalStation;
  // station_id => profile
  Map<Integer, ParetoOptima> profiles;

  CSA(BufferedReader in) {
    timetable = new Timetable(in);
  }

  /**
   * 
   * @param arrivalStation
   */
  void main_loop(int arrivalStation) {
    int earliest = Integer.MAX_VALUE;
    for (Connection connection : timetable.connections) {
      if (connection.departureTimestamp >= earliestArrival[connection.departureStation]
          && connection.arrivalTimestamp < earliestArrival[connection.arrival_station]) {
        earliestArrival[connection.arrival_station] = connection.arrivalTimestamp;
        inConnection[connection.arrival_station] = connection;

        if (connection.arrival_station == arrivalStation) {
          earliest = Math.min(earliest, connection.arrivalTimestamp);
        }
      } else if (connection.arrivalTimestamp > earliest) {
        return;
      }
    }
  }

  /**
   * 
   * @param arrivalStation
   */
  void print_result(int arrivalStation) {
    if (inConnection[arrivalStation] == null) {
      System.out.println("NO_SOLUTION");
    } else {
      List<Connection> route = new ArrayList<Connection>();
      // We have to rebuild the route from the arrival station
      Connection lastConnection = inConnection[arrivalStation];
      while (lastConnection != null) {
        route.add(lastConnection);
        lastConnection = inConnection[lastConnection.departureStation];
      }

      // And now print it out in the right direction
      Collections.reverse(route);
      for (Connection connection : route) {
        System.out.println(connection.departureStation + " " + connection.arrival_station + " "
            + connection.departureTimestamp + " " + connection.arrivalTimestamp);
      }
    }
    System.out.println("");
    System.out.flush();
  }

  /**
   * 
   * @param departure_station
   * @param arrival_station
   * @param departure_time
   */
  void compute(int departure_station, int arrival_station, int departure_time) {
    inConnection = new Connection[MAX_STATIONS];
    earliestArrival = new int[MAX_STATIONS];
    for (int i = 0; i < MAX_STATIONS; ++i) {
      inConnection[i] = null;
      earliestArrival[i] = Integer.MAX_VALUE;
    }
    earliestArrival[departure_station] = departure_time;

    if (departure_station <= MAX_STATIONS && arrival_station <= MAX_STATIONS) {
      main_loop(arrival_station);
    }
    print_result(arrival_station);
  }

  /**
   * 
   * @param departureStation
   * @param arrivalStation
   * @param departureTime
   */
  void computeProfiles(int departureStation, int arrivalStation, int departureTime) {
    inConnection = new Connection[MAX_STATIONS];
    this.profiles = new HashMap<Integer, ParetoOptima>();
    this.arrivalStation = arrivalStation;
    this.departureStation = departureStation;
    
    // Generate profiles
    for (int connectionIndex = this.timetable.connections.size() - 1; connectionIndex >= 0; connectionIndex--) {
      // TODO : check if necessary to reinitialize earliest_arrival at
      // each profile update
      int currentDepartureTimeStamp = this.timetable.connections.get(connectionIndex).departureTimestamp;
      int currentDepartureStation = this.timetable.connections.get(connectionIndex).departureStation;
      
      this.initialize(currentDepartureStation, currentDepartureTimeStamp);
      this.generateProfile(connectionIndex, arrivalStation);
      this.saveProfile(this.timetable.connections.get(connectionIndex));
    }
    if(CSA.interactive) {
      this.displayProfiles();
      this.runInteractiveShell();
    }
    else {
      int departureTimestamp = this.profiles.get(departureStation).getDepartureTimeForQuickest();
      this.compute(departureStation, arrivalStation, departureTimestamp);
    }
  }

  /**
   * 
   */
  private void runInteractiveShell() {
    System.out.println("Type the number of the desired path or type 'quickest':");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = in.readLine();
      Integer realDepartureTime = this.getDepartureTime(line);
      if(!(realDepartureTime == null)) {
        this.compute(departureStation, arrivalStation, realDepartureTime);
      }
      else {
        boolean validEntry = false;
        while(!validEntry) {
          System.out.println("Invalid entry + add advice");
          line = in.readLine();
          if(!(realDepartureTime == null)) {
            this.compute(departureStation, arrivalStation, realDepartureTime);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param entry
   * @return
   */
  int getDepartureTime(String entry) {
    if(entry.equals("quickest")) {
      System.out.println("The quickest path is :");
      return this.profiles.get(departureStation).getDepartureTimeForQuickest();
    }
    else {
      int pathNumber = Integer.parseInt(entry);
      if(this.profiles.get(departureStation).paretoOptima.size() > pathNumber) {
        return this.profiles.get(departureStation).paretoOptima.get(pathNumber).departureTimestamp;
      }
    }
    return (Integer)null;
  }

  /**
   * 
   * @param departureStation
   * @param departureTimeStamp
   */
  void initialize(int departureStation, int departureTimeStamp) {
    earliestArrival = new int[MAX_STATIONS];
    inConnection = new Connection[MAX_STATIONS];
    for (int i = 0; i < MAX_STATIONS; ++i) {
      inConnection[i] = null;
      earliestArrival[i] = Integer.MAX_VALUE;
    }
    earliestArrival[departureStation] = departureTimeStamp;
  }

  /**
   * 
   * @param connectionIndex
   * @param arrivalStation
   */
  void generateProfile(int connectionIndex, int arrivalStation) {
    int earliest = Integer.MAX_VALUE;
    // update the profile with the basic main loop, with a subset of
    // connection
    for (int i = connectionIndex; i < timetable.connections.size(); i++) {
      Connection connection = this.timetable.connections.get(i);
      if (connection.departureTimestamp >= earliestArrival[connection.departureStation]
          && connection.arrivalTimestamp < earliestArrival[connection.arrival_station]) {
        earliestArrival[connection.arrival_station] = connection.arrivalTimestamp;
        inConnection[connection.arrival_station] = connection;

        if (connection.arrival_station == arrivalStation) {
          earliest = Math.min(earliest, connection.arrivalTimestamp);
        }
      } else if (connection.arrivalTimestamp > earliest) {
        return;
      }
    }
  }

  /**
   * 
   */
  void saveProfile(Connection currentConnection) {
    int connectionDepartureStation =
        currentConnection.departureStation;
    boolean profileSetUp = this.profiles.containsKey(connectionDepartureStation);
    if (inConnection[arrivalStation] != null) {
      PathSchedule candidate = new PathSchedule(currentConnection.departureTimestamp,this.earliestArrival[arrivalStation]);
      if (!profileSetUp) {
        ParetoOptima profile = new ParetoOptima();
        profile.addCandidate(candidate);
        this.profiles.put(connectionDepartureStation, profile);
      } else {
        this.profiles.get(connectionDepartureStation).addCandidate(candidate);
      }
    }
  }

  /**
   * 
   */
  protected void displayProfiles() {
    for (Map.Entry<Integer, ParetoOptima> entry : this.profiles.entrySet()) {
      Integer stationId = entry.getKey();
      ParetoOptima profile = entry.getValue();
      System.out.println("Profile for " + stationId + " : \n" + profile);
    }
  }

  /**
   * 
   */
  protected void displayTimetable() {
    for (Connection c : timetable.connections) {
      System.out.println(c.departureStation + " : " + c.departureTimestamp + "; "
          + c.arrival_station + " : " + c.arrivalTimestamp + ".");
    }
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    CSA.interactive = false;
    if (args.length > 0) {
      // default behaviour is non interactive mode
      CSA.interactive = args[0].equals("interactive");
    }
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    CSA csa = new CSA(in);

    String line;
    try {
      line = in.readLine();

      while (null != line && !line.isEmpty()) {
        String[] tokens = line.split(" ");
        csa.computeProfiles(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]),
            Integer.parseInt(tokens[2]));
        line = in.readLine();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Something went wrong while reading the parameters: " + e.getMessage());
    }
  }
}