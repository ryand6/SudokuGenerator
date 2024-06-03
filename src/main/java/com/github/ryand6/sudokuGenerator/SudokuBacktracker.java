package com.github.ryand6.sudokuGenerator;

import java.util.Random;

public class SudokuBacktracker implements SudokuValidation, SudokuPrinter {

    private int[][] grid;
    private int count;
    private boolean unique;

    public SudokuBacktracker() {
    }

    public boolean isUnique(int[][] puzzle) {
        // Default value - turns to false if backtracking algorithm finds more than one solution
        unique = true;
        grid = new int[9][9];
        // Create copy of puzzle to use for solving
        for (int x = 0; x < puzzle.length; x++) {
            for (int y = 0; y < puzzle[x].length; y++) {
                grid[x][y] = puzzle[x][y];
            }
        }
        backtrackSolver();
        // Reset counter for validating other boards
        count = 0;
        return unique;
    }

    // Recursive backtracking solution to find all solutions to a puzzle, modified to stop the algorithm
    // if more than one solution has been found, rendering the puzzle obsolete for use in an app
    private void backtrackSolver() {
        for (int i = 0; i < 9; i ++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] == 0) {
                    for (int val = 1; val < 10; val++) {
                        if (validateCell(grid, val, i, j)) {
                            grid[i][j] = val;
                            backtrackSolver();
                            // Exit recursive loop when more than one solution is found
                            if (count > 1) {
                                unique = false;
                                return;
                            }
                            grid[i][j] = 0;
                        }
                    }
                    return;
                }
            }
        }
        count += 1;
    }

    public static void main(String[] args) {
        int[][] initGrid = new int[9][9];
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                initGrid[x][y] = 0;
            }
        }
        initGrid[0][0] = new Random().nextInt(9) + 1;
        SudokuBacktracker sbt = new SudokuBacktracker();
        System.out.println(sbt.isUnique(initGrid));
    }

}
