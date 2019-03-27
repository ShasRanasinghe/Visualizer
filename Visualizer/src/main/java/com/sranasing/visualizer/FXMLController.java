package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import TORCS_Sensors.Sensors_Message.Sensors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.zeromq.ZMQ;

public class FXMLController implements Initializable {

    @FXML
    private HBox root;

    @FXML
    private JFXComboBox<String> trackList, modelList;

    @FXML
    private VBox graphList;

    @FXML
    private JFXButton simulation_button, load_data_button, settings_button;

    @FXML
    private ScrollPane load_data_pane;

    @FXML
    private StackPane simulation_pane;

    @FXML
    private AnchorPane settings_pane, running_offline_pane, running_online_pane;

    @FXML
    private BorderPane run_pane, initializing_pane;

    @FXML
    private JFXTextField main_sim_folder_abspath;

    @FXML
    private JFXButton backButton;

    @FXML
    private JFXButton stopButton;

    //Initialize the context of the device
    private ZMQ.Context context;

    //Suscriber socket to receive updates
    private ZMQ.Socket subscriber;

    //Request socket used for synchronizing
    private ZMQ.Socket sync_socket;

    //task used to update the sensors
    private Task updateStatusTask;

    private LapGraphController graphController;

    private List<LapGraphController> controllerList;

    //Constants
    private static final String SUBSCRIBER_PORT = "tcp://localhost:5555";
    private static final String SYNC_PORT = "tcp://localhost:5556";
    private static final String HANDSHAKE_INIT = "Init";
    private static final String HANDSHAKE_ACK = "SyncAck";

    private float previousVal = 0;

    private File torcsFolder;
    private String runtimedFolderPath;
    private String tracksFolderPath;
    private String modelsFolderPath;
    private String quickRaceFilePath;

    private Task<Boolean> torcsTask;

    /**
     * Initialize the controller
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createLapGraph();
        controllerList = new ArrayList<>();

        run_pane.toFront();
        simulation_pane.toFront();
    }

    private void createLapGraph() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LapGraph.fxml"));
            AnchorPane graph = loader.load();
            graphController = loader.getController();
            graphList.getChildren().add(graph);
        } catch (IOException ex) {
            System.out.println("There was an issue loading the graph FXML file");
        }
    }

    private boolean performHandshake() {
        //Create the context
        context = ZMQ.context(1);

        //create the subscrier socket
        subscriber = context.socket(ZMQ.SUB);
        subscriber.setRcvHWM(0);
        //Connect our subscriber socket with a PUB/SUB pair
        subscriber.connect(SUBSCRIBER_PORT);
        //subscribe to empty string to get all messages
        subscriber.subscribe("".getBytes());

        //Create the request socket
        sync_socket = context.socket(ZMQ.REQ);
        //Create synchronize port with a REQ/REP pair
        sync_socket.connect(SYNC_PORT);

        //wait for handshake
        while (true) {
            String recvString = subscriber.recvStr(0);
            if (recvString.equals(HANDSHAKE_INIT)) {
                sync_socket.send(HANDSHAKE_ACK);
                return true;
            }
        }
    }

    /**
     * Get the task that handles the updating of the fields
     *
     * @return The task object
     */
    private Task<Sensors> getPortListenerTask() {
        Task task = new Task<Sensors>() {
            @Override
            protected Sensors call() throws Exception {
                if (performHandshake()) {
                    updateMessage("Connection Complete");
                } else {
                    return null;
                }

                Sensors message;
                while (true) {
                    if (isCancelled()) {
                        break;
                    }

                    byte[] update = subscriber.recv();

                    if (Arrays.equals(update, "END".getBytes())) {
                        System.out.println("Ending updates");
                        break;
                    }

                    try {
                        message = Sensors_Message.Sensors.parseFrom(update);
                        updateValue(message);
                    } catch (InvalidProtocolBufferException ex) {
                        System.out.println("Something went wrong");
                        //Invalid data has been sent through the port
                    }
                }
                return null;
            }
        };

        task.valueProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            updateSensors((Sensors) newValue);
        });

        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            running_online_pane.toFront();
        });

        return task;
    }

    /**
     * Update sensors
     *
     * @param message message object updated with the last message
     */
    private void updateSensors(Sensors message) {
        if (message.getTotalDistFromStart() > 0) { //race has started
            if (message.getCuLapTime() < previousVal) { //new lap started
                System.out.println("New Lap Started");
                graphController.finishLap();
                controllerList.add(graphController);
                createLapGraph();
            }
            addData(message);
        }
        previousVal = message.getCuLapTime();
    }

    private void addData(Sensors message) {
        graphController.addData(message);
    }

    private Task getTORCSTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                ProcessBuilder builder = new ProcessBuilder(
                        "cmd.exe", "/c",
                        String.format("cd \"%s\\runtimed\" && wtorcs.exe -m quickrace.xml", torcsFolder.getAbsolutePath()));
                builder.redirectErrorStream(true);
                Process p = builder.start();

                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while (true) {
                        if (isCancelled()) {
                            p.destroy();
                            break;
                        }
                        line = r.readLine();
                        if (line == null) {;
                            break;
                        }
                        System.out.println(line);
                    }
                    return true;
                }
            }
        };
    }

    /**
     * Create context, set up connections, and perform handshake
     */
    private void connect() {
        //Get updates from publisher
        updateStatusTask = getPortListenerTask();
        Thread th = new Thread(updateStatusTask);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Close the sockets and connections
     */
    private void closeConnection() {
        if (updateStatusTask.isRunning()) {
            updateStatusTask.cancel(false);
        }
        subscriber.close();
        sync_socket.close();
        context.term();
    }

    public void killTORCS() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM wtorcs.exe");
        } catch (IOException ex) {
            System.out.println("Exception thrown here");
        }
    }

    /**
     * Clear the fields
     */
    @FXML
    private void stop() {
        killTORCS();

        if (torcsTask != null && torcsTask.isRunning()) {
            torcsTask.cancel(true);
        }

        graphController.finishLap();
        closeConnection();
        controllerList.add(graphController);
        backButton.setVisible(true);
        stopButton.setVisible(false);

        //Save data
        for (int i = 0; i < controllerList.size(); i++) {
            saveData(controllerList.get(i).getDataList(), i);
        }

    }

    @FXML
    private void loadData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Lap Data");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Lap Data Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());
        List<float[]> data = Utils.loadCSV(selectedFile);

        System.out.println("Predicted: " + Arrays.toString(data.get(0)));
        System.out.println("Expected: " + Arrays.toString(data.get(1)));
    }

    @FXML
    private void saveData(float[][] lapData, int lap) {
        Utils.saveToCSV(lapData, trackList.getValue(), lap);
    }

    @FXML
    private void selectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose TORCS Folder");
        torcsFolder = directoryChooser.showDialog(root.getScene().getWindow());

        if (torcsFolder == null) {
            //No Directory selected
        } else {
            String torcsPath = torcsFolder.getAbsolutePath();
            main_sim_folder_abspath.setText(torcsPath);
            runtimedFolderPath = torcsPath + "\\runtimed";
            tracksFolderPath = runtimedFolderPath + "\\tracks";
            modelsFolderPath = torcsPath + "\\networks";
            quickRaceFilePath = torcsPath + "\\runtimed\\quickrace.xml";

            File tracksFolder = new File(tracksFolderPath);
            trackList.getItems().clear();
            for (File trackType : tracksFolder.listFiles()) {
                for (File track : trackType.listFiles()) {
                    trackList.getItems().add(String.format("%s--%s", trackType.getName(), track.getName()));
                }
            }

            File modelsFolder = new File(modelsFolderPath);
            modelList.getItems().clear();
            modelList.getItems().setAll(modelsFolder.list());
        }
    }

    @FXML
    private void run() {
        initializing_pane.toFront();

        runRace();
        if (torcsFolder == null) {
            System.out.println("No Directory Selected");
        } else {
            torcsTask = getTORCSTask();

            Thread th = new Thread(torcsTask);
            th.setDaemon(true);
            th.start();
            connect();
            backButton.setVisible(false);
            stopButton.setVisible(true);
        }
    }

    private void runRace() {
        //Update track
        try {
            String[] trackDetails = trackList.getValue().split("--");
            Utils.updateTrack(trackDetails[1], trackDetails[0], quickRaceFilePath);
        } catch (Exception ex) {
            //Create new exception message
            System.out.println("Had trouble with this");
        }

        //Remove existing model files (if any)
        Utils.removeExistingModelFiles(runtimedFolderPath);

        try {
            //Move Model File
            Utils.moveModelFiles(modelList.getValue(), modelsFolderPath, runtimedFolderPath);
        } catch (Exception ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleMainMenuButtons(ActionEvent event) {
        if (event.getSource() == simulation_button) {
            simulation_pane.toFront();
        } else if (event.getSource() == load_data_button) {
            load_data_pane.toFront();
        } else if (event.getSource() == settings_button) {
            settings_pane.toFront();
        }
    }

    @FXML
    void returnToRunPane(ActionEvent event) {
        graphList.getChildren().clear();
        createLapGraph();
        controllerList = new ArrayList<>();

        run_pane.toFront();
        simulation_pane.toFront();
    }

}
