package dao;

import model.Reminder;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC operations for reminder scheduling, one-time notification claims, and outcomes. */
public class ReminderDAO {

    public void addReminder(Reminder reminder) throws SQLException {
        if (reminder == null) throw new IllegalArgumentException("Reminder is required.");
        if (reminder.getMedicineId() <= 0) throw new IllegalArgumentException("A valid medicine ID is required.");
        if (reminder.getReminderTime() == null) throw new IllegalArgumentException("Reminder date and time are required.");

        String sql = "INSERT INTO reminders(medicine_id, reminder_time, status) VALUES (?, ?, 'PENDING')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reminder.getMedicineId());
            // The schema should use DATETIME for local wall-clock reminder times.
            ps.setObject(2, reminder.getReminderTime());
            ps.executeUpdate();
        }
    }

    /** Active reminders includes both waiting and already-notified items, so users can record outcomes. */
    public List<Reminder> getAllPendingReminders() throws SQLException {
        return getReminders("WHERE r.status IN ('PENDING','NOTIFIED')");
    }

    public List<Reminder> getAllReminders() throws SQLException {
        return getReminders("");
    }

    private List<Reminder> getReminders(String filter) throws SQLException {
        List<Reminder> result = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.medicine_id, r.reminder_time, r.status, m.name AS medicine_name "
                + "FROM reminders r JOIN medicines m ON m.medicine_id=r.medicine_id "
                + filter + " ORDER BY r.reminder_time";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    /**
     * Claims only reminders that become due during this application session.
     * Overdue reminders from before startup remain visible in the active table,
     * but will not unexpectedly produce a popup when the application opens.
     * Requires reminders.reminder_time to be DATETIME storing local time consistent with NOW().
     */
    public List<Reminder> claimDueReminders(LocalDateTime sessionStartedAt) throws SQLException {
        if (sessionStartedAt == null) throw new IllegalArgumentException("Session start time is required.");

        List<Reminder> due = new ArrayList<>();
        String select = "SELECT r.reminder_id, r.medicine_id, r.reminder_time, r.status, m.name AS medicine_name "
                + "FROM reminders r JOIN medicines m ON m.medicine_id=r.medicine_id "
                + "WHERE r.status='PENDING' AND r.reminder_time >= ? AND r.reminder_time <= NOW() "
                + "ORDER BY r.reminder_time FOR UPDATE";
        String update = "UPDATE reminders SET status='NOTIFIED', notified_at=NOW() "
                + "WHERE reminder_id=? AND status='PENDING'";

        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                // setObject avoids Timestamp's instant/time-zone conversion for a DATETIME column.
                try (PreparedStatement ps = con.prepareStatement(select)) {
                    ps.setObject(1, sessionStartedAt);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) due.add(map(rs));
                    }
                }

                if (!due.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(update)) {
                        for (Reminder reminder : due) {
                            ps.setInt(1, reminder.getReminderId());
                            ps.addBatch();
                        }
                        int[] updates = ps.executeBatch();
                        for (int count : updates) {
                            if (count == 0) throw new SQLException("A due reminder could not be claimed; transaction cancelled.");
                        }
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollbackError) { ex.addSuppressed(rollbackError); }
                throw ex;
            } finally {
                try { con.setAutoCommit(oldAutoCommit); } catch (SQLException ignored) { /* Connection closes below. */ }
            }
        }

        for (Reminder reminder : due) reminder.setStatus("NOTIFIED");
        return due;
    }

    /** Compatibility overload: treats method invocation time as the session boundary. */
    public List<Reminder> claimDueReminders() throws SQLException {
        return claimDueReminders(LocalDateTime.now());
    }

    public boolean markReminderStatus(int reminderId, String status) throws SQLException {
        if (reminderId <= 0) throw new IllegalArgumentException("A valid reminder ID is required.");
        if (status == null || !List.of("TAKEN", "MISSED", "SKIPPED").contains(status)) {
            throw new IllegalArgumentException("Status must be TAKEN, MISSED, or SKIPPED.");
        }
        String sql = "UPDATE reminders SET status=?, completed_at=NOW() "
                + "WHERE reminder_id=? AND status IN ('PENDING','NOTIFIED')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reminderId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markReminderAsCompleted(int reminderId) throws SQLException {
        return markReminderStatus(reminderId, "TAKEN");
    }

    private Reminder map(ResultSet rs) throws SQLException {
        Reminder reminder = new Reminder();
        reminder.setReminderId(rs.getInt("reminder_id"));
        reminder.setMedicineId(rs.getInt("medicine_id"));
        reminder.setReminderTime(rs.getObject("reminder_time", LocalDateTime.class));
        reminder.setStatus(rs.getString("status"));
        try { reminder.setMedicineName(rs.getString("medicine_name")); }
        catch (SQLException ignored) { /* Optional for query compatibility. */ }
        return reminder;
    }
}
