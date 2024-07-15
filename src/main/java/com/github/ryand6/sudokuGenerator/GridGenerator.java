package com.github.ryand6.sudokuGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GridGenerator implements SudokuValidation, SudokuPrinter {

    private int[][] grid;
    private final List<Integer> rowIndices;
    private final List<Integer> colIndices;
    private final List<Integer> values;
    private int count;

    public GridGenerator() {
        this.grid = new int[9][9];
        this.rowIndices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
        this.colIndices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
        this.values = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    public void generateGrid() {
        // reset counter and nested array so that new grid can be generated each time
        count = 0;
        this.grid = new int[9][9];
        // Randomly shuffle row and col indices and values used to attempt to fill cell
        Collections.shuffle(rowIndices);
        Collections.shuffle(colIndices);
        Collections.shuffle(values);
        backtrackingFill();
    }

    /*
    Backtracking algorithm using random sorting to generate unique grids
     */
    private void backtrackingFill() {
        for (int i: rowIndices) {
            for (int j: colIndices) {
                if (grid[i][j] == 0) {
                    for (int val: values) {
                        if (validateCell(grid, val, i, j)) {
                            grid[i][j] = val;
                            backtrackingFill();
                            // Exit recursive loop when more than one solution is found
                            if (count > 1) {
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

    public int[][] getGrid() {
        return this.grid;
    }

    public static void main(String[] args) {
        GridGenerator gg = new GridGenerator();
        gg.generateGrid();
        gg.printGrid(gg.grid);

        gg.generateGrid();
        gg.printGrid(gg.grid);

        gg.generateGrid();
        gg.printGrid(gg.grid);
    }

}
