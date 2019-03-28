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

/**
 *
 * @author shast
 */
public class SensorData implements Serializable {

    private ArrayList<Tuple> intermediate_Data;

    //data series used in the line chart
    private Series<Number, Number> sensorDataList;

    private float[][] data;

    private boolean isLive = false;

    private String name;

    private boolean lapFinished = false;

    public SensorData(String name) {
        this.name = name;
        sensorDataList = new Series<>();
        sensorDataList.setName(name);
        intermediate_Data = new ArrayList<>();
    }

    public void addData(float x, float y) {
        if (isLive) {
            sensorDataList.getData().add(new Data<>(x, y));
        }
        intermediate_Data.add(new Tuple(x, y));
    }

    public Series<Number, Number> addToGraph() {
        isLive = true;
        if (lapFinished) {
            for (int i = 0; i < data[0].length; i++) {
                sensorDataList.getData().add(new Data<>(data[0][i], data[1][i]));
            }
        } else {
            for (Tuple tp : intermediate_Data) {
                sensorDataList.getData().add(new Data<>(tp.x, tp.y));
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
        data = new float[2][intermediate_Data.size()];
        for (int i = 0; i < intermediate_Data.size(); i++) {
            data[0][i] = intermediate_Data.get(i).x;
            data[1][i] = intermediate_Data.get(i).y;
        }

        intermediate_Data = new ArrayList<>();
        lapFinished = true;
    }

    public float[][] getData() {
        return data;
    }

    public void setData(float[][] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return name;
    }

}
