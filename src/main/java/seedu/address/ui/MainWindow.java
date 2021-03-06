package seedu.address.ui;

import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.commons.core.Config;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.ui.ExitAppRequestEvent;
import seedu.address.commons.events.ui.ExitBudgetWindowRequestEvent;
import seedu.address.commons.events.ui.ShowBudgetViewEvent;
import seedu.address.commons.events.ui.ShowHelpRequestEvent;
import seedu.address.commons.events.ui.ToggleBrowserPlaceholderEvent;
import seedu.address.logic.Logic;
import seedu.address.model.UserPrefs;
import seedu.address.model.cca.CcaName;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private String topPanel;

    // Independent Ui parts residing in this Ui container
    private BrowserPanel browserPanel;
    private CalendarPanel calendarPanel;
    private PersonListPanel personListPanel;
    private Config config;
    private UserPrefs prefs;
    private HelpWindow helpWindow;
    private BudgetWindow budgetWindow;

    @FXML
    private StackPane browserPlaceholder;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private MenuItem budgetMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    public MainWindow(Stage primaryStage, Config config, UserPrefs prefs, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.config = config;
        this.prefs = prefs;
        this.topPanel = ToggleBrowserPlaceholderEvent.BROWSER_PANEL;

        // Configure the UI
        setTitle(config.getAppTitle());
        setWindowDefaultSize(prefs);

        setAccelerators();
        registerAsAnEventHandler(this);

        helpWindow = new HelpWindow();
        budgetWindow = new BudgetWindow(logic, prefs, this);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
        setAccelerator(budgetMenuItem, KeyCombination.keyCombination("F2"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        browserPanel = new BrowserPanel();
        calendarPanel = new CalendarPanel();
        // The last node in the list will be the top view
        // BrowserPanel will be the default top view
        browserPlaceholder.getChildren().add(calendarPanel.getRoot());
        browserPlaceholder.getChildren().add(browserPanel.getRoot());

        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        ResultDisplay resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(prefs.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(logic);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    void hide() {
        primaryStage.hide();
    }

    private void setTitle(String appTitle) {
        primaryStage.setTitle(appTitle);
    }

    /**
     * Sets the default size based on user preferences.
     */
    private void setWindowDefaultSize(UserPrefs prefs) {
        primaryStage.setHeight(prefs.getGuiSettings().getWindowHeight());
        primaryStage.setWidth(prefs.getGuiSettings().getWindowWidth());
        if (prefs.getGuiSettings().getWindowCoordinates() != null) {
            primaryStage.setX(prefs.getGuiSettings().getWindowCoordinates().getX());
            primaryStage.setY(prefs.getGuiSettings().getWindowCoordinates().getY());
        }
    }

    /**
     * Returns the current size and the position of the main Window.
     */
    GuiSettings getCurrentGuiSetting() {
        return new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
    }

    private void setTopPanel(String newTopPanel) {
        topPanel = newTopPanel;

    }

    /**
     * Checks if the specified view is the top view in BrowserPlaceholder.
     */
    public boolean isCorrectPanelOnTopBrowserPlaceholder(String view) {
        if (browserPlaceholder.getChildren().size() < 2) {
            return false;
        }

        switch (view) {
        case ToggleBrowserPlaceholderEvent.BROWSER_PANEL:
            return topPanel.equals(ToggleBrowserPlaceholderEvent.BROWSER_PANEL);

        case ToggleBrowserPlaceholderEvent.CALENDAR_PANEL:
            return topPanel.equals(ToggleBrowserPlaceholderEvent.CALENDAR_PANEL);

        default:
            return false;
        }
    }

    /**
     * Push the top Node in browserPlaceholder to the back.
     */
    private void toggleTopPanel() {
        ObservableList<Node> children = browserPlaceholder.getChildren();

        if (children.size() > 1) {
            Node topNode = children.get(children.size() - 1);
            topNode.toBack();
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleBudget() {
        if (!Optional.ofNullable(budgetWindow).isPresent()) {
            budgetWindow = new BudgetWindow(logic, prefs, this);
        }

        if (!budgetWindow.isShowing()) {
            budgetWindow.show(null);
        } else {
            budgetWindow.focus(null);
        }
    }

    /**
     * Opens the budget window or focuses on it if it's already opened.
     * @param ccaName
     */
    @FXML
    public void handleBudget(CcaName ccaName) {
        if (!Optional.ofNullable(budgetWindow).isPresent()) {
            budgetWindow = new BudgetWindow(logic, prefs, this);
        }

        if (!budgetWindow.isShowing()) {
            budgetWindow.show(ccaName);
        } else {
            budgetWindow.focus(ccaName);
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Returns true if the help window is currently being shown.
     */
    public boolean isShowing() {
        return getRoot().isShowing();
    }

    /**
     * Focuses on the help window.
     */
    public void focus() {
        getRoot().requestFocus();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        raise(new ExitAppRequestEvent());
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    void releaseResources() {
        browserPanel.freeResources();
        calendarPanel.freeResources();
    }

    @Subscribe
    private void handleShowHelpEvent(ShowHelpRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        handleHelp();
    }

    @Subscribe
    private void handleShowBudgetEvent(ShowBudgetViewEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        CcaName ccaName = event.getCcaName();
        handleBudget(ccaName);
    }

    @Subscribe
    private void handleExitBudgetWindowRequestEvent(ExitBudgetWindowRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        primaryStage.show();
    }

    @Subscribe
    private void handleToggleBrowserPlaceholderEvent(ToggleBrowserPlaceholderEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        if (!isCorrectPanelOnTopBrowserPlaceholder(event.view)) {
            logger.info("BrowserPlaceholder toggled");
            setTopPanel(event.view);
            toggleTopPanel();
        }

    }

}
