
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

/**
 * A generic panel that creates a CRUD interface for any database table.
 * It dynamically inspects columns to generate forms and tables.
 */
public class CRUDPanel extends JPanel {
    private final String tableName;
    private final String primaryKeyColumn;
    private JTable table;
    private DefaultTableModel tableModel;

    public CRUDPanel(String tableName, String primaryKeyColumn) {
        this.tableName = tableName;
        this.primaryKeyColumn = primaryKeyColumn;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Setup UI
        setupTable();
        setupButtons();

        // Load initial data
        refreshData();
    }

    private void setupTable() {
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only, use buttons to edit
            }
        };
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Editar");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnRefresh = new JButton("Actualizar");

        btnAdd.addActionListener(e -> showEntryDialog(null));
        btnEdit.addActionListener(e -> editSelectedRecord());
        btnDelete.addActionListener(e -> deleteSelectedRecord());
        btnRefresh.addActionListener(e -> refreshData());

        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " ORDER BY " + primaryKeyColumn)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Add Column Names
            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnName(i));
            }

            // Add Rows
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                tableModel.addRow(row);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error cargando datos: " + e.getMessage(), "Erros en la base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Porfavor seleccione un registro");
            return;
        }

        // Map visual row index to model index (in case of sorting)
        int modelRow = table.convertRowIndexToModel(selectedRow);

        // Find the ID column index
        int idColIndex = findColumnIndex(primaryKeyColumn);
        if (idColIndex == -1) return;

        Object idValue = tableModel.getValueAt(modelRow, idColIndex);

        int confirm = JOptionPane.showConfirmDialog(this, "Seguro quequieres borrar el empleado con ID: " + idValue + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?")) {

                pstmt.setObject(1, idValue);
                pstmt.executeUpdate();
                refreshData();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error Borrando: " + e.getMessage());
            }
        }
    }

    private void editSelectedRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Porfavor seleccione un registro para editar");
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);

        // Gather current values
        Vector<Object> currentValues = (Vector<Object>) tableModel.getDataVector().elementAt(modelRow);
        showEntryDialog(currentValues);
    }

    /**
     * Shows a dialog to Add or Edit a record.
     * Automatically creates fields based on table columns.
     */
    private void showEntryDialog(Vector<Object> existingData) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), existingData == null ? "Agregar" : "Editar", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Vector<JTextField> textFields = new Vector<>();
        Vector<String> columnNames = new Vector<>();
        Vector<Integer> columnTypes = new Vector<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData dbMeta = conn.getMetaData();
            ResultSet rsColumns = dbMeta.getColumns(null, null, tableName, null);

            int row = 0;
            int dataIndex = 0;

            while (rsColumns.next()) {
                String colName = rsColumns.getString("COLUMN_NAME");
                int dataType = rsColumns.getInt("DATA_TYPE");

                // Skip auto-increment ID fields only when Adding
                // Simplicity logic: if it's the PK and it's an Integer/Serial, assume auto-gen for add
                boolean isAutoId = colName.equals(primaryKeyColumn) &&
                        (dataType == java.sql.Types.INTEGER || dataType == java.sql.Types.BIGINT);

                if (isAutoId && existingData == null) {
                    columnNames.add(colName); // Keep track but don't add field
                    columnTypes.add(dataType);
                    textFields.add(null); // Placeholder
                    dataIndex++;
                    continue;
                }

                gbc.gridx = 0; gbc.gridy = row;
                formPanel.add(new JLabel(colName + ":"), gbc);

                JTextField field = new JTextField(20);
                if (existingData != null && dataIndex < existingData.size()) {
                    Object val = existingData.get(dataIndex);
                    field.setText(val != null ? val.toString() : "");
                    // If editing, make ID read-only
                    if (colName.equals(primaryKeyColumn)) field.setEditable(false);
                }

                gbc.gridx = 1;
                formPanel.add(field, gbc);

                textFields.add(field);
                columnNames.add(colName);
                columnTypes.add(dataType);

                row++;
                dataIndex++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        JPanel buttonPanel = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");

        btnSave.addActionListener(e -> {
            saveRecord(existingData, textFields, columnNames, columnTypes);
            dialog.dispose();
        });
        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveRecord(Vector<Object> oldData, Vector<JTextField> fields, Vector<String> colNames, Vector<Integer> colTypes) {
        StringBuilder sql = new StringBuilder();
        boolean isUpdate = (oldData != null);

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (isUpdate) {
                // UPDATE tableName SET col1=?, col2=? WHERE pk=?
                sql.append("UPDATE ").append(tableName).append(" SET ");
                for (int i = 0; i < colNames.size(); i++) {
                    if (fields.get(i) != null && !colNames.get(i).equals(primaryKeyColumn)) {
                        sql.append(colNames.get(i)).append("=?,");
                    }
                }
                sql.setLength(sql.length() - 1); // remove last comma
                sql.append(" WHERE ").append(primaryKeyColumn).append("=?");
            } else {
                // INSERT INTO tableName (col1, col2) VALUES (?,?)
                sql.append("INSERT INTO ").append(tableName).append(" (");
                for (int i = 0; i < colNames.size(); i++) {
                    if (fields.get(i) != null) sql.append(colNames.get(i)).append(",");
                }
                sql.setLength(sql.length() - 1);
                sql.append(") VALUES (");
                for (int i = 0; i < colNames.size(); i++) {
                    if (fields.get(i) != null) sql.append("?,");
                }
                sql.setLength(sql.length() - 1);
                sql.append(")");
            }

            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            int paramIndex = 1;

            // Set parameters
            for (int i = 0; i < fields.size(); i++) {
                JTextField f = fields.get(i);
                if (f != null) {
                    // Skip PK if updating (it goes to the end of query)
                    if (isUpdate && colNames.get(i).equals(primaryKeyColumn)) continue;

                    setParam(pstmt, paramIndex++, f.getText(), colTypes.get(i));
                }
            }

            // If update, set the PK for WHERE clause
            if (isUpdate) {
                // Find PK field
                int pkIndex = colNames.indexOf(primaryKeyColumn);
                setParam(pstmt, paramIndex, fields.get(pkIndex).getText(), colTypes.get(pkIndex));
            }

            pstmt.executeUpdate();
            refreshData();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardando: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Helper to handle basic type conversion
    private void setParam(PreparedStatement pstmt, int index, String value, int sqlType) throws SQLException {
        if (value == null || value.isEmpty()) {
            pstmt.setNull(index, sqlType);
            return;
        }
        try {
            switch (sqlType) {
                case java.sql.Types.INTEGER:
                case java.sql.Types.SMALLINT:
                    pstmt.setInt(index, Integer.parseInt(value));
                    break;
                case java.sql.Types.NUMERIC:
                case java.sql.Types.DECIMAL:
                case java.sql.Types.DOUBLE:
                    pstmt.setDouble(index, Double.parseDouble(value));
                    break;
                case java.sql.Types.BOOLEAN:
                case java.sql.Types.BIT:
                    pstmt.setBoolean(index, Boolean.parseBoolean(value));
                    break;
                case java.sql.Types.DATE:
                    pstmt.setDate(index, java.sql.Date.valueOf(value)); // Format YYYY-MM-DD
                    break;
                default:
                    pstmt.setString(index, value);
            }
        } catch (NumberFormatException e) {
            throw new SQLException("Formato invalido para: " + value);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Formato Invalido (YYYY/MM/DD) para: " + value);
        }
    }

    private int findColumnIndex(String name) {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equals(name)) return i;
        }
        return -1;
    }
}