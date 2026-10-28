# 5–8 Minute Demonstration Guide

Use fictional data only. Confirm the MySQL server is running and the schema is prepared before the demonstration.

1. Launch the app and show the successful database connection.
2. Add a fictional medicine, for example: `Demo Vitamin`, type `Tablet`, dosage `1 tablet (demo only)`, quantity `3`, threshold `5`, expiry date `2027-12-31`.
3. View all medicines and search for `Demo`.
4. Show that the medicine appears in the low-stock list because quantity (3) is at or below threshold (5).
5. Schedule a reminder for the medicine 1–2 minutes in the future using `YYYY-MM-DDTHH:MM` format.
6. Keep the application running until the reminder alert appears. Show the reminder in the active reminder list.
7. Record the outcome as `TAKEN` (for demonstration only), then open reminder history to show the saved status.
8. Explain that the report contains the test protocol and that measured results must be filled in after running the tests.

## Backup plan

If the reminder is not due during the presentation, use a pre-scheduled reminder created before the demonstration. Do not claim that a reminder was delivered unless the alert was observed. Have the MySQL Workbench table view ready as backup evidence.
