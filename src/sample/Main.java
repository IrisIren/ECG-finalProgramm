package sample;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private static final int MAX_FRAME_POINTS = 4000;
    private static ConcurrentLinkedQueue<Double> dataQ = new ConcurrentLinkedQueue<Double>();
    private static ConcurrentLinkedQueue<Double> RRPeaks = new ConcurrentLinkedQueue<Double>();
    private XYChart.Series<Number, Number> series1;
    private XYChart.Series<Number, Number> series2;
    private int xSeriesData;

    private NumberAxis xAxis;

    private void init(Stage primaryStage) {
        xAxis = new NumberAxis(0, MAX_FRAME_POINTS, MAX_FRAME_POINTS / 10);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);

        final AreaChart<Number, Number> sc = new AreaChart<Number, Number>(xAxis, yAxis) {
            @Override
            protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
            }
        };

        sc.setAnimated(false);
        sc.setId("realtime ECG graph");
        sc.setTitle("ECG-signal");
        sc.getStylesheets().add(Main.class.getResource("chart.css").toExternalForm());
        series1 = new AreaChart.Series<Number, Number>();
        series1.setName("realtime ECG graph");
        sc.getData().add(series1);

        series2 = new Series<Number, Number>();
        series2 = new AreaChart.Series<Number, Number>();
        series2.setName("RRPeaks");
        sc.getData().add(series2);
        //ObservableList<XYChart.Data> datas = FXCollections.observableArrayList();
        //datas.add(new XYChart.Data(400, 300));
       // datas.add(new XYChart.Data(500, 400));
        //series2.setData(datas);

        //sc.getData().add(series2);
        primaryStage.setScene(new Scene(sc));
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        init(primaryStage);
        primaryStage.show();

        prepareTimeline();
    }

    public static void main(String[] args) {

        AtomicBoolean fileIsFinished = new AtomicBoolean(false);
        BlockingQueue<Double> queue = new LinkedBlockingQueue<Double>();

        BluetoothModelReceiver bluetoothModelReceiver = new BluetoothModelReceiver(queue, "D:\\ECG.txt", fileIsFinished);
        bluetoothModelReceiver.start();

        ECGSignalHandler ecgSignalHandler = new ECGSignalHandler(queue, dataQ, RRPeaks, fileIsFinished);
        ecgSignalHandler.start();
        launch(args);
    }

    private void prepareTimeline() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                addDataToSeries();
            }
        }.start();
    }

    private void addDataToSeries() {
        for (int i = 0; i < MAX_FRAME_POINTS / 50; i++) {
            if (dataQ.isEmpty()) break;
            series1.getData().add(new AreaChart.Data<Number, Number>(xSeriesData++, dataQ.remove()));
        }

        for (int i = 0; i < MAX_FRAME_POINTS / 50; i++) {
            if (RRPeaks.isEmpty()) break;
            series2.getData().add(new AreaChart.Data<Number, Number>(xSeriesData++, RRPeaks.remove()));
        }

        if (series1.getData().size() > MAX_FRAME_POINTS) {
            series1.getData().remove(0, series1.getData().size() - MAX_FRAME_POINTS);
        }
        xAxis.setLowerBound(Math.max(xSeriesData - MAX_FRAME_POINTS, 0));
        xAxis.setUpperBound(Math.max(xSeriesData - 1, MAX_FRAME_POINTS));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}