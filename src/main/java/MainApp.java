

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainApp {
    public static void main(String[] args) {
        // Apply FlatLaf Dark Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Fallo al inicializar flatlaf");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Equipment Maintenance DB");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            JTabbedPane tabbedPane = new JTabbedPane();

            // Define your tables and their Primary Key columns here
            Map<String, String> tables = new LinkedHashMap<>();
            tables.put("equipment", "equipment_id");
            tables.put("personnel", "id_persona");
            tables.put("schedules", "horarios_id");
            tables.put("equipment_types", "codigo_tipo_equipo");
            tables.put("skills", "codigo_habilidad");
            tables.put("equipment_schedules", "equipment_id"); // Note: Join tables technically have composite keys, using one for display
            tables.put("personnel_skills", "id_persona");
            tables.put("schedules_personnel", "horarios_id");
            tables.put("schedules_skills", "horarios_id");

            // Create a CRUD tab for each table
            for (Map.Entry<String, String> entry : tables.entrySet()) {
                String tableName = entry.getKey();
                String pkColumn = entry.getValue();

                // Capitalize tab title
                String title = tableName.substring(0, 1).toUpperCase() + tableName.substring(1).replace("_", " ");

                tabbedPane.addTab(title, new    CRUDPanel(tableName, pkColumn));
            }

            frame.add(tabbedPane);
            frame.setVisible(true);
        });
    }
}