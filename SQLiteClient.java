import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;  // Corrected import

public class SQLiteClient extends Application {
    
    private static final String DB_URL = "jdbc:sqlite:tiny_zoo.db";
    private TextArea sqlInput;
    private TableView<ObservableList<String>> resultTable;
    private Label statusLabel;
    
    public static void main(String[] args) {
        // Load SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            e.printStackTrace();
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SQLite Client - Tiny Zoo Database");
        
        // Create UI components
        sqlInput = new TextArea();
        sqlInput.setPromptText("Enter SQL command here...");
        sqlInput.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
        
        Button executeButton = new Button("Execute");
        executeButton.setDefaultButton(true);  // Allows execution with Enter key
        executeButton.setOnAction(e -> executeSQL());
        
        resultTable = new TableView<>();
        statusLabel = new Label("Ready to connect to: " + DB_URL);
        
        // Layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(
            new Label("SQL Command:"),
            sqlInput,
            executeButton,
            new Label("Results:"),
            new ScrollPane(resultTable),
            statusLabel
        );
        
        // Configure table to use available space
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Test connection on startup
        testConnection();
    }
    
    private void testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            statusLabel.setText("Connected to: " + DB_URL);
            DatabaseMetaData meta = conn.getMetaData();
            statusLabel.setText(statusLabel.getText() + 
                " | SQLite v" + meta.getDatabaseProductVersion());
                
            // Check if Animal table exists (from your original zoo app)
            ResultSet tables = conn.getMetaData().getTables(null, null, "Animal", null);
            if (!tables.next()) {
                statusLabel.setText(statusLabel.getText() + " | Warning: Animal table not found");
            }
        } catch (SQLException e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            showAlert("Connection Error", "Failed to connect to database", 
                     "Please ensure:\n" +
                     "1. SQLite JDBC driver is available\n" +
                     "2. The database file exists\n" +
                     "3. You have proper permissions\n\n" +
                     "Error: " + e.getMessage());
        }
    }
    
    private void executeSQL() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("Please enter an SQL command");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Clear previous results
            resultTable.getColumns().clear();
            resultTable.getItems().clear();
            
            if (sql.toLowerCase().startsWith("select")) {
                // Handle SELECT queries
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Create columns
                for (int i = 1; i <= columnCount; i++) {
                    final int columnIndex = i;
                    TableColumn<ObservableList<String>, String> column = 
                        new TableColumn<>(metaData.getColumnName(i));
                    column.setCellValueFactory(data -> 
                        new SimpleStringProperty(data.getValue().get(columnIndex - 1)));
                    resultTable.getColumns().add(column);
                }
                
                // Add data
                ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row.add(value != null ? value.toString() : "NULL");
                    }
                    data.add(row);
                }
                
                resultTable.setItems(data);
                statusLabel.setText("Query executed successfully. Rows returned: " + data.size());
                
            } else {
                // Handle UPDATE, INSERT, DELETE, etc.
                int rowsAffected = stmt.executeUpdate(sql);
                statusLabel.setText("Command executed successfully. Rows affected: " + rowsAffected);
                
                // If the command modified the Animal table, show a hint
                if (sql.toLowerCase().contains("animal")) {
                    statusLabel.setText(statusLabel.getText() + 
                        " | You might want to run: SELECT * FROM Animal");
                }
            }
            
        } catch (SQLException e) {
            statusLabel.setText("Error executing SQL: " + e.getMessage());
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
            showAlert("SQL Error", "Failed to execute command", e.getMessage());
        }
    }
    
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}