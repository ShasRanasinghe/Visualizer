package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import TORCS_Sensors.Sensors_Message.Sensors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfoenix.controls.JFXTextField;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.Notifications;
import org.zeromq.ZMQ;

public class FXMLController implements Initializable {

    @FXML
    private GridPane gridPane;

    @FXML
    private JFXTextField accel;

    @FXML
    private JFXTextField breaking;

    @FXML
    private JFXTextField gear;

    @FXML
    private JFXTextField steer;

    @FXML
    private JFXTextField meta;

    @FXML
    private JFXTextField clutch;

    @FXML
    private JFXTextField focus;

    @FXML
    private JFXTextField angle;

    @FXML
    private JFXTextField cuLapTime;

    @FXML
    private JFXTextField damage;

    @FXML
    private JFXTextField distFromStart;

    @FXML
    private JFXTextField totalDistFromStart;

    @FXML
    private JFXTextField distRaced;

    @FXML
    private JFXTextField fuel;

    @FXML
    private JFXTextField lastLapTime;

    @FXML
    private JFXTextField racePos;

    @FXML
    private JFXTextField rpm;

    @FXML
    private JFXTextField speedX;

    @FXML
    private JFXTextField speedY;

    @FXML
    private JFXTextField speedZ;

    @FXML
    private JFXTextField distToMiddle;

    @FXML
    private JFXTextField posZ;

    @FXML
    private JFXTextField fps;

    @FXML
    private JFXTextField count;

    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private ListSelectionView<Series<Number, Number>> sensorSelector;

    @FXML
    private LineChart<Number, Number> plot;

    //Initialize the context of the device
    private ZMQ.Context context;

    //Suscriber socket to receive updates
    private ZMQ.Socket subscriber;

    //Request socket used for synchronizing
    private ZMQ.Socket sync_socket;

    //task used to update the sensors
    private Task updateStatusTask;

    //data series used in the line chart
    private Series<Number, Number> series;

    //List containing all the available Sensors
    private ObservableList<Series<Number, Number>> sensorDataList;

    //Define the series used for the sensors
    private Series<Number, Number> accel_Data;
    private Series<Number, Number> breaking_Data;
    private Series<Number, Number> gear_Data;
    private Series<Number, Number> steer_Data;
    private Series<Number, Number> angle_Data;
    private Series<Number, Number> cuLapTime_Data;
    private Series<Number, Number> distFromStart_Data;
    private Series<Number, Number> totalDistFromStart_Data;
    private Series<Number, Number> distRaced_Data;
    private Series<Number, Number> lastLapTime_Data;
    private Series<Number, Number> rpm_Data;
    private Series<Number, Number> speedX_Data;
    private Series<Number, Number> speedY_Data;
    private Series<Number, Number> distToMiddle_Data;
    private Series<Number, Number> fps_Data;

    //Constants
    private static final String SUBSCRIBER_PORT = "tcp://localhost:5555";
    private static final String SYNC_PORT = "tcp://localhost:5556";
    private static final String HANDSHAKE_INIT = "Init";
    private static final String HANDSHAKE_ACK = "SyncAck";

    /**
     * Initialize the controller
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        intitializeSeries();

        initializeListView();

        //Test Chart
        series = new Series<>();
        chart.getData().add(series);
    }

    /**
     * Initialize the series that would hold the sensor data to be plotted
     */
    private void intitializeSeries() {
        accel_Data = new Series<>();
        accel_Data.setName("Acceleration");
        breaking_Data = new Series<>();
        breaking_Data.setName("Breaking");
        gear_Data = new Series<>();
        gear_Data.setName("Gear");
        steer_Data = new Series<>();
        steer_Data.setName("Steering");
        angle_Data = new Series<>();
        angle_Data.setName("Angle");
        cuLapTime_Data = new Series<>();
        cuLapTime_Data.setName("Current Lap Time");
        distFromStart_Data = new Series<>();
        distFromStart_Data.setName("Distance From Start");
        totalDistFromStart_Data = new Series<>();
        totalDistFromStart_Data.setName("Total Distance From Start");
        distRaced_Data = new Series<>();
        distRaced_Data.setName("Distance Raced");
        lastLapTime_Data = new Series<>();
        lastLapTime_Data.setName("Last Lap Time");
        rpm_Data = new Series<>();
        rpm_Data.setName("RPM");
        speedX_Data = new Series<>();
        speedX_Data.setName("SpeedX");
        speedY_Data = new Series<>();
        speedY_Data.setName("SpeedY");
        distToMiddle_Data = new Series<>();
        distToMiddle_Data.setName("Distance To Middle");
        fps_Data = new Series<>();
        fps_Data.setName("FPS");
    }

    private void initializeListView() {
        sensorDataList = FXCollections.observableArrayList();
        sensorDataList.addAll(
                accel_Data,
                breaking_Data,
                gear_Data,
                steer_Data,
                angle_Data,
                cuLapTime_Data,
                distFromStart_Data,
                totalDistFromStart_Data,
                distRaced_Data,
                lastLapTime_Data,
                rpm_Data,
                speedX_Data,
                speedY_Data,
                distToMiddle_Data,
                fps_Data);

        sensorSelector.setTargetItems(plot.getData());
        sensorSelector.setSourceItems(sensorDataList);
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
        accel_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getAccel()));
        breaking_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getBreaking()));
        gear_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getGear()));
        steer_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getSteer()));
        angle_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getAngle()));
        cuLapTime_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getCuLapTime()));
        distFromStart_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getDistFromStart()));
        totalDistFromStart_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getTotalDistFromStart()));
        distRaced_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getDistRaced()));
        lastLapTime_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getLastLapTime()));
        rpm_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getRpm()));
        speedX_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getSpeedX()));
        speedY_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getSpeedY()));
        distToMiddle_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getDistToMiddle()));
        fps_Data.getData().add(new XYChart.Data<>(message.getCount(), message.getFps()));

        accel.setText(Float.toString(message.getAccel()));
        breaking.setText(Float.toString(message.getBreaking()));
        gear.setText(Integer.toString(message.getGear()));
        steer.setText(Float.toString(message.getSteer()));
        meta.setText(Integer.toString(message.getMeta()));
        clutch.setText(Float.toString(message.getClutch()));
        focus.setText(Integer.toString(message.getFocus()));
        angle.setText(Float.toString(message.getAngle()));
        cuLapTime.setText(Float.toString(message.getCuLapTime()));
        damage.setText(Integer.toString(message.getDamage()));
        distFromStart.setText(Float.toString(message.getDistFromStart()));
        totalDistFromStart.setText(Float.toString(message.getTotalDistFromStart()));
        distRaced.setText(Float.toString(message.getDistRaced()));
        fuel.setText(Float.toString(message.getFuel()));
        lastLapTime.setText(Float.toString(message.getLastLapTime()));
        racePos.setText(Integer.toString(message.getRacePos()));
        rpm.setText(Float.toString(message.getRpm()));
        speedX.setText(Float.toString(message.getSpeedX()));
        speedY.setText(Float.toString(message.getSpeedY()));
        speedZ.setText(Float.toString(message.getSpeedZ()));
        distToMiddle.setText(Float.toString(message.getDistToMiddle()));
        posZ.setText(Float.toString(message.getPosZ()));
        fps.setText(Float.toString(message.getFps()));
        count.setText(Integer.toString(message.getCount()));
    }

    /**
     * Create a notification with the given text
     *
     * @param text Text to be displayed in the notification
     */
    private void notification(String text) {
        Notifications.create()
                .owner(gridPane)
                .text(text)
                .hideAfter(Duration.seconds(3))
                .position(Pos.BOTTOM_RIGHT)
                .showInformation();
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

        //notification("Successfully Connected");
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

        notification("Connection Reset\nReconnect to continue");
    }

    /**
     * Clear the fields
     */
    @FXML
    private void clear() {
        gridPane.getChildren().stream().filter((node) -> (node instanceof JFXTextField)).forEachOrdered((node) -> {
            ((JFXTextField) node).clear();
        });
    }
}
