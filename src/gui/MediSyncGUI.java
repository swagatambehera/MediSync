package gui;

import dao.MedicineDAO;
import dao.ReminderDAO;
import model.Medicine;
import model.Reminder;
import util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class MediSyncGUI extends JFrame {
    private static final Color NAVY = new Color(25, 43, 69);
    private static final Color TEAL = new Color(22, 142, 130);
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color TEXT = new Color(39, 52, 70);
    // User input accepts minutes; tables and notifications display seconds too.
    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Reminders already overdue when this app session starts must not trigger a popup.
    private final LocalDateTime appStartedAt = LocalDateTime.now();

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final ReminderDAO reminderDAO = new ReminderDAO();

    private final DefaultTableModel medicineModel = tableModel(
            "ID", "Name", "Type", "Dosage", "Stock", "Low threshold", "Expiry", "Notes");
    private final DefaultTableModel alertModel = tableModel(
            "ID", "Medicine", "Stock", "Threshold", "Expiry", "Alert");
    private final DefaultTableModel reminderModel = tableModel(
            "ID", "Medicine", "Scheduled time", "Status");
    private final DefaultTableModel historyModel = tableModel(
            "ID", "Medicine", "Scheduled time", "Status");

    private final JTable medicineTable = new JTable(medicineModel);
    private final JTable alertTable = new JTable(alertModel);
    private final JTable reminderTable = new JTable(reminderModel);
    private final JTable historyTable = new JTable(historyModel);

    private final JLabel totalValue = new JLabel("—");
    private final JLabel lowValue = new JLabel("—");
    private final JLabel expiryValue = new JLabel("—");
    private final JLabel activeValue = new JLabel("—");

    private final JTextField searchField = new JTextField(15);
    private JComboBox<Medicine> medicineCombo;
    private JTextField reminderTimeField;
    private JTabbedPane pages;
    private boolean reminderCheckInProgress;

    public MediSyncGUI() {
        super("MediSync | Medicine Management");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1240, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildPages(), BorderLayout.CENTER);

        styleTable(medicineTable);
        styleTable(alertTable);
        styleTable(reminderTable);
        styleTable(historyTable);

        refreshAll();

        Timer reminderTimer = new Timer(5000, e -> checkDueReminders());
        reminderTimer.setCoalesce(true);
        reminderTimer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                reminderTimer.stop();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(17, 24, 17, 24));

        JLabel title = new JLabel("MediSync");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel("Medicine reminder & stock monitoring");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(105, 118, 135));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(3));
        labels.add(subtitle);

        JLabel badge = new JLabel("  ●  MYSQL / JDBC  ");
        badge.setOpaque(true);
        badge.setBackground(new Color(225, 246, 239));
        badge.setForeground(new Color(20, 115, 88));
        badge.setBorder(new EmptyBorder(9, 10, 9, 10));

        panel.add(labels, BorderLayout.WEST);
        panel.add(badge, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setBackground(NAVY);
        panel.setPreferredSize(new Dimension(185, 0));
        panel.setBorder(new EmptyBorder(18, 12, 18, 12));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("WORKSPACE");
        heading.setForeground(new Color(174, 191, 211));
        heading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heading.setBorder(new EmptyBorder(5, 10, 16, 0));
        panel.add(heading);

        String[] items = {"Dashboard", "Medicines", "Stock & Expiry", "Reminders", "History"};
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            JButton button = new JButton(items[i]);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            button.setForeground(Color.WHITE);
            button.setBackground(NAVY);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(false);
            button.setBorder(new EmptyBorder(12, 10, 12, 10));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(e -> pages.setSelectedIndex(index));
            panel.add(button);
        }

        panel.add(Box.createVerticalGlue());
        JLabel footer = new JLabel("<html><font color='#AEBFD3'>Java · JDBC · MySQL</font></html>");
        footer.setBorder(new EmptyBorder(10, 8, 0, 0));
        panel.add(footer);
        return panel;
    }

    private JTabbedPane buildPages() {
        pages = new JTabbedPane();
        pages.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pages.setForeground(TEXT);
        pages.setBackground(BACKGROUND);
        pages.setOpaque(true);
        pages.addTab("Dashboard", buildDashboard());
        pages.addTab("Medicines", buildMedicinePage());
        pages.addTab("Stock & Expiry", buildAlertPage());
        pages.addTab("Reminders", buildReminderPage());
        pages.addTab("History", buildHistoryPage());
        return pages;
    }

    private JPanel page(String title, String description) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(23, 23, 23, 23));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        titleLabel.setForeground(TEXT);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionLabel.setForeground(new Color(105, 118, 135));

        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(5));
        heading.add(descriptionLabel);
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildDashboard() {
        JPanel panel = page("Overview", "A summary of your medicine inventory and reminders.");
        JPanel body = new JPanel(new BorderLayout(0, 17));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.add(statCard("TOTAL MEDICINES", totalValue));
        cards.add(statCard("LOW STOCK", lowValue));
        cards.add(statCard("EXPIRING / EXPIRED", expiryValue));
        cards.add(statCard("ACTIVE REMINDERS", activeValue));

        JPanel alerts = whitePanel(new BorderLayout(0, 10));
        alerts.add(sectionTitle("Inventory alerts"), BorderLayout.NORTH);
        alerts.add(new JScrollPane(alertTable), BorderLayout.CENTER);
        JButton refresh = button("Refresh dashboard");
        refresh.addActionListener(e -> refreshAll());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(refresh);
        alerts.add(actions, BorderLayout.SOUTH);

        body.add(cards, BorderLayout.NORTH);
        body.add(alerts, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMedicinePage() {
        JPanel panel = page("Medicine inventory", "Add, search, edit, update stock, and delete medicine records.");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        toolbar.setOpaque(false);
        searchField.setPreferredSize(new Dimension(165, 34));

        JButton search = button("Search");
        JButton all = button("Show all");
        JButton add = button("Add medicine");
        JButton edit = button("Edit");
        JButton stock = button("Update stock");
        JButton delete = button("Delete");

        search.addActionListener(e -> searchMedicines());
        all.addActionListener(e -> refreshAll());
        add.addActionListener(e -> medicineDialog(null));
        edit.addActionListener(e -> {
            Medicine selected = selectedMedicine();
            if (selected != null) medicineDialog(selected);
        });
        stock.addActionListener(e -> updateStock());
        delete.addActionListener(e -> deleteMedicine());

        toolbar.add(new JLabel("Name:"));
        toolbar.add(searchField);
        toolbar.add(search);
        toolbar.add(all);
        toolbar.add(add);
        toolbar.add(edit);
        toolbar.add(stock);
        toolbar.add(delete);

        content.add(toolbar, BorderLayout.NORTH);
        content.add(tablePanel(medicineTable), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAlertPage() {
        JPanel panel = page("Stock & expiry alerts", "Filter medicines by stock level and expiry date.");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        JButton all = button("All medicines");
        JButton low = button("Low stock");
        JButton expired = button("Expired");
        JButton soon = button("Expiring in 30 days");
        all.addActionListener(e -> loadAlerts("all"));
        low.addActionListener(e -> loadAlerts("low"));
        expired.addActionListener(e -> loadAlerts("expired"));
        soon.addActionListener(e -> loadAlerts("soon"));
        toolbar.add(all);
        toolbar.add(low);
        toolbar.add(expired);
        toolbar.add(soon);

        content.add(toolbar, BorderLayout.NORTH);
        content.add(tablePanel(alertTable), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReminderPage() {
        JPanel panel = page("Reminders", "Schedule medicine reminders and record dose outcomes.");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel form = whitePanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        medicineCombo = new JComboBox<>();
        medicineCombo.setPreferredSize(new Dimension(190, 32));
        reminderTimeField = new JTextField(16);
        reminderTimeField.setText(LocalDateTime.now().plusMinutes(10)
                .withSecond(0).withNano(0).format(INPUT_DATE_TIME));

        JButton schedule = button("Schedule");
        JButton refresh = button("Refresh");
        JButton outcome = button("Record outcome");
        schedule.addActionListener(e -> scheduleReminder());
        refresh.addActionListener(e -> loadReminders(true));
        outcome.addActionListener(e -> recordOutcome());

        form.add(new JLabel("Medicine:"));
        form.add(medicineCombo);
        form.add(new JLabel("Time (yyyy-MM-dd HH:mm):"));
        form.add(reminderTimeField);
        form.add(schedule);
        form.add(refresh);
        form.add(outcome);

        content.add(form, BorderLayout.NORTH);
        content.add(tablePanel(reminderTable), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPage() {
        JPanel panel = page("Reminder history", "All reminders, including completed and pending records.");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JButton refresh = button("Refresh history");
        refresh.addActionListener(e -> loadReminders(false));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setOpaque(false);
        toolbar.add(refresh);

        content.add(toolbar, BorderLayout.NORTH);
        content.add(tablePanel(historyTable), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel statCard(String label, JLabel value) {
        JPanel panel = whitePanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 234, 240)),
                new EmptyBorder(15, 15, 15, 15)));
        JLabel heading = new JLabel(label);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 10));
        heading.setForeground(new Color(100, 115, 132));
        value.setFont(new Font("Segoe UI", Font.BOLD, 29));
        value.setForeground(TEXT);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JPanel whitePanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(13, 13, 13, 13));
        return panel;
    }

    private JPanel tablePanel(JTable table) {
        JPanel panel = whitePanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(TEXT);
        return label;
    }

    private JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(TEAL);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(29);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(232, 236, 241));
        table.setSelectionBackground(new Color(210, 239, 234));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setBackground(new Color(237, 242, 247));
        table.getTableHeader().setOpaque(true);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void refreshAll() {
        try {
            loadMedicines(medicineDAO.getAllMedicines());
            loadAlerts("all");
            loadReminders(true);
            loadReminders(false);
            updateCards();
            refreshMedicineChoices();
        } catch (Exception ex) {
            showError("Could not refresh data", ex);
        }
    }

    private void updateCards() throws SQLException {
        totalValue.setText(String.valueOf(medicineDAO.getAllMedicines().size()));
        lowValue.setText(String.valueOf(medicineDAO.getLowStockMedicines().size()));
        int expired = medicineDAO.getExpiredMedicines().size();
        int soon = medicineDAO.getMedicinesExpiringSoon(30).size();
        expiryValue.setText(String.valueOf(expired + soon));
        activeValue.setText(String.valueOf(reminderDAO.getAllPendingReminders().size()));
    }

    private void loadMedicines(List<Medicine> medicines) {
        medicineModel.setRowCount(0);
        for (Medicine m : medicines) {
            medicineModel.addRow(new Object[]{
                    m.getMedicineId(), m.getName(), m.getType(), m.getDosage(),
                    m.getQuantity(), m.getLowStockThreshold(), m.getExpiryDate(),
                    m.getNotes() == null ? "" : m.getNotes()
            });
        }
    }

    private void loadAlerts(String filter) {
        try {
            List<Medicine> medicines = switch (filter) {
                case "low" -> medicineDAO.getLowStockMedicines();
                case "expired" -> medicineDAO.getExpiredMedicines();
                case "soon" -> medicineDAO.getMedicinesExpiringSoon(30);
                default -> medicineDAO.getAllMedicines();
            };

            alertModel.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (Medicine m : medicines) {
                String alert;
                if (m.getExpiryDate() != null && m.getExpiryDate().isBefore(today)) {
                    alert = "EXPIRED";
                } else if (m.getExpiryDate() != null
                        && !m.getExpiryDate().isAfter(today.plusDays(30))) {
                    alert = "EXPIRING SOON";
                } else if (m.getQuantity() <= m.getLowStockThreshold()) {
                    alert = "LOW STOCK";
                } else {
                    alert = "OK";
                }
                alertModel.addRow(new Object[]{
                        m.getMedicineId(), m.getName(), m.getQuantity(),
                        m.getLowStockThreshold(), m.getExpiryDate(), alert
                });
            }
        } catch (Exception ex) {
            showError("Could not load alerts", ex);
        }
    }

    private void loadReminders(boolean activeOnly) {
        try {
            List<Reminder> reminders = activeOnly
                    ? reminderDAO.getAllPendingReminders()
                    : reminderDAO.getAllReminders();
            DefaultTableModel target = activeOnly ? reminderModel : historyModel;
            target.setRowCount(0);
            for (Reminder r : reminders) {
                target.addRow(new Object[]{
                        r.getReminderId(), r.getMedicineName(),
                        r.getReminderTime().format(DISPLAY_DATE_TIME), r.getStatus()
                });
            }
        } catch (Exception ex) {
            showError("Could not load reminders", ex);
        }
    }

    private void refreshMedicineChoices() throws SQLException {
        if (medicineCombo == null) return;
        int previousId = -1;
        Medicine previous = (Medicine) medicineCombo.getSelectedItem();
        if (previous != null) previousId = previous.getMedicineId();

        medicineCombo.removeAllItems();
        for (Medicine medicine : medicineDAO.getAllMedicines()) {
            medicineCombo.addItem(medicine);
        }
        medicineCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Medicine m) {
                    setText(m.getName() + " (ID " + m.getMedicineId() + ")");
                }
                return this;
            }
        });
        for (int i = 0; i < medicineCombo.getItemCount(); i++) {
            if (medicineCombo.getItemAt(i).getMedicineId() == previousId) {
                medicineCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void searchMedicines() {
        try {
            loadMedicines(medicineDAO.searchMedicineByName(searchField.getText().trim()));
        } catch (Exception ex) {
            showError("Search failed", ex);
        }
    }

    private Medicine selectedMedicine() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a medicine row first.");
            return null;
        }
        int row = medicineTable.convertRowIndexToModel(selectedRow);
        int id = Integer.parseInt(medicineModel.getValueAt(row, 0).toString());
        try {
            return medicineDAO.getMedicineById(id);
        } catch (Exception ex) {
            showError("Could not load selected medicine", ex);
            return null;
        }
    }

    private void medicineDialog(Medicine existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName(), 18);
        JTextField type = new JTextField(existing == null ? "" : existing.getType(), 18);
        JTextField dosage = new JTextField(existing == null ? "" : existing.getDosage(), 18);
        JTextField quantity = new JTextField(existing == null ? "0" : String.valueOf(existing.getQuantity()), 18);
        JTextField threshold = new JTextField(existing == null ? "5" : String.valueOf(existing.getLowStockThreshold()), 18);
        JTextField expiry = new JTextField(existing == null ? LocalDate.now().plusYears(1).toString() : String.valueOf(existing.getExpiryDate()), 18);
        JTextField notes = new JTextField(existing == null || existing.getNotes() == null ? "" : existing.getNotes(), 18);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Medicine name *")); form.add(name);
        form.add(new JLabel("Type *")); form.add(type);
        form.add(new JLabel("Dosage *")); form.add(dosage);
        form.add(new JLabel("Quantity *")); form.add(quantity);
        form.add(new JLabel("Low-stock threshold *")); form.add(threshold);
        form.add(new JLabel("Expiry (YYYY-MM-DD) *")); form.add(expiry);
        form.add(new JLabel("Notes")); form.add(notes);

        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add medicine" : "Edit medicine",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            Medicine medicine = existing == null ? new Medicine() : existing;
            medicine.setName(name.getText().trim());
            medicine.setType(type.getText().trim());
            medicine.setDosage(dosage.getText().trim());
            medicine.setQuantity(Integer.parseInt(quantity.getText().trim()));
            medicine.setLowStockThreshold(Integer.parseInt(threshold.getText().trim()));
            medicine.setExpiryDate(LocalDate.parse(expiry.getText().trim()));
            medicine.setNotes(notes.getText().trim());

            if (existing == null) {
                medicineDAO.addMedicine(medicine);
            } else {
                medicineDAO.updateMedicine(medicine);
            }
            refreshAll();
            JOptionPane.showMessageDialog(this, "Medicine saved successfully.");
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Expiry must use YYYY-MM-DD format.");
        } catch (Exception ex) {
            showError("Could not save medicine", ex);
        }
    }

    private void updateStock() {
        Medicine medicine = selectedMedicine();
        if (medicine == null) return;
        String value = JOptionPane.showInputDialog(this,
                "Enter the new stock quantity:", medicine.getQuantity());
        if (value == null) return;
        try {
            int quantity = Integer.parseInt(value.trim());
            if (quantity < 0) throw new IllegalArgumentException("Stock cannot be negative.");
            if (medicineDAO.updateMedicineStock(medicine.getMedicineId(), quantity)) {
                refreshAll();
                JOptionPane.showMessageDialog(this, "Stock updated.");
            }
        } catch (Exception ex) {
            showError("Could not update stock", ex);
        }
    }

    private void deleteMedicine() {
        Medicine medicine = selectedMedicine();
        if (medicine == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + medicine.getName() + " and its reminders?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            if (medicineDAO.deleteMedicineById(medicine.getMedicineId())) {
                refreshAll();
                JOptionPane.showMessageDialog(this, "Medicine deleted.");
            }
        } catch (Exception ex) {
            showError("Could not delete medicine", ex);
        }
    }

    private void scheduleReminder() {
        Medicine medicine = (Medicine) medicineCombo.getSelectedItem();
        if (medicine == null) {
            JOptionPane.showMessageDialog(this, "Add a medicine first.");
            return;
        }
        try {
            LocalDateTime when = LocalDateTime.parse(reminderTimeField.getText().trim(), INPUT_DATE_TIME);
            if (when.isBefore(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this, "Choose a future date and time.");
                return;
            }
            Reminder reminder = new Reminder();
            reminder.setMedicineId(medicine.getMedicineId());
            reminder.setReminderTime(when);
            reminderDAO.addReminder(reminder);
            refreshAll();
            JOptionPane.showMessageDialog(this, "Reminder scheduled.");
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Use yyyy-MM-dd HH:mm, for example 2026-10-28 19:30.");
        } catch (Exception ex) {
            showError("Could not schedule reminder", ex);
        }
    }

    private void recordOutcome() {
        int selectedRow = reminderTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an active reminder first.");
            return;
        }
        int row = reminderTable.convertRowIndexToModel(selectedRow);
        int id = Integer.parseInt(reminderModel.getValueAt(row, 0).toString());
        String[] options = {"TAKEN", "MISSED", "SKIPPED"};
        String status = (String) JOptionPane.showInputDialog(this,
                "Select the reminder outcome:", "Record outcome",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (status == null) return;
        try {
            if (reminderDAO.markReminderStatus(id, status)) {
                refreshAll();
                JOptionPane.showMessageDialog(this, "Outcome saved: " + status);
            } else {
                JOptionPane.showMessageDialog(this, "Reminder is no longer active.");
            }
        } catch (Exception ex) {
            showError("Could not update reminder status", ex);
        }
    }

    private void checkDueReminders() {
        if (reminderCheckInProgress || !isShowing()) return;
        reminderCheckInProgress = true;
        try {
            List<Reminder> due = reminderDAO.claimDueReminders(appStartedAt);
            for (Reminder reminder : due) {
                JOptionPane.showMessageDialog(this,
                        "It is time for: " + reminder.getMedicineName()
                                + "\nScheduled: " + reminder.getReminderTime().format(DISPLAY_DATE_TIME)
                                + "\nOpen Reminders to record the outcome.",
                        "MediSync medicine reminder", JOptionPane.INFORMATION_MESSAGE);
            }
            if (!due.isEmpty()) refreshAll();
        } catch (Exception ex) {
            System.err.println("Reminder check failed: " + ex.getMessage());
        } finally {
            reminderCheckInProgress = false;
        }
    }

    private void showError(String message, Exception ex) {
        String detail = ex.getMessage() == null
                ? ex.getClass().getSimpleName() : ex.getMessage();
        JOptionPane.showMessageDialog(this, message + "\n" + detail,
                "MediSync", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try (var connection = DBConnection.getConnection()) {
            // Verify database connectivity before showing the UI.
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to MySQL. Check MySQL Server and the "
                            + "MEDICINE_DB_URL, MEDICINE_DB_USER, and "
                            + "MEDICINE_DB_PASSWORD environment variables.\n\n"
                            + ex.getMessage(),
                    "MediSync database connection failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing's default look and feel.
        }

        // Explicit readable colors prevent the operating-system look and feel
        // from rendering light text on light buttons or tabs.
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.disabledText", new Color(115, 125, 138));
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);

        SwingUtilities.invokeLater(() -> new MediSyncGUI().setVisible(true));
    }
}
