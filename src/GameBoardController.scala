import Files.{readGame, readSeed, saveGame, saveSeed}
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.{Scene}
import javafx.scene.control.{Button, Label}
import javafx.scene.effect.DropShadow
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, GridPane, Pane}
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import scala.collection.mutable

class GameBoardController {

  @FXML private var grid: GridPane = _
  @FXML private var circlePane: Pane = _
  @FXML private var messageLabel: Label = _
  @FXML private var gameOverPane: AnchorPane = _
  @FXML private var winnerLabel: Label = _
  @FXML private var playAgainButton: Button = _
  @FXML private var menuButtonOverlay: Button = _
  @FXML private var exitButtonOverlay: Button = _
  @FXML private var undoButton: Button = _
  @FXML private var menuButton: Button = _
  @FXML private var exitButton: Button = _
  @FXML private var difficultyLabel: Label = _
  @FXML private var timeLabel: Label = _
  @FXML private var capturesLabel: Label = _

  private var captureLimit: Int = 5
  private var timeLimitMillis: Long = 0
  private var difficulty: Int = 2
  private val boardSize = 5
  private var startTime: Long = 0
  private var board: AtariGo.Board = List.fill(boardSize)(List.fill(boardSize)(AtariGo.Stone.Empty))

  private var lstOpenCoords: List[AtariGo.Coord2D] =
    (0 until boardSize).toList.map(r =>
      (0 until boardSize).toList.map(c => (r, c))
    ).flatten

  private var currentPlayer: AtariGo.Stone.Stone = AtariGo.Stone.Black
  private var capturesBlack: Int = 0
  private var capturesWhite: Int = 0
  private val circleMap: mutable.Map[(Int, Int), Circle] = mutable.Map.empty

  private var rand: MyRandom = MyRandom(readSeed())
  private var alternateFlag: Boolean = true

  private val hoverShadow = new DropShadow()
  hoverShadow.setRadius(10)
  hoverShadow.setColor(Color.gray(0, 0.3))
  private var history: List[(AtariGo.Board, List[AtariGo.Coord2D], Int, Int, AtariGo.Stone.Stone)] = List()

  def setupGame(captureLimit: Int, timeLimitMillis: Long, difficulty: Int, loadSaved: Boolean = false, fileName: String = ""): Unit = {
    if (loadSaved) {
      readGame(fileName) match {
        case Some((b, lst, player, r, capB, capW, alterna, modo, diff, capLimit, tempo)) =>

          this.board = b
          this.lstOpenCoords = lst
          this.currentPlayer = AtariGo.Stone.Black
          this.rand = r
          this.capturesBlack = capB
          this.capturesWhite = capW
          this.alternateFlag = alterna
          this.captureLimit = capLimit
          this.difficulty = diff
          this.timeLimitMillis = tempo
          this.startTime = System.currentTimeMillis()

        case None =>
          try {
            val loader = new FXMLLoader(getClass.getResource("/Menu.fxml"))
            val root = loader.load[javafx.scene.Parent]()
            val scene = new Scene(root)
            val stage = messageLabel.getScene.getWindow.asInstanceOf[Stage]
            stage.setScene(scene)
          } catch {
            case ex: Exception =>
              ex.printStackTrace()
              messageLabel.setText("Error loading menu.")
          }
      }
    } else {
      this.captureLimit = captureLimit
      this.timeLimitMillis = timeLimitMillis
      this.difficulty = difficulty
      startTime = System.currentTimeMillis()

      board = List.fill(boardSize)(List.fill(boardSize)(AtariGo.Stone.Empty))
      lstOpenCoords = (for {
        r <- 0 until boardSize
        c <- 0 until boardSize
      } yield (r, c)).toList

      currentPlayer = AtariGo.Stone.Black
      capturesBlack = 0
      capturesWhite = 0
    }

    history = List()
    circleMap.clear()

    circlePane.getChildren.forEach {
      case c: Circle =>
        val x = (c.getCenterX / 100).toInt
        val y = (c.getCenterY / 100).toInt
        if (x >= 0 && x < boardSize && y >= 0 && y < boardSize) {
          circleMap((y, x)) = c
          c.setOnMouseClicked((e: MouseEvent) => onCircleClick(y, x))
          c.setOnMouseEntered(_ => c.setEffect(hoverShadow))
          c.setOnMouseExited(_ => c.setEffect(null))
          c.hoverProperty().addListener { (_, _, hovering) =>
            if (hovering) {
              c.setFill(Color.rgb(0, 0, 0, 0.3))
            } else {
              board(y)(x) match {
                case AtariGo.Stone.Black => c.setFill(Color.BLACK)
                case AtariGo.Stone.White => c.setFill(Color.WHITE)
                case AtariGo.Stone.Empty => c.setFill(Color.TRANSPARENT)
              }
            }
          }
        }
      case _ =>
    }
    difficultyLabel.setText(s"Difficulty: ${difficultyToString(this.difficulty)}")
    timeLabel.setText(
      if (timeLimitMillis > 0) f"Max time per move: ${this.timeLimitMillis / 60000}%02d:${(this.timeLimitMillis / 1000) % 60}%02d"
      else "Time: Unlimited"
    )
    capturesLabel.setText(s"Captures needed: ${this.captureLimit} | Black: ${this.capturesBlack} | White: ${this.capturesWhite}")
    updateBoardUI()
  }

  private def onCircleClick(row: Int, col: Int): Unit = {
    if (currentPlayer != AtariGo.Stone.Black) return

    if (!lstOpenCoords.contains((row, col))) {
      messageLabel.setText("This position is already occupied!")
      return
    }
    val elapsedTime = System.currentTimeMillis() - startTime
    if (elapsedTime > timeLimitMillis) {
      messageLabel.setText("Time expired. You lost your turn.")
      updateBoardUI()

      val pause = new PauseTransition(Duration.seconds(2))
      pause.setOnFinished(_ => {
        currentPlayer = AtariGo.Stone.White
        computerMove()
        checkGameOverByNoMoves()
      })
      pause.play()
      return
    }

    history = (deepCopyBoard(board), lstOpenCoords, capturesBlack, capturesWhite, currentPlayer) :: history

    val (maybeNewBoard, newOpenCoords, captures) = AtariGo.play(board, currentPlayer, (row, col), lstOpenCoords)
    checkGameOverByNoMoves()

    maybeNewBoard match {
      case Some(newBoard) =>
        board = newBoard
        lstOpenCoords = newOpenCoords
        capturesBlack += captures
        updateBoardUI()
        capturesLabel.setText(s"Captures needed: $captureLimit | Black: $capturesBlack | White: $capturesWhite")

        AtariGo.checkWinner(capturesBlack, capturesWhite, captureLimit) match {
          case Some(winner) =>
            showEndGameDialog(winner)
            disableInput()
          case None =>
            currentPlayer = AtariGo.Stone.White

            new Thread(() => {
              Thread.sleep(500)
              Platform.runLater(() => computerMove())
            }).start()
        }
      case None =>
        messageLabel.setText("Invalid move: the stone would have no liberties!")
    }
  }

  private def computerMove(): Unit = {
    if (currentPlayer != AtariGo.Stone.White) return

    history = (deepCopyBoard(board), lstOpenCoords, capturesBlack, capturesWhite, currentPlayer) :: history
    val (newBoard, newRand, newOpenCoords, newFlag, capturesThisMove) =
      AtariGo.playWithDifficultyAndCaptures(board, rand, AtariGo.Stone.White, lstOpenCoords, difficulty, alternateFlag)

    rand = newRand
    saveSeed(newRand.nextInt()._1)
    alternateFlag = newFlag
    capturesWhite += capturesThisMove
    board = newBoard
    lstOpenCoords = newOpenCoords

    updateBoardUI()
    capturesLabel.setText(s"Captures needed: $captureLimit | Black: $capturesBlack | White: $capturesWhite")
    startTime = System.currentTimeMillis()

    AtariGo.checkWinner(capturesBlack, capturesWhite, captureLimit) match {
      case Some(winner) =>
        disableInput()
        showEndGameDialog(winner)
      case None =>
        currentPlayer = AtariGo.Stone.Black
        messageLabel.setText("It's your turn to play!")
    }
  }

  private def updateBoardUI(): Unit = {
    Platform.runLater(() => {
      for {
        r <- 0 until boardSize
        c <- 0 until boardSize
        circle <- circleMap.get((r, c))
      } {
        board(r)(c) match {
          case AtariGo.Stone.Black => circle.setFill(Color.BLACK)
          case AtariGo.Stone.White => circle.setFill(Color.WHITE)
          case AtariGo.Stone.Empty => circle.setFill(Color.TRANSPARENT)
        }
      }
    })
  }

  private def disableInput(): Unit = {
    circleMap.values.map(_.setOnMouseClicked(_ => ()))
  }

  private def enableInput(): Unit = {
    val it = circleMap.iterator
    while (it.hasNext) {
      val entry = it.next()
      val r = entry._1._1
      val c = entry._1._2
      val circle = entry._2
      circle.setOnMouseClicked((e: MouseEvent) => onCircleClick(r, c))
    }
  }

  @FXML
  def initialize(): Unit = {
    undoButton.setOnAction(_ => onUndo())
    menuButton.setOnAction(_ => onMenu(1))
    exitButton.setOnAction(_ => onExit())

    playAgainButton.setOnAction(_ => {
      gameOverPane.setVisible(false)
      setupGame(captureLimit, timeLimitMillis, difficulty)
      messageLabel.setText("New game started!")
      enableInput()
    })
    menuButtonOverlay.setOnAction(_ => onMenu(2))
    exitButtonOverlay.setOnAction(_ => Platform.exit())

  }

  private def onUndo(): Unit = {
    history match {
      case _ :: (prevBoard, prevOpenCoords, prevCaptBlack, prevCaptWhite, prevPlayer) :: tail =>
        history = tail

        board = prevBoard
        lstOpenCoords = prevOpenCoords
        capturesBlack = prevCaptBlack
        capturesWhite = prevCaptWhite
        currentPlayer = prevPlayer

        updateBoardUI()
        enableInput()
        capturesLabel.setText(s"Captures needed: $captureLimit | Black: $capturesBlack | White: $capturesWhite")

      case _ =>
        messageLabel.setText("Not enough moves to undo!")
    }
  }

  private def onMenu(x: Int): Unit = {
    x match {
      case 1 =>
        saveGame(
          board,
          lstOpenCoords,
          currentPlayer,
          rand,
          capturesBlack,
          capturesWhite,
          alternateFlag,
          0,
          difficulty,
          captureLimit,
          timeLimitMillis,
          "GUI"
        )
        loadMenu()

      case 2 =>
        loadMenu()

      case _ =>
        println("Invalid option")
    }
  }

  private def loadMenu(): Unit = {
    val loader = new FXMLLoader(getClass.getResource("Menu.fxml"))
    val root = loader.load[javafx.scene.Parent]()
    val scene = new Scene(root)
    val stage = menuButton.getScene.getWindow.asInstanceOf[Stage]
    stage.setScene(scene)
  }

  private def onExit(): Unit = {
    saveGame(
      board,
      lstOpenCoords,
      currentPlayer,
      rand,
      capturesBlack,
      capturesWhite,
      alternateFlag,
      0,
      difficulty,
      captureLimit,
      timeLimitMillis,
      "GUI"
    )
    val stage = exitButton.getScene.getWindow.asInstanceOf[Stage]
    stage.close()
  }

  private def deepCopyBoard(b: AtariGo.Board): AtariGo.Board = {
    b.map(row => row.map(identity))
  }

  private def difficultyToString(diff: Int): String = diff match {
    case 1 => "Easy"
    case 2 => "Medium"
    case 3 => "Hard"
    case _ => "Unknown"
  }

  private def showEndGameDialog(winner: AtariGo.Stone.Stone): Unit = {
    val winnerText = winner match {
      case AtariGo.Stone.Black => "Congratulations! You won the game!"
      case AtariGo.Stone.White => "Oh no! Try again!"
      case _ => "Game over!"
    }
    winnerLabel.setText(winnerText)
    gameOverPane.setVisible(true)
  }

  private def checkGameOverByNoMoves(): Unit = {
    if (lstOpenCoords.isEmpty) {
      val winner = if (capturesBlack > capturesWhite) AtariGo.Stone.Black
      else if (capturesWhite > capturesBlack) AtariGo.Stone.White
      else null

      disableInput()

      if (winner != null) showEndGameDialog(winner)
      else {
        gameOverPane.setVisible(true)
        winnerLabel.setText("Draw! No more moves possible.")
      }
    }
  }
}
