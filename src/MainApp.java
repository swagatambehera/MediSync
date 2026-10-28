import dao.MedicineDAO;
import dao.ReminderDAO;
import model.Medicine;
import model.Reminder;
import threads.ReminderMonitorThread;
import util.DBConnection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Console entry point for the Medicine Reminder and Stock Monitoring System. */
public class MainApp {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final MedicineDAO MEDICINES = new MedicineDAO();
    private static final ReminderDAO REMINDERS = new ReminderDAO();

    public static void main(String[] args) {
        ReminderMonitorThread monitor = null;
        try {
            // Fail early with a useful message if MySQL is not configured correctly.
            try (var ignored = DBConnection.getConnection()) {
                System.out.println("Database connection successful.");
            }
            monitor = new ReminderMonitorThread(REMINDERS);
            monitor.start();
            runMenu();
        } catch (SQLException e) {
            System.err.println("Cannot connect to MySQL. Check that MySQL Server is running, database " +
                    "medicine_system exists, and MEDICINE_DB_USER / MEDICINE_DB_PASSWORD are configured.");
            System.err.println("Details: " + e.getMessage());
        } finally {
            if (monitor != null) monitor.shutdown();
            SCANNER.close();
            System.out.println("Application closed.");
        }
    }

    private static void runMenu() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ", 1, 12);
            try {
                switch (choice) {
                    case 1 -> addMedicine();
                    case 2 -> showMedicines(MEDICINES.getAllMedicines(), "All medicines");
                    case 3 -> searchMedicine();
                    case 4 -> deleteMedicine();
                    case 5 -> updateStock();
                    case 6 -> showMedicines(MEDICINES.getLowStockMedicines(), "Low-stock medicines");
                    case 7 -> expiryMenu();
                    case 8 -> addReminder();
                    case 9 -> showReminders(REMINDERS.getAllPendingReminders(), true);
                    case 10 -> updateReminderOutcome();
                    case 11 -> showReminders(REMINDERS.getAllReminders(), false);
                    case 12 -> running = false;
                }
            } catch (SQLException e) {
                System.err.println("Database operation failed: " + e.getMessage());
            } catch (IllegalArgumentException | DateTimeParseException e) {
                System.err.println("Invalid input: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n====================================================");
        System.out.println(" MEDICINE REMINDER & STOCK MONITORING SYSTEM");
        System.out.println("====================================================");
        System.out.println("1.  Add medicine");
        System.out.println("2.  View all medicines");
        System.out.println("3.  Search medicine by name");
        System.out.println("4.  Delete medicine by ID");
        System.out.println("5.  Update medicine stock");
        System.out.println("6.  View low-stock medicines");
        System.out.println("7.  Check expiry alerts");
        System.out.println("8.  Schedule a medicine reminder");
        System.out.println("9.  View active reminders");
        System.out.println("10. Record reminder outcome (taken/missed/skipped)");
        System.out.println("11. View reminder history");
        System.out.println("12. Exit");
    }

    private static void addMedicine() throws SQLException {
        String name = readRequired("Medicine name: ");
        String type = readRequired("Type (e.g. tablet/syrup): ");
        String dosage = readRequired("Dosage (as prescribed): ");
        int quantity = readInt("Current quantity (0 or more): ", 0, Integer.MAX_VALUE);
        int threshold = readInt("Low-stock threshold (0 or more): ", 0, Integer.MAX_VALUE);
        LocalDate expiry = readDate("Expiry date (YYYY-MM-DD): ");
        String notes = readOptional("Notes (optional): ");
        MEDICINES.addMedicine(new Medicine(0, name, type, dosage, quantity, threshold, expiry, notes));
        System.out.println("Medicine saved successfully.");
    }

    private static void searchMedicine() throws SQLException {
        String keyword = readRequired("Search name: ");
        showMedicines(MEDICINES.searchMedicineByName(keyword), "Search results");
    }

    private static void deleteMedicine() throws SQLException {
        int id = readInt("Medicine ID to delete: ", 1, Integer.MAX_VALUE);
        if (confirm("Delete this medicine and its reminders? (y/n): ")) {
            System.out.println(MEDICINES.deleteMedicineById(id) ? "Medicine deleted." : "Medicine ID not found.");
        } else System.out.println("Delete cancelled.");
    }

    private static void updateStock() throws SQLException {
        int id = readInt("Medicine ID: ", 1, Integer.MAX_VALUE);
        Medicine medicine = MEDICINES.getMedicineById(id);
        if (medicine == null) { System.out.println("Medicine not found."); return; }
        System.out.printf("%s — current quantity: %d%n", medicine.getName(), medicine.getQuantity());
        int quantity = readInt("New quantity (0 or more): ", 0, Integer.MAX_VALUE);
        System.out.println(MEDICINES.updateMedicineStock(id, quantity) ? "Stock updated." : "Stock update failed.");
    }

    private static void expiryMenu() throws SQLException {
        System.out.println("1. Already expired\n2. Expiring soon");
        int option = readInt("Choose: ", 1, 2);
        if (option == 1) showMedicines(MEDICINES.getExpiredMedicines(), "Expired medicines");
        else {
            int days = readInt("Look ahead how many days? ", 0, 3650);
            showMedicines(MEDICINES.getMedicinesExpiringSoon(days), "Medicines expiring within " + days + " days");
        }
    }

    private static void addReminder() throws SQLException {
        int id = readInt("Medicine ID: ", 1, Integer.MAX_VALUE);
        Medicine medicine = MEDICINES.getMedicineById(id);
        if (medicine == null) { System.out.println("Medicine not found; reminder was not created."); return; }
        System.out.println("Selected: " + medicine.getName() + " — " + medicine.getDosage());
        LocalDateTime when = readDateTime("Reminder time (YYYY-MM-DDTHH:MM): ");
        if (when.isBefore(LocalDateTime.now().minusMinutes(1))) {
            System.out.println("This time is in the past. Schedule a future time so it can be tested reliably."); return;
        }
        Reminder reminder = new Reminder();
        reminder.setMedicineId(id);
        reminder.setReminderTime(when);
        reminder.setStatus("PENDING");
        REMINDERS.addReminder(reminder);
        System.out.println("Reminder scheduled. Keep this application running for the alert.");
    }

    private static void updateReminderOutcome() throws SQLException {
        List<Reminder> active = REMINDERS.getAllPendingReminders();
        showReminders(active, true);
        if (active.isEmpty()) return;
        int id = readInt("Reminder ID to update (0 to cancel): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        System.out.println("1. Taken\n2. Missed\n3. Skipped");
        int outcome = readInt("Outcome: ", 1, 3);
        String status = switch (outcome) { case 1 -> "TAKEN"; case 2 -> "MISSED"; default -> "SKIPPED"; };
        System.out.println(REMINDERS.markReminderStatus(id, status) ? "Reminder outcome saved as " + status + "." :
                "No active reminder found with that ID.");
    }

    private static void showReminders(List<Reminder> reminders, boolean activeOnly) {
        if (reminders.isEmpty()) { System.out.println(activeOnly ? "No active reminders." : "No reminder history yet."); return; }
        System.out.println("\nReminder ID | Medicine | Scheduled time | Status");
        System.out.println("---------------------------------------------------------------");
        for (Reminder r : reminders) {
            System.out.printf("%11d | %-20s | %s | %s%n", r.getReminderId(),
                    r.getMedicineName() == null ? "Medicine #" + r.getMedicineId() : r.getMedicineName(),
                    r.getReminderTime(), r.getStatus());
        }
    }

    private static void showMedicines(List<Medicine> medicines, String heading) {
        System.out.println("\n--- " + heading + " ---");
        if (medicines.isEmpty()) { System.out.println("No matching medicines found."); return; }
        for (Medicine m : medicines) {
            System.out.printf("ID: %d | %s | Type: %s | Dosage: %s | Qty: %d | Low-stock at: %d | Expiry: %s%n",
                    m.getMedicineId(), m.getName(), m.getType(), m.getDosage(), m.getQuantity(),
                    m.getLowStockThreshold(), m.getExpiryDate());
            if (m.getNotes() != null && !m.getNotes().isBlank()) System.out.println("  Notes: " + m.getNotes());
        }
    }

    private static String readRequired(String prompt) {
        while (true) { System.out.print(prompt); String value = SCANNER.nextLine().trim(); if (!value.isEmpty()) return value; System.out.println("This field is required."); }
    }
    private static String readOptional(String prompt) { System.out.print(prompt); return SCANNER.nextLine().trim(); }
    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt); String raw = SCANNER.nextLine().trim();
            try { int value = Integer.parseInt(raw); if (value >= min && value <= max) return value; }
            catch (NumberFormatException ignored) { }
            System.out.printf("Enter a whole number from %d to %d.%n", min, max);
        }
    }
    private static LocalDate readDate(String prompt) {
        while (true) { System.out.print(prompt); try { return LocalDate.parse(SCANNER.nextLine().trim()); } catch (DateTimeParseException e) { System.out.println("Use YYYY-MM-DD, for example 2027-03-31."); } }
    }
    private static LocalDateTime readDateTime(String prompt) {
        while (true) { System.out.print(prompt); try { return LocalDateTime.parse(SCANNER.nextLine().trim()); } catch (DateTimeParseException e) { System.out.println("Use YYYY-MM-DDTHH:MM, for example 2026-10-27T18:30."); } }
    }
    private static boolean confirm(String prompt) { System.out.print(prompt); return SCANNER.nextLine().trim().equalsIgnoreCase("y"); }
}
