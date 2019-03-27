package com.sranasing.visualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author shast
 */
public final class Utils {

    private static final String COMMA_DELIMITER = ",";

    private Utils() {
    }

    public static void moveModelFiles(String selectedModelDir, String modelsFolderPath, String runtimedFolderPath) throws Exception {
        if (selectedModelDir != null) {
            try {
                moveFilesInDir(modelsFolderPath + "\\" + selectedModelDir, runtimedFolderPath);
            } catch (IOException ex) {
                throw new Exception("Error");
            }
        } else {
            throw new Exception("Error");
        }
    }

    public static void removeExistingModelFiles(String runtimedFolderPath) {
        File runtimedFolder = new File(runtimedFolderPath);
        File[] modelFiles = runtimedFolder.listFiles((File dir, String name) -> {
            String ext = getFileExtension(name);
            return (ext.equals("h5") || ext.equals("json"));
        });

        if (modelFiles != null) {
            for (File modelFile : modelFiles) {
                modelFile.delete();
            }
        }
    }

    public static void updateTrack(String name, String category, String quickRaceFilePath) throws Exception {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(quickRaceFilePath);

            Node quickRace = doc.getFirstChild();
            NodeList sections = doc.getElementsByTagName("section");
            Node node;
            for (int i = 0; i < sections.getLength(); i++) {
                node = sections.item(i);
                if (node.getAttributes().getNamedItem("name").getTextContent().equals("Tracks")) {
                    NodeList sectionChild = node.getChildNodes();
                    for (int j = 0; j < sectionChild.getLength(); j++) {
                        node = sectionChild.item(j);
                        if (node.getNodeName().equals("section")) {
                            NodeList attrs = node.getChildNodes();
                            for (int k = 0; k < attrs.getLength(); k++) {
                                node = attrs.item(k);
                                if (node.getAttributes() != null) {
                                    if (node.getAttributes().getNamedItem("name").getTextContent().equals("name")) {
                                        node.getAttributes().getNamedItem("val").setTextContent(name);
                                    }
                                    if (node.getAttributes().getNamedItem("name").getTextContent().equals("category")) {
                                        node.getAttributes().getNamedItem("val").setTextContent(category);
                                    }
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(quickRaceFilePath));
            transformer.transform(source, result);

        } catch (SAXException ex) {
            throw new Exception("ERROR");
        } catch (IOException ex) {
            throw new Exception("ERROR");
        } catch (ParserConfigurationException ex) {
            throw new Exception("ERROR");
        } catch (TransformerConfigurationException ex) {
            throw new Exception("ERROR");
        } catch (TransformerException ex) {
            throw new Exception("ERROR");
        }
    }

    public static void saveToCSV(float[][] data, String trackname, int lap) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-d HH-mm-ss");
        Date date = new Date();
        File file = new File(dateFormat.format(date) + "___" + trackname + "___Lap-" + (lap + 1) + ".csv");
        try (FileWriter writer = new FileWriter(file)) {
            for (int i = 0; i < data[0].length; i++) {
                writer.append(data[0][i] + COMMA_DELIMITER + data[1][i]);
                writer.append("\n");
            }
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static List<float[]> loadCSV(File file) {
        List<float[]> data = new ArrayList<>();
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            List<Float> predicted = new ArrayList<>();
            List<Float> expected = new ArrayList<>();
            String line = "";
            while ((line = fileReader.readLine()) != null) {
                String[] tokens = line.split(COMMA_DELIMITER);
                if (!line.isEmpty() && tokens.length > 0) {
                    predicted.add(Float.parseFloat(tokens[0]));
                    expected.add(Float.parseFloat(tokens[1]));
                }
            }
            data.add(ArrayUtils.toPrimitive(predicted.toArray(new Float[predicted.size()])));
            data.add(ArrayUtils.toPrimitive(expected.toArray(new Float[expected.size()])));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    private static void moveFilesInDir(String srcDirPath, String destPath) throws Exception {
        File srcDir = new File(srcDirPath);

        CopyOption[] options = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};

        for (File file : srcDir.listFiles()) {
            Path src = Paths.get(file.getAbsolutePath(), "");
            Path target = Paths.get(destPath, "").resolve(file.getName());
            try {
                Files.copy(src, target, options);
            } catch (IOException ex) {
                throw new Exception("Error");
            }
        }
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}
