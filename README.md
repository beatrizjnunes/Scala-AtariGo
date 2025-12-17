# AtariGo

An AtariGo game implementation in Scala with both TUI (Terminal User Interface) and GUI (Graphical User Interface) using JavaFX.

## About the Project

AtariGo is a simplified variant of the game Go, where the objective is to capture opponent stones. The first player to capture a predetermined number of stones wins the game.

## Features

- **Two game modes:**
  - Player vs Computer
  - Player vs Player

- **Three difficulty levels:**
  - Easy: Random moves
  - Medium: Alternates between strategic and random moves
  - Hard: Prioritizes captures and strategic moves

- **Dual interface:**
  - TUI (Terminal User Interface) for playing in the terminal
  - GUI (Graphical User Interface) with JavaFX for a visual experience

- **Additional features:**
  - Save and load games
  - Undo moves
  - Time limit per move
  - Configurable board size
  - Configurable number of captures to win

## Requirements

- **Scala** 2.12 or higher
- **Java** 8 or higher
- **JavaFX** (included in project dependencies)
- **sbt** (Scala Build Tool) or IntelliJ IDEA with Scala plugin

## Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/AtariGo.git
cd AtariGo
```

2. Make sure you have Scala, Java, and sbt installed:
```bash
scala -version
java -version
sbt --version
```

3. Download dependencies:
```bash
sbt update
```

## How to Play

### TUI Mode (Terminal)

Run the `mainTUI.scala` file:

```bash
scala src/mainTUI.scala
```

**Commands:**
- To make a move: enter coordinates in the format `row,col` (e.g., `1,2`)
- To undo: type `undo`
- To exit: type `exit`

### GUI Mode (Graphical Interface)

Run the `mainGUI.scala` file:

```bash
scala src/mainGUI.scala
```

Or through IntelliJ IDEA by running the `FxApp` class.

**Controls:**
- Click on board cells to make moves
- Use the "Undo" button to undo moves
- Configure options in the menu before starting

## Project Structure

```
AtariGo/
├── src/
│   ├── AtariGo.scala          # Main game logic
│   ├── mainTUI.scala          # Terminal interface
│   ├── mainGUI.scala          # Graphical interface
│   ├── Files.scala            # File operations (save/load)
│   ├── MyRandom.scala         # Random number generator
│   ├── RandomWithState.scala  # Trait for stateful random
│   ├── GameBoardController.scala  # GUI controller
│   ├── MenuController.scala   # Menu controller
│   ├── GameBoard.fxml         # Board layout
│   └── Menu.fxml              # Menu layout
├── SavedGames/                # Directory for saved games
└── README.md
```

## Game Rules

1. Players alternate turns (Black starts)
2. Each player places a stone on an empty board position
3. Adjacent stones of the same color form groups
4. Groups without liberties (adjacent empty spaces) are captured
5. The first player to capture the predetermined number of stones wins
6. Suicide moves (that capture your own group) are not allowed

## Authors

- Beatriz Nunes
- José Jarmela
- Nuno Neves

## License

This project was developed as an academic work.

