import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SubScene;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.chart.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;

/**
 * Takes information sent to the display and prints it to the screen.
 * The screen is a JavaFX pane.
 * Denis Labrecque, November 2020
 */
public class ScreenDisplay extends DisplayAssembly {

    Pane pane;
    Circle tonerLight;
    Circle drumLight;
    Circle errorLight;
    Circle readyLight;
	
	int tonerLevel = 65;
	int drumLevel = 95;
		
	XYChart.Series series = new XYChart.Series();

    /**
     * Constructor. Create a console display with a reference back to the printer.
     * @param printer The printer this display is showing information for.
     */
    public ScreenDisplay(LaserPrinter printer, Pane screen) {
        super(printer);
        screen.setPrefSize(400, 300);
        this.pane = screen;
        displayWindowOff();
    }

    /**
     * Passes in the different JavaFX LEDs so they can be updated and animated by the display.
     * @param tonerLight
     * @param drumLight
     * @param errorLight
     * @param readyLight
     */
    public void addLights(Circle tonerLight, Circle drumLight, Circle errorLight, Circle readyLight) {
        System.out.println("DEBUG: lights added");
        this.tonerLight = tonerLight;
        this.drumLight = drumLight;
        this.errorLight = errorLight;
        this.readyLight = readyLight;
    }

    /**
     * Trigger a re-print of the display's information. Only prints lights if the display is not on.
     */
    @Override
    public void refresh() {
        clearScreen();

        if(activated) {
            printMessages();
        }

        displayTonerWarningError();
        displayDrumWarningError();
        displayGeneralError();
        displayReadyState();

        System.out.println("DEBUG: refresh() called and displaying window ->");
        displayWindow(currentWindow);
    }

    public void clearScreen() {
        for (int i = 0; i < 10; i++)
            System.out.println();

        //pane.getChildren().clear();
    }

    private void printMessages() {
        // Print message
        if (currentMessage != null) {
            System.out.println("MESSAGE");
            System.out.println("   " + currentMessage);
        }
    }


    @Override
    public void reportStatus() {
        clearScreen();

        System.out.println("PAPER TRAY");
        System.out.println("   " + printer.paperTray().getValue());

        System.out.println("PRINT QUEUE");
        System.out.println("   " + printer.queue().getValue());

        System.out.println("OUTPUT TRAY");
        System.out.println("   " + printer.outputTray().getValue());

        System.out.println("PRINT ASSEMBLY");
        System.out.println("   " + printer.printAssembly().getValue());

        System.out.println("FUSER TEMPERATURE");
        System.out.println("   " + printer.fuser().getValue());

        System.out.println("TONER");
        System.out.println("   " + printer.toner().getValue());
    }

    /**
     * Show the status of the queue.
     */
    @Override
    public void reportQueue() {
        if(activated) { // Display must be on to show messages
            clearScreen();
            System.out.println("QUEUE");
            System.out.println("   " + printer.queue().getValue());
        }
    }

    /**
     * Display toner warning/error LED.
     * Yellow if warning, flashing red if error.
     */
    @Override
    public void displayTonerWarningError() {
        setTonerLightState();
        setLED(tonerLED, tonerLight);
        System.out.println("TONER");
        System.out.println("   " + tonerLED);

        // Display must be on to show error messages
        if(activated && printer.containsErrorFor(printer.toner()) != null)
            System.out.println("   " + printer.containsErrorFor(printer.toner()).getMessage());
        if(activated && printer.toner().isWarning())
            System.out.println("   " + printer.toner().warning());
    }

    /**
     * Display drum warning/error LED.
     * Yellow if warning, flashing red if error.
     */
    @Override
    public void displayDrumWarningError() {
        setDrumLightState();
        setLED(drumLED, drumLight);
        System.out.println("DRUM");
        System.out.println("   " + drumLED);

        if(activated && printer.exceptions().containsKey(AssemblyException.PrinterIssue.DRUM))
            System.out.print(printer.exceptions().get(AssemblyException.PrinterIssue.DRUM).getMessage());
        if(activated && printer.printAssembly().isWarning())
            System.out.print(printer.printAssembly().drumWarning());
    }

    /**
     * Display general error LED (jam typically).
     * Flashing red if error.
     */
    @Override
    public void displayGeneralError() {
        setErrorLightState();
        setLED(errorLED, errorLight);
        System.out.println("ERROR");
        System.out.println("   " + errorLED);

        if(activated) { // Display must be on to show error messages
            if (printer.isError()) {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<AssemblyException.PrinterIssue, AssemblyException> entry : printer.exceptions().entrySet())
                    builder.append("   " + entry.getValue().getMessage() + "\n");
                System.out.print(builder.toString());
            }
        }
    }

    /**
     * Display powering up, printing, waiting LED.
     * Flashing green while powering up or printing, solid green otherwise.
     */
    @Override
    public void displayReadyState() {
        setReadyLightState();
        setLED(readyLED, readyLight);
        System.out.println("READY");
        System.out.println("   " + readyLED);
    }

    /**
     * Set the JavaFX circle that represents the LED to match its object representation.
     * @param lightObject Object that contains what the light's pattern and color should be
     * @param circle JavaFX light that represents a real-world LED
     */
    private void setLED(Light lightObject, Circle circle) {
        if(lightObject.pattern() == Light.Pattern.OFF) {
            circle.setFill(Color.WHITE);
            return;
        }
        else {
            circle.setFill(lightObject.color());
        }


//        // Flashing transition
//        if(lightObject.pattern() == Light.Pattern.FLASHING) {
//            FillTransition transition = new FillTransition(Duration.millis(1000), circle, lightObject.color(), Color.WHITE);
//            transition.setCycleCount(Timeline.INDEFINITE);
//            transition.setAutoReverse(true);
//            transition.play();
//        }
//        else {
//            FillTransition transition = new FillTransition(Duration.ZERO, circle, lightObject.color(), lightObject.color());
//            transition.setCycleCount(1);
//            transition.play();
//            transition.stop();
//        }
    }


    @Override
    protected void displayWindowOff() {
        System.out.println("DEBUG: window " + currentWindow);
        pane.getChildren().clear();
        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    @Override
    protected void displayWelcomeWindow() {
        clearWithColor(Color.BLACK);
        Label greeting = new Label("Laser Printer");
        greeting.getStyleClass().add("white");
        greeting.getStyleClass().add("h1");

        Label label1 = new Label("A project presented by:");
        label1.getStyleClass().add("white");
        Label label2 = new Label("Joel Luther");
        label2.getStyleClass().add("white");
        Label label3 = new Label("Cameron Keefe");
        label3.getStyleClass().add("white");
        Label label4 = new Label("Denis Labrecque");
        label4.getStyleClass().add("white");
        Label label5 = new Label("Hendy Nathaniel");
        label5.getStyleClass().add("white");
        Label label6 = new Label("Zahin Uriostegui");
        label6.getStyleClass().add("white");
        pane.getChildren().addAll(greeting, label1, label2, label3, label4, label5, label6);
    }

    @Override
    protected void displayExitWindow() {
        clearWithColor(Color.WHITE);
        Text text = new Text("Exit Window"); // TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(text);
    }

    @Override
    protected void displayErrorWindow() {
        clearWithColor(Color.WHITE);
        Text text = new Text("Error Screen"); // TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(text);
    }

    @Override
    protected void displayPrintQueueWindow() {
        clearWithColor(Color.WHITE);
        Text text = new Text("Print Queue"); // TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(text);
    }

    @Override
    protected void displayFuserWindow() {
        clearWithColor(Color.WHITE);
        Text text = new Text("Fuser Screen"); // TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(text);
    }

    @Override
    protected void displayTonerAndDrumWindow() {
		
		Stage mainStage = new Stage();
		
		Pane rootPane = new Pane();
		Scene scene = new Scene(rootPane, 750, 500);
		
		mainStage.setScene(scene);
		
        clearWithColor(Color.ORANGE);
		Label screenHeader = new Label("Toner and Drum Levels");
        screenHeader.getStyleClass().add("black");
		screenHeader.getStyleClass().add("h1");
		// TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(screenHeader);
		
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Levels");
		
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Quanity %");
		
		BarChart barChart = new BarChart(xAxis, yAxis);
		
		series.getData().add(new XYChart.Data("Toner", tonerLevel));
		series.getData().add(new XYChart.Data("Drum", drumLevel));
		
		barChart.getData().add(series);
		
		Button replaceTonerButton = new Button("Replace Toner");
		replaceTonerButton.setOnAction( e -> replaceTonerAction());
		Button replaceDrumButton = new Button("Replace Drum");
		replaceDrumButton.setOnAction(e -> replaceDrumAction());
		
		HBox levelButtons = new HBox();
		levelButtons.setSpacing(50);
		levelButtons.setAlignment(Pos.CENTER);
		levelButtons.getChildren().addAll(replaceTonerButton, replaceDrumButton);
		
		VBox levelLayout = new VBox(10);
		levelLayout.setPadding(new Insets(5, 5, 5, 50));
		levelLayout.getChildren().addAll(barChart, levelButtons);
		
		HBox displayElements = new HBox();
		displayElements.setSpacing(10);
		displayElements.setAlignment(Pos.CENTER);
		displayElements.getChildren().addAll(levelLayout);
		
		Line sepLine = new Line(0, 0, 700, 0);
		sepLine.setStrokeWidth(10);
		sepLine.setStroke(Color.BLACK);
		
		VBox lastLayout = new VBox(10);
		lastLayout.setPadding(new Insets(5, 5, 5, 50));
		lastLayout.getChildren().addAll(displayElements, sepLine);
		
		scene.setRoot(lastLayout);
		
		mainStage.show();
    }

    @Override
    protected void displayPaperWindow() {
        clearWithColor(Color.WHITE);
        Text text = new Text("Paper Trays Screen"); // TODO this is just a stub; put a panel here with your graphics
        pane.getChildren().add(text);
    }

    /**
     * Empty the window of any contents, and change the background color.
     * @param color Black if the printer is supposed to be off, and white if the printer is on.
     */
    private void clearWithColor(Color color) {
        System.out.println("DEBUG: window " + currentWindow);
        pane.getChildren().clear();
        pane.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
    }
	
	private void replaceTonerAction() {
		tonerLevel = 100;
		series.getData().clear();
		series.getData().add(new XYChart.Data("Toner", tonerLevel));
		series.getData().add(new XYChart.Data("Drum", drumLevel));
	}
	
	private void replaceDrumAction() {
		drumLevel = 100;
		series.getData().clear();
		series.getData().add(new XYChart.Data("Toner", tonerLevel));
		series.getData().add(new XYChart.Data("Drum", drumLevel));
	}
}