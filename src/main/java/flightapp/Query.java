package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.sql.Types;

import javax.naming.spi.DirStateFactory.Result;

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
  private List<Itinerary> searchResults;
  //private ResultSet currentDirectSearchResults;
  //private ResultSet currentIndirectSearchResults;
  //private int currentSearchSize;
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

   /* */
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
    // first use controlled queries to retrieve salthash password from the table
    // then verify the password with the salt and hash
    // if its correct, then if the user is already logged in, then do user already log in
    // else log in the user and change your current user
    String fetchUser = "SELECT * FROM USERS_ayush123 WHERE username = ?";
    try {
      PreparedStatement statement = conn.prepareStatement(fetchUser);
      // System.out.println("made it this far");
      username = username.toLowerCase();
      statement.setString(1, username);
      ResultSet resultSet = statement.executeQuery();
      resultSet.next();
      byte[] hashedPasswordTrue = resultSet.getBytes("password");
      resultSet.close();
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

  public String transaction_createCustomer(String username, String password, int initAmount) {
    try {
      if (initAmount < 0) {
        throw new SQLException();
      }
      // check to see if user exists
      username = username.toLowerCase();
      String checkUserExists = "SELECT COUNT(*) as count FROM USERS_ayush123 WHERE EXISTS " + "(SELECT * FROM USERS_ayush123 WHERE username = ?);";
      PreparedStatement userExistingStatement = conn.prepareStatement(checkUserExists);
      userExistingStatement.setString(1, username);
      ResultSet userExistingSet = userExistingStatement.executeQuery();
      userExistingSet.next();
      int numUsers = userExistingSet.getInt("count");
      userExistingSet.close();
      if (numUsers != 0) { // user already in the database
        throw new SQLException();
      }
      String createCustomer = "INSERT INTO USERS_ayush123 VALUES( ? , ? , ? );";
      PreparedStatement customerCreationStatement = conn.prepareStatement(createCustomer);
      customerCreationStatement.setString(1, username);
      customerCreationStatement.setBytes(2, PasswordUtils.hashPassword(password));
      customerCreationStatement.setInt(3, initAmount);
      customerCreationStatement.executeUpdate();
    } catch (SQLException e) {
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

    String result = "";
    if (numberOfItineraries == 0) {
      return "Failed to search\n";
    }
    try {
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
      }
      oneHopResults.close();
      if (!directFlight) {
        String indirectSearchStatement = "SELECT TOP ( ? ) f1.day_of_month as day_of_month,f1.carrier_id as f1_carrier_id,f1.flight_num as f1_flight_num,"
          + "f2.flight_num as f2_flight_num, f1.origin_city as f1_origin_city, f2.dest_city as f2_dest_city, f2.carrier_id as f2_carrier_id,"
          + "f2.origin_city as intermediate_city, f1.actual_time as f1_actual_time, f2.actual_time as f2_actual_time,"
          + "f1.capacity as f1_capacity, f2.capacity as f2_capacity, f1.price as f1_price, f2.price as f2_price,"
          + " f1.fid as f1_fid, f2.fid as f2_fid "
          + "FROM Flights as f1, Flights as f2 " + "WHERE f1.origin_city = ? AND f1.canceled != 1 AND f1.day_of_month = ? "
          + "AND f2.day_of_month = f1.day_of_month AND f1.dest_city = f2.origin_city "
          + "AND f2.dest_city = ? and f2.canceled != 1 "
          + "ORDER BY (f1.actual_time + f2.actual_time) ASC;";
        PreparedStatement preparedIndirectSearch = conn.prepareStatement(indirectSearchStatement);
        preparedIndirectSearch.setInt(1, numberOfItineraries);
        preparedIndirectSearch.setString(2, originCity);
        preparedIndirectSearch.setInt(3, dayOfMonth);
        preparedIndirectSearch.setString(4, destinationCity);
        ResultSet twoHopResults = preparedIndirectSearch.executeQuery();
        while (twoHopResults.next()) {
          int f1_fid = twoHopResults.getInt("f1_fid");
          int f2_fid = twoHopResults.getInt("f2_fid");
          int result_dayOfMonth = twoHopResults.getInt("day_of_month");
          String f1_carrierId = twoHopResults.getString("f1_carrier_id");
          String f2_carrierId = twoHopResults.getString("f2_carrier_id");
          String f1_flightNum = twoHopResults.getString("f1_flight_num");
          String f2_flightNum = twoHopResults.getString("f2_flight_num");
          String f1_originCity = twoHopResults.getString("f1_origin_city");
          String f2_destCity = twoHopResults.getString("f2_dest_city");
          String intermediate_city = twoHopResults.getString("intermediate_city");
          int f1_actual_time = twoHopResults.getInt("f1_actual_time");
          int f2_actual_time = twoHopResults.getInt("f2_actual_time");
          int f1_capacity = twoHopResults.getInt("f1_capacity");
          int f2_capacity = twoHopResults.getInt("f2_capacity");
          int f1_price = twoHopResults.getInt("f1_price");
          int f2_price = twoHopResults.getInt("f2_price");
          Itinerary newItinerary = new Itinerary(2);
          newItinerary.f1 = new Flight(f1_fid, result_dayOfMonth, f1_carrierId, f1_flightNum, 
                                       f1_originCity, intermediate_city, f1_actual_time, f1_capacity, f1_price);
          newItinerary.f2 = new Flight(f2_fid, result_dayOfMonth, f2_carrierId, f2_flightNum, 
                                       intermediate_city, f2_destCity, f2_actual_time, f2_capacity, f2_price);
          searchResults.add(newItinerary);
        }
        twoHopResults.close();
      }
      int oldSize = searchResults.size();
      if (oldSize == 0) {
        return "No flights match your selection\n";
      }
      for (int index = numberOfItineraries; index < oldSize; index++) {
        searchResults.remove(searchResults.size() - 1);
      }
      Collections.sort(searchResults);
      this.searchResults = searchResults;
      for (int i = 0; i < searchResults.size(); i++) {
        result += currentItinerary(searchResults.get(i), i);
      }
    } catch (Exception e) {
      return "Failed to search\n";
    }
    return result;
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
    if (currentUser == null) {
      return "Cannot book reservations, not logged in\n";
    }
    if (searchResults == null || itineraryId >= searchResults.size()) {
      return "No such itinerary " + itineraryId + "\n";
    }
    try {
      Itinerary desiredItinerary = searchResults.get(itineraryId);
      String checkFlight1Reservations = "SELECT COUNT(R.id) as count FROM RESERVATIONS_ayush123 as R WHERE R.fid1 = ? ;";
      PreparedStatement flight1ReservationStatement = conn.prepareStatement(checkFlight1Reservations);
      flight1ReservationStatement.setInt(1, desiredItinerary.f1.fid);
      ResultSet flight1ReservationSet = flight1ReservationStatement.executeQuery();
      flight1ReservationSet.next();
      int numReservationsf1 = flight1ReservationSet.getInt("count");
      flight1ReservationSet.close();
      if (desiredItinerary.f1.capacity <= numReservationsf1) {
        return "Booking failed\n";
      }

      if (desiredItinerary.numFlights == 2) {
        String checkFlight2Reservations = "SELECT COUNT(R.id) as count FROM RESERVATIONS_ayush123 as R WHERE R.fid2 = ? ;";
        PreparedStatement flight2ReservationStatement = conn.prepareStatement(checkFlight2Reservations);
        flight2ReservationStatement.setInt(1, desiredItinerary.f2.fid);
        ResultSet flight2ReservationSet = flight2ReservationStatement.executeQuery();
        flight2ReservationSet.next();
        int numReservationsf2 = flight2ReservationSet.getInt("count");
        flight2ReservationSet.close();
        if (desiredItinerary.f2.capacity <= numReservationsf2) {
          return "Booking failed\n";
        }
      }

      int flightDay = desiredItinerary.f1.dayOfMonth;
      String checkReservationDay = "SELECT COUNT(R.id) as count FROM RESERVATIONS_ayush123 as R, Flights as F WHERE " +
                                    "R.fid1 = F.fid AND F.day_of_month = ? and R.username = ? ;";
      PreparedStatement reservationCheckStatement = conn.prepareStatement(checkReservationDay);
      reservationCheckStatement.setInt(1, flightDay);
      reservationCheckStatement.setString(2, currentUser);
      ResultSet numReservationsOnDaySet = reservationCheckStatement.executeQuery();
      numReservationsOnDaySet.next();
      int numReservations = numReservationsOnDaySet.getInt("count");
      numReservationsOnDaySet.close();
      if (numReservations != 0) {
        return "You cannot book two flights in the same day\n";
      }

      String checkNumReservations = "SELECT COUNT(R.id) as count FROM RESERVATIONS_ayush123 as R;";
      PreparedStatement numReservationsStatement = conn.prepareStatement(checkNumReservations);
      ResultSet numUserReservationsSet = numReservationsStatement.executeQuery();
      numUserReservationsSet.next();
      int reservationIndex = numUserReservationsSet.getInt("count") + 1;
      numUserReservationsSet.close();
      String createReservation = "INSERT INTO RESERVATIONS_ayush123 VALUES( ? , ? , ? , ? , ? );";
      PreparedStatement createReservationStatement = conn.prepareStatement(createReservation);
      createReservationStatement.setInt(1, reservationIndex);
      createReservationStatement.setString(2, currentUser);
      createReservationStatement.setInt(3, 0);
      createReservationStatement.setInt(4, desiredItinerary.f1.fid);
      if (desiredItinerary.numFlights == 1) {
        createReservationStatement.setNull(5, Types.INTEGER);
      } else {
        createReservationStatement.setInt(5, desiredItinerary.f2.fid);
      }
      createReservationStatement.executeUpdate();
      return "Booked flight(s), reservation ID: " + reservationIndex + "\n";
    } catch (Exception e) {
      return "Booking failed\n";
    }
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
    if (currentUser == null) {
      return "Cannot pay, not logged in\n";
    }
    try {
      String verifyReservation = "SELECT COUNT(R.id) as count FROM RESERVATIONS_ayush123 as R WHERE R.id = ? AND R.username = ? AND R.ispaid = ?;";
      PreparedStatement verifyReservationStatement = conn.prepareStatement(verifyReservation);
      verifyReservationStatement.setInt(1, reservationId);
      verifyReservationStatement.setString(2, currentUser);
      verifyReservationStatement.setInt(3, 0);
      ResultSet reservationVerificationSet = verifyReservationStatement.executeQuery();
      reservationVerificationSet.next();
      int reservationExists = reservationVerificationSet.getInt("count");
      reservationVerificationSet.close();
      if (reservationExists == 0) {
        return "Cannot find unpaid reservation " + reservationId + " under user: " + currentUser + "\n";
      }
      // get flights
      String getReservationFlights = "SELECT R.fid1 as fid1, R.fid2 as fid2 FROM RESERVATIONS_ayush123 as R WHERE R.id = ? ;";
      PreparedStatement getFlightsStatement = conn.prepareStatement(getReservationFlights);
      getFlightsStatement.setInt(1, reservationId);
      ResultSet flightInfoSet = getFlightsStatement.executeQuery();
      flightInfoSet.next();
      int fid1 = flightInfoSet.getInt("fid1");
      int fid2 = flightInfoSet.getInt("fid2"); // 0 if direct itinerary
      // get flight costs
      flightInfoSet.close();
      String getFlight1Cost = "SELECT F.price as price from Flights as F where fid = ? ;";
      PreparedStatement getFlight1CostStatement = conn.prepareStatement(getFlight1Cost);
      getFlight1CostStatement.setInt(1, fid1);
      ResultSet flight1CostSet = getFlight1CostStatement.executeQuery();
      flight1CostSet.next();
      int itineraryCost = flight1CostSet.getInt("price");
      flight1CostSet.close();
      if (fid2 != 0) { // 2 flight itinerary
        String getFlight2Cost = "SELECT F.price as price from Flights as F where fid = ? ;";
        PreparedStatement getFlight2CostStatement = conn.prepareStatement(getFlight2Cost);
        getFlight2CostStatement.setInt(1, fid2);
        ResultSet flight2CostSet = getFlight2CostStatement.executeQuery();
        flight2CostSet.next();
        itineraryCost += flight2CostSet.getInt("price");
        flight2CostSet.close();
      }
      // get user balance
      String getUserBalance = "SELECT U.balance as balance FROM USERS_ayush123 as U WHERE U.username = ? ;";
      PreparedStatement getUserBalanceStatement = conn.prepareStatement(getUserBalance);
      getUserBalanceStatement.setString(1, currentUser);
      ResultSet userBalanceSet = getUserBalanceStatement.executeQuery();
      userBalanceSet.next();
      int userBalance = userBalanceSet.getInt("balance");
      userBalanceSet.close();
      if (userBalance < itineraryCost) {
        return "User has only " + userBalance + " in account but itinerary costs " + itineraryCost + "\n";
      }

      // update user balance and pay for reservation
      userBalance -= itineraryCost;
      String updateUserBalance = "UPDATE USERS_ayush123 SET balance = ? WHERE username = ? ;";
      PreparedStatement updateUserBalanceStatement =  conn.prepareStatement(updateUserBalance);
      updateUserBalanceStatement.setInt(1, userBalance);
      updateUserBalanceStatement.setString(2, currentUser);
      updateUserBalanceStatement.executeUpdate();

      String updateReservationPayment = "UPDATE RESERVATIONS_ayush123 SET ispaid = ? WHERE id = ? ;";
      PreparedStatement updateReservationPaymentStatement = conn.prepareStatement(updateReservationPayment);
      updateReservationPaymentStatement.setInt(1, 1);
      updateReservationPaymentStatement.setInt(2, reservationId);
      updateReservationPaymentStatement.executeUpdate();

      return "Paid reservation: " + reservationId + " remaining balance: " + userBalance + "\n";
    } catch (SQLException e) {
      return "Failed to pay for reservation " + reservationId + "\n";
    }
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
    if (currentUser == null) {
      return "Cannot view reservations, not logged in\n";
    }
    try {
      String getNumReservations = "SELECT count(id) as count FROM RESERVATIONS_ayush123 WHERE username = ? ;";
      PreparedStatement getNumReservationsStatement = conn.prepareStatement(getNumReservations);
      getNumReservationsStatement.setString(1, currentUser);
      ResultSet numReservationsSet = getNumReservationsStatement.executeQuery();
      numReservationsSet.next();
      int numReservations = numReservationsSet.getInt("count");
      numReservationsSet.close();
      if (numReservations == 0) {
        return "No reservations found\n";
      }

      String getReservations = "SELECT id, ispaid, fid1, fid2 FROM RESERVATIONS_ayush123 WHERE username = ? ORDER BY id;";
      PreparedStatement getReservationsStatement = conn.prepareStatement(getReservations);
      getReservationsStatement.setString(1, currentUser);
      ResultSet userReservations = getReservationsStatement.executeQuery();
      String reservations = "";
      while (userReservations.next()) {
        int fid1 = userReservations.getInt("fid1");
        int fid2 = userReservations.getInt("fid2");
        int reservationId = userReservations.getInt("id");
        int ispaid = userReservations.getInt("ispaid");
        Flight flight1 = getFlight(fid1);
        Flight flight2 = getFlight(fid2);
        reservations += printReservation(flight1, flight2, reservationId, ispaid);
      }
      userReservations.close();
      return reservations;
    } catch(Exception e) {
      return "Failed to retrieve reservations\n";
    }
  }

  private Flight getFlight(int fid) throws SQLException {
    if (fid == 0) {
      return null;
    }
    String getFlightValues = "SELECT day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price " +
                              "FROM Flights WHERE fid = ? ;";
    PreparedStatement getFlightStatement = conn.prepareStatement(getFlightValues);
    getFlightStatement.setInt(1, fid);
    ResultSet flightDetails = getFlightStatement.executeQuery();
    flightDetails.next();
    int flight_dayOfMonth = flightDetails.getInt("day_of_month");
    String flight_carrierId = flightDetails.getString("carrier_id");
    String flight_flightNum = flightDetails.getString("flight_num");
    String flight_originCity = flightDetails.getString("origin_city");
    String flight_destCity = flightDetails.getString("dest_city");
    int flight_time = flightDetails.getInt("actual_time");
    int flight_capacity = flightDetails.getInt("capacity");
    int flight_price = flightDetails.getInt("price");
    Flight currentFlight = new Flight(fid, flight_dayOfMonth, flight_carrierId, flight_flightNum, 
                                      flight_originCity, flight_destCity, flight_time, flight_capacity, flight_price);
    return currentFlight;
  }

  private String printReservation(Flight flight1, Flight flight2, int reservationId, int ispaid) {
    String result = "";
    String paidBoolean = ispaid == 1 ? "true" : "false";
    result += ("Reservation " + reservationId + " paid: " + paidBoolean + ":\n");
    result += flight1.toString();
    result += "\n";
    if (flight2 != null) {
      result += flight2.toString();
      result += "\n";
    }
    return result;
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
      if (thisDirect && otherDirect) {
        return f1.fid - other.f1.fid;
      }
      if (!thisDirect && !otherDirect) {
        if (f1.fid - other.f1.fid != 0) {
          return f1.fid - other.f1.fid;
        }
        return f2.fid - other.f2.fid;  
      }
      return 1;
    }
  }
}

