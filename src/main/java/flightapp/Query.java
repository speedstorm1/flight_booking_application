package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;
  private String currentUser;
  private ResultSet currentDirectSearchResults;
  private ResultSet currentIndirectSearchResults;
  private int currentSearchSize;
  //
  // Instance variables
  //


  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */

   /* DO I need to change this exception to sql exception? should i throw anything QUESTION */
  public void clearTables() {
    try {
      String clearReservationsTable = "DELETE FROM RESERVATIONS_ayush123";
      PreparedStatement deleteReservationsStatement = conn.prepareStatement(clearReservationsTable);
      deleteReservationsStatement.executeUpdate();
      String clearUsersTable = "DELETE FROM USERS_ayush123";
      PreparedStatement deleteUsersStatement = conn.prepareStatement(clearUsersTable);
      deleteUsersStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    // first use controlled queries to retrieve salthash password from the table
    // then verify the password with the salt and hash
    // if its correct, then if the user is already logged in, then do user already log in
    // else log in the user and change your current user
    String fetchUser = "SELECT * FROM USERS_ayush123 WHERE username = ?";
    try {
      PreparedStatement statement = conn.prepareStatement(fetchUser);
      // System.out.println("made it this far");
      statement.setString(1, username);
      ResultSet resultSet = statement.executeQuery();
      resultSet.next();
      byte[] hashedPasswordTrue = resultSet.getBytes("password");
      // System.out.println(Arrays.toString(hashedPasswordTrue));
      if (!PasswordUtils.plaintextMatchesHash(password, hashedPasswordTrue)) { // wrong login
        return "Login failed\n";
      }
      if (currentUser != null) {
        return "User already logged in\n";
      }
      currentUser = username;
      return ("Logged in as " + username + "\n");
    } catch (SQLException e) {
   //   e.printStackTrace();
      return "Login failed\n";
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */

   /*QUESTION How to hash the password */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE
    try {
      if (initAmount < 0) {
        throw new SQLException();
      }
      String createCustomer = "INSERT INTO USERS_ayush123 VALUES( ? , ? , ? );";
      PreparedStatement customerCreationStatement = conn.prepareStatement(createCustomer);
      customerCreationStatement.setString(1, username);
      customerCreationStatement.setBytes(2, PasswordUtils.hashPassword(password));
      // customerCreationStatement.setString(2, password);
      customerCreationStatement.setInt(3, initAmount);
      customerCreationStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to create user\n";
    }
    return ("Created user " + username + "\n");
    
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return, must be positive
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    StringBuffer sb = new StringBuffer();

    try {
  //  if (directFlight) {
      String directSearchStatement = "SELECT TOP ( ? ) fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
        + "FROM Flights " + "WHERE origin_city = ? AND dest_city = ? AND canceled != 1"
        + "AND day_of_month = ? "
        + "ORDER BY actual_time ASC;";
      PreparedStatement preparedDirectSearch = conn.prepareStatement(directSearchStatement);
      preparedDirectSearch.setInt(1, numberOfItineraries);
      preparedDirectSearch.setString(2, originCity);
      preparedDirectSearch.setString(3, destinationCity);
      preparedDirectSearch.setInt(4, dayOfMonth);
      ResultSet oneHopResults = preparedDirectSearch.executeQuery();
      List<Itinerary> searchResults = new ArrayList<>();

      if (directFlight) {
        while (oneHopResults.next()) {
          int result_fid = oneHopResults.getInt("fid");
          int result_dayOfMonth = oneHopResults.getInt("day_of_month");
          String result_carrierId = oneHopResults.getString("carrier_id");
          String result_flightNum = oneHopResults.getString("flight_num");
          String result_originCity = oneHopResults.getString("origin_city");
          String result_destCity = oneHopResults.getString("dest_city");
          int result_time = oneHopResults.getInt("actual_time");
          int result_capacity = oneHopResults.getInt("capacity");
          int result_price = oneHopResults.getInt("price");
          Itinerary newItinerary = new Itinerary(1);
          newItinerary.f1 = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum, 
                                       result_originCity, result_destCity, result_time, result_capacity, result_price);
          searchResults.add(newItinerary);
          // sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
          //           + result_flightNum + " Origin: " + result_originCity + " Destination: "
          //           + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
          //           + " Price: " + result_price + "\n");
        }
        this.currentIndirectSearchResults = null;
        currentSearchSize = 1;
      } else {
        String indirectSearchStatement = "SELECT TOP ( ? ) f1.day_of_month as day_of_month,f1.carrier_id as f1_carrier_id,f1.flight_num as f1_flight_num,"
          + "f2.flight_num as f2_flight_num, f1.origin_city as f1_origin_city, f2.dest_city as f2_dest_city, f2.carrier_id as f2_carrier_id,"
          + "f2.origin_city as intermediate_city, f1.actual_time as f1_actual_time, f2.actual_time as f2_actual_time,"
          + "f1.capacity as f1_capacity, f2.capacity as f2_capacity, f1.price as f1_price, f2.price as f2_price,"
          + " f1.fid as f1_fid, f2.fid as f2_fid "
          + "FROM Flights as f1, Flights as f2" + "WHERE f1.origin_city = ? AND f1.canceled != 1 AND f1.day_of_month = ? "
          + "AND f2.day_of_month = ? AND f1.dest_city = f2.origin_city "
          + "AND f2.dest_city = ? and f2.canceled != 1 "
          + "ORDER BY (f1.actual_time + f2.actual_time) ASC;";
        PreparedStatement preparedIndirectSearch = conn.prepareStatement(indirectSearchStatement);
        preparedIndirectSearch.setInt(1, numberOfItineraries);
        preparedIndirectSearch.setString(2, originCity);
        preparedIndirectSearch.setInt(3, dayOfMonth);
        preparedIndirectSearch.setString(4, destinationCity);
        ResultSet twoHopResults = preparedIndirectSearch.executeQuery();
        // check to make sure two hop results are valid
        if (twoHopResults.isAfterLast()) {
          // do direct flight commands
          // return
        } 
        oneHopResults.next();
        twoHopResults.next();
        for (int i = 0; i < numberOfItineraries; i++) {
          if (twoHopResults.isAfterLast()) {
            while (i < numberOfItineraries) {
              // loop from i -> num itineraries for single hop results and break
            }
          }
          if (oneHopResults.isAfterLast()) {
            while (i < numberOfItineraries) {
              // loop from i -> num itineraries for two hop results and break
            }
          }
          int oneHopTime = oneHopResults.getInt("actual_time");
          int twoHopTime = twoHopResults.getInt("f1_actual_time") + twoHopResults.getInt("f2_actual_time");
      //    if (oneHopTime > )
        }





        currentSearchSize = 2;
        twoHopResults.beforeFirst();
      }

      oneHopResults.beforeFirst();

      this.currentSearchSize = numberOfItineraries;




    } catch (SQLException e) {
      e.printStackTrace();
    }

    return sb.toString();
  }


  private String currentItinerary(Itinerary currentItinerary, int index) {
    String result = "";
    result += ("Itinerary " + index + ": " + currentItinerary.numFlights + " flight(s), ");
    result += (currentItinerary.totalTime() + " minutes" + "\n");
    result += currentItinerary.f1.toString();
    result += "\n";
    if (currentItinerary.numFlights == 2) {
      result += currentItinerary.f2.toString();
      result += "\n";
    }
    return result;
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    return "Booking failed\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  public class Itinerary implements Comparable<Itinerary> {
    public int numFlights;
    public Flight f1;
    public Flight f2;

    public Itinerary(int numFlights) {
      this.numFlights = numFlights;
    }

    public int totalTime() {
      if (this.numFlights == 1) {
        return f1.time; 
      } else {
        return f1.time + f2.time;
      }
    }

    public int compareTo(Itinerary other) {
      int thisTime;
      int otherTime;
      boolean thisDirect;
      boolean otherDirect;

      if (this.numFlights == 1) {
        thisTime = f1.time; 
        thisDirect = true;
      } else {
        thisTime = f1.time + f2.time;
        thisDirect = false;
      }
      if (other.numFlights == 1) {
        otherTime = other.f1.time; 
        otherDirect = true;
      } else {
        otherTime = other.f1.time + other.f2.time;
        otherDirect = false;
      }
      if (thisTime > otherTime) {
        return 1;
      }
      if (thisTime < otherTime) {
        return -1;
      }
      if (thisDirect && !otherDirect) {
        return 1;
      }
      if (!thisDirect && otherDirect) {
        return -1;
      }
      return 1;
    }
  }
}

