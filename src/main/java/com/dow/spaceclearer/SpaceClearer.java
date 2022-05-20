package com.dow.spaceclearer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class SpaceClearer extends Application {
    private int start = 0;
    private File file;
    private List<Path> paths;
    private Stage stage;

    public static void main(String[] args) {
        Application.launch();
    }

    public void start(Stage stage) throws IOException {
        this.stage = stage;
        fileSelect(stage);
        VBox pane = paneCreator();
        stage.setScene(new Scene(pane));
        stage.setTitle("Space Clearer");
        stage.show();
    }

    private void fileSelect(Stage stage) throws IOException {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File newFile = dirChooser.showDialog(stage);

        if (newFile != null) {
            file = newFile;

            // Displays Loading... So the user knows that the program didn't crash
            Stage loadingStage = new Stage();
            loadingStage.setTitle("Loading...");
            Scene scene = new Scene(new VBox(), 250, 250);
            loadingStage.setScene(scene);
            loadingStage.show();

            paths = FileLister.getFilesInDirectory(file);

            loadingStage.close();

            if (paths == null) {
                file = null;
            }
        } else if (file == null) {
            System.exit(0);
        }
    }

    private VBox paneCreator() {
        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10, 10, 10, 10));
        Label disclaimer = new Label("NOTE: Recycle only works on Windows, Perm Delete works for all OSs");
        disclaimer.setFont(Font.font("Lucida Sans Unicode"));
        vBox.getChildren().add(disclaimer);

        for (int i  = start; i < start + 5 && i < paths.size(); i++) {
            try {
                // Checks if the folder is null, stop a NullPointerException exception
                if (file == null) {
                    break;
                } else if (paths.isEmpty()) {
                    Label empty = new Label("File is Empty");
                    empty.setFont(Font.font("Lucida Sans Unicode", FontWeight.BOLD, 18));
                    vBox.getChildren().add(empty);
                    break;
                }

                // If there's files in folder and the path is not null, add single line to GUI
                vBox.getChildren().add(singleFile(i));

            } catch (ArrayIndexOutOfBoundsException | IOException e) {
                e.printStackTrace();
                break;
            }
        }

        vBox.getChildren().add(finalLine());
        return vBox;
    }

    private HBox singleFile(int index) throws IOException {

        Path current = paths.get(index);
        HBox hBox = new HBox(10);
        double size = Files.size(current) / 1024.0;
        Label currentFile = new Label(String.format("%s - %.2f MB", current.getFileName().toString(), size));
        currentFile.setFont(Font.font("Lucida Sans Unicode"));

        Button bRecycle = new Button("Recycle");
        bRecycle.setOnAction(actionEvent -> {
            String message = "Are you sure you would like to recycle this file?";
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                FileActions.recycleFile(current);
                paths.remove(current);
                stage.setScene(new Scene(paneCreator()));
            }
        });

        Button bDelete = new Button("Perm Delete");
        bDelete.setOnAction(actionEvent -> {
            String message = "Are you sure you want to permanently delete this file (cannot be undone)?";
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                FileActions.permanentlyDelete(current);
                paths.remove(current);
                stage.setScene(new Scene(paneCreator()));
            }
        });

        Button bOpen = new Button("Open");
        bOpen.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().open(current.toFile());
            } catch (IOException e) {
                String message = "Unable to open file";
                Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
                alert.showAndWait();
            }
        });

        hBox.getChildren().addAll(bRecycle, bDelete, currentFile);

        return hBox;
    }

    private HBox finalLine() {

        HBox hBox = new HBox(10);

        Button bNextPage = new Button("Next Page");
        bNextPage.setOnAction(actionEvent -> {
                start += 5;
                stage.setScene(new Scene(paneCreator()));
        });

        Button bPreviousPage = new Button("Previous Page");
        bPreviousPage.setOnAction(actionEvent -> {
                start -= 5;
                stage.setScene(new Scene(paneCreator()));
        });

        Button bNewFile = new Button("Change File");
        bNewFile.setOnAction(actionEvent -> {
            try {
                fileSelect(stage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stage.setScene(new Scene(paneCreator()));
        });

        if (start + 5 < paths.size()) {
            hBox.getChildren().add(bNextPage);
        }
        if (start >= 5)  {
            hBox.getChildren().add(bPreviousPage);
        }

        hBox.getChildren().add(bNewFile);
        return hBox;
    }
}


class FileLister {
    public static List<Path> getFilesInDirectory(File file) {
        // Gets all the files in the given directory and returns a Path array sorted by size
        try {
            List<Path> paths = Files.walk(file.toPath()).filter(Files::isRegularFile).collect(Collectors.toList());
            return fileArraySorter(paths);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Access Denied", ButtonType.OK);
            alert.showAndWait();
            return null;
        }
    }

    private static List<Path> fileArraySorter(List<Path> paths) {
        // Sorts a Path List by size and returns it
        paths.sort(Comparator.comparing(path -> {
            try {
                return Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }));

        // Reverse the array by converting it to a list and using the method reverse from Collections
        Collections.reverse(paths);
        return paths;
    }
}


class FileActions {
    public static void recycleFile(Path path) {
        try {
            if (!Desktop.getDesktop().moveToTrash(path.toFile())) { // Moves file to recycling bin
                throw new Exception();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to Recycle File, are you not on windows?", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public static void permanentlyDelete(Path path) { // Perm deletes file
        try {
            if (!path.toFile().delete()) {
                throw new Exception();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to Delete File", ButtonType.OK);
            alert.showAndWait();
        }
    }
}