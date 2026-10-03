package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {

    private final TravelTypeDAO travelTypeDAO = new TravelTypeDAO();
    private final TravelRecordDAO travelRecordDAO = new TravelRecordDAO();

    private ResourceBundle bundle;
    private Stage stage;

    // UI elements to update dynamically upon language change
    private Label speedLabel;
    private Label distanceLabel;
    private Label typeLabel;
    private Label savedRecordsLabel;
    private TextField speedField;
    private TextField distanceField;
    private ComboBox<TravelType> typeComboBox;
    private ComboBox<String> languageComboBox;
    private Button calcButton;
    private Label resultLabel;
    private TableView<TravelRecord> tableView;

    private TableColumn<TravelRecord, Number> idCol;
    private TableColumn<TravelRecord, Number> speedCol;
    private TableColumn<TravelRecord, Number> distCol;
    private TableColumn<TravelRecord, Number> timeCol;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Initialize default bundle (English)
        setLocale(new Locale("en"));

        // Top bar with language selector
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("English", "فارسی (Farsi)", "中文 (Chinese)", "日本語 (Japanese)", "हिन्दी (Hindi)");
        languageComboBox.getSelectionModel().select("English");
        languageComboBox.setOnAction(e -> handleLanguageChange());

        HBox topBar = new HBox(10, new Label("Language / زبان / 语言:"), languageComboBox);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(15));

        speedLabel = new Label();
        distanceLabel = new Label();
        typeLabel = new Label();
        savedRecordsLabel = new Label();

        speedField = new TextField();
        distanceField = new TextField();
        typeComboBox = new ComboBox<>();
        loadTravelTypes();

        calcButton = new Button();
        resultLabel = new Label();

        form.add(speedLabel, 0, 0);
        form.add(speedField, 1, 0);
        form.add(distanceLabel, 0, 1);
        form.add(distanceField, 1, 1);
        form.add(typeLabel, 0, 2);
        form.add(typeComboBox, 1, 2);
        form.add(calcButton, 1, 3);
        form.add(resultLabel, 1, 4);

        tableView = buildTableView();
        loadRecords();

        calcButton.setOnAction(e -> handleCalculateAndSave());

        VBox root = new VBox(15, topBar, form, savedRecordsLabel, tableView);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_LEFT);

        // Apply initial strings
        updateTexts(root);

        stage.setScene(new Scene(root, 550, 550));
        stage.show();
    }

    private void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    private void handleLanguageChange() {
        String selected = languageComboBox.getValue();
        Locale locale = switch (selected) {
            case "فارسی (Farsi)" -> new Locale("fa");
            case "中文 (Chinese)" -> new Locale("zh");
            case "日本語 (Japanese)" -> new Locale("ja");
            case "हिन्दी (Hindi)" -> new Locale("hi");
            default -> new Locale("en");
        };

        setLocale(locale);
        updateTexts((VBox) stage.getScene().getRoot());
    }

    private void updateTexts(VBox root) {
        stage.setTitle(bundle.getString("app.title"));
        speedLabel.setText(bundle.getString("label.speed"));
        distanceLabel.setText(bundle.getString("label.distance"));
        typeLabel.setText(bundle.getString("label.travelType"));
        calcButton.setText(bundle.getString("button.calculate"));
        savedRecordsLabel.setText(bundle.getString("label.savedRecords"));

        idCol.setText(bundle.getString("col.id"));
        speedCol.setText(bundle.getString("col.speed"));
        distCol.setText(bundle.getString("col.distance"));
        timeCol.setText(bundle.getString("col.time"));

        // Adjust text orientation for Right-to-Left languages (Farsi)
        if (bundle.getLocale().getLanguage().equals("fa")) {
            root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            root.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }

    private void loadTravelTypes() {
        try {
            List<TravelType> types = travelTypeDAO.getAllTypes();
            typeComboBox.getItems().addAll(types);
            if (!types.isEmpty()) {
                typeComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError(bundle.getString("err.loadTypes") + " " + e.getMessage());
        }
    }

    private void handleCalculateAndSave() {
        try {
            double speed = Double.parseDouble(speedField.getText());
            double distance = Double.parseDouble(distanceField.getText());
            TravelType selectedType = typeComboBox.getValue();

            if (selectedType == null) {
                showError(bundle.getString("err.selectType"));
                return;
            }

            double time = TravelCalculator.timeCal(speed, distance);
            resultLabel.setText(String.format(bundle.getString("msg.timeResult"), time));

            TravelRecord record = new TravelRecord(speed, distance, time, selectedType.getId());
            travelRecordDAO.save(record);

            loadRecords();
            speedField.clear();
            distanceField.clear();

        } catch (NumberFormatException ex) {
            showError(bundle.getString("err.numeric"));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError(bundle.getString("err.db") + " " + ex.getMessage());
        }
    }

    private TableView<TravelRecord> buildTableView() {
        TableView<TravelRecord> table = new TableView<>();

        idCol = new TableColumn<>();
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));

        speedCol = new TableColumn<>();
        speedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getSpeed()));

        distCol = new TableColumn<>();
        distCol.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getDistance()));

        timeCol = new TableColumn<>();
        timeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getTimeTaken()));

        table.getColumns().addAll(idCol, speedCol, distCol, timeCol);
        return table;
    }

    private void loadRecords() {
        try {
            List<TravelRecord> records = travelRecordDAO.getAllRecords();
            tableView.getItems().setAll(records);
        } catch (SQLException e) {
            showError(bundle.getString("err.loadRecords") + " " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}