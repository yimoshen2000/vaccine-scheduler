package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");
        System.out.println("> reserve <date> <vaccine>");
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");
        System.out.println("> logout");
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }
    // method for determining if password is strong
    private static boolean strongPassword (String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}";
        Matcher matcher = Pattern.compile(regex).matcher(password);
        return matcher.matches();
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check 3: check if the password created is strong
        if (!strongPassword(password)) {
            System.out.println("Password is too weak, please follow the following guidelines when creating password!");
            System.out.println("At least 8 characters.");
            System.out.println("A mixture of both uppercase and lowercase letters.");
            System.out.println("A mixture of letters and numbers.");
            System.out.println("Inclusion of at least one special character, from “!”, “@”, “#”, “?”.");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Patient account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check 3: check if the password created is strong
        if (!strongPassword(password)) {
            System.out.println("Password is too weak, please follow the following guidelines when creating password!");
            System.out.println("At least 8 characters.");
            System.out.println("A mixture of both uppercase and lowercase letters.");
            System.out.println("A mixture of letters and numbers.");
            System.out.println("Inclusion of at least one special character, from “!”, “@”, “#”, “?”.");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Caregiver account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // check 1: check to see if user has logged in or not
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // check 3: date should be correct format
        Date time = null;
        try {
            time = Date.valueOf(tokens[1]);

        } catch (IllegalArgumentException e) {
            System.err.println("Error while entering date! The format should be YYYY-MM-DD.");
            e.printStackTrace();
        }
        // output consists of username of caregivers and vaccines available for the specific date
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String caregiverAvailability = "SELECT Username FROM Availabilities WHERE Time = ?";
        String vaccineAvailability = "SELECT Name, Doses FROM Vaccines";
        try {
            PreparedStatement availability = con.prepareStatement(caregiverAvailability);
            PreparedStatement vaccine = con.prepareStatement(vaccineAvailability);
            availability.setDate(1, time);
            ResultSet rsAvailability = availability.executeQuery();
            ResultSet rsVaccine = vaccine.executeQuery();
            System.out.println("available caregivers:");
            while (rsAvailability.next()) {
                String username = rsAvailability.getString("Username");
                System.out.print("|" + username);
            }
            System.out.println("available vaccines & doses:");
            while (rsVaccine.next()) {
                String name = rsVaccine.getString("Name");
                int doses = rsVaccine.getInt("Doses");
                System.out.println("vaccine name: " + name + " available doses: " + doses);
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    // input: desired date and vaccine name
    // output: caregiver name and appointment ID
    private static void reserve(String[] tokens) throws SQLException {
        // check 1: only patient can perform this operation
        if (currentPatient == null) {
            System.out.println("Please login as a patient first to reserve your appointment!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        // check 3: date should be correct format
        Date time = null;
        try {
            time = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error while entering date! The format should be YYYY-MM-DD.");
        }
        // check 4: if vaccine input exists
        String vaccineName = tokens[2];
        Vaccine vax;
        try {
            vax = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when generating vaccine!");
        }
        // check 5: if vaccine doses are still enough
        if (vax == null) {
            System.out.println(vaccineName + " is not available at this time. Check availability of other vaccines!");
            return;
        }
        // check 6: select random available caregiver username in Availabilities and take that availability away
        // check 7: generate new appointment id and add new appointment to the appointments table
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchCaregiver = "SELECT * FROM Availabilities WHERE Time = ? LIMIT 1";
        String getMaxID = "SELECT MAX(id) FROM Appointments";
        try {
            PreparedStatement caregiver = con.prepareStatement(searchCaregiver);
            PreparedStatement app_id = con.prepareStatement(getMaxID);
            caregiver.setDate(1, time);
            ResultSet rsCaregiver = caregiver.executeQuery();
            ResultSet rsAppID = app_id.executeQuery();
            while (rsCaregiver.next()) {
                String caregiverUsername = rsCaregiver.getString("Username");
                System.out.println("You have successfully made a reservation with " + caregiverUsername + "!");
                int id = 0;
                if (rsAppID.next()) {
                    id = rsAppID.getInt(1);
                }
                addAppointment(id, caregiverUsername, vaccineName, currentPatient.getUsername(), time);
                removeAvailability(time, caregiverUsername);
                System.out.println("Your appointment id is " + id + ".");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when reserving appointment!");
        }
    }
    //Helper method for reserve; Adds newly reserved appointment to Appointments table
    private static void addAppointment(int app_id, String caregiver_name, String vaccine_name,
                               String patient_name, Date app_time) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, app_id);
            statement.setString(2, caregiver_name);
            statement.setString(3, vaccine_name);
            statement.setString(4, patient_name);
            statement.setDate(5, app_time);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when adding appointments!");
        } finally {
            cm.closeConnection();
        }
    }
    //Helper method for reserve; Remove newly reserved caregiver from Availability table
    private static void removeAvailability(Date app_time, String caregiver_name) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND TIME = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeCaregiver);
            statement.setDate(1, app_time);
            statement.setString(2, caregiver_name);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when removing caregiver from Availabilities!");
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        System.out.println("Sorry, operation currently not available.");
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    // output scheduled appointments for current user
    private static void showAppointments(String[] tokens) throws SQLException {
        // check if current user is patient or caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAppointment;
        if (currentPatient != null) {
            getAppointment = "SELECT app_id, vaccine_name, app_time, caregiver_name FROM Appointments WHERE patient_name = ?";
        } else {
            getAppointment = "SELECT app_id, vaccine_name, app_time, patient_name FROM Appointments WHERE caregiver_name = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            if (currentPatient != null) {
                statement.setString(1, currentPatient.getUsername());
            } else {
                statement.setString(1, currentCaregiver.getUsername());
            }
            ResultSet resultSet = statement.executeQuery();
            // check 4:
            if (!resultSet.isBeforeFirst()) {
                System.out.println("You have no appointment");
                return;
            }
            while (resultSet.next()) {
                int app_id = resultSet.getInt("app_id");
                String vaccine_name = resultSet.getString("vaccine_name");
                Date app_time = resultSet.getDate("app_time");
                String caregiver_name = resultSet.getString("caregiver_name");
                String patient_name = resultSet.getString("patient_name");

                System.out.println("Appointment details: ");
                System.out.print("Appointment ID: " + app_id);
                System.out.print(" Vaccine Scheduled: " + vaccine_name);
                System.out.print(" Appointment Time: " + app_time);
                if (currentPatient != null) {
                    System.out.print(" Caregiver Name: " + caregiver_name);
                } else {
                    System.out.print(" Patient Name: " + patient_name);
                }
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver != null) {
            currentCaregiver = null;
            System.out.println("You have logged out successfully.");
        } else if (currentPatient != null) {
            currentPatient = null;
            System.out.println("You have logged out successfully.");
        } else {
            System.out.println("Error! User already logged out.");
        }
    }
}
