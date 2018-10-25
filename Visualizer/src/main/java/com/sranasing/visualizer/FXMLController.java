package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import TORCS_Sensors.Sensors_Message.Sensors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfoenix.controls.JFXTextField;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
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

    //Initialize the context of the device
    private ZMQ.Context context;

    //Suscriber socket to receive updates
    private ZMQ.Socket subscriber;

    //Request socket used for synchronizing
    private ZMQ.Socket sync_socket;

    //task used to update the sensors
    private Task updateStatusTask;

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
    }

    /**
     * Get the task that handles the updating of the fields
     *
     * @return The task object
     */
    private Task<?> getTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
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
                        updateSensors(message);
                    } catch (InvalidProtocolBufferException ex) {
                        System.out.println("Something went wrong");
                        //Invalid data has been through the port
                    }
                }
                return null;
            }
        };
    }

    /**
     * Update sensors
     *
     * @param message message object updated with the last message
     */
    private void updateSensors(Sensors message) {
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
        updateStatusTask = getTask();
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
