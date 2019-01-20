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

    private SensorData steerPredicted;
    private SensorData steerExpected;
    private SensorData angle;
    private SensorData distToMiddle;
    private SensorData absError;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDataStuctures();
    }

    private void initializeDataStuctures() {
        sensorMap = new HashMap<>();

        angle = new SensorData("Angle");
        sensorMap.put("angle", angle);

        distToMiddle = new SensorData("Distance To The Middle");
        sensorMap.put("distToMiddle", distToMiddle);

        steerPredicted = new SensorData("Steering Predicted");
        sensorMap.put("steerPredicted", steerPredicted);

        steerExpected = new SensorData("Steering Expected");
        sensorMap.put("steerExpected", steerExpected);

        absError = new SensorData("Absolute Error");
        sensorMap.put("absError", absError);
    }

    public void addData(Sensors_Message.Sensors message) {
        angle.addData(message.getAngle());
        steerPredicted.addData(message.getSteerPredicted());
        steerExpected.addData(message.getSteerExpected());
        distToMiddle.addData(message.getDistToMiddle());
        absError.addData(Math.abs(message.getSteerPredicted() - message.getSteerExpected()));
    }

    public void clear() {
        plot.getData().clear();
    }

    public void finishLap() {
        for (SensorData value : sensorMap.values()) {
            value.finishLap();
        }
    }

    public float[][] getDataList() {
        return new float[][]{
            steerPredicted.getData(),
            steerExpected.getData()
        };
    }

    @FXML
    private void addLine(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        toggleButton.setDisable(true);
        if (toggleButton.isSelected()) {
            plot.getData().add(sensorMap.get(toggleButton.getAccessibleText()).addToGraph());
        } else {
            plot.getData().remove(sensorMap.get(toggleButton.getAccessibleText()).removeFromGraph());
        }
        toggleButton.setDisable(false);
    }

}
