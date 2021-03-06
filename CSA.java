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

class PathScheduleComparatorArrival implements Comparator<PathSchedule> {
  @Override
  public int compare(PathSchedule p1, PathSchedule p2) {
    if (p1.arrivalTimestamp < p2.arrivalTimestamp)
      return 1;
    else if (p1.arrivalTimestamp > p2.arrivalTimestamp)
      return -1;
    // Prevents two different PathSchedule to be considered equals by TreeSet
    else if (p1.departureTimestamp > p2.departureTimestamp)
      return 1;
    else
      return 0;
  }
}

class PathSchedule{
  int departureTimestamp, arrivalTimestamp, duration;

  public PathSchedule(int departureTimestamp, int arrivalTimestamp) {
    this.departureTimestamp = departureTimestamp;
    this.arrivalTimestamp = arrivalTimestamp;
    this.duration = arrivalTimestamp - departureTimestamp;
  }
}


class ParetoOptima {
  protected TreeSet<PathSchedule> paretoOptima;

  public ParetoOptima() {
    this.paretoOptima = new TreeSet<PathSchedule>(new PathScheduleComparatorArrival());
  }

  /**
   * 
   * @param candidate
   */
  public void addCandidate(PathSchedule candidate) {
    // If there is already a pareto optima in the profile
    if (!this.paretoOptima.isEmpty()) {      
      PathSchedule last = this.paretoOptima.last();
      if (last.arrivalTimestamp <= candidate.arrivalTimestamp) {
        // dominated
        return;
      }
      else if(last.departureTimestamp == candidate.departureTimestamp) {
        this.paretoOptima.remove(last);
      }
      this.paretoOptima.add(candidate);
    }
    else {
      this.paretoOptima.add(candidate);
    }
  }

  /**
   * 
   * @param candidate
   * @return
   */
  boolean isDominatedByLastEntry(PathSchedule candidate) {
    return this.paretoOptima.last().arrivalTimestamp <= candidate.arrivalTimestamp;
  }
  /**
   * 
   * @param departureTime
   * @return
   */
  public int getEarliestArrival(int departureTime) {
    // Iterator ordered by increasing arrival time
    Iterator<PathSchedule> it =  this.paretoOptima.descendingIterator();
    int earliestArrival = Integer.MAX_VALUE;
    PathSchedule p;
    while (it.hasNext()) {
      p = it.next();
      if(p.departureTimestamp >= departureTime) {
        earliestArrival = p.arrivalTimestamp;
        break;
      }
    }
    return earliestArrival;
  }

  public int getDepartureTimeForQuickest() {
    if(!this.paretoOptima.isEmpty()) {
      return this.paretoOptima.last().departureTimestamp;
    }
    return Integer.MAX_VALUE;
  }

  /**
   * @override
   */
  public String toString() {
    String result = "";
    int position = 0;
    for (PathSchedule optima : this.paretoOptima) {
      result +=
          "* " + position + " : Departure time : " + optima.departureTimestamp
              + "; Arrival time : " + optima.arrivalTimestamp + "\n";
      position++;
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
  void mainLoop(int arrivalStation) {
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
  void printResult(int arrivalStation) {
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
      mainLoop(arrival_station);
    }
    printResult(arrival_station);
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

    // Initialize profile for departure station
    this.profiles.put(departureStation, new ParetoOptima());
    // Generate profiles
    for (int connectionIndex = this.timetable.connections.size() - 1; connectionIndex >= 0; connectionIndex--) {
      Connection connection = this.timetable.connections.get(connectionIndex);
      if(connection.departureTimestamp < departureTime) {
        break;
      }
      boolean profileSetUp = this.profiles.containsKey(connection.departureStation);
      if (!profileSetUp) {
        ParetoOptima profile = new ParetoOptima();
        this.profiles.put(connection.departureStation, profile);
      }
      // If the connection is directly linked to arrival station
      if (this.timetable.connections.get(connectionIndex).arrival_station == this.arrivalStation) {
        PathSchedule candidate =
            new PathSchedule(connection.departureTimestamp, connection.arrivalTimestamp);
        this.profiles.get(connection.departureStation).addCandidate(candidate);
      } else {
        // If profile is set for arrival station, check if paths to final target station are available
        if(this.profiles.containsKey(connection.arrival_station)) {
          int bestArrivalTime =
              this.profiles.get(connection.arrival_station).getEarliestArrival(
                  connection.arrivalTimestamp);
          // 1-to-1 profile query pruning rule
          if (this.profiles.get(this.departureStation).getEarliestArrival(0) > 
          bestArrivalTime) {
            PathSchedule candidate = new PathSchedule(connection.departureTimestamp, bestArrivalTime);
            this.profiles.get(connection.departureStation).addCandidate(candidate);
          }
        }
      }
    }
    if (CSA.interactive) {
      this.runInteractiveShell();
    } else {
      int departureTimestamp = this.profiles.get(departureStation).getDepartureTimeForQuickest();
      this.compute(departureStation, arrivalStation, departureTimestamp);
    }
  }

  /**
   * 
   */
  private void runInteractiveShell() {
    // Transform schedule into a list to benefits from indices
    List<PathSchedule> schedule = new ArrayList<PathSchedule>(this.profiles.get(this.departureStation).paretoOptima);
    this.displayPathSchedules(schedule);
    System.out.println("Type the number of the desired path or type 'quickest':");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = in.readLine();
      Integer realDepartureTime = this.getDepartureTime(line, schedule);
      if (realDepartureTime > -1) {
        this.compute(departureStation, arrivalStation, realDepartureTime);
      } else {
        boolean validEntry = false;
        while (!validEntry) {
          System.out.println("Invalid entry, please choose a valid path or type 'quickest'");
          line = in.readLine();
          realDepartureTime = this.getDepartureTime(line, schedule);
          if (realDepartureTime > -1) {
            validEntry = true;
            this.compute(departureStation, arrivalStation, realDepartureTime);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Return -1 for invalid entry
   * 
   * @param entry
   * @return
   */
  int getDepartureTime(String entry, List<PathSchedule> schedule) {
    if (entry.equals("quickest")) {
      System.out.println("The quickest path is :");
      return this.profiles.get(departureStation).getDepartureTimeForQuickest();
    } else {
      int pathNumber;
      try {
        pathNumber = Integer.parseInt(entry);
      } catch (Exception e) {
        // return -1 if input is not a number
        return -1;
      }
      if (schedule.size() > pathNumber) {
        System.out.println("The complete path is :");
        return schedule.get(pathNumber).departureTimestamp;
      }
    }
    return -1;
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
   */
  protected void displayPathSchedules(List<PathSchedule> schedules) {
    System.out.println("Best paths available are :");
    String result = "";
    int position = 0;
    for (PathSchedule optima : schedules) {
      result +=
          "* " + position + " : Departure time : " + optima.departureTimestamp
              + "; Arrival time : " + optima.arrivalTimestamp + "\n";
      position++;
    }
    System.out.println(result);
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
        System.out.println("line : " + line);
        csa.computeProfiles(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]),
            Integer.parseInt(tokens[2]));
        if (CSA.interactive) {
          System.out
              .println("You can type another request (departure_station arrival_station departure_time), or type entry to quit");
        }
        line = in.readLine();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Something went wrong while reading the parameters: " + e.getMessage());
    }
  }
}
