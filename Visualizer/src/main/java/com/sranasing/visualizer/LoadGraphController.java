/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sranasing.visualizer;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author shast
 */
public class LoadGraphController implements Initializable {

    @FXML
    private LineChart<Number, Number> plot;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    public void load() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.setTitle("Load Lap Data");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Lap Data Files", "*.csv"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(plot.getScene().getWindow());
        if (selectedFiles == null) {
            return;
        }
        for (File selectedFile : selectedFiles) {
            ArrayList<Tuple> data = Utils.loadCSV(selectedFile);

            Series<Number, Number> series = new Series();
            series.setName(selectedFile.getName());

            for (int i = 0; i < data.size(); i++) {
                series.getData().add(new XYChart.Data<>(data.get(i).x, data.get(i).y));
            }

            plot.getData().addAll(series);
        }
    }

    @FXML
    public void clear() {
        plot.getData().clear();
    }

}
