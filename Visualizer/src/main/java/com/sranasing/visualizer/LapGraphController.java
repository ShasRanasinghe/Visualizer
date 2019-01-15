/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sranasing.visualizer;

import TORCS_Sensors.Sensors_Message;
import com.jfoenix.controls.JFXToggleButton;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;

/**
 * FXML Controller class
 *
 * @author shast
 */
public class LapGraphController implements Initializable {

    @FXML
    private LineChart<Number, Number> plot;

    private HashMap<String, SensorData> sensorMap;

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

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDataStuctures();
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

    public void addData(Sensors_Message.Sensors message) {
        accel.addData(message.getAccel());
        braking.addData(message.getBraking());
        gear.addData((float) message.getGear());
        steerPredicted.addData(message.getSteerPredicted());
        steerExpected.addData(message.getSteerExpected());
        angle.addData(message.getAngle());
        distFromStart.addData(message.getDistFromStart());
        totalDistFromStart.addData(message.getTotalDistFromStart());
        distRaced.addData(message.getDistRaced());
        rpm.addData(message.getRpm());
        speedX.addData(message.getSpeedX());
        speedY.addData(message.getSpeedY());
        distToMiddle.addData(message.getDistToMiddle());
        fps.addData(message.getFps());
    }

    public void clear() {
        plot.getData().clear();
    }

    @FXML
    private void addLine(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        if (toggleButton.isSelected()) {
            plot.getData().add(sensorMap.get(toggleButton.getAccessibleText()).addToGraph());
        } else {
            plot.getData().remove(sensorMap.get(toggleButton.getAccessibleText()).removeFromGraph());
        }
    }

}
