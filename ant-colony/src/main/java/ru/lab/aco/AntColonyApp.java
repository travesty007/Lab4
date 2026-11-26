package ru.lab.aco;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class AntColonyApp extends Application {
    private final Canvas canvas = new Canvas(800, 530);
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Label status = new Label(), stats = new Label(), routeLabel = new Label();
    private final Button start = new Button("Старт"), step = new Button("Один цикл"), replay = new Button("Показать лучший путь");
    private final Slider speed = new Slider(.2, 15, 2);
    private final CheckBox showBest = new CheckBox("Лучший маршрут"), showWeights = new CheckBox("Длины ребер");
    private final XYChart.Series<Number, Number> bestSeries = new XYChart.Series<>(), meanSeries = new XYChart.Series<>();
    private AntColony colony;
    private ru.lab.aco.Parameters parameters;
    private boolean running, replaying;
    private double progress, optimum = Double.NaN;
    private long previous;
    private Stage stage;
    private VBox inputs;
    private BorderPane root;

    @Override public void start(Stage stage) {
        stage.initStyle(javafx.stage.StageStyle.DECORATED);
        stage.setResizable(true);
        this.stage = stage;
        Label title = new Label("Муравьиная колония"); title.setStyle("-fx-font-size: 27px; -fx-font-weight: bold;");
        Label subtitle = new Label("Задача коммивояжёра · Ant System · неориентированный граф");
        VBox header = new VBox(4, title, subtitle); header.setPadding(new Insets(18, 22, 15, 22));
        inputs = new VBox(7);
        addInput("Города", "10"); addInput("Муравьи", "30"); addInput("Циклы M", "200");
        addInput("α · феромон", "1"); addInput("β · расстояние", "3");
        addInput("ρ · испарение", "0.35"); addInput("Q · отложение", "100"); addInput("Seed", "42");
        Button reset = new Button("Применить / сбросить"); reset.setMaxWidth(Double.MAX_VALUE); reset.setOnAction(e -> reset());
        start.setOnAction(e -> toggle()); step.setOnAction(e -> step()); replay.setOnAction(e -> replay());
        for (Button b : List.of(start, step, replay)) b.setMaxWidth(Double.MAX_VALUE);
        speed.setShowTickLabels(true); speed.setMajorTickUnit(5);
        showBest.setSelected(true); showBest.setOnAction(e -> draw()); showWeights.setOnAction(e -> draw());
        Label hint = new Label("Первый цикл: равномерный случайный выбор. Далее: вероятность по феромону и длине."); hint.setWrapText(true);
        VBox sidebar = new VBox(10, inputs, reset, new Separator(), start, step, replay,
                new Label("Скорость · ребер/сек"), speed, showBest, showWeights, hint);
        sidebar.setPrefWidth(260); sidebar.setPadding(new Insets(16));
        NumberAxis x = new NumberAxis(), y = new NumberAxis(); x.setLabel("Цикл"); y.setLabel("Длина маршрута"); y.setForceZeroInRange(false);
        LineChart<Number,Number> chart = new LineChart<>(x,y);
        chart.setAnimated(false); chart.setCreateSymbols(false); chart.setPrefHeight(210); chart.setMinHeight(180);
        bestSeries.setName("Лучший за все циклы"); meanSeries.setName("Средний в цикле"); chart.getData().add(bestSeries); chart.getData().add(meanSeries);
        Label legend = new Label("Синие ребра: феромон     Зелёный: лучший путь     Оранжевые точки: муравьи");
        VBox graphPane = new VBox(6, canvas, legend, chart); graphPane.setPadding(new Insets(10));
        ScrollPane center = new ScrollPane(graphPane); center.setFitToWidth(true);
        stats.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"); routeLabel.setWrapText(true);
        VBox footer = new VBox(5, status, stats, routeLabel); footer.setPadding(new Insets(12,22,16,22));
        root = new BorderPane(center,header,null,footer,sidebar);
        root.setStyle("-fx-font-family: 'DejaVu Sans'; -fx-font-size: 12px; -fx-background-color: #f3f6fb;");
        stage.setTitle("Лабораторная работа · Муравьиный алгоритм");
        var screen = javafx.stage.Screen.getPrimary().getVisualBounds();

        double width = Math.min(1120, screen.getWidth() * 0.95);
        double height = Math.min(965, screen.getHeight() * 0.90);

        stage.setScene(new Scene(root, width, height));
        stage.setMinWidth(Math.min(800, width));
        stage.setMinHeight(Math.min(600, height));

        reset();
        stage.show();

// Учитываем системную рамку и размещаем окно внутри экрана.
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = previous == 0 ? 0 : Math.min(.1, (now-previous)/1e9); previous = now;
                if (replaying) { progress = (progress + dt * speed.getValue()) % colony.graph().size(); draw(); }
                else if (running) {
                    if (!colony.pending()) colony.beginIteration();
                    progress += dt * speed.getValue();
                    if (progress >= colony.graph().size()) {
                        colony.completeIteration(); progress = 0; update();
                        if (colony.finished()) running = false;
                    }
                    draw(); updateControls();
                }
            }
        }.start();
        // Автоматическое создание настоящих кадров JavaFX для отчета.
        if (!getParameters().getRaw().isEmpty()) Platform.runLater(() -> captureDemo(getParameters().getRaw().getFirst()));
    }
    private void addInput(String name, String value) {
        TextField field = new TextField(value); field.setPrefWidth(95); fields.put(name,field);
        Label label = new Label(name); label.setPrefWidth(120); inputs.getChildren().add(new HBox(8,label,field));
    }
    private String value(String name) { return fields.get(name).getText().trim(); }
    private double number(String name) { return Double.parseDouble(value(name).replace(',','.')); }
    private void reset() {
        try {
            int count = Integer.parseInt(value("Города"));
            ru.lab.aco.Parameters next = new ru.lab.aco.Parameters(Integer.parseInt(value("Муравьи")), Integer.parseInt(value("Циклы M")),
                    number("α · феромон"),number("β · расстояние"),number("ρ · испарение"),number("Q · отложение"),Long.parseLong(value("Seed")));
            Graph graph = Graph.random(count,next.seed());
            parameters = next; running = replaying = false; progress = 0;
            colony = new AntColony(graph,parameters);
            optimum = count <= 14 ? ExactSolver.solve(graph) : Double.NaN;
            bestSeries.getData().clear(); meanSeries.getData().clear(); update(); draw();
        } catch (IllegalArgumentException ex) { error("Проверьте параметры", ex); }
    }
    private void toggle() {
        if (colony.finished()) return;
        replaying = false; running = !running; updateControls();
    }
    private void step() {
        running = replaying = false;
        if (!colony.finished()) {
            if (!colony.pending()) colony.beginIteration();
            colony.completeIteration(); progress = 0; update(); draw();
        }
    }
    private void replay() {
        if (colony.bestRoute().isEmpty()) return;
        running = false; replaying = !replaying; progress = 0; updateControls(); draw();
    }
    private void update() {
        if (!colony.history().isEmpty()) {
            var s = colony.history().getLast();
            bestSeries.getData().add(new XYChart.Data<>(s.iteration(), s.best()));
            meanSeries.getData().add(new XYChart.Data<>(s.iteration(), s.mean()));
            String exact = Double.isFinite(optimum) ? String.format(Locale.ROOT,"Эталон: %.2f · Отклонение: %.2f%%",optimum,100*(s.best()/optimum-1)) : "Эталон: доступен до 14 городов";
            stats.setText(String.format(Locale.ROOT,"Лучший: %.2f · Средний: %.2f · Совпадений: %.0f%%\n%s",s.best(),s.mean(),s.matchingPercent(),exact));
            routeLabel.setText("Маршрут: " + String.join(" → ",colony.bestRoute().stream().map(i -> String.valueOf(i+1)).toList()));
        } else {
            stats.setText(Double.isFinite(optimum) ? String.format(Locale.ROOT,"Точная минимальная длина: %.2f",optimum) : "Точный эталон отключен для графов более 14 городов");
            routeLabel.setText("Нажмите «Старт» или «Один цикл». Изменения параметров действуют после сброса.");
        }
        updateControls();
    }
    private void updateControls() {
        start.setText(running ? "Пауза" : "Старт"); start.setDisable(colony.finished());
        step.setDisable(running || colony.finished()); replay.setDisable(colony.bestRoute().isEmpty());
        replay.setText(replaying ? "Остановить показ" : "Показать лучший путь");
        status.setText(replaying ? "ДЕМОНСТРАЦИЯ: все муравьи повторяют найденный путь; поиск приостановлен."
                : (colony.finished() ? "Поиск завершён" : running ? "Поиск" : "Пауза / готовность")
                + " · Цикл " + colony.iteration() + " / " + parameters.iterations()
                + (colony.iteration() == 0 ? " · Начальное случайное исследование" : ""));
    }
    private void line(GraphicsContext gc, int a, int b) {
        City p = colony.graph().cities().get(a), q = colony.graph().cities().get(b);
        gc.strokeLine(p.x(), p.y(), q.x(), q.y());
    }
    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D(); gc.setFill(Color.web("#101d31")); gc.fillRect(0,0,800,530);
        double max = colony.graph().edges().stream().mapToDouble(Edge::pheromone).max().orElse(1);
        for (Edge edge : colony.graph().edges()) {
            double ratio = edge.pheromone() / max;
            gc.setStroke(Color.color(.26,.56,.94,.06+.68*ratio)); gc.setLineWidth(.4+3*ratio);
            line(gc,edge.from().id(),edge.to().id());
            if (showWeights.isSelected()) {
                gc.setFill(Color.web("#bccbdf")); gc.setFont(Font.font(10));
                gc.fillText(String.format(Locale.ROOT,"%.0f",edge.length()),(edge.from().x()+edge.to().x())/2,(edge.from().y()+edge.to().y())/2);
            }
        }
        if (showBest.isSelected() || replaying) {
            gc.setStroke(Color.web("#4ee3a0")); gc.setLineWidth(3);
            for (int i=1; i<colony.bestRoute().size(); i++) line(gc,colony.bestRoute().get(i-1),colony.bestRoute().get(i));
        }
        for (City city : colony.graph().cities()) {
            gc.setFill(Color.web("#f1f5fd")); gc.fillOval(city.x()-9,city.y()-9,18,18);
            gc.setFill(Color.web("#ffffff")); gc.setFont(Font.font(13)); gc.fillText(""+(city.id()+1),city.x()+12,city.y()-10);
        }
        int antIndex = 0;
        for (Ant ant : colony.ants()) {
            List<Integer> route = replaying ? colony.bestRoute() : ant.route();
            double pos = replaying ? (progress + antIndex++ * (double)colony.graph().size()/parameters.ants()) % colony.graph().size()
                    : colony.pending() ? progress : colony.graph().size() - .00001;
            int edge = Math.min((int)pos,route.size()-2); double t = pos-edge;
            City a = colony.graph().cities().get(route.get(edge)), b = colony.graph().cities().get(route.get(edge+1));
            gc.setFill(Color.web("#ffb454")); gc.fillOval(a.x()+(b.x()-a.x())*t-4,a.y()+(b.y()-a.y())*t-4,8,8);
        }
    }
    private File choose(String title, String pattern) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(title,pattern));
        chooser.setInitialFileName(pattern.equals("*.csv") ? "statistics.csv" : "ant-colony.png");
        return chooser.showSaveDialog(stage);
    }
    private void writeCsv(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            writer.write("iteration,best,mean,matching_percent\n");
            for (var s : colony.history()) writer.write(String.format(Locale.ROOT,"%d,%.8f,%.8f,%.4f%n",s.iteration(),s.best(),s.mean(),s.matchingPercent()));
        }
    }
    private void savePng(Path path) throws IOException {
        root.applyCss(); root.layout(); ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",path.toFile());
    }
    private void captureDemo(String directory) {
        try {
            Path dir = Path.of(directory); Files.createDirectories(dir);
            colony.beginIteration(); progress = 3.4; draw(); savePng(dir.resolve("01-random.png"));
            colony.completeIteration(); update();
            while (!colony.finished()) { colony.step(); update(); }
            replaying = true; progress = 2.4; updateControls(); draw(); savePng(dir.resolve("02-result.png"));
            writeCsv(dir.resolve("statistics.csv"));
            Files.writeString(dir.resolve("result.txt"), "best="+colony.bestLength()+"\nexact="+optimum+"\nroute="+colony.bestRoute()+"\n");
            Platform.exit();
        } catch (Exception ex) { ex.printStackTrace(); Platform.exit(); }
    }
    private void error(String title, Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(title);
        alert.setContentText(error.getMessage()); alert.showAndWait();
    }
}
