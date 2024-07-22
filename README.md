# Sudoku Generator

JAR file used to generate unique sudoku problems and save them to file based on their difficulty rating.

## Steps

 - Sudoku puzzles are generated randomly using backtracking, and tested to ensure only one unique solution exists.
 - A target number of clues to be removed from the grid is then set, and clues are removed in opposite pairs to ensure symmetry of the board.
 - Whilst removing clues, uses a priority queue with a heuristic approach to assess whether the current board state is likely to result in a unique solution with the desired number of clues, otherwise reverts to a more probabilistic board state and continues.
 - When pairs of clues are removed, the board is tested to ensure that it is still solvable and unique.
 - When a desired board state has been reached, uses set of human techniques used to solve Sudoku puzzles to assess the difficulty of the board.
 - Generates unique id for the board and saves it to the corresponding file based on the boards difficulty rating if the unique id doesn't exist in the file.
 - Output directory for generated puzzles is set by the user, as well as the number of puzzles to be generated.

## Logical Solver

Series of strategies employed to solve the board and assess the difficulty rating as follows:

#### Easy

 - Basic Elimination

 #### Medium

 - Hidden Single
 - Naked Pair
 - Naked Triple

#### Hard

 - Hidden Pair
 - Hidden Triple
 - Intersection
 - X Wing

#### Extreme

 - Simple Colouring
 - Y Wing
 - Swordfish

## Usage

Sudoku puzzles can by running the JAR file, then specifying the output filepath for the puzzles
as well as the number of puzzles to generate when prompted in the terminal window. 

Download the SudokuGenerator.jar file from the Releases section (v1.0.0), in your terminal navigate to the directory
where you have stored the JAR file and run the following command:

```
java -jar SudokuGenerator.jar
```
