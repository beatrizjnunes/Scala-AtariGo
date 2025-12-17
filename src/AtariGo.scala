import AtariGo.Stone.Stone

object AtariGo {

  object Stone extends Enumeration {
    type Stone = Value
    val Black, White, Empty = Value
  }

  type Board = List[List[Stone]]
  type Coord2D = (Int, Int)

  private def randomMove(lstOpenCoords: List[Coord2D], rand: MyRandom): (Coord2D, MyRandom) = {
    if (lstOpenCoords.isEmpty)
      throw new Exception("No free positions available.")
    val (x, r1) = rand.nextInt(lstOpenCoords.length)
    (lstOpenCoords(x), r1.asInstanceOf[MyRandom])
  }

  def play(
            board: Board,
            player: Stone,
            coord: Coord2D,
            lstOpenCoords: List[Coord2D]
          ): (Option[Board], List[Coord2D], Int) = {
    if (!lstOpenCoords.contains(coord)) {
      (None, lstOpenCoords, 0)
    } else {
      val (rowIdx, colIdx) = coord
      val newRow = board(rowIdx).updated(colIdx, player)
      val newBoard = board.updated(rowIdx, newRow)

      val opponent = if (player == Stone.Black) Stone.White else Stone.Black
      val directions = List((-1, 0), (1, 0), (0, -1), (0, 1))

      val toCapture = directions.flatMap { case (dr, dc) =>
        val (r, c) = (coord._1 + dr, coord._2 + dc)
        if (r >= 0 && r < board.length && c >= 0 && c < board.head.length && newBoard(r)(c) == opponent) {
          val group = getGroup((r, c), newBoard, opponent)
          val hasLiberties = group.exists(g => hasLiberty(g, newBoard))
          if (!hasLiberties) group else Nil
        } else Nil
      }

      val capturedCoords = toCapture.toSet

      val clearedBoard = capturedCoords.foldLeft(newBoard) { case (b, (r, c)) =>
        b.updated(r, b(r).updated(c, Stone.Empty))
      }

      val liberties = getLiberties(clearedBoard, coord, player)

      if (liberties.isEmpty) {
        (None, lstOpenCoords, 0)
      } else {
        val newLstOpenCoords = lstOpenCoords.filterNot(_ == coord) ++ capturedCoords
        (Some(clearedBoard), newLstOpenCoords.distinct, capturedCoords.size)
      }
    }
  }

  private def playRandomly(
                    board: Board,
                    r: MyRandom,
                    player: Stone,
                    lstOpenCoords: List[Coord2D],
                    f: (List[Coord2D], MyRandom) => (Coord2D, MyRandom)
                  ): (Board, MyRandom, List[Coord2D], Int) = {
    val (coord, newRand) = f(lstOpenCoords, r)
    val (maybeBoard, newOpenCoords, captures) = play(board, player, coord, lstOpenCoords)
    maybeBoard match {
      case Some(newBoard) => (newBoard, newRand, newOpenCoords, captures)
      case None => (board, r, lstOpenCoords, 0)
    }
  }

  def displayBoard(board: Board): String = {
    board.map { row =>
      row.map {
        case Stone.Black => "B"
        case Stone.White => "W"
        case Stone.Empty => "."
      }.mkString(" ")
    }.mkString("\n")
  }
  private def tryCaptureMove(
                      board: Board,
                      player: Stone,
                      lstOpenCoords: List[Coord2D]
                    ): Option[(Board, List[Coord2D], Int)] = {
    val capturesMoves = lstOpenCoords.iterator.flatMap { coord =>
      val (maybeBoard, newOpenCoords, captured) = play(board, player, coord, lstOpenCoords)
      if (maybeBoard.isDefined && captured > 0)
        Some((maybeBoard.get, newOpenCoords, captured))
      else
        None
    }.toSeq

    if (capturesMoves.isEmpty) None
    else Some(capturesMoves.maxBy(_._3))
  }

  def checkWinner(blackCaptures: Int, whiteCaptures: Int, limit: Int): Option[Stone] = {
    if (blackCaptures >= limit) {
      Some(Stone.Black)
    } else if (whiteCaptures >= limit) {
      Some(Stone.White)
    } else {
      None
    }
  }

  private def getLiberties(board: Board, coord: Coord2D, player: Stone): Set[Coord2D] = {
    val directions = List((-1, 0), (1, 0), (0, -1), (0, 1))

    def inBounds(coord: Coord2D): Boolean =
      coord._1 >= 0 && coord._1 < board.length &&
        coord._2 >= 0 && coord._2 < board.head.length

    def getLibertiesAux(currentCoord: Coord2D, visited: Set[Coord2D]): (Set[Coord2D], Set[Coord2D]) = {
      if (visited.contains(currentCoord) || !inBounds(currentCoord))
        (Set.empty, Set.empty)
      else {
        board(currentCoord._1)(currentCoord._2) match {
          case `player` =>
            val neighbors = directions.map {
              case (dr, dc) => (currentCoord._1 + dr, currentCoord._2 + dc)
            }
            neighbors.foldLeft((Set(currentCoord), Set.empty[Coord2D])) {
              case ((groupAcc, libsAcc), nextCoord) =>
                val (subGroup, subLibs) = getLibertiesAux(nextCoord, visited + currentCoord)
                (groupAcc ++ subGroup, libsAcc ++ subLibs)
            }
          case Stone.Empty => (Set.empty, Set(currentCoord))
          case _ => (Set.empty, Set.empty)
        }
      }
    }

    val (_, liberties) = getLibertiesAux(coord, Set.empty)
    liberties
  }

  private def getGroup(start: Coord2D, board: Board, color: Stone): Set[Coord2D] = {
    val directions = List((-1, 0), (1, 0), (0, -1), (0, 1))

    def inBounds(r: Int, c: Int): Boolean = r >= 0 && r < board.length && c >= 0 && c < board.head.length

    @scala.annotation.tailrec
    def bfs(queue: List[Coord2D], visited: Set[Coord2D]): Set[Coord2D] = queue match {
      case Nil => visited
      case (r, c) :: tail =>
        val neighbors = directions
          .map { case (dr, dc) => (r + dr, c + dc) }
          .filter { case (nr, nc) => inBounds(nr, nc) && board(nr)(nc) == color && !visited.contains((nr, nc)) }

        bfs(tail ++ neighbors, visited + ((r, c)))
    }
    bfs(List(start), Set.empty)
  }

  private def hasLiberty(coord: Coord2D, board: Board): Boolean = {
    val directions = List((-1, 0), (1, 0), (0, -1), (0, 1))
    directions.exists { case (dr, dc) =>
      val (r, c) = (coord._1 + dr, coord._2 + dc)
      r >= 0 && r < board.length && c >= 0 && c < board.head.length && board(r)(c) == Stone.Empty
    }
  }

  def playWithDifficultyAndCaptures(
                                     board: Board,
                                     r: MyRandom,
                                     player: Stone,
                                     lstOpenCoords: List[Coord2D],
                                     difficulty: Int,
                                     alternateFlag: Boolean
                                   ): (Board, MyRandom, List[Coord2D], Boolean, Int) = {
    difficulty match {
      case 1 =>
        val (b, nr, open, captures) = playRandomly(board, r, player, lstOpenCoords, randomMove)
        (b, nr, open, alternateFlag, captures)

      case 2 =>
        if (alternateFlag) {
          val (b, nr, open, captures) = playRandomly(board, r, player, lstOpenCoords, randomMove)
          (b, nr, open, !alternateFlag, captures)
        } else {
          tryCaptureMove(board, player, lstOpenCoords) match {
            case Some((newBoard, newOpenCoords, captures)) =>
              (newBoard, r, newOpenCoords, !alternateFlag, captures)
            case None =>
              tryAdjacentMove(board, player, lstOpenCoords) match {
                case Some((newBoard, newOpenCoords, captures)) =>
                  (newBoard, r, newOpenCoords, !alternateFlag, captures)
                case None =>
                  val (b, nr, open, captures) = playRandomly(board, r, player, lstOpenCoords, randomMove)
                  (b, nr, open, !alternateFlag, captures)
              }
          }
        }

      case 3 =>
        tryCaptureMove(board, player, lstOpenCoords) match {
          case Some((newBoard, newOpenCoords, captures)) =>
            (newBoard, r, newOpenCoords, alternateFlag, captures)
          case None =>
            tryAdjacentMove(board, player, lstOpenCoords) match {
              case Some((newBoard, newOpenCoords, captures)) =>
                (newBoard, r, newOpenCoords, alternateFlag, captures)
              case None =>
                val (b, nr, open, captures) = playRandomly(board, r, player, lstOpenCoords, randomMove)
                (b, nr, open, alternateFlag, captures)
            }
        }

      case _ =>
        throw new Exception("Unknown difficulty level.")
    }
  }

  private def tryAdjacentMove(
                       board: Board,
                       player: Stone,
                       lstOpenCoords: List[Coord2D]
                     ): Option[(Board, List[Coord2D], Int)] = {
    val opponent = if (player == Stone.Black) Stone.White else Stone.Black
    val directions = List((-1, 0), (1, 0), (0, -1), (0, 1))

    val adjMoves = lstOpenCoords.iterator.flatMap { coord =>
      val (r, c) = coord
      val isNextToOpponent = directions.exists { case (dr, dc) =>
        val nr = r + dr
        val nc = c + dc
        nr >= 0 && nr < board.length && nc >= 0 && nc < board.head.length &&
          board(nr)(nc) == opponent
      }

      if (isNextToOpponent) {
        val (maybeBoard, newOpenCoords, captured) = play(board, player, coord, lstOpenCoords)
        if (maybeBoard.isDefined)
          Some((maybeBoard.get, newOpenCoords, captured))
        else
          None
      } else None
    }.toSeq

    adjMoves.headOption
  }
}