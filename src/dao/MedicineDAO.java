package dao;

import model.Medicine;
import util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    public void addMedicine(Medicine medicine) throws SQLException {
        if (medicine.getName() == null || medicine.getName().isBlank()) throw new IllegalArgumentException("Medicine name is required.");
        if (medicine.getType() == null || medicine.getType().isBlank()) throw new IllegalArgumentException("Medicine type is required.");
        if (medicine.getDosage() == null || medicine.getDosage().isBlank()) throw new IllegalArgumentException("Dosage is required.");
        if (medicine.getQuantity() < 0 || medicine.getLowStockThreshold() < 0) throw new IllegalArgumentException("Quantity and threshold cannot be negative.");
        if (medicine.getExpiryDate() == null) throw new IllegalArgumentException("Expiry date is required.");
        String query = "INSERT INTO medicines(name, type, dosage, quantity, low_stock_threshold, expiry_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, medicine.getName());
            ps.setString(2, medicine.getType());
            ps.setString(3, medicine.getDosage());
            ps.setInt(4, medicine.getQuantity());
            ps.setInt(5, medicine.getLowStockThreshold());
            ps.setDate(6, Date.valueOf(medicine.getExpiryDate()));
            ps.setString(7, medicine.getNotes());

            ps.executeUpdate();
        }
    }

    public List<Medicine> getAllMedicines() throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM medicines ORDER BY name";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                medicines.add(extractMedicineFromResultSet(rs));
            }
        }
        return medicines;
    }

    public List<Medicine> searchMedicineByName(String name) throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM medicines WHERE name LIKE ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, "%" + name + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicines.add(extractMedicineFromResultSet(rs));
                }
            }
        }
        return medicines;
    }

    public boolean deleteMedicineById(int medicineId) throws SQLException {
        // Delete related reminders first so this works with older schemas without ON DELETE CASCADE.
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement reminderPs = con.prepareStatement("DELETE FROM reminders WHERE medicine_id = ?")) {
                    reminderPs.setInt(1, medicineId);
                    reminderPs.executeUpdate();
                }
                int affected;
                try (PreparedStatement medicinePs = con.prepareStatement("DELETE FROM medicines WHERE medicine_id = ?")) {
                    medicinePs.setInt(1, medicineId);
                    affected = medicinePs.executeUpdate();
                }
                con.commit();
                return affected > 0;
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollbackError) { ex.addSuppressed(rollbackError); }
                throw ex;
            }
        }
    }

    public boolean updateMedicineStock(int medicineId, int newQuantity) throws SQLException {
        if (newQuantity < 0) throw new IllegalArgumentException("Stock quantity cannot be negative.");
        String query = "UPDATE medicines SET quantity = ? WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, newQuantity);
            ps.setInt(2, medicineId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    
public boolean updateMedicine(Medicine medicine) throws SQLException {
    if (medicine == null || medicine.getMedicineId() <= 0) {
        throw new IllegalArgumentException("Invalid medicine.");
    }

    if (medicine.getName() == null || medicine.getName().isBlank()) {
        throw new IllegalArgumentException("Medicine name is required.");
    }

    if (medicine.getType() == null || medicine.getType().isBlank()) {
        throw new IllegalArgumentException("Medicine type is required.");
    }

    if (medicine.getDosage() == null || medicine.getDosage().isBlank()) {
        throw new IllegalArgumentException("Dosage is required.");
    }

    if (medicine.getQuantity() < 0 ||
            medicine.getLowStockThreshold() < 0) {
        throw new IllegalArgumentException(
                "Quantity and threshold cannot be negative.");
    }

    if (medicine.getExpiryDate() == null) {
        throw new IllegalArgumentException("Expiry date is required.");
    }

    String query = """
            UPDATE medicines
            SET name = ?,
                type = ?,
                dosage = ?,
                quantity = ?,
                low_stock_threshold = ?,
                expiry_date = ?,
                notes = ?
            WHERE medicine_id = ?
            """;

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        ps.setString(1, medicine.getName().trim());
        ps.setString(2, medicine.getType().trim());
        ps.setString(3, medicine.getDosage().trim());
        ps.setInt(4, medicine.getQuantity());
        ps.setInt(5, medicine.getLowStockThreshold());
        ps.setDate(6, Date.valueOf(medicine.getExpiryDate()));
        ps.setString(7, medicine.getNotes());
        ps.setInt(8, medicine.getMedicineId());

        return ps.executeUpdate() > 0;
    }
}


    public List<Medicine> getLowStockMedicines() throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM medicines WHERE quantity <= low_stock_threshold";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                medicines.add(extractMedicineFromResultSet(rs));
            }
        }
        return medicines;
    }

    public List<Medicine> getExpiredMedicines() throws SQLException {
        List<Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM medicines WHERE expiry_date < CURDATE()";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                medicines.add(extractMedicineFromResultSet(rs));
            }
        }
        return medicines;
    }

    public List<Medicine> getMedicinesExpiringSoon(int days) throws SQLException {
        if (days < 0) throw new IllegalArgumentException("Days ahead cannot be negative.");
        List<Medicine> medicines = new ArrayList<>();
        String query = "SELECT * FROM medicines WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, days);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicines.add(extractMedicineFromResultSet(rs));
                }
            }
        }
        return medicines;
    }

    public Medicine getMedicineById(int medicineId) throws SQLException {
        String query = "SELECT * FROM medicines WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, medicineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractMedicineFromResultSet(rs);
                }
            }
        }
        return null;
    }

    private Medicine extractMedicineFromResultSet(ResultSet rs) throws SQLException {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(rs.getInt("medicine_id"));
        medicine.setName(rs.getString("name"));
        medicine.setType(rs.getString("type"));
        medicine.setDosage(rs.getString("dosage"));
        medicine.setQuantity(rs.getInt("quantity"));
        medicine.setLowStockThreshold(rs.getInt("low_stock_threshold"));

        Date expirySqlDate = rs.getDate("expiry_date");
        if (expirySqlDate != null) {
            medicine.setExpiryDate(expirySqlDate.toLocalDate());
        } else {
            medicine.setExpiryDate((LocalDate) null);
        }

        medicine.setNotes(rs.getString("notes"));
        return medicine;
    }
}