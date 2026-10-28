package threads;

import dao.ReminderDAO;
import model.Reminder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Background reminder poller. A reminder is claimed in the database before alerting. */
public class ReminderMonitorThread extends Thread {
    private final ReminderDAO reminderDAO;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public ReminderMonitorThread(ReminderDAO reminderDAO) {
        super("medicine-reminder-monitor");
        this.reminderDAO = reminderDAO;
        setDaemon(true);
    }

    @Override public void run() {
        while (running.get() && !isInterrupted()) {
            try {
                List<Reminder> dueReminders = reminderDAO.claimDueReminders();
                for (Reminder reminder : dueReminders) {
                    System.out.printf("%n[MEDICINE REMINDER] %s (Medicine ID %d), scheduled %s. " +
                                    "Use option 10 to mark the outcome.%n",
                            reminder.getMedicineName(), reminder.getMedicineId(), reminder.getReminderTime().format(FORMAT));
                }
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Reminder monitor could not check reminders: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException interrupted) { interrupt(); break; }
            }
        }
    }

    public void shutdown() { running.set(false); interrupt(); }
}
