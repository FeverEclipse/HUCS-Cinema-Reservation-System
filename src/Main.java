import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Main extends Application {
    File error = new File("assets/effects/error.mp3");
    AudioClip errorSound = new AudioClip(error.toURI().toString());
    ArrayList<User> userList = new ArrayList<>();
    ArrayList<Film> filmList = new ArrayList<>();
    ArrayList<Hall> hallList = new ArrayList<>();
    int maxTries;
    String appTitle;
    int discountPercentage;
    int blockTime;
    public static void main(String[] args) {
        launch();
    }
    public String hashPassword(String password){
        byte [] bytesOfPassword = password.getBytes(StandardCharsets.UTF_8);
        byte [] md5Digest = new byte [0];
        try {
            md5Digest = MessageDigest.getInstance("MD5").digest(bytesOfPassword);
        } catch (NoSuchAlgorithmException e) {
            return null ;
        }
        return Base64.getEncoder().encodeToString(md5Digest);
    }

    public void readData(){
        // Initialization of the app
        try{
            File backup = new File("assets/data/backup.dat");
            File properties = new File("assets/data/properties.dat");
            Scanner backupScanner = new Scanner(backup);
            Scanner propertiesScanner = new Scanner(properties);
            while (backupScanner.hasNextLine()){
                String currentLine = backupScanner.nextLine();
                String[] currentList = currentLine.split("\t");
                switch (currentList[0]){
                    case "user":
                        Boolean isAdmin;
                        Boolean isClubMember;
                        isClubMember = currentList[3].equals("true");
                        isAdmin = currentList[4].equals("true");
                        userList.add(new User(currentList[1], currentList[2], isClubMember, isAdmin));
                        break;
                    case "film":
                        filmList.add(new Film(currentList[1], currentList[2], Integer.parseInt(currentList[3])));
                        break;
                    case "hall":
                        hallList.add(new Hall(currentList[2], Integer.parseInt(currentList[3]), Integer.parseInt(currentList[4]), Integer.parseInt(currentList[5]), currentList[1]));
                        break;
                    case "seat":
                        for (int i = 0; i < hallList.size(); i++){
                            if (hallList.get(i).getName().equals(currentList[2])){
                                hallList.get(i).addSeat(new Seat(Integer.parseInt(currentList[3]), Integer.parseInt(currentList[4]), currentList[5], Integer.parseInt(currentList[6])));
                            }
                        }
                        break;
                }
            }
            while (propertiesScanner.hasNextLine()){
                String[] currentList = propertiesScanner.nextLine().split("=");
                switch (currentList[0]){
                    case "maximum-error-without-getting-blocked":
                        maxTries = Integer.parseInt(currentList[1]);
                        break;
                    case "title":
                        appTitle = currentList[1];
                        break;
                    case "discount-percentage":
                        discountPercentage = Integer.parseInt(currentList[1]);
                        break;
                    case "block-time":
                        blockTime = Integer.parseInt(currentList[1]);
                        break;
                }
            }
            backupScanner.close();
            propertiesScanner.close();
        }
        catch (IOException e){
            System.out.println("IOException");
        }
    }

    public Pane seatSelector(Stage primaryStage,Hall hall, String username, Film selectedFilm){
        Pane seatSelector = new Pane();

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setY(hall.getTotalRow() * 70 - 40);
        bottomText.setX(hall.getTotalColumn() * 20 - 40);

        final Boolean[] boughtSnacks = {false};
        Button getSnacks = new Button();
        getSnacks.setText("Include Snacks! (Free for Club Members!)");
        getSnacks.setLayoutX(hall.getTotalColumn() * 10 + 90);
        getSnacks.setLayoutY(hall.getTotalRow() * 70 - 30);
        getSnacks.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!boughtSnacks[0]){
                    boughtSnacks[0] = true;
                    getSnacks.setText("Remove Snacks. (Free for Club Members!)");
                }
                else {
                    boughtSnacks[0] = false;
                    getSnacks.setText("Include Snacks! (Free for Club Members!)");
                }
            }
        });

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(hall.getTotalColumn() * 10 - 10);
        backButton.setLayoutY(hall.getTotalRow() * 70 - 30);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(trailerScreen(primaryStage, selectedFilm, username), 650, 450));
            }
        });

        seatSelector.getChildren().addAll(bottomText, backButton, getSnacks);


        Button[][] buttons = new Button[hall.getTotalRow()][hall.getTotalColumn()];
        File empty_path = new File("assets/icons/empty_seat.png");
        File reserved_path = new File("assets/icons/reserved_seat.png");
        Image emptySeat = new Image(empty_path.toURI().toString());
        Image reservedSeat = new Image(reserved_path.toURI().toString());

        for (int i = 0; i < hall.getTotalRow(); i++){
            for (int j = 0; j < hall.getTotalColumn(); j++){
                ImageView emptyView = new ImageView(emptySeat);
                ImageView reservedView = new ImageView(reservedSeat);
                buttons[i][j] = new Button();
                buttons[i][j].setMinSize(35, 55);
                buttons[i][j].setMaxSize(35, 55);
                emptyView.fitWidthProperty().bind(buttons[i][j].widthProperty());
                emptyView.fitHeightProperty().bind(buttons[i][j].heightProperty());
                reservedView.fitWidthProperty().bind(buttons[i][j].widthProperty());
                reservedView.fitHeightProperty().bind(buttons[i][j].heightProperty());
                if (hall.getSeats()[i][j].getOwner().equals("null")){
                    buttons[i][j].setGraphic(emptyView);
                }
                else if (hall.getSeats()[i][j].getOwner().equals(username)){
                    buttons[i][j].setGraphic(reservedView);
                }
                else {
                    buttons[i][j].setGraphic(reservedView);
                    buttons[i][j].setDisable(true);
                }
                buttons[i][j].setLayoutX(40 + j * 40);
                buttons[i][j].setLayoutY(10 + i * 60);
                int finalI = i;
                int finalJ = j;
                buttons[i][j].setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        if (buttons[finalI][finalJ].getGraphic().equals(emptyView)){
                            User user = null;
                            for (User i: userList){
                                if (i.getUsername().equals(username)){
                                    user = i;
                                }
                            }
                            if (user.getClubMember() && !boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() * (100 - discountPercentage) / 100 + " TL successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(username);
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice() * (100 - discountPercentage) / 100);
                            }
                            else if (!user.getClubMember() && !boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() + " TL successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(username);
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice());
                            }
                            else if (user.getClubMember() && boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() + " TL with free snacks successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(username);
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice());
                                hall.getSeats()[finalI][finalJ].setGotSnacks(true);
                            }
                            else {
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + (hall.getPrice() + 10) + " TL with 10 TL snacks fee successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(username);
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice());
                                hall.getSeats()[finalI][finalJ].setGotSnacks(true);
                            }
                        }
                        else {
                            buttons[finalI][finalJ].setGraphic(emptyView);
                            bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " is refunded successfully!");
                            hall.getSeats()[finalI][finalJ].setOwner("null");
                        }
                    }
                });
                seatSelector.getChildren().add(buttons[i][j]);
            }
        }
        for (Hall i:hallList){
            if (i.getName().equals(hall.getName())){
                i.setSeats(hall.getSeats());
            }
        }
        return seatSelector;
    }

    public Pane adminSeatSelector(Stage primaryStage, Hall hall, String username, Film film){
        Pane adminSeatSelector = new Pane();

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setY(hall.getTotalRow() * 70 - 40);
        bottomText.setX(hall.getTotalColumn() * 20 - 60);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(hall.getTotalColumn() * 10 - 10);
        backButton.setLayoutY(hall.getTotalRow() * 70 - 30);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminTrailerScreen(primaryStage, film, username), 650, 450));
            }
        });

        ChoiceBox userPicker = new ChoiceBox();
        for (User i: userList){
            userPicker.getItems().add(i.getUsername());
            userPicker.setValue(i.getUsername());
        }
        userPicker.setLayoutY(hall.getTotalRow() * 70 - 30);
        userPicker.setLayoutX(hall.getTotalColumn() * 10 + 50);

        final Boolean[] boughtSnacks = {false};
        Button getSnacks = new Button();
        getSnacks.setText("Include Snacks! (Free for Club Members!)");
        getSnacks.setLayoutX(hall.getTotalColumn() * 10 + 180);
        getSnacks.setLayoutY(hall.getTotalRow() * 70 - 30);
        getSnacks.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!boughtSnacks[0]){
                    boughtSnacks[0] = true;
                    getSnacks.setText("Remove Snacks. (Free for Club Members!)");
                }
                else {
                    boughtSnacks[0] = false;
                    getSnacks.setText("Include Snacks! (Free for Club Members!)");
                }
            }
        });

        adminSeatSelector.getChildren().addAll(bottomText, backButton, userPicker, getSnacks);


        Button[][] buttons = new Button[hall.getTotalRow()][hall.getTotalColumn()];
        File empty_path = new File("assets/icons/empty_seat.png");
        File reserved_path = new File("assets/icons/reserved_seat.png");
        Image emptySeat = new Image(empty_path.toURI().toString());
        Image reservedSeat = new Image(reserved_path.toURI().toString());

        for (int i = 0; i < hall.getTotalRow(); i++){
            for (int j = 0; j < hall.getTotalColumn(); j++){
                ImageView emptyView = new ImageView(emptySeat);
                ImageView reservedView = new ImageView(reservedSeat);
                buttons[i][j] = new Button();
                buttons[i][j].setMinSize(35, 55);
                buttons[i][j].setMaxSize(35, 55);
                emptyView.fitWidthProperty().bind(buttons[i][j].widthProperty());
                emptyView.fitHeightProperty().bind(buttons[i][j].heightProperty());
                reservedView.fitWidthProperty().bind(buttons[i][j].widthProperty());
                reservedView.fitHeightProperty().bind(buttons[i][j].heightProperty());
                if (hall.getSeats()[i][j].getOwner().equals("null")){
                    buttons[i][j].setGraphic(emptyView);
                }
                else {
                    buttons[i][j].setGraphic(reservedView);
                }
                buttons[i][j].setLayoutX(60 + j * 40);
                buttons[i][j].setLayoutY(10 + i * 60);
                int finalI = i;
                int finalJ = j;
                buttons[i][j].setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        if (buttons[finalI][finalJ].getGraphic().equals(emptyView)){
                            User user = null;
                            for (User i: userList){
                                if (i.getUsername().equals(userPicker.getValue().toString())){
                                    user = i;
                                }
                            }
                            if (user.getClubMember() && !boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() * (100 - discountPercentage) / 100 + " TL successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(userPicker.getValue().toString());
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice() * (100 - discountPercentage) / 100);
                            }
                            else if (!user.getClubMember() && !boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() + " TL successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(userPicker.getValue().toString());
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice());
                            }
                            else if (user.getClubMember() && boughtSnacks[0]){
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + hall.getPrice() * (100 - discountPercentage) / 100 + " TL with free snacks successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(userPicker.getValue().toString());
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice() * (100 - discountPercentage) / 100);
                                hall.getSeats()[finalI][finalJ].setGotSnacks(true);
                            }
                            else {
                                buttons[finalI][finalJ].setGraphic(reservedView);
                                bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " bought for " + (hall.getPrice() + 10) + " TL with 10 TL snacks fee successfully!");
                                hall.getSeats()[finalI][finalJ].setOwner(username);
                                hall.getSeats()[finalI][finalJ].setPriceBought(hall.getPrice());
                                hall.getSeats()[finalI][finalJ].setGotSnacks(true);
                            }
                        }
                        else {
                            buttons[finalI][finalJ].setGraphic(emptyView);
                            bottomText.setText("Seat at " + (finalI + 1) + "-" + (finalJ + 1) + " is refunded successfully!");
                            hall.getSeats()[finalI][finalJ].setOwner("null");
                        }
                    }
                });
                buttons[i][j].setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        if (hall.getSeats()[finalI][finalJ].getOwner().equals("null")){
                            bottomText.setText("Not bought yet!");
                        }
                        else {
                            bottomText.setText("Bought by " + hall.getSeats()[finalI][finalJ].getOwner() + " for " + hall.getSeats()[finalI][finalJ].getPriceBought() + " TL!");
                        }
                    }
                });
                buttons[i][j].setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        bottomText.setText("");
                    }
                });
                adminSeatSelector.getChildren().add(buttons[i][j]);
            }
        }
        return adminSeatSelector;
    }

    public Pane trailerScreen(Stage primaryStage, Film film, String username) {
        File path = new File("assets/trailers/" + film.getTrailerPath());
        Media media = new Media(path.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView();
        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.setFitWidth(550);
        mediaView.setPreserveRatio(true);
        mediaView.setX(20);
        mediaView.setY(40);

        Text topText = new Text();
        topText.setText(film.getTitle() + " (" + film.getDuration() + ")");
        topText.setX(250);
        topText.setY(25);

        Button playPause = new Button();
        playPause.setLayoutX(590);
        playPause.setLayoutY(50);
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)){
            playPause.setText("⏸");
        }
        else {
            playPause.setText("▶");
        }
        playPause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)){
                    mediaPlayer.pause();
                    playPause.setText("▶");
                }
                else {
                    mediaPlayer.play();
                    playPause.setText("⏸");
                }
            }
        });

        Button rewind5sec = new Button();
        rewind5sec.setText("<<");
        rewind5sec.setLayoutX(590);
        rewind5sec.setLayoutY(85);
        rewind5sec.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5))){
                    mediaPlayer.seek(Duration.seconds(mediaPlayer.getCurrentTime().toSeconds() - 5));
                }
                else {
                    mediaPlayer.seek(mediaPlayer.getStartTime());
                }
            }
        });

        Button forward5sec = new Button();
        forward5sec.setText(">>");
        forward5sec.setLayoutX(590);
        forward5sec.setLayoutY(120);
        forward5sec.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(mediaPlayer.getTotalDuration().toSeconds() - 5))){
                    mediaPlayer.seek(mediaPlayer.getTotalDuration());
                }
                else {
                    mediaPlayer.seek(Duration.seconds(mediaPlayer.getCurrentTime().toSeconds() + 5));
                }
            }
        });

        Button rewindToStart = new Button();
        rewindToStart.setText("|<<");
        rewindToStart.setLayoutX(590);
        rewindToStart.setLayoutY(155);
        rewindToStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                mediaPlayer.seek(mediaPlayer.getStartTime());
            }
        });

        Slider volumeSlider = new Slider();
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumeSlider.setValue(100);
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                mediaPlayer.setVolume(volumeSlider.getValue() / 100);
            }
        });
        volumeSlider.setLayoutX(590);
        volumeSlider.setLayoutY(200);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(100);
        backButton.setLayoutY(405);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                mediaPlayer.stop();
                primaryStage.setScene(new Scene(normalFilmSelector(primaryStage, username), 450, 200));
            }
        });

        ChoiceBox hallSelector = new ChoiceBox<>();
        for (Hall i: hallList){
            if (i.getFilm().equals(film.getTitle())){
                hallSelector.getItems().add(i.getName());
                hallSelector.setValue(i.getName());
            }
        }
        hallSelector.setLayoutX(250);
        hallSelector.setLayoutY(405);

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutX(420);
        ok.setLayoutY(405);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Hall selectedHall = null;
                for (Hall i: hallList){
                    if (i.getName().equals(hallSelector.getValue())){
                        selectedHall = i;
                    }
                }
                primaryStage.setScene(new Scene(seatSelector(primaryStage, selectedHall, username, film), selectedHall.getTotalColumn() * 55, selectedHall.getTotalRow() * 70));
            }
        });

        Pane trailerScreen = new Pane();
        trailerScreen.getChildren().addAll(mediaView, playPause, topText, rewind5sec, forward5sec, rewindToStart, volumeSlider, backButton, hallSelector, ok);
        return trailerScreen;
    }

    public Pane normalFilmSelector(Stage primaryStage, String username){
        // Film Select Screen
        Text topText = new Text();
        topText.setText("             Welcome " + username + "!\n" + "Select a film and then click OK to continue.");
        topText.setX(80);
        topText.setY(40);

        ChoiceBox choiceBox = new ChoiceBox<>();
        for (Film i:filmList){
            choiceBox.setValue(i.getTitle());
            choiceBox.getItems().add(i.getTitle());
        }
        choiceBox.setLayoutX(40);
        choiceBox.setLayoutY(90);

        Button okButton = new Button();
        okButton.setText("OK");
        okButton.setLayoutX(390);
        okButton.setLayoutY(90);
        okButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String selection = choiceBox.getValue().toString();
                Film selectedFilm = null;
                for (Film i: filmList){
                    if (selection.equals(i.getTitle())){
                        selectedFilm = i;
                    }
                }
                primaryStage.setScene(new Scene(trailerScreen(primaryStage, selectedFilm, username), 650, 450));
            }
        });

        Button logOut = new Button();
        logOut.setText("LOG OUT");
        logOut.setLayoutX(350);
        logOut.setLayoutY(130);
        logOut.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.setScene(new Scene(welcomeRoot(primaryStage), 450, 250));
            }
        });

        Pane filmSelector = new Pane();
        filmSelector.getChildren().addAll(topText, choiceBox, okButton, logOut);
        return filmSelector;
    }

    public Pane addHall(Stage primaryStage, Film film, String username){
        Text topText = new Text();
        topText.setText(film.getTitle() + " (" + film.getDuration() + ")");
        topText.setX(100);
        topText.setY(25);

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setX(40);
        bottomText.setY(250);

        Text rowText = new Text();
        rowText.setText("Row:");
        rowText.setX(20);
        rowText.setY(55);

        Text columnText = new Text();
        columnText.setText("Column:");
        columnText.setX(20);
        columnText.setY(85);

        Text nameText = new Text();
        nameText.setText("Name:");
        nameText.setX(20);
        nameText.setY(115);

        Text priceText = new Text();
        priceText.setText("Price:");
        priceText.setX(20);
        priceText.setY(145);

        ChoiceBox rowChoice = new ChoiceBox<>();
        for (int i = 3; i < 11 ; i++){
            rowChoice.getItems().add(i);
            rowChoice.setValue(i);
        }
        rowChoice.setLayoutX(120);
        rowChoice.setLayoutY(40);

        ChoiceBox columnChoice = new ChoiceBox<>();
        for (int i = 3; i < 11; i++){
            columnChoice.getItems().add(i);
            columnChoice.setValue(i);
        }
        columnChoice.setLayoutX(120);
        columnChoice.setLayoutY(70);

        TextField nameField = new TextField();
        nameField.setLayoutX(80);
        nameField.setLayoutY(100);

        TextField priceField = new TextField();
        priceField.setLayoutX(80);
        priceField.setLayoutY(140);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(20);
        backButton.setLayoutY(200);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminTrailerScreen(primaryStage, film, username), 650, 450));
            }
        });

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutY(200);
        ok.setLayoutX(190);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Boolean isPosInt = false;
                try{
                    int test = Integer.parseInt(priceField.getText());
                    if (test > 0){
                        isPosInt = true;
                    }
                }
                catch (NumberFormatException e){
                    isPosInt = false;
                }
                if (nameField.getText().isEmpty()){
                    bottomText.setText("ERROR : Name could not be empty!");
                    errorSound.play();
                } else if (priceField.getText().isEmpty()) {
                    bottomText.setText("ERROR : Price could not be empty!");
                    errorSound.play();
                } else if (!isPosInt) {
                    bottomText.setText("ERROR : Price must be positive integer!");
                    errorSound.play();
                }
                else {
                    Hall hallToAdd = new Hall(nameField.getText(), Integer.parseInt(priceField.getText()), Integer.parseInt(rowChoice.getValue().toString()), Integer.parseInt(columnChoice.getValue().toString()), film.getTitle());
                    for (int i = 0; i < Integer.parseInt(rowChoice.getValue().toString()); i++){
                        for (int j = 0; j < Integer.parseInt(columnChoice.getValue().toString()); j++){
                            hallToAdd.addSeat(new Seat(i, j, "null", 0));
                        }
                    }
                    hallList.add(hallToAdd);
                    bottomText.setText("SUCCESS : Hall successfully added!");
                }
            }
        });


        Pane addHall = new Pane();
        addHall.getChildren().addAll(topText, nameText, columnText, priceText, rowText, bottomText, rowChoice, columnChoice, ok, backButton, nameField, priceField);
        return addHall;
    }

    public Pane removeHall(Stage primaryStage, Film film, String username){

        Text topText = new Text();
        topText.setText("Select the hall that you desire to remove from " + film.getTitle() + " and then click OK.");
        topText.setX(15);
        topText.setY(15);

        ChoiceBox hallPicker = new ChoiceBox();
        for (Hall i: hallList){
            if (i.getFilm().equals(film.getTitle())){
                hallPicker.getItems().add(i.getName());
                hallPicker.setValue(i.getName());
            }
        }
        hallPicker.setLayoutX(200);
        hallPicker.setLayoutY(40);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(30);
        backButton.setLayoutY(90);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminTrailerScreen(primaryStage, film, username), 650, 450));
            }
        });

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutX(410);
        ok.setLayoutY(90);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Hall hallToRemove = null;
                for (Hall i: hallList){
                    if (i.getName().equals(hallPicker.getValue().toString())){
                        hallToRemove = i;
                    }
                }
                hallList.remove(hallToRemove);
                primaryStage.setScene(new Scene(removeHall(primaryStage, film, username), 480, 125));
            }
        });

        Pane removeHall = new Pane();
        removeHall.getChildren().addAll(topText, hallPicker, backButton, ok);
        return removeHall;
    }

    public Pane adminTrailerScreen(Stage primaryStage,Film film, String username){
        File path = new File("assets/trailers/" + film.getTrailerPath());
        Media media = new Media(path.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView();
        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.setFitWidth(550);
        mediaView.setPreserveRatio(true);
        mediaView.setX(20);
        mediaView.setY(40);

        Text topText = new Text();
        topText.setText(film.getTitle() + " (" + film.getDuration() + ")");
        topText.setX(250);
        topText.setY(25);

        Button playPause = new Button();
        playPause.setLayoutX(590);
        playPause.setLayoutY(50);
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)){
            playPause.setText("⏸");
        }
        else {
            playPause.setText("▶");
        }
        playPause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)){
                    mediaPlayer.pause();
                    playPause.setText("▶");
                }
                else {
                    mediaPlayer.play();
                    playPause.setText("⏸");
                }
            }
        });

        Button rewind5sec = new Button();
        rewind5sec.setText("<<");
        rewind5sec.setLayoutX(590);
        rewind5sec.setLayoutY(85);
        rewind5sec.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5))){
                    mediaPlayer.seek(Duration.seconds(mediaPlayer.getCurrentTime().toSeconds() - 5));
                }
                else {
                    mediaPlayer.seek(mediaPlayer.getStartTime());
                }
            }
        });

        Button forward5sec = new Button();
        forward5sec.setText(">>");
        forward5sec.setLayoutX(590);
        forward5sec.setLayoutY(120);
        forward5sec.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(mediaPlayer.getTotalDuration().toSeconds() - 5))){
                    mediaPlayer.seek(mediaPlayer.getTotalDuration());
                }
                else {
                    mediaPlayer.seek(Duration.seconds(mediaPlayer.getCurrentTime().toSeconds() + 5));
                }
            }
        });

        Button rewindToStart = new Button();
        rewindToStart.setText("|<<");
        rewindToStart.setLayoutX(590);
        rewindToStart.setLayoutY(155);
        rewindToStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                mediaPlayer.seek(mediaPlayer.getStartTime());
            }
        });

        Slider volumeSlider = new Slider();
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumeSlider.setValue(100);
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                mediaPlayer.setVolume(volumeSlider.getValue() / 100);
            }
        });
        volumeSlider.setLayoutX(590);
        volumeSlider.setLayoutY(200);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(100);
        backButton.setLayoutY(405);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                mediaPlayer.stop();
                primaryStage.setScene(new Scene(adminFilmSelector(primaryStage, username), 450, 200));
            }
        });

        Button addHall = new Button();
        addHall.setText("Add Hall");
        addHall.setLayoutY(405);
        addHall.setLayoutX(160);
        addHall.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(addHall(primaryStage, film, username), 250, 280));
            }
        });

        Button removeHall = new Button();
        removeHall.setText("Remove Hall");
        removeHall.setLayoutY(405);
        removeHall.setLayoutX(230);
        removeHall.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(removeHall(primaryStage, film, username), 480, 125));
            }
        });

        ChoiceBox hallSelector = new ChoiceBox<>();
        for (Hall i: hallList){
            if (i.getFilm().equals(film.getTitle())){
                hallSelector.getItems().add(i.getName());
                hallSelector.setValue(i.getName());
            }
        }
        hallSelector.setLayoutX(380);
        hallSelector.setLayoutY(405);

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutX(460);
        ok.setLayoutY(405);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Hall selectedHall = null;
                for (Hall i: hallList){
                    if (i.getName().equals(hallSelector.getValue())){
                        selectedHall = i;
                    }
                }
                primaryStage.setScene(new Scene(adminSeatSelector(primaryStage, selectedHall, username, film), selectedHall.getTotalColumn() * 62, selectedHall.getTotalRow() * 70));
            }
        });

        Pane adminTrailerScreen = new Pane();
        adminTrailerScreen.getChildren().addAll(mediaView, playPause, topText, rewind5sec, forward5sec, rewindToStart, volumeSlider, backButton, hallSelector, ok, addHall, removeHall);
        return adminTrailerScreen;
    }

    public Pane editUsers(Stage primaryStage, String username){
        TableView<User> userTable = new TableView<User>();
        TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<User, String> clubMemberColumn = new TableColumn<>("Club Member");
        clubMemberColumn.setCellValueFactory(cellData -> {
            boolean tf = cellData.getValue().getClubMember();
            String tfString;
            if(tf)
            {
                tfString = "True";
            }
            else
            {
                tfString = "False";
            }

            return new ReadOnlyStringWrapper(tfString);
        });
        TableColumn<User, String> adminColumn = new TableColumn<>("Admin");
        adminColumn.setCellValueFactory(cellData -> {
            boolean tf = cellData.getValue().getAdmin();
            String tfString;
            if(tf)
            {
                tfString = "True";
            }
            else
            {
                tfString = "False";
            }

            return new ReadOnlyStringWrapper(tfString);
        });
        for (User i: userList){
            userTable.getItems().add(i);
        }
        userTable.getColumns().addAll(usernameColumn, clubMemberColumn, adminColumn);
        userTable.setLayoutX(130);
        userTable.setLayoutY(15);
        userTable.setMaxHeight(360);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(15);
        backButton.setLayoutY(390);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminFilmSelector(primaryStage, username), 450, 200));
            }
        });

        Button clubMember = new Button();
        clubMember.setText("Promote/Demote Club Member");
        clubMember.setLayoutY(390);
        clubMember.setLayoutX(115);
        clubMember.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                for (User i: userList){
                    if (userTable.getSelectionModel().getSelectedItem() != null && i.getUsername().equals(userTable.getSelectionModel().getSelectedItem().getUsername())){
                        i.setClubMember();
                        primaryStage.setScene(new Scene(editUsers(primaryStage, username), 500, 420));
                    }
                }
            }
        });

        Button admin = new Button();
        admin.setText("Promote/Demote Admin");
        admin.setLayoutX(320);
        admin.setLayoutY(390);
        admin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                for (User i: userList){
                    if (userTable.getSelectionModel().getSelectedItem() != null && i.getUsername().equals(userTable.getSelectionModel().getSelectedItem().getUsername())){
                        i.setAdmin();
                        primaryStage.setScene(new Scene(editUsers(primaryStage, username), 500, 420));
                    }
                }
            }
        });

        Pane editUsers = new Pane();
        editUsers.getChildren().addAll(userTable, backButton, clubMember, admin);
        return editUsers;
    }

    public Pane removeFilm(Stage primaryStage, String username){

        Text topText = new Text();
        topText.setText("Select the film that you desire to remove and then click OK.");
        topText.setX(20);
        topText.setY(20);

        ChoiceBox selectedFilm = new ChoiceBox<>();
        for (Film i: filmList){
            selectedFilm.getItems().add(i.getTitle());
            selectedFilm.setValue(i.getTitle());
        }
        selectedFilm.setLayoutX(40);
        selectedFilm.setLayoutY(50);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(40);
        backButton.setLayoutY(90);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminFilmSelector(primaryStage, username), 450, 200));
            }
        });

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutX(280);
        ok.setLayoutY(90);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String film = selectedFilm.getValue().toString();
                Film filmToRemove = null;
                Hall hallToRemove = null;
                for (Film i: filmList){
                    if (i.getTitle().equals(film)){
                        filmToRemove = i;
                    }
                }
                for (Hall i: hallList){
                    if (i.getFilm().equals(film)){
                        hallToRemove = i;
                    }
                }
                filmList.remove(filmToRemove);
                hallList.remove(hallToRemove);
                primaryStage.setScene(new Scene(removeFilm(primaryStage, username), 350, 150));
            }
        });

        Pane removeFilm = new Pane();
        removeFilm.getChildren().addAll(topText, selectedFilm, backButton, ok);
        return removeFilm;
    }

    public Pane addFilm(Stage primaryStage, String username){

        Text topText = new Text();
        topText.setText("Please give name, relative path of the trailer and duration of the film.");
        topText.setY(20);
        topText.setX(20);

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setY(230);
        bottomText.setX(100);

        Text name = new Text();
        name.setText("Name:");
        name.setX(70);
        name.setY(60);

        Text trailerPath = new Text();
        trailerPath.setText("Trailer (Path):");
        trailerPath.setX(70);
        trailerPath.setY(90);

        Text duration = new Text();
        duration.setText("Duration (m):");
        duration.setX(70);
        duration.setY(120);

        TextField nameField = new TextField();
        nameField.setLayoutX(160);
        nameField.setLayoutY(45);

        TextField trailerField = new TextField();
        trailerField.setLayoutX(160);
        trailerField.setLayoutY(75);

        TextField durationField = new TextField();
        durationField.setLayoutX(160);
        durationField.setLayoutY(105);

        Button backButton = new Button();
        backButton.setText("◀ BACK");
        backButton.setLayoutX(60);
        backButton.setLayoutY(170);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(adminFilmSelector(primaryStage, username), 450, 200));
            }
        });

        Button ok = new Button();
        ok.setText("OK");
        ok.setLayoutX(300);
        ok.setLayoutY(170);
        ok.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                File existCheck = new File("assets/trailers/" + trailerField.getText());
                Boolean exists = existCheck.exists();
                Boolean isPosInt = false;
                try{
                    if (Integer.parseInt(durationField.getText()) > 0){
                        isPosInt = true;
                    }
                }catch (NumberFormatException e){
                    isPosInt = false;
                }
                if (nameField.getText().isEmpty()){
                    bottomText.setText("ERROR : Film name could not be empty!");
                    errorSound.play();
                } else if (trailerField.getText().isEmpty()) {
                    bottomText.setText("ERROR : Trailer path could not be empty!");
                    errorSound.play();
                } else if (durationField.getText().isEmpty()) {
                    bottomText.setText("ERROR : Duration could not be empty!");
                    errorSound.play();
                } else if (!isPosInt) {
                    bottomText.setText("ERROR : Duration has to be a positive integer!");
                    errorSound.play();
                } else if (!exists) {
                    bottomText.setText("ERROR : There is no such trailer!");
                    errorSound.play();
                }
                else {
                    filmList.add(new Film(nameField.getText(), trailerField.getText(), Integer.parseInt(durationField.getText())));
                    bottomText.setText("SUCCESS : Film added successfully!");
                }

            }
        });


        Pane addFilm = new Pane();
        addFilm.getChildren().addAll(topText, name, trailerPath, duration, nameField, trailerField, durationField, backButton, ok, bottomText);
        return addFilm;
    }

    public Pane adminFilmSelector(Stage primaryStage,String username){
        // Film Select Screen
        Text topText = new Text();
        topText.setText("             Welcome " + username + "\nYou can either select film below or do edits.");
        topText.setX(80);
        topText.setY(40);

        ChoiceBox choiceBox = new ChoiceBox<>();
        for (Film i:filmList){
            choiceBox.setValue(i.getTitle());
            choiceBox.getItems().add(i.getTitle());
        }

        choiceBox.setLayoutX(40);
        choiceBox.setLayoutY(90);

        Button okButton = new Button();
        okButton.setText("OK");
        okButton.setLayoutX(390);
        okButton.setLayoutY(90);
        okButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String selection = choiceBox.getValue().toString();
                Film selectedFilm = null;
                for (Film i: filmList){
                    if (selection.equals(i.getTitle())){
                        selectedFilm = i;
                    }
                }
                primaryStage.setScene(new Scene(adminTrailerScreen(primaryStage, selectedFilm, username), 650, 450));
            }
        });

        Button logOut = new Button();
        logOut.setText("LOG OUT");
        logOut.setLayoutX(350);
        logOut.setLayoutY(130);
        logOut.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.setScene(new Scene(welcomeRoot(primaryStage), 450, 250));
            }
        });

        Button addFilm = new Button();
        addFilm.setText("Add Film");
        addFilm.setLayoutX(40);
        addFilm.setLayoutY(130);
        addFilm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(addFilm(primaryStage, username), 400, 250));
            }
        });

        Button removeFilm = new Button();
        removeFilm.setText("Remove Film");
        removeFilm.setLayoutX(120);
        removeFilm.setLayoutY(130);
        removeFilm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(removeFilm(primaryStage, username), 350, 150));
            }
        });

        Button editUsers = new Button();
        editUsers.setText("Edit Users");
        editUsers.setLayoutX(220);
        editUsers.setLayoutY(130);
        editUsers.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(new Scene(editUsers(primaryStage, username), 500, 420));
            }
        });


        Pane filmSelector = new Pane();
        filmSelector.getChildren().addAll(topText, choiceBox, okButton, logOut, addFilm, removeFilm, editUsers);
        return filmSelector;
    }
    public Pane signupRoot(Stage primaryStage){
        // SIGN UP SCREEN
        Text topText = new Text();
        topText.setText("Welcome to the HUCS Cinema Reservation System!\n     Fill the form below to create a new account.\nYou can go to Log in page by clicking LOG IN Button.");
        topText.setX(50);
        topText.setY(40);

        Text signUpUsername = new Text();
        signUpUsername.setText("Username: ");
        signUpUsername.setX(80);
        signUpUsername.setY(120);

        Text signUpPassword = new Text();
        signUpPassword.setText("Password: ");
        signUpPassword.setX(80);
        signUpPassword.setY(160);

        Text signUpPassword2 = new Text();
        signUpPassword2.setText("Password: ");
        signUpPassword2.setX(80);
        signUpPassword2.setY(200);

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setX(80);
        bottomText.setY(290);

        TextField usernameField = new TextField();
        usernameField.setLayoutX(170);
        usernameField.setLayoutY(100);

        PasswordField passwordField = new PasswordField();
        passwordField.setLayoutX(170);
        passwordField.setLayoutY(140);

        PasswordField passwordField2 = new PasswordField();
        passwordField2.setLayoutX(170);
        passwordField2.setLayoutY(180);

        Button signupButton = new Button();
        signupButton.setLayoutX(90);
        signupButton.setLayoutY(240);
        signupButton.setText("SIGN UP");
        signupButton.setOnAction(new EventHandler() {
            @Override
            public void handle(Event event) {
                Boolean usernameError = false;
                String usernameInput = usernameField.getText();
                String passwordInput = passwordField.getText();
                String passwordInput2 = passwordField2.getText();
                for (User i : userList){
                    if (usernameInput.equals(i.getUsername())){
                        usernameError = true;
                        bottomText.setText("ERROR : This username already exists!");
                        errorSound.play();
                    }
                }
                if (usernameInput.isEmpty() && !usernameError){
                    bottomText.setText("ERROR : Username cannot be empty!");
                    errorSound.play();
                }
                else if (passwordInput.isEmpty() && !usernameError){
                    bottomText.setText("ERROR : Password cannot be empty!");
                    errorSound.play();
                }
                else if (!passwordInput.equals(passwordInput2) && !usernameError){
                    bottomText.setText("ERROR : Passwords do not match!");
                    errorSound.play();
                }
                else if (!usernameError){
                    userList.add(new User(usernameInput, hashPassword(passwordInput), false, false));
                    bottomText.setText("SUCCESS : You hace succesfully registered.");
                }

            }
        });

        Button signinButton = new Button();
        signinButton.setLayoutX(280);
        signinButton.setLayoutY(240);
        signinButton.setText("SIGN IN");
        signinButton.setOnAction(new EventHandler() {
            @Override
            public void handle(Event event) {
                primaryStage.setScene(new Scene(welcomeRoot(primaryStage), 450, 270));
            }
        });

        Pane signupRoot = new Pane();
        signupRoot.getChildren().addAll(topText, signUpUsername, signUpPassword, signUpPassword2, usernameField, passwordField, passwordField2, signupButton, signinButton, bottomText);
        return signupRoot;
    }

    public Pane welcomeRoot(Stage primaryStage){
        // WELCOME SCREEN
        Button signup = new Button();
        signup.setLayoutX(113);
        signup.setLayoutY(200);
        signup.setText("SIGN UP");
        signup.setOnAction(event -> primaryStage.setScene(new Scene(signupRoot(primaryStage), 450, 300)));

        TextField usernameInput = new TextField();
        usernameInput.setLayoutX(170);
        usernameInput.setLayoutY(100);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setLayoutX(170);
        passwordInput.setLayoutY(140);

        Text bottomText = new Text();
        bottomText.setText("");
        bottomText.setX(95);
        bottomText.setY(250);

        Button signin = new Button();
        final int[] wrongTries = {0};
        signin.setLayoutX(253);
        signin.setLayoutY(200);
        signin.setText("SIGN IN");
        signin.setOnAction(new EventHandler() {
            @Override
            public void handle(Event event) {
                if (wrongTries[0] < maxTries){
                    Boolean isAdmin = false;
                    Boolean isClubMember = false;
                    Boolean userExists = false;
                    Boolean passwordTrue = false;
                    String username = usernameInput.getText();
                    String hashedPassword = hashPassword(passwordInput.getText());
                    for (User i: userList){
                        if (i.getUsername().equals(username)){
                            userExists = true;
                            if (i.getHashedPassword().equals(hashedPassword)){
                                if (i.getAdmin()){
                                    isAdmin = true;
                                }
                                if (i.getClubMember()){
                                    isClubMember = true;
                                }
                                passwordTrue = true;
                            }
                        }
                    }
                    if (username.isEmpty()){
                        bottomText.setText("ERROR : Username cannot be empty!");
                        errorSound.play();
                        wrongTries[0]++;
                    }
                    else if (hashedPassword.isEmpty()){
                        bottomText.setText("ERROR : Password cannot be empty!");
                        errorSound.play();
                        wrongTries[0]++;
                    }
                    else if (!userExists){
                        bottomText.setText("ERROR : User doesn't exist!");
                        errorSound.play();
                        wrongTries[0]++;
                    }
                    else if (!passwordTrue) {
                        bottomText.setText("ERROR : Invalid password!");
                        errorSound.play();
                        wrongTries[0]++;
                    }
                    else {
                        if (!isAdmin){
                            primaryStage.setScene(new Scene(normalFilmSelector(primaryStage, username), 450, 200));
                        }
                        else {
                            primaryStage.setScene(new Scene(adminFilmSelector(primaryStage, username), 450, 200));
                        }

                    }
                }
                else {
                    bottomText.setText("ERROR : Wait for 5 seconds before new try!");
                    errorSound.play();
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            wrongTries[0] = 0;
                            bottomText.setText("You can try again now.");
                        }}, blockTime * 1000);
                }
            }
        });

        Text welcome = new Text();
        welcome.setText("     Welcome to HUCS Cinema Reservation System!\n  Please enter your credentials below and click LOGIN.\nYou can create a new account by clicking SIGN UP button.");
        welcome.setX(40);
        welcome.setY(40);
        welcome.setFont(Font.font("Verdana",13));

        Text username = new Text();
        username.setText("Username: ");
        username.setX(80);
        username.setY(120);

        Text password = new Text();
        password.setText("Password: ");
        password.setX(80);
        password.setY(160);

        Pane login = new Pane();
        login.getChildren().addAll(signup,welcome,signin,username,password,usernameInput,passwordInput,bottomText);
        return login;
    }


    @Override
    public void start(Stage primaryStage) {
        readData();
        File appIcon = new File("assets/icons/logo.png");
        primaryStage.getIcons().add(new Image(appIcon.toURI().toString()));
        primaryStage.setTitle(appTitle);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(welcomeRoot(primaryStage), 450, 270));
        primaryStage.show();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File backup = new File("assets/data/backup.dat");
            FileWriter backupWriter = null;
            try{
                backupWriter = new FileWriter(backup);
                for (User i: userList){
                    backupWriter.write("user\t" + i.getUsername() + "\t" + i.getHashedPassword() + "\t" + i.getClubMember().toString() + "\t" + i.getAdmin().toString() + "\n");
                }
                for (Film i: filmList){
                    backupWriter.write("film\t" + i.getTitle() + "\t" + i.getTrailerPath() + "\t" + i.getDuration() + "\n");
                }
                for (Hall i: hallList){
                    backupWriter.write("hall\t" + i.getFilm() + "\t" + i.getName() + "\t" + i.getPrice() + "\t" + i.getTotalRow() + "\t" + i.getTotalColumn() + "\n");
                }
                for (Hall i: hallList){
                    for (int i1 = 0; i1 < i.getSeats().length; i1++){
                        for (int i2 = 0; i2 < i.getSeats()[i1].length; i2++){
                            backupWriter.write("seat\t" + i.getFilm() + "\t" + i.getName() + "\t" + i1 + "\t" + i2 + "\t" + i.getSeats()[i1][i2].getOwner() + "\t" + i.getSeats()[i1][i2].getPriceBought() + "\n");
                        }
                    }
                }
                backupWriter.close();
            }
            catch (IOException e){
                System.out.println("IOExceptionnnnn");
            }
        }));

    }

    public static class User {
        private String username;
        private String hashedPassword;
        private Boolean isAdmin;
        private Boolean isClubMember;

        public User(String username, String hashedPassword, Boolean isClubMember, Boolean isAdmin) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.isAdmin = isAdmin;
            this.isClubMember = isClubMember;
        }

        public String getUsername() {
            return username;
        }

        public String getHashedPassword() {
            return hashedPassword;
        }

        public Boolean getAdmin() {
            return isAdmin;
        }

        public Boolean getClubMember() {
            return isClubMember;
        }

        public void setAdmin() {
            if (this.isAdmin){
                this.isAdmin = false;
            }
            else {
                this.isAdmin = true;
            }
        }

        public void setClubMember() {
            if (this.isClubMember){
                this.isClubMember = false;
            }
            else {
                this.isClubMember = true;
            }
        }
    }

    public static class Seat {
        private int row;
        private int column;
        private String owner;
        private int priceBought;

        private Boolean gotSnacks;

        public Seat(int row, int column, String owner, int priceBought) {
            this.row = row;
            this.column = column;
            this.owner = owner;
            this.priceBought = priceBought;
            gotSnacks = false;
        }

        public int getColumn() {
            return column;
        }

        public int getPriceBought() {
            return priceBought;
        }

        public int getRow() {
            return row;
        }

        public String getOwner() {
            return owner;
        }

        public Boolean getGotSnacks() {
            return gotSnacks;
        }

        public void setGotSnacks(Boolean gotSnacks) {
            this.gotSnacks = gotSnacks;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public void setPriceBought(int priceBought) {
            this.priceBought = priceBought;
        }
    }

    public static class Hall {
        private String film;
        private String name;
        private int price;
        private int totalRow;
        private int totalColumn;
        private Seat[][] seats;

        public Hall(String name, int price, int row, int column, String film) {
            this.name = name;
            this.price = price;
            this.totalRow = row;
            this.totalColumn = column;
            this.film = film;
            seats = new Seat[totalRow][totalColumn];
        }

        public void addSeat(Seat seat) {
            seats[seat.getRow()][seat.getColumn()] = seat;
        }

        public Seat[][] getSeats() {
            return seats;
        }

        public void setSeats(Seat[][] seats) {
            this.seats = seats;
        }

        public int getPrice() {
            return price;
        }

        public int getTotalColumn() {
            return totalColumn;
        }

        public int getTotalRow() {
            return totalRow;
        }

        public String getFilm() {
            return film;
        }

        public String getName() {
            return name;
        }
    }

    public static class Film {
        private String title;
        private int duration;
        private String trailerPath;

        public Film(String title, String trailerPath, int duration) {
            this.title = title;
            this.duration = duration;
            this.trailerPath = trailerPath;
        }

        public String getTitle() {
            return title;
        }

        public int getDuration() {
            return duration;
        }

        public String getTrailerPath() {
            return trailerPath;
        }
    }
}