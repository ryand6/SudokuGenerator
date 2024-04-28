package com.github.ryand6.sudokuGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
    Used to assess the validity of the problem through human logical solving techniques, rejecting
    problems not solvable without backtracking/guess work. Determines difficulty rating for valid
    problems based on strategies deployed to solve.
 */
public class LogicalAssessor {

    private int [][] grid;
    private HashSet<Integer>[][] candidatesGrid;
    private List<List<Cell>> allHouses;
    private HashMap<String, Integer> strategyMap;
    private String rating;

    // Represents cell in Sudoku grid based on its row and col values
    private static class Cell {
        final int row;
        final int col;

        public Cell (int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public String toString() {
            return "(" + this.row + ", " + this.col + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Cell)) {
                return false;
            }
            Cell comp = (Cell) obj;
            return comp.row == this.row && comp.col == this.col;
        }
    }

    public LogicalAssessor() {
        initialiseAllHouse();
    }

    // Pencil in potential candidates for each grid cell, where any cell already containing a number
    // only contains that as it's candidate, and empty cells have potential candidates 1 - 9. These will
    // be reduced once solving strategies are applied.
    private void fillInitialCandidates() {
        this.candidatesGrid = new HashSet[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                candidatesGrid[row][col] = new HashSet<>();
                // If number already exists, add the number as the only candidate
                if (grid[row][col] != 0) {
                    candidatesGrid[row][col].add(grid[row][col]);
                } else {
                    fillCandidates1to9(row, col);
                }
            }
        }
    }

    // Helper function to fill in empty cells with all possible candidates
    private void fillCandidates1to9(int row, int col) {
        for (int num = 1; num < 10; num++) {
            candidatesGrid[row][col].add(num);
        }
    }

    private void initialiseAllHouse() {
        // Each nested list represents a house (row) which contains each of the cells in that house
        List<List<Cell>> rowHouses = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            List<Cell> rowHouse = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                Cell cell = new Cell(row, col);
                rowHouse.add(cell);
            }
            rowHouses.add(rowHouse);
        }

        // Each nested list represents a house (column) which contains each of the cells in that house
        List<List<Cell>> colHouses = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            List<Cell> colHouse = new ArrayList<>();
            for (int row = 0; row < 9; row++) {
                Cell cell = new Cell(row, col);
                colHouse.add(cell);
            }
            colHouses.add(colHouse);
        }

        // Each nested list represents a house (block) which contains each of the cells in that house
        List<List<Cell>> blockHouses = new ArrayList<>();
        for (int bRow = 0; bRow < 3; bRow++) {
            for (int bCol = 0; bCol < 3; bCol++) {
                blockHouses.add(getBlockHouse(bRow * 3, bCol * 3));
            }
        }

        // Merge each list of houses into one list of houses
        List<List<Cell>> combinedHouses = new ArrayList<>();
        combinedHouses.addAll(rowHouses);
        combinedHouses.addAll(colHouses);
        combinedHouses.addAll(blockHouses);
        this.allHouses = combinedHouses;
    }

    // For testing
    public List<List<Cell>> getAllHouses() {
        return this.allHouses;
    }

    // Helper used to create a block house
    private List<Cell> getBlockHouse(int row, int col) {
        List<Cell> blockHouse = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            for (int q = 0; q < 3; q++) {
                Cell cell = new Cell(row + p, col + q);
                blockHouse.add(cell);
            }
        }
        return blockHouse;
    }

    // Get count of cells left to solve on the board
    private int cellsToSolve() {
        int count = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (candidatesGrid[row][col].size() != 1) {
                    count++;
                }
            }
        }
        return count;
    }

    /*
    Simple elimination strategy, removing illegal candidates from cells. If a cell with candidates ends up with
    only 1x possible candidate through this method then the result is that the cell contains a Naked Single
     */
    private boolean basicElimination() {
        strategyMap.putIfAbsent("Basic Elimination", 0);
        boolean candidatesEliminated = false;
        for (List<Cell> house: allHouses) {
            for (Cell cell1: house) {
                if (candidatesGrid[cell1.row][cell1.col].size() != 1) {
                    continue;
                }
                // Get the only value in the set - this represents the known answer to that cell
                int val = candidatesGrid[cell1.row][cell1.col].iterator().next();
                // For each cell in the house, remove any occurrences of a known value from its list of candidates
                for (Cell cell2: house) {
                    if (candidatesGrid[cell2.row][cell2.col].contains(val) && !cell1.equals(cell2)) {
                        candidatesGrid[cell2.row][cell2.col].remove(val);
                        int count = strategyMap.get("Basic Elimination");
                        strategyMap.put("Basic Elimination", ++count);
                        candidatesEliminated = true;
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    private boolean hiddenSingle() {
        return false;
    }

    // Used to attempt to solve the grid using variety of techniques, as well as storing the counts of techniques used
    // so a difficulty rating can be defined. Returns a boolean based on whether the grid can be solved using these
    // techniques or not.
    public boolean solve(int[][] grid) {
        this.grid = grid;
        // Used to store counts for each strategy used
        this.strategyMap = new HashMap<>();
        int unsolvedCells = cellsToSolve();
        // Loop through techniques, returning to the start of the loop when a technique is successful - this is used to try to simulate human
        // order of strategy, returning to the easier techniques first to see if any more solutions can be found before employing more difficult
        // techniques
        while (unsolvedCells != 0) {
            boolean techniqueSuccessful = false;

            techniqueSuccessful = basicElimination();

            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenSingle();
            }

            // Exhausted all available techniques with no solution
            if (!techniqueSuccessful) {
                break;
            }

            unsolvedCells = cellsToSolve();
        }

        return false;
    }

    // Determines difficulty rating based on strategies used for solution
    private void setRating() {

    }

    public static void main(String[] args) {
        LogicalAssessor logicalAssessor = new LogicalAssessor();
        List<List<Cell>> allHouses = logicalAssessor.getAllHouses();
        for (int i = 0; i < allHouses.size(); i++) {
            System.out.println(allHouses.get(i).toString());
        }
    }
}
