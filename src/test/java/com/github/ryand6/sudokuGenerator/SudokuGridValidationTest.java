package com.github.ryand6.sudokuGenerator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SudokuGridValidationTest {

    @Test
    public void testValidGrid_Example1() {
        // Test that the validateGrid method recognises a valid Sudoku grid
        int[][] validGrid = {
                {5, 3, 4, 6, 7, 8, 9, 1, 2},
                {6, 7, 2, 1, 9, 5, 3, 4, 8},
                {1, 9, 8, 3, 4, 2, 5, 6, 7},
                {8, 5, 9, 7, 6, 1, 4, 2, 3},
                {4, 2, 6, 8, 5, 3, 7, 9, 1},
                {7, 1, 3, 9, 2, 4, 8, 5, 6},
                {9, 6, 1, 5, 3, 7, 2, 8, 4},
                {2, 8, 7, 4, 1, 9, 6, 3, 5},
                {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
        SudokuValidation validator = new GridGenerator();
        assertTrue(validator.validateGrid(validGrid));
    }

    @Test
    public void testInvalidGrid_Example1() {
        // Test that the validateGrid method recognises an invalid Sudoku grid
        int[][] invalidGrid = {
                {5, 3, 4, 6, 7, 8, 9, 1, 2},
                {6, 7, 2, 1, 9, 5, 3, 4, 8},
                {1, 9, 8, 3, 4, 2, 5, 6, 7},
                {8, 5, 9, 7, 6, 1, 4, 2, 3},
                {4, 2, 6, 8, 5, 3, 7, 9, 1},
                {7, 1, 3, 9, 2, 4, 8, 5, 6},
                {9, 6, 1, 5, 3, 7, 2, 8, 4},
                {2, 8, 7, 4, 1, 9, 6, 3, 5},
                {3, 4, 5, 2, 8, 6, 1, 7, 8} // Invalid: 8 repeated in last row and col
        };
        SudokuValidation validator = new GridGenerator();
        assertFalse(validator.validateGrid(invalidGrid));
    }

    @Test
    public void testMultipleGeneratedGrids() {
        // Generate random sudoku grids and test their validity
        GridGenerator gridGen = new GridGenerator();
        for (int i = 0; i < 100; i++) {
            gridGen.generateGrid();
            assertTrue(gridGen.validateGrid(gridGen.getGrid()));
        }
    }

}
