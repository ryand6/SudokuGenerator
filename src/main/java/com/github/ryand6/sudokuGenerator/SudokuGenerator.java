package com.github.ryand6.sudokuGenerator;

public class SudokuGenerator {

    public int[][] generatePuzzle() {
        int[][] grid = GridGenerator.fillGrid();
        return grid;
    }

}
