/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sranasing.visualizer;

import java.io.File;
import java.net.URL;
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
            List<float[]> data = Utils.loadCSV(selectedFile);

            Series<Number, Number> predicted = new Series();
            predicted.setName(selectedFile.getName() + "-Predicted");
            Series<Number, Number> expected = new Series();
            expected.setName(selectedFile.getName() + "-Expected");

            for (int i = 0; i < data.get(0).length; i++) {
                predicted.getData().add(new XYChart.Data<>(i, data.get(0)[i]));
                expected.getData().add(new XYChart.Data<>(i, data.get(1)[i]));
            }

            plot.getData().addAll(predicted, expected);
        }
    }

    @FXML
    public void clear() {
        plot.getData().clear();
    }

}
