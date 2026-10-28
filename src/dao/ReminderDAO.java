package dao;

import model.Reminder;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC operations for reminder lifecycle and reliable one-time notification claims. */
public class ReminderDAO {
    public void addReminder(Reminder reminder) throws SQLException {
        if (reminder == null) throw new IllegalArgumentException("Reminder is required.");
        if (reminder.getMedicineId() <= 0) throw new IllegalArgumentException("A valid medicine ID is required.");
        if (reminder.getReminderTime() == null) throw new IllegalArgumentException("Reminder date and time are required.");
        String sql = "INSERT INTO reminders(medicine_id, reminder_time, status) VALUES (?, ?, 'PENDING')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reminder.getMedicineId());
            ps.setTimestamp(2, Timestamp.valueOf(reminder.getReminderTime()));
            ps.executeUpdate();
        }
    }

    public List<Reminder> getAllPendingReminders() throws SQLException {
        return getReminders("WHERE status IN ('PENDING','NOTIFIED')");
    }

    public List<Reminder> getAllReminders() throws SQLException {
        return getReminders("");
    }

    private List<Reminder> getReminders(String filter) throws SQLException {
        List<Reminder> result = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.medicine_id, r.reminder_time, r.status, m.name AS medicine_name " +
                "FROM reminders r JOIN medicines m ON m.medicine_id=r.medicine_id " + filter + " ORDER BY r.reminder_time";
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    /** Atomically claims due reminders so concurrent monitor instances cannot alert twice. */
    public List<Reminder> claimDueReminders() throws SQLException {
        List<Reminder> due = new ArrayList<>();
        String select = "SELECT r.reminder_id, r.medicine_id, r.reminder_time, r.status, m.name AS medicine_name " +
                "FROM reminders r JOIN medicines m ON m.medicine_id=r.medicine_id " +
                "WHERE r.status='PENDING' AND r.reminder_time <= NOW() ORDER BY r.reminder_time FOR UPDATE";
        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(select)) {
                    while (rs.next()) due.add(map(rs));
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE reminders SET status='NOTIFIED', notified_at=NOW() WHERE reminder_id=? AND status='PENDING'")) {
                    for (Reminder reminder : due) {
                        ps.setInt(1, reminder.getReminderId());
                        ps.addBatch();
                    }
                    int[] updates = ps.executeBatch();
                    for (int count : updates) {
                        if (count == 0) throw new SQLException("A due reminder could not be claimed; transaction cancelled.");
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollbackError) { ex.addSuppressed(rollbackError); }
                throw ex;
            } finally {
                try { con.setAutoCommit(oldAutoCommit); } catch (SQLException ignored) { /* Connection closes immediately after this method. */ }
            }
        }
        for (Reminder reminder : due) reminder.setStatus("NOTIFIED");
        return due;
    }

    public boolean markReminderStatus(int reminderId, String status) throws SQLException {
        if (!List.of("TAKEN", "MISSED", "SKIPPED").contains(status)) {
            throw new IllegalArgumentException("Unsupported final reminder status: " + status);
        }
        String sql = "UPDATE reminders SET status=?, completed_at=NOW() WHERE reminder_id=? AND status IN ('PENDING','NOTIFIED')";
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
        reminder.setReminderTime(rs.getTimestamp("reminder_time").toLocalDateTime());
        reminder.setStatus(rs.getString("status"));
        try { reminder.setMedicineName(rs.getString("medicine_name")); } catch (SQLException ignored) { }
        return reminder;
    }
}
