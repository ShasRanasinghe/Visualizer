/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sranasing.visualizer;

import java.io.Serializable;
import java.util.ArrayList;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author shast
 */
public class SensorData implements Serializable {

    private ArrayList<Float> intermediate_Data;

    //data series used in the line chart
    private Series<Number, Number> sensorDataList;

    private float[] data;

    private boolean isLive = false;

    private int lapMessageCount = 0;

    private String name;

    private boolean lapFinished = false;

    public SensorData(String name) {
        this.name = name;
        sensorDataList = new Series<>();
        sensorDataList.setName(name);
        intermediate_Data = new ArrayList<>();
    }

    public void addData(Float data_point) {
        if (isLive) {
            sensorDataList.getData().add(new Data<>(lapMessageCount, data_point));
        }
        intermediate_Data.add(data_point);
        lapMessageCount++;
    }

    public Series<Number, Number> addToGraph() {
        isLive = true;
        if (lapFinished) {
            for (int i = 0; i < intermediate_Data.size(); i++) {
                sensorDataList.getData().add(new Data<>(i, intermediate_Data.get(i)));
            }
        } else {
            for (int i = 0; i < lapMessageCount; i++) {
                sensorDataList.getData().add(new Data<>(i, data[i]));
            }
        }
        return sensorDataList;
    }

    public Series<Number, Number> removeFromGraph() {
        isLive = false;
        sensorDataList.getData().clear();
        return sensorDataList;
    }

    public void finishLap() {
        data = ArrayUtils.toPrimitive(intermediate_Data.toArray(new Float[intermediate_Data.size()]));
        intermediate_Data = new ArrayList<>();
        lapFinished = true;
    }

    public float[] getData() {
        return data;
    }

    public void setData(float[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return name;
    }

}
