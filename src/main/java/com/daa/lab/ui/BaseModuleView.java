package com.daa.lab.ui;

import com.daa.lab.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Abstract base class for every algorithm module view.
 *
 * <p>
 * Provides a consistent top-bar with navigation / reset, delegates the
 * content area to concrete subclasses via {@link #buildContent()}, and offers
 * shared helper methods for common UI elements.
 * </p>
 *
 * <h3>Subclass contract</h3>
 * <ol>
 * <li>Initialise ALL instance fields in the subclass constructor body.</li>
 * <li>Call {@link #initializeView(String)} as the LAST statement of the
 * subclass constructor.</li>
 * <li>Implement {@link #buildContent()} – return the Node that fills the center
 * pane.</li>
 * <li>Implement {@link #reset()} – restore the view to its initial state.</li>
 * </ol>
 *
 * <p>
 * <b>Why deferred initialization?</b> Java calls {@code super()} first, before
 * the subclass field initializers run. If {@code buildContent()} were called
 * from
 * {@code super()}, any subclass field it touches would still be {@code null},
 * causing
 * a NullPointerException. By deferring {@code buildContent()} to
 * {@link #initializeView(String)} – called explicitly at the end of each
 * subclass
 * constructor – all fields are guaranteed to be initialised.
 * </p>
 */
public abstract class BaseModuleView extends BorderPane {

    /**
     * No-arg constructor. Does NOT build the UI.
     * Subclasses must call {@link #initializeView(String)} themselves.
     */
    @SuppressWarnings("this-escape")
    protected BaseModuleView() {
        setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");
    }

    /**
     * Builds the top bar and content area. Must be called by each subclass
     * constructor as its LAST statement, after all instance fields are set.
     *
     * @param moduleTitle human-readable module name shown in the top bar
     */
    protected final void initializeView(String moduleTitle) {
        setTop(buildTopBar(moduleTitle));
        setCenter(buildContent());
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private HBox buildTopBar(String moduleTitle) {
        // ← Dashboard button
        Button backBtn = new Button("\u2190 Dashboard");
        backBtn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: " + StyleConstants.ACCENT_BLUE + ";"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: " + StyleConstants.FONT_BODY + "px;");
        backBtn.setOnAction(e -> MainApp.navigateTo(new DashboardView()));

        // Module title (centre of bar)
        Label titleLabel = new Label(moduleTitle);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_SUBHEADING));
        titleLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        // Filler to push Reset to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Reset button
        Button resetBtn = new Button("Reset");
        resetBtn.setStyle(
                "-fx-background-color: " + StyleConstants.ACCENT_RED + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 6 16 6 16;");
        resetBtn.setOnAction(e -> reset());

        HBox bar = new HBox(12, backBtn, titleLabel, spacer, resetBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(StyleConstants.PADDING));
        bar.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        return bar;
    }

    // ── Abstract contract ─────────────────────────────────────────────────────

    /**
     * Build and return the main content {@link Node} for this module.
     * Called once from {@link #initializeView(String)}, after all subclass
     * fields have been initialised.
     */
    protected abstract Node buildContent();

    /**
     * Reset the module to its initial / default state.
     * Called when the user clicks the "Reset" button in the top bar.
     */
    protected abstract void reset();

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Creates a styled section heading {@link Label}.
     *
     * @param text the section title
     * @return a {@link Label} styled as a section header
     */
    protected Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_SUBHEADING));
        label.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        return label;
    }

    /**
     * Creates a styled info box that displays algorithm metadata.
     *
     * @param realWorldApp short description of the real-world application
     * @param steps        summary of algorithmic steps / approach
     * @param complexity   time / space complexity string
     * @return a {@link VBox} ready to embed in any layout
     */
    protected VBox algorithmInfoBox(String realWorldApp, String steps, String complexity) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(StyleConstants.PADDING));
        box.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";"
                        + "-fx-background-radius: " + StyleConstants.RADIUS + ";"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 1);");

        box.getChildren().addAll(
                infoRow("\uD83C\uDF10 Real-World Application", realWorldApp),
                infoRow("\u2699\uFE0F Algorithm Steps", steps),
                infoRow("\u23F1 Complexity", complexity));
        return box;
    }

    private VBox infoRow(String heading, String body) {
        Label h = new Label(heading);
        h.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        h.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        Label b = new Label(body);
        b.setFont(Font.font("System", StyleConstants.FONT_BODY));
        b.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        b.setWrapText(true);

        VBox row = new VBox(2, h, b);
        row.setPadding(new Insets(0, 0, 4, 0));
        return row;
    }
}
