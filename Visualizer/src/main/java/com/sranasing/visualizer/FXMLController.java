package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import TORCS_Sensors.Sensors_Message.Sensors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfoenix.controls.JFXToggleButton;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import java.net.URL;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import org.controlsfx.control.SegmentedBar;
import org.zeromq.ZMQ;

public class FXMLController implements Initializable {

    @FXML
    private LineChart<Number, Number> plot;

    @FXML
    private VBox timelineList;

    @FXML
    private FlowPane frontFlowPane;

    //Initialize the context of the device
    private ZMQ.Context context;

    //Suscriber socket to receive updates
    private ZMQ.Socket subscriber;

    //Request socket used for synchronizing
    private ZMQ.Socket sync_socket;

    //task used to update the sensors
    private Task updateStatusTask;

    //List containing all the available Sensors
    private ObservableList<Series<Number, Number>> sensorDataList;

    private SensorData accel;
    private SensorData braking;
    private SensorData gear;
    private SensorData steerPredicted;
    private SensorData steerExpected;
    private SensorData angle;
    private SensorData cuLapTime;
    private SensorData distFromStart;
    private SensorData totalDistFromStart;
    private SensorData distRaced;
    private SensorData lastLapTime;
    private SensorData rpm;
    private SensorData speedX;
    private SensorData speedY;
    private SensorData distToMiddle;
    private SensorData fps;

    private HashMap<String, SensorData> sensorMap;

    //Constants
    private static final String SUBSCRIBER_PORT = "tcp://localhost:5555";
    private static final String SYNC_PORT = "tcp://localhost:5556";
    private static final String HANDSHAKE_INIT = "Init";
    private static final String HANDSHAKE_ACK = "SyncAck";

    private Tile gaugeTile;
    private Tile barGaugeTile;
    private Tile timeTile;
    private Tile gaugeSparkLineTile;

    private static final double TILE_WIDTH = 150;
    private static final double TILE_HEIGHT = 150;

    private float previousVal = 0;

    SegmentedBar bar1;

    /**
     * Initialize the controller
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDataStuctures();

        testSegmentedBar();

        gaugeTile = TileBuilder.create()
                .skinType(SkinType.GAUGE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .minValue(0)
                .maxValue(300)
                .title("Gauge Tile")
                .unit("FPS")
                .threshold(75)
                .build();
        frontFlowPane.getChildren().add(gaugeTile);

        barGaugeTile = TileBuilder.create()
                .skinType(SkinType.BAR_GAUGE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .minValue(0)
                .maxValue(200)
                .startFromZero(true)
                .threshold(80)
                .thresholdVisible(true)
                .title("BarGauge Tile")
                .unit("F")
                .text("Whatever text")
                .gradientStops(new Stop(0, Bright.BLUE),
                        new Stop(0.1, Bright.BLUE_GREEN),
                        new Stop(0.2, Bright.GREEN),
                        new Stop(0.3, Bright.GREEN_YELLOW),
                        new Stop(0.4, Bright.YELLOW),
                        new Stop(0.5, Bright.YELLOW_ORANGE),
                        new Stop(0.6, Bright.ORANGE),
                        new Stop(0.7, Bright.ORANGE_RED),
                        new Stop(0.8, Bright.RED),
                        new Stop(1.0, Dark.RED))
                .strokeWithGradient(true)
                .animated(true)
                .build();
        frontFlowPane.getChildren().add(barGaugeTile);

        timeTile = TileBuilder.create()
                .skinType(SkinType.TIME)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Time Tile")
                .text("Whatever text")
                .duration(LocalTime.of(1, 22, 50))
                .description("Average reply time")
                .textVisible(true)
                .build();
        frontFlowPane.getChildren().add(timeTile);
        timeTile.setValue(123.0);

        gaugeSparkLineTile = TileBuilder.create()
                .skinType(SkinType.GAUGE_SPARK_LINE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("GaugeSparkLine")
                .animated(true)
                .maxValue(200)
                .textVisible(false)
                .averagingPeriod(25)
                .autoReferenceValue(true)
                .barColor(Tile.YELLOW_ORANGE)
                .barBackgroundColor(Color.rgb(255, 255, 255, 0.1))
                .sections(new eu.hansolo.tilesfx.Section(0, 33, Tile.LIGHT_GREEN),
                        new eu.hansolo.tilesfx.Section(33, 67, Tile.YELLOW),
                        new eu.hansolo.tilesfx.Section(67, 100, Tile.LIGHT_RED))
                .sectionsVisible(true)
                .highlightSections(true)
                .strokeWithGradient(true)
                .gradientStops(new Stop(0.0, Tile.LIGHT_GREEN),
                        new Stop(0.33, Tile.LIGHT_GREEN),
                        new Stop(0.33, Tile.YELLOW),
                        new Stop(0.67, Tile.YELLOW),
                        new Stop(0.67, Tile.LIGHT_RED),
                        new Stop(1.0, Tile.LIGHT_RED))
                .build();
        frontFlowPane.getChildren().add(gaugeSparkLineTile);
    }

    private void testSegmentedBar() {
        bar1 = new SegmentedBar();
        bar1.setOrientation(Orientation.HORIZONTAL);
        bar1.setMinHeight(15);
        bar1.setPrefHeight(15);
        bar1.setMaxHeight(15);
        timelineList.getChildren().add(bar1);
    }

    private void initializeDataStuctures() {
        sensorMap = new HashMap<>();

        accel = new SensorData("Acceleration");
        sensorMap.put("accel", accel);
        braking = new SensorData("Braking");
        sensorMap.put("braking", braking);
        gear = new SensorData("Gear");
        sensorMap.put("gear", gear);
        steerPredicted = new SensorData("Steering Predicted");
        sensorMap.put("steerPredicted", steerPredicted);
        steerExpected = new SensorData("Steering Expected");
        sensorMap.put("steerExpected", steerExpected);
        angle = new SensorData("Angle");
        sensorMap.put("angle", angle);
        cuLapTime = new SensorData("Current Lap Time");
        sensorMap.put("cuLapTime", cuLapTime);
        distFromStart = new SensorData("Distance From The Start");
        sensorMap.put("distFromStart", distFromStart);
        totalDistFromStart = new SensorData("Total Distance From The Start");
        sensorMap.put("totalDistFromStart", totalDistFromStart);
        distRaced = new SensorData("Distance Raced");
        sensorMap.put("distRaced", distRaced);
        lastLapTime = new SensorData("Last Lap Time");
        sensorMap.put("lastLapTime", lastLapTime);
        rpm = new SensorData("RPM");
        sensorMap.put("rpm", rpm);
        speedX = new SensorData("Speed X");
        sensorMap.put("speedX", speedX);
        speedY = new SensorData("Speed Y");
        sensorMap.put("speedY", speedY);
        distToMiddle = new SensorData("Distance To The Middle");
        sensorMap.put("distToMiddle", distToMiddle);
        fps = new SensorData("FPS");
        sensorMap.put("fps", fps);
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
            if (message.getDistFromStart() < previousVal) { //new lap started
                System.out.println("New Lap Started");
                sensorMap.forEach((t, u) -> {
                    u.newLap();
                });
            }
            addData(message);
        }
        previousVal = message.getDistFromStart();
    }

    private void addData(Sensors message) {
        accel.addData(message.getAccel());
        braking.addData(message.getBraking());
        gear.addData((float) message.getGear());
        steerPredicted.addData(message.getSteerPredicted());
        steerExpected.addData(message.getSteerExpected());
        angle.addData(message.getAngle());
        cuLapTime.addData(message.getCuLapTime());
        distFromStart.addData(message.getDistFromStart());
        totalDistFromStart.addData(message.getTotalDistFromStart());
        distRaced.addData(message.getDistRaced());
        lastLapTime.addData(message.getLastLapTime());
        rpm.addData(message.getRpm());
        speedX.addData(message.getSpeedX());
        speedY.addData(message.getSpeedY());
        distToMiddle.addData(message.getDistToMiddle());
        fps.addData(message.getFps());

        gaugeTile.setValue(message.getFps());//fps
        barGaugeTile.setValue(message.getSpeedX());//rps
        gaugeSparkLineTile.setValue(message.getSpeedX());//speedX
        //bar1.getSegments().add(new SegmentedBar.Segment(1, String.valueOf(message.getSteer())));
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
        initializeDataStuctures();
        plot.getData().clear();
    }

    @FXML
    private void addLine(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        if (toggleButton.isSelected()) {
            plot.getData().add(sensorMap.get(toggleButton.getAccessibleText()).goLive());
        } else {
            plot.getData().remove(sensorMap.get(toggleButton.getAccessibleText()).goOffline());
        }
    }
}
