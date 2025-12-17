import AtariGo.Stone.Stone
import AtariGo.{Board, Coord2D, Stone}

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.{Date, Scanner}

object Files {

  def saveSeed(seed: Int): Unit = {
    val writer = new PrintWriter("src/seed.txt")
    try {
      writer.write(seed.toString)
    } finally {
      writer.close()
    }
  }

  def readSeed(): Int = {
    val file = new File("src/seed.txt")
    val scanner = new Scanner(file)
    try {
      if (scanner.hasNextInt())
        scanner.nextInt()
      else throw new Exception("Invalid seed in file")
    } finally {
      scanner.close()
    }
  }

  def saveGame(board: Board,
               lstOpenCoords: List[Coord2D],
               currentPlayer: Stone,
               rand: MyRandom,
               blackCaptures: Int,
               whiteCaptures: Int,
               alternateFlag: Boolean,
               gameMode: Int,
               difficulty: Int,
               capturesToWin: Int,
               timeLimitMillis: Long,
               interface: String): Unit = {

    val timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
    val baseName = if (interface.contains("GUI")) "gameGUI" else "gameTUI"
    val file = new File(s"SavedGames/${baseName}_$timestamp.txt")

    val writer = new PrintWriter(file)
    try {
      writer.println(board.length)
      writer.println(board.head.length)

      board.foreach { row =>
        val line = row.map {
          case Stone.Black => "B"
          case Stone.White => "W"
          case Stone.Empty => "."
        }.mkString(" ")
        writer.println(line)
      }

      val coordsStr = lstOpenCoords.map {
        case (r, c) => s"$r,$c"
      }.mkString(" ")
      writer.println(coordsStr)

      writer.println(currentPlayer match {
        case Stone.Black => "B"
        case Stone.White => "W"
        case Stone.Empty => "."
      })

      writer.println(rand.seed.toString)
      writer.println(blackCaptures.toString)
      writer.println(whiteCaptures.toString)
      writer.println(alternateFlag.toString)
      writer.println(gameMode.toString)
      writer.println(difficulty.toString)
      writer.println(capturesToWin.toString)
      writer.println(timeLimitMillis.toString)

    } finally {
      writer.close()
    }
  }

  def readGame(fileName: String): Option[(Board, List[Coord2D], Stone, MyRandom, Int, Int, Boolean, Int, Int, Int, Long)] = {

    val file = new File(fileName)
    if (!file.exists()) {
      println(s"File '$fileName' not found.")
      return None
    }

    val scanner = new Scanner(file)
    try {
      val height = scanner.nextLine().trim.toInt
      val width = scanner.nextLine().trim.toInt

      val boardLines = (1 to height).map { _ =>
        val line = scanner.nextLine().trim.split(" ")
        line.map {
          case "B" => Stone.Black
          case "W" => Stone.White
          case "." => Stone.Empty
          case other => throw new Exception(s"Invalid value: $other")
        }.toList
      }.toList

      val board = boardLines

      if (!scanner.hasNextLine()) throw new Exception("Incomplete file")
      val coordsLine = scanner.nextLine().trim
      val lstOpenCoords = if (coordsLine.isEmpty) List.empty else
        coordsLine.split(" ").map { s =>
          val parts = s.split(",")
          (parts(0).toInt, parts(1).toInt)
        }.toList

      val playerChar = scanner.nextLine().trim match {
        case "B" => Stone.Black
        case "W" => Stone.White
        case "." => Stone.Empty
        case other => throw new Exception(s"Invalid player: $other")
      }

      val randSeed = scanner.nextLine().trim.toLong
      val rand = MyRandom(randSeed)

      val blackCaptures = scanner.nextLine().trim.toInt
      val whiteCaptures = scanner.nextLine().trim.toInt
      val alternateFlag = scanner.nextLine().trim.toBoolean
      val gameMode = scanner.nextLine().trim.toInt
      val difficulty = scanner.nextLine().trim.toInt
      val capturesToWin = scanner.nextLine().trim.toInt
      val timeLimitMillis = scanner.nextLine().trim.toLong

      Some((board, lstOpenCoords, playerChar, rand, blackCaptures, whiteCaptures, alternateFlag,
        gameMode, difficulty, capturesToWin, timeLimitMillis))
    } catch {
      case e: Exception =>
        println(s"Error reading game: ${e.getMessage}")
        None
    } finally {
      scanner.close()
    }
  }
}
