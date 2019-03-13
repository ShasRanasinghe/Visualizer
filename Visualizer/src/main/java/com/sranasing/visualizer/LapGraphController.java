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
    private JFXToggleButton expectedSteering;

    @FXML
    private JFXToggleButton predictedSteering;

    @FXML
    private LineChart<Number, Number> plot;

    private HashMap<String, SensorData> sensorMap;

    private SensorData steerPredicted;
    private SensorData steerExpected;
    private SensorData angle;
    private SensorData distToMiddle;
    private SensorData absError;

    private float errorSum = 0;
    private long count = 0;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDataStuctures();

        addLine(new ActionEvent(expectedSteering, expectedSteering));
        addLine(new ActionEvent(predictedSteering, predictedSteering));
    }

    private void initializeDataStuctures() {
        sensorMap = new HashMap<>();

        angle = new SensorData("Angle");
        sensorMap.put("angle", angle);

        distToMiddle = new SensorData("Distance To The Middle");
        sensorMap.put("distToMiddle", distToMiddle);

        steerPredicted = new SensorData("Predicted Steering");
        sensorMap.put("steerPredicted", steerPredicted);

        steerExpected = new SensorData("Expected Steering ");
        sensorMap.put("steerExpected", steerExpected);

        absError = new SensorData("Absolute Error");
        sensorMap.put("absError", absError);
    }

    public void addData(Sensors_Message.Sensors message) {
        Float x = message.getDistFromStart();
        angle.addData(x, message.getAngle());
        steerPredicted.addData(x, message.getSteerPredicted());
        steerExpected.addData(x, message.getSteerExpected());
        distToMiddle.addData(x, message.getDistToMiddle());
        float error = Math.abs(message.getSteerPredicted() - message.getSteerExpected());
        errorSum += error;
        absError.addData(x, error);
        count++;
    }

    public void clear() {
        plot.getData().clear();
    }

    public void finishLap() {
        for (SensorData value : sensorMap.values()) {
            value.finishLap();
        }
        System.out.println("***********************************");
        System.out.println("Average Error: " + errorSum / count);
        System.out.println("***********************************");

        count = 0;
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
