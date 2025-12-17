import AtariGo.Stone.{Empty, Stone}
import AtariGo.*
import Files.*

import scala.io.StdIn.readLine
import scala.annotation.tailrec

object mainTUI extends App {

  private case class GameState(
                                board: Board,
                                lstOpenCoords: List[Coord2D],
                                currentPlayer: Stone,
                                rand: MyRandom,
                                blackCaptures: Int,
                                whiteCaptures: Int,
                                alternateFlag: Boolean)
  private var boardSize = 5
  private var captureLimit = 1
  private var timeLimitMillis: Long = 50000
  private var difficulty = 1

  @tailrec
  private def mainLoop(gameState: GameState, hist: List[GameState], capturesToWin: Int, gameMode: Int): Unit = {
    println("Current Board:")
    println(displayBoard(gameState.board))
    println(s"Captures -> Black: ${gameState.blackCaptures}, White: ${gameState.whiteCaptures}")

    AtariGo.checkWinner(gameState.blackCaptures, gameState.whiteCaptures, capturesToWin) match {
      case Some(Stone.Black) =>
        println("Black player won by captures!")
        println("Game over.")
        printHistory(hist :+ gameState)
        return
      case Some(Stone.White) =>
        println("White player won by captures!")
        println("Game over.")
        printHistory(hist :+ gameState)
        return
      case None =>
      case Some(_) =>
    }

    if (gameState.lstOpenCoords.isEmpty) {
      println("Game over. No more free positions available.")
      println("Game over.")
      printHistory(hist :+ gameState)
      return
    }

    val (nextGameState, _, undoRequested, updatedHist) =
      if (gameMode == 2 || (gameMode == 1 && gameState.currentPlayer == Stone.Black)) {
        println(s"Player ${gameState.currentPlayer} - Choose a coordinate (row,col), 'undo' or 'exit':")
        val startTime = System.currentTimeMillis()
        val userInput = readLine()
        val elapsedTime = System.currentTimeMillis() - startTime

        if (elapsedTime > timeLimitMillis) {
          println(s"Time expired! Player ${gameState.currentPlayer} lost their turn.")
          val newState = gameState.copy(currentPlayer = nextPlayer(gameState.currentPlayer))
          (newState, false, false, hist)
        } else if (userInput.trim.toLowerCase == "exit") {
          println("Exiting game...")

          saveGame(gameState.board, gameState.lstOpenCoords, gameState.currentPlayer, gameState.rand,
            gameState.blackCaptures, gameState.whiteCaptures, gameState.alternateFlag,
            gameMode, difficulty, capturesToWin, timeLimitMillis, "TUI")

          sys.exit()

        } else if (userInput.trim.toLowerCase == "undo") {
          if (hist.length >= 2) {
            val previousState = hist(hist.length - 2)
            val newHist = hist.dropRight(2)
            println("Previous move undone.")
            (previousState, false, true, newHist)
          } else {
            println("Cannot undo more moves.")
            (gameState, false, false, hist)
          }
        } else {
          val coords = userInput.split(",").flatMap(_.trim.toIntOption)
          if (coords.length != 2) {
            println("Invalid coordinates. Try again.")
            (gameState, false, false, hist)
          } else {
            val (row, col) = (coords(0) - 1, coords(1) - 1)
            if (!gameState.lstOpenCoords.contains((row, col))) {
              println(s"Coordinate (${row + 1}, ${col + 1}) is not valid or already played.")
              (gameState, false, false, hist)
            } else {
              val (maybeBoard, newCoords, captures) =
                play(gameState.board, gameState.currentPlayer, (row, col), gameState.lstOpenCoords)
              maybeBoard match {
                case Some(newBoard) =>
                  val (newBlackCaptures, newWhiteCaptures) =
                    if (gameState.currentPlayer == Stone.Black)
                      (gameState.blackCaptures + captures, gameState.whiteCaptures)
                    else
                      (gameState.blackCaptures, gameState.whiteCaptures + captures)

                  val updatedGameState = gameState.copy(
                    board = newBoard,
                    lstOpenCoords = newCoords,
                    currentPlayer = nextPlayer(gameState.currentPlayer),
                    blackCaptures = newBlackCaptures,
                    whiteCaptures = newWhiteCaptures
                  )
                  (updatedGameState, true, false, hist :+ gameState)
                case None =>
                  println("Invalid move. No liberties.")
                  (gameState, false, false, hist)
              }
            }
          }
        }
      } else {
        val (newBoard, newRand, newCoords, newAlternateFlag, captures) =
          playWithDifficultyAndCaptures(
            gameState.board,
            gameState.rand,
            gameState.currentPlayer,
            gameState.lstOpenCoords,
            difficulty,
            gameState.alternateFlag
          )
        saveSeed(newRand.nextInt()._1)

        val (newBlackCaptures, newWhiteCaptures) =
          if (gameState.currentPlayer == Stone.Black)
            (gameState.blackCaptures + captures, gameState.whiteCaptures)
          else
            (gameState.blackCaptures, gameState.whiteCaptures + captures)

        val updatedGameState = gameState.copy(
          board = newBoard,
          lstOpenCoords = newCoords,
          currentPlayer = nextPlayer(gameState.currentPlayer),
          blackCaptures = newBlackCaptures,
          whiteCaptures = newWhiteCaptures,
          rand = newRand,
          alternateFlag = newAlternateFlag
        )

        (updatedGameState, true, false, hist :+ gameState)
      }

    mainLoop(nextGameState, updatedHist, capturesToWin, gameMode)
  }

  private def nextPlayer(current: Stone): Stone =
    if (current == Stone.Black) Stone.White else Stone.Black

  private def printHistory(hist: List[GameState]): Unit = {
    println("\nMove history:")
    for (state <- hist) {
      println(displayBoard(state.board))
      println(s"Captures -> Black: ${state.blackCaptures}, White: ${state.whiteCaptures}")
      println("-----")
    }
  }

  private def startGame(): Unit = {
    println("Choose game mode:")
    println("1. Player vs Computer")
    println("2. Player vs Player")
    val gameMode = readLine().trim match {
      case "1" => 1
      case "2" => 2
      case _ =>
        println("Invalid mode, starting Player vs Computer.")
        1
    }

    val initialBoard: Board = List.fill(boardSize)(List.fill(boardSize)(Stone.Empty))

    val initialCoords: List[Coord2D] = (0 until boardSize).flatMap { row =>
      (0 until boardSize).map { col => (row, col) }
    }.toList

    val rand = MyRandom(readSeed())
    val initialState = GameState(initialBoard, initialCoords, Stone.Black, rand, 0, 0, alternateFlag = true)

    mainLoop(initialState, List(), captureLimit, gameMode)
  }

  @tailrec
  private def menu(): Unit = {
    println(
      s"""|===== MENU =====
          |1 - Play
          |2 - Change board dimensions (current: $boardSize)
          |3 - Set number of pieces to capture (current: $captureLimit)
          |4 - Set maximum time per move (current: ${timeLimitMillis / 1000} seconds)
          |5 - Set difficulty level (current: $difficulty)
          |6 - Load saved game
          |0 - Exit
          |Choose an option:""".stripMargin)

    readLine() match {
      case "1" =>
        println("Starting game...")
        startGame()
      case "2" =>
        println("Enter new board size (minimum 3):")
        val input = readLine()
        if (input.forall(_.isDigit)) {
          boardSize = input.toInt.max(3)
          println(s"Board size set to $boardSize")
        } else {
          println("Invalid input.")
        }
        menu()
      case "3" =>
        println("Enter new capture limit to win:")
        val input = readLine()
        if (input.forall(_.isDigit)) {
          captureLimit = input.toInt max 1
          println(s"Capture limit set to $captureLimit")
        } else {
          println("Invalid input.")
        }
        menu()
      case "4" =>
        println("Enter maximum time per move (in seconds):")
        val input = readLine()
        if (input.forall(_.isDigit)) {
          timeLimitMillis = input.toLong * 1000
          println(s"Time limit per move set to $input seconds")
        } else {
          println("Invalid input.")
        }
        menu()
      case "5" =>
        println("Enter difficulty level (1, 2 or 3):")
        val input = readLine()
        if (input.forall(_.isDigit)) {
          val dif = input.toInt
          if (dif >= 1 && dif <= 3) {
            difficulty = dif
            println(s"Difficulty level set to $difficulty")
          } else println("Invalid difficulty. Must be 1, 2 or 3.")
        } else {
          println("Invalid input.")
        }
        menu()

      case "6" =>
        println("Enter the file name (without '.txt') to load:")
        val fileName = readLine().trim

        readGame("SavedGames/" + fileName + ".txt") match {
          case Some((board, coords, stone, rand, blackC, whiteC, flag, modo, dif, capLimit, tempo)) =>
            val initialState = GameState(board, coords, stone, rand, blackC, whiteC, flag)
            timeLimitMillis = tempo
            difficulty = dif
            mainLoop(initialState, List(), capLimit, modo)
          case None =>
            println("No saved game found with that name. Try again.")
            menu()
        }

      case "0" =>
        println("Exiting game. See you next time!")
      case _ =>
        println("Invalid option. Try again.")
        menu()
    }
  }

  menu()
}
