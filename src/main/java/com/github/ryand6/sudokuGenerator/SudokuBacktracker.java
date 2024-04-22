package com.github.ryand6.sudokuGenerator;

import java.util.Arrays;
import java.util.Random;

// Uses backtracking algorithm to find puzzle with unique solution
public class SudokuBacktracker {

    private int[][] grid;
    private int count;

    public SudokuBacktracker(int[][] puzzle) {
        grid = new int[9][9];

        // Create copy of puzzle to use for solving
        for (int x = 0; x < puzzle.length; x++) {
            for (int y = 0; y < puzzle[x].length; y++) {
                grid[x][y] = puzzle[x][y];
            }
        }
    }

    /*
    Check that the value in the cell doesn't violate Sudoku rules
     */
    public boolean validateCell(int val, int row, int col) {

        // Get cell position of upper left corner of block value is in
        int blockRow = (int) Math.floor(row / 3) * 3;
        int blockCol = (int) Math.floor(col / 3) * 3;

        // Check column
        for (int x = 0; x < 9; x++) {
            if (grid[row][x] == val) {
                return false;
            }
        }

        // Check row
        for (int y = 0; y < 9; y++) {
            if (grid[y][col] == val) {
                return false;
            }
        }

        // Check block
        for (int p = 0; p < 3; p++) {
            for (int q = 0; q < 3; q++) {
                if (grid[blockRow + p][blockCol + q] == val) {
                    return false;
                }
            }
        }

        return true;
    }

    // Recursive backtracking solution to find all solutions to a puzzle, modified to stop the algorithm
    // if more than one solution has been found, rendering the puzzle obsolete for use in an app
    public void backtrackSolver() {
        for (int i = 0; i < 9; i ++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] == 0) {
                    for (int val = 1; val < 10; val++) {
                        if (validateCell(val, i, j)) {
                            grid[i][j] = val;
                            backtrackSolver();
                            grid[i][j] = 0;
                            if (count > 1) {
                                return;
                            }
                        }
                    }
                    return;
                }
            }
        }
        System.out.println(Arrays.deepToString(grid).replace("], ", "]\n"));
        count += 1;
    }

    public int getCount() {
        return this.count;
    }

    public static void main(String[] args) {
        int[][] initGrid = new int[9][9];
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                initGrid[x][y] = 0;
            }
        }
        initGrid[0][0] = new Random().nextInt(9) + 1;
        SudokuBacktracker sbt = new SudokuBacktracker(initGrid);
        sbt.backtrackSolver();
    }

}
