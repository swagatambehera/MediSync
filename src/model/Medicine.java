package model;

import java.time.LocalDate;

public class Medicine {
    private int medicineId;
    private String name;
    private String type;
    private String dosage;
    private int quantity;
    private int lowStockThreshold;
    private LocalDate expiryDate;
    private String notes;

    public Medicine() {
    }

    public Medicine(int medicineId, String name, String type, String dosage,
                    int quantity, int lowStockThreshold, LocalDate expiryDate, String notes) {
        this.medicineId = medicineId;
        this.name = name;
        this.type = type;
        this.dosage = dosage;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.expiryDate = expiryDate;
        this.notes = notes;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void displayDetails() {
        System.out.println("--------------------------------------------------");
        System.out.println("Medicine ID         : " + medicineId);
        System.out.println("Name                : " + name);
        System.out.println("Type                : " + type);
        System.out.println("Dosage              : " + dosage);
        System.out.println("Quantity            : " + quantity);
        System.out.println("Low Stock Threshold : " + lowStockThreshold);
        System.out.println("Expiry Date         : " + expiryDate);
        System.out.println("Notes               : " + notes);
        System.out.println("--------------------------------------------------");
    }
}