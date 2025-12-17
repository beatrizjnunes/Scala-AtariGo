import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.{Parent, Scene}
import javafx.scene.control.{Button, Label, TextField, ToggleButton, ToggleGroup}
import javafx.stage.{Stage, FileChooser}
import javafx.stage.FileChooser.ExtensionFilter
import javafx.event.ActionEvent
import java.io.File

class MenuController {
  private val DEFAULT_CAPTURE_LIMIT = 1
  private val DEFAULT_TIME_LIMIT_SECONDS = 50

  @FXML private var btnPlay: Button = _
  @FXML private var btnExit: Button = _
  @FXML private var btnLoadGame: Button = _
  @FXML private var inputCaptureLimit: TextField = _
  @FXML private var inputTimeLimit: TextField = _
  @FXML private var lblMessage: Label = _
  @FXML private var easyToggle: ToggleButton = _
  @FXML private var mediumToggle: ToggleButton = _
  @FXML private var hardToggle: ToggleButton = _

  private val difficultyGroup = new ToggleGroup()
  private var loadSaved: Boolean = false
  private var fileName: String = ""

  @FXML
  def initialize(): Unit = {
    inputCaptureLimit.setText(DEFAULT_CAPTURE_LIMIT.toString)
    inputTimeLimit.setText(DEFAULT_TIME_LIMIT_SECONDS.toString)

    easyToggle.setToggleGroup(difficultyGroup)
    mediumToggle.setToggleGroup(difficultyGroup)
    hardToggle.setToggleGroup(difficultyGroup)
    easyToggle.setSelected(true)
    List(easyToggle, mediumToggle, hardToggle).foreach { toggle =>
      toggle.selectedProperty().addListener { (_, _, isSelected) =>
        if (isSelected) {
          toggle.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #f39c12, #d35400);" + "-fx-text-fill: white;" + "-fx-font-weight: bold;" +
              "-fx-background-radius: 12;" + "-fx-padding: 8 18 8 18;")
        } else {
          toggle.setStyle("-fx-background-color: #34495e;" + "-fx-text-fill: #ecf0f1;" + "-fx-font-weight: bold;" + "-fx-background-radius: 12;" + "-fx-padding: 8 18 8 18;")
        }
      }
    }
    if (lblMessage != null) {
      lblMessage.setText("")
    }
  }

  @FXML
  def onPlayClicked(event: ActionEvent): Unit = {
    try {
      val captureLimit = inputCaptureLimit.getText.toInt
      val timeLimitSeconds = inputTimeLimit.getText.toLong
      val selected = difficultyGroup.getSelectedToggle
      val difficulty =
        if (selected == easyToggle) 1
        else if (selected == mediumToggle) 2
        else if (selected == hardToggle) 3
        else {
          if (lblMessage != null) lblMessage.setText("Select a difficulty")
          return
        }
      if (captureLimit <= 0) {
        lblMessage.setText("Number of pieces must be positive")
        return
      }
      if (timeLimitSeconds <= 0) {
        lblMessage.setText("Time must be positive")
        return
      }
      val loader = new FXMLLoader(getClass.getResource("/GameBoard.fxml"))
      val root: Parent = loader.load()
      val boardController = loader.getController.asInstanceOf[GameBoardController]
      boardController.setupGame(captureLimit, (timeLimitSeconds * 1000).toInt, difficulty, loadSaved, fileName)
      val stage = btnPlay.getScene.getWindow.asInstanceOf[Stage]
      stage.setScene(new Scene(root))
      stage.setTitle("Atari Go - Game")
    } catch {
      case _: NumberFormatException =>
        if (lblMessage != null) lblMessage.setText("Invalid values. Use numbers only.")
      case ex: Exception =>
        if (lblMessage != null) lblMessage.setText(s"Error: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }

  @FXML
  def onExitClicked(event: ActionEvent): Unit = {
    System.exit(0)
  }

  @FXML
  def onLoadGameClicked(event: ActionEvent): Unit = {
    loadSaved = true
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Select game file")
    fileChooser.getExtensionFilters.add(new ExtensionFilter("Text files", "*.txt"))
    val selectedFile: File = fileChooser.showOpenDialog(null)
    fileName = s"JogosGuardados/${selectedFile.getName()}"

    if (selectedFile != null) {
      onPlayClicked(event)
    }
  }
}
