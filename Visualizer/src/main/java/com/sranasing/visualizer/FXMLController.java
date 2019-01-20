package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import TORCS_Sensors.Sensors_Message.Sensors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfoenix.controls.JFXComboBox;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.zeromq.ZMQ;

public class FXMLController implements Initializable {

    @FXML
    private VBox root;

    @FXML
    private JFXComboBox<String> trackList;

    @FXML
    private JFXComboBox<String> modelList;

    @FXML
    private VBox graphList;

    //Initialize the context of the device
    private ZMQ.Context context;

    //Suscriber socket to receive updates
    private ZMQ.Socket subscriber;

    //Request socket used for synchronizing
    private ZMQ.Socket sync_socket;

    //task used to update the sensors
    private Task updateStatusTask;

    private LapGraphController graphController;

    private List<float[][]> lapData;

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
        lapData = new ArrayList<>();
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

    /**
     * Get the task that handles the updating of the fields
     *
     * @return The task object
     */
    private Task<Sensors> getPortListenerTask() {
        Task task = new Task<Sensors>() {
            @Override
            protected Sensors call() throws Exception {
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
                        //Invalid data has been through the port
                    }
                }
                return null;
            }
        };

        task.valueProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            updateSensors((Sensors) newValue);
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
                lapData.add(graphController.getDataList());
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
    @FXML
    private void connect() {
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
                break;
            }
        }

        //Get updates from publisher
        updateStatusTask = getPortListenerTask();
        Thread th = new Thread(updateStatusTask);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Close the sockets and connections
     */
    @FXML
    private void reset() {
        if (updateStatusTask.isRunning()) {
            updateStatusTask.cancel(false);
        }
        subscriber.close();
        sync_socket.close();
        context.term();
    }

    /**
     * Clear the fields
     */
    @FXML
    private void clear() {
        try {
            //graphController.clear();
            Runtime.getRuntime().exec("taskkill /F /IM wtorcs.exe");
        } catch (IOException ex) {
            System.out.println("Exception thrown here");
        }

        if (torcsTask != null && torcsTask.isRunning()) {
            torcsTask.cancel(true);
        }

        graphController.finishLap();
        lapData.add(graphController.getDataList());
    }

    @FXML
    private void loadData() {
        //runRace();

        List<float[]> data = Utils.loadCSV("dataFile-Lap 1.csv");

        System.out.println(Arrays.toString(data.get(0)));
        System.out.println(Arrays.toString(data.get(1)));
    }

    @FXML
    private void saveData() {
        Utils.saveToCSV(lapData, "dataFile");
    }

    @FXML
    private void selectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        torcsFolder = directoryChooser.showDialog(root.getScene().getWindow());

        if (torcsFolder == null) {
            //No Directory selected
        } else {
            String torcsPath = torcsFolder.getAbsolutePath();
            runtimedFolderPath = torcsPath + "\\runtimed";
            tracksFolderPath = torcsPath + "\\tracks";
            modelsFolderPath = torcsPath + "\\networks";
            quickRaceFilePath = torcsPath + "\\runtimed\\quickrace.xml";

            File tracksFolder = new File(tracksFolderPath);
            trackList.getItems().clear();
            for (File trackType : tracksFolder.listFiles()) {
                for (File track : trackType.listFiles()) {
                    trackList.getItems().add(String.format("%s-->%s", trackType.getName(), track.getName()));
                }
            }

            File modelsFolder = new File(modelsFolderPath);
            modelList.getItems().clear();
            modelList.getItems().setAll(modelsFolder.list());
        }
    }

    @FXML
    private void run() {
        runRace();
        if (torcsFolder == null) {
            System.out.println("No Directory Selected");
        } else {
            torcsTask = getTORCSTask();

            Thread th = new Thread(torcsTask);
            th.setDaemon(true);
            th.start();
            connect();
        }
    }

    private void runRace() {
        //Update track
        try {
            String[] trackDetails = trackList.getValue().split("-->");
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

        //run race
        //run();
    }

}
