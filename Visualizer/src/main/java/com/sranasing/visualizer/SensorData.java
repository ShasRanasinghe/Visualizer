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

    private ArrayList<float[]> data;

    private boolean isLive = false;

    private int lapMessageCount = 0;

    private String name;

    public SensorData(String name) {
        this.name = name;
        sensorDataList = new Series<>();
        sensorDataList.setName(name);
        data = new ArrayList<>();
        intermediate_Data = new ArrayList<>();
    }

    public void newLap() {
        data.add(ArrayUtils.toPrimitive(intermediate_Data.toArray(new Float[intermediate_Data.size()])));
        intermediate_Data = new ArrayList<>();
        sensorDataList.getData().clear();
        lapMessageCount = 0;
    }

    public Series<Number, Number> goLive() {
        isLive = true;
        for (int i = 0; i < intermediate_Data.size(); i++) {
            sensorDataList.getData().add(new Data<>(i, intermediate_Data.get(i)));
        }
        return sensorDataList;
    }

    public Series<Number, Number> goOffline() {
        isLive = false;
        sensorDataList.getData().clear();
        return sensorDataList;
    }

    public void addData(Float data_point) {
        if (isLive) {
            sensorDataList.getData().add(new Data<>(lapMessageCount, data_point));
        }
        intermediate_Data.add(data_point);
        lapMessageCount++;
    }

    public ArrayList<float[]> getLapData() {
        return data;
    }

    @Override
    public String toString() {
        return name;
    }

}
