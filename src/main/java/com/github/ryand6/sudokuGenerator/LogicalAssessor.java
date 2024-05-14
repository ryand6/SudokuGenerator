package com.github.ryand6.sudokuGenerator;

import java.util.*;

/*
    Used to assess the validity of the problem through human logical solving techniques, rejecting
    problems not solvable without backtracking/guess work. Determines difficulty rating for valid
    problems based on strategies deployed to solve.
 */
public class LogicalAssessor {

    private int [][] grid;
    private List<Integer>[][] candidatesGrid;
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
        this.candidatesGrid = new List[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                candidatesGrid[row][col] = new ArrayList<>();
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
    Strategy 1) Simple elimination strategy, removing illegal candidates from cells. If a cell with candidates ends up with
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
                int val = candidatesGrid[cell1.row][cell1.col].get(0);
                // For each cell in the house, remove any occurrences of a known value from its list of candidates
                for (Cell cell2: house) {
                    if (candidatesGrid[cell2.row][cell2.col].contains(val) && !cell1.equals(cell2)) {
                        // Using wrapper type to remove the value object itself, not the element at index position equal to val
                        candidatesGrid[cell2.row][cell2.col].remove((Integer) val);
                        int count = strategyMap.get("Basic Elimination");
                        strategyMap.put("Basic Elimination", ++count);
                        candidatesEliminated = true;
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    /*
    Strategy 2) seeks to find cell with a candidate not present in the rest of the house,
    and if so removes all other candidates from that cell
     */
    private boolean hiddenSingle() {
        strategyMap.putIfAbsent("Hidden Single", 0);
        boolean candidatesEliminated = false;
        for (int i = 1; i < 10; i++) {
            for (List<Cell> house : allHouses) {
                int cellsEliminated = findUniqueCandidateInHouse(house, i);
                if (cellsEliminated != 0) {
                    int count = strategyMap.get("Hidden Single");
                    strategyMap.put("Hidden Single", (count + cellsEliminated));
                    candidatesEliminated = true;
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for the hiddenSingle method - used to check if a value is unique in house, and
    // if so, clean the rest of the candidates from the cell where the value is held, returning the number
    // of cleaned candidates so the strategyMap can be updated
    private int findUniqueCandidateInHouse(List<Cell> house, int num) {
        int count = 0;
        int candidatesEliminated = 0;
        Cell cellToClean = new Cell(-1, -1);
        for (Cell cell: house) {
            if (candidatesGrid[cell.row][cell.col].contains(num)) {
                count++;
                cellToClean = cell;
            }
        }
        if (count == 1) {
            for (int i = 1; i < 10; i++) {
                if (candidatesGrid[cellToClean.row][cellToClean.col].contains(i) && i != num) {
                    // Using wrapper type to remove the value object itself, not the element at index position equal to i
                    candidatesGrid[cellToClean.row][cellToClean.col].remove((Integer) i);
                    candidatesEliminated++;
                }
            }
        }
        return candidatesEliminated;
    }

    /*
    Strategy 3) Where two cells that contain matching candidate pairs are found in a house,
    remove those candidates from the other cells in the house if present
     */
    private boolean nakedPair() {
        strategyMap.putIfAbsent("Naked Pair", 0);
        boolean candidatesEliminated = false;
        for (List<Cell> house: allHouses) {
            for (Cell cell1 : house) {
                // Only investigate cells that contain a pair of candidates
                if (candidatesGrid[cell1.row][cell1.col].size() != 2) {
                    continue;
                }
                for (Cell cell2 : house) {
                    if (cell1 == cell2){
                        continue;
                    }
                    // Clean up rest of cells in house of numbers found in the pair if two cells consist of a matching pair
                    // of candidates within the house
                    if (candidatesGrid[cell1.row][cell1.col].equals(candidatesGrid[cell2.row][cell2.col])) {
                        int cellsEliminated = cleanHouseOfPairs(house, cell1, cell2);
                        if (cellsEliminated != 0) {
                            int count = strategyMap.get("Naked Pair");
                            strategyMap.put("Naked Pair", (count + cellsEliminated));
                            candidatesEliminated = true;
                        }
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for nakedDouble method used to remove values contained in the naked pair from all other cells in
    // the house in which the naked pair was found
    private int cleanHouseOfPairs(List<Cell> house, Cell cell1, Cell cell2) {
        int candidatesEliminated = 0;
        List<Integer> vals = candidatesGrid[cell1.row][cell1.col];
        for (Cell cell: house) {
            if (cell == cell1 || cell == cell2 || candidatesGrid[cell.row][cell.col].size() == 1) {
                continue;
            }
            for (int i: vals) {
                if (candidatesGrid[cell.row][cell.col].contains(i)) {
                    candidatesGrid[cell.row][cell.col].remove((Integer) i);
                    candidatesEliminated++;
                }
            }
        }
        return candidatesEliminated;
    }

    /*
    Strategy 4) Where 3x cells in a house contain in total 3x unique candidates between them, and each cell has either 2 or 3 candidates. When
    this pattern is discovered, remove the unique candidates from all other cells within the house
     */
    private boolean nakedTriple() {
        strategyMap.putIfAbsent("Naked Triple", 0);
        boolean candidatesEliminated = false;
        for (List<Cell> house: allHouses) {
            for (Cell cell1 : house) {
                HashSet<Integer> tripleCandidates = new HashSet<>();
                List<Cell> agreedCells = new ArrayList<>();
                int cell1Size = candidatesGrid[cell1.row][cell1.col].size();
                // Only investigate cells that contain either a pair or triple of candidates
                if (cell1Size != 2 && cell1Size != 3) {
                    continue;
                }
                tripleCandidates.addAll(candidatesGrid[cell1.row][cell1.col]);
                agreedCells.add(cell1);
                candidatesEliminated = assessRestOfHouseForTriple(candidatesEliminated, house, tripleCandidates, agreedCells);
            }
        }
        return candidatesEliminated;
    }

    // Helper function carrying out the inner loop that checks the rest of the house against the active cell to see if a naked triple can be found
    private boolean assessRestOfHouseForTriple(boolean candidatesEliminated, List<Cell> house, HashSet<Integer> tripleCandidates, List<Cell> agreedCells) {
        for (Cell cell2 : house) {
            int cell2Size = candidatesGrid[cell2.row][cell2.col].size();
            if (cell2Size != 2 && cell2Size != 3) {
                continue;
            }
            if (agreedCells.contains(cell2)){
                continue;
            }
            // Used to store any candidates that don't belong to other members of the agreed cells,
            // so that these can be removed if the currently checked cell isn't part of a valid triple
            List<Integer> uniqueCandidates = new ArrayList<>();
            for (int candidate: candidatesGrid[cell2.row][cell2.col]) {
                if (!tripleCandidates.contains(candidate)) {
                    uniqueCandidates.add(candidate);
                }
            }
            tripleCandidates.addAll(uniqueCandidates);
            agreedCells.add(cell2);
            // If the set contains more than 3x values, a triple has not been found therefore
            // remove the candidates from the most recently checked cell to continue searching
            if (tripleCandidates.size() > 3) {
                tripleCandidates.removeAll(uniqueCandidates);
                agreedCells.remove(cell2);
            }
            if (agreedCells.size() == 3 && tripleCandidates.size() == 3) {
                Cell agreedCell1 = agreedCells.get(0);
                Cell agreedCell2 = agreedCells.get(1);
                Cell agreedCell3 = agreedCells.get(2);
                int cellsEliminated = cleanHouseOfTriple(house, agreedCell1, agreedCell2, agreedCell3, tripleCandidates);
                if (cellsEliminated != 0) {
                    int count = strategyMap.get("Naked Triple");
                    strategyMap.put("Naked Triple", (count + cellsEliminated));
                    candidatesEliminated = true;
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for nakedTriple method used to remove values contained in the naked triple from all other cells in
    // the house in which the naked triple was found
    private int cleanHouseOfTriple(List<Cell> house, Cell cell1, Cell cell2, Cell cell3, HashSet<Integer> tripleCandidates) {
        int candidatesEliminated = 0;
        List<Integer> vals = tripleCandidates.stream().toList();
        for (Cell cell: house) {
            if (cell == cell1 || cell == cell2 || cell == cell3 || candidatesGrid[cell.row][cell.col].size() == 1) {
                continue;
            }
            for (int i: vals) {
                if (candidatesGrid[cell.row][cell.col].contains(i)) {
                    candidatesGrid[cell.row][cell.col].remove((Integer) i);
                    candidatesEliminated++;
                }
            }
        }
        return candidatesEliminated;
    }

    /*
    Strategy 5) Search house for two cells that contain a pair of candidates unique to only those cells,
    that are amongst other candidates which as a result can be removed once the pair is found
     */
    private boolean hiddenPair() {
        strategyMap.putIfAbsent("Hidden Pair", 0);
        boolean candidatesEliminated = false;
        for (List<Cell> house : allHouses) {
            for (int i = 1; i < 10; i++) {
                List<Cell> potentialPair = new ArrayList<>();
                List<Integer> pairCandidates = new ArrayList<>();
                checkCellCandidatesForHiddenPair(house, potentialPair, pairCandidates, i);
                if (pairCandidates.size() == 2) {
                    int cellsEliminated = cleanCellsOfOtherCandidates(pairCandidates, potentialPair);
                    if (cellsEliminated != 0) {
                        int count = strategyMap.get("Hidden Pair");
                        strategyMap.put("Hidden Pair", (count + cellsEliminated));
                        candidatesEliminated = true;
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for hidden pair to find the first potential candidate of the hidden pair
    private void checkCellCandidatesForHiddenPair(List<Cell> house, List<Cell> potentialPair, List<Integer> pairCandidates, int num) {
        for (Cell cell : house) {
            // If number being checked is already an answer to a cell, it cannot be a member of the hidden pair
            if (candidatesGrid[cell.row][cell.col].contains(num) && candidatesGrid[cell.row][cell.col].size() == 1) {
                return;
            }
            if (candidatesGrid[cell.row][cell.col].contains(num)) {
                if (potentialPair.size() < 2) {
                    potentialPair.add(cell);
                }
                else if (potentialPair.size() == 2) {
                    potentialPair.clear();
                    return;
                }
            }
        }
        if (potentialPair.size() == 2) {
            pairCandidates.add(num);
            findSecondValueOfHiddenPair(house, potentialPair, pairCandidates);
        }
    }

    // Helper function for hidden pair to find the second potential candidate of the hidden pair
    private void findSecondValueOfHiddenPair(List<Cell> house, List<Cell> potentialPair, List<Integer> pairCandidates) {
        Cell potentialCell1 = potentialPair.get(0);
        Cell potentialCell2 = potentialPair.get(1);
        for (int i = 1; i < 10; i++) {
            boolean valid = true;
            if (pairCandidates.contains(i)) {
                continue;
            }
            if (!candidatesGrid[potentialCell1.row][potentialCell1.col].contains(i) || !candidatesGrid[potentialCell2.row][potentialCell2.col].contains(i)) {
                continue;
            }
            for (Cell cell2 : house) {
                if (candidatesGrid[cell2.row][cell2.col].contains(i) && candidatesGrid[cell2.row][cell2.col].size() == 1) {
                    valid = false;
                    break;
                }
                if (candidatesGrid[cell2.row][cell2.col].contains(i) && !potentialPair.contains(cell2)) {
                    valid = false;
                    break;
                }
            }
            // If second number of pair passes all validity checks, a hidden pair has been found and the loop can be broken
            if (valid) {
                pairCandidates.add(i);
                break;
            }
        }
    }

    // Remove any candidates not a part of the hidden pair/triple from the cells in which the hidden pair/triple exists
    private int cleanCellsOfOtherCandidates(List<Integer> candidates, List<Cell> cellsToClean) {
        int candidatesEliminated = 0;
        for (Cell cell : cellsToClean) {
            for (Iterator<Integer> iterator = candidatesGrid[cell.row][cell.col].iterator(); iterator.hasNext();) {
                Integer candidate = iterator.next();
                if (!candidates.contains(candidate)) {
                    iterator.remove();
                    candidatesEliminated++;
                }
            }
        }

        return candidatesEliminated;
    }

    /*
    Strategy 6)
     */
    private boolean hiddenTriple() {
        strategyMap.putIfAbsent("Hidden Triple", 0);
        boolean candidatesEliminated = false;
        List<List<Integer>> combinations = generateTriplesCombinations();
        for (List<Cell> house : allHouses) {
            CombinationLoop:
            for (List<Integer> combination : combinations) {
                boolean skipCombination = false;
                List<Cell> possibleCellsContainingTriple = new ArrayList<>();
                HashSet<Integer> foundCandidates = new HashSet<>();
                for (Cell cell : house) {
                    skipCombination = checkCellForTripleCombination(cell, combination, foundCandidates, possibleCellsContainingTriple);
                    if (skipCombination) {
                        continue CombinationLoop;
                    }
                }
                if (possibleCellsContainingTriple.size() == 3 && foundCandidates.size() == 3) {
                    int cellsEliminated = cleanCellsOfOtherCandidates(combination, possibleCellsContainingTriple);
                    if (cellsEliminated != 0) {
                        int strategyCount = strategyMap.get("Hidden Triple");
                        strategyMap.put("Hidden Triple", (strategyCount + cellsEliminated));
                        candidatesEliminated = true;
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function used to make sure that at least two of the candidates in the combination appear in the cell
    private boolean checkCellForTripleCombination(Cell cell, List<Integer> combination, HashSet<Integer> foundCandidates, List<Cell> possibleCellsContainingTriple) {
        int candidatesInCombination = 0;
        List<Integer> tempCandidateArr = new ArrayList<>();
        for (int candidate : candidatesGrid[cell.row][cell.col]) {
            if (combination.contains(candidate)) {
                candidatesInCombination++;
                tempCandidateArr.add(candidate);
            }
        }
        if (candidatesInCombination >= 2) {
            possibleCellsContainingTriple.add(cell);
            foundCandidates.addAll(tempCandidateArr);
            return false;
            // Skip the current combination loop if a single candidate is found, as it cannot be part of a triple then
        } else return candidatesInCombination > 0;
    }

    // Helper function used to create a list of int arrays that contains each of the possible
    // hidden triple combinations so that these can be iterated over
    private List<List<Integer>> generateTriplesCombinations() {
        List<List<Integer>> combinations = new ArrayList<>();
        // Generate all combinations of triples that contain the numbers from 1 to 9
        for (int i = 1; i <= 7; i++) {
            for (int j = i + 1; j <= 8; j++) {
                for (int k = j + 1; k <= 9; k++) {
                    List<Integer> combination = new ArrayList<>(Arrays.asList(i, j, k));
                    combinations.add(combination);
                }
            }
        }
        return combinations;
    }

    // Used to attempt to solve the grid using variety of techniques, as well as storing the counts of techniques used
    // so a difficulty rating can be defined. Returns a boolean based on whether the grid can be solved using these
    // techniques or not.
    public boolean solve(int[][] grid) {
        this.grid = grid;
        fillInitialCandidates();
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
            if (!techniqueSuccessful) {
                techniqueSuccessful = nakedPair();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = nakedTriple();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenPair();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenTriple();
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

    public HashMap<String, Integer> getStrategyMap() {
        return this.strategyMap;
    };

    public void printCandidatesGrid() {
        System.out.println(Arrays.deepToString(candidatesGrid).replace("]], ", "]]\n"));
    };

    public static void main(String[] args) {
//        int[][] sudokuGrid1 = {
//                {5, 3, 4, 6, 0, 8, 9, 1, 2},
//                {6, 7, 2, 1, 9, 5, 3, 4, 8},
//                {1, 9, 8, 3, 4, 2, 5, 6, 7},
//                {8, 5, 9, 7, 6, 1, 4, 2, 3},
//                {0, 2, 6, 8, 5, 3, 7, 9, 0},
//                {7, 1, 3, 9, 2, 4, 8, 5, 6},
//                {9, 6, 1, 5, 3, 7, 2, 8, 4},
//                {2, 8, 7, 4, 1, 9, 6, 3, 5},
//                {3, 4, 5, 2, 0, 6, 1, 7, 9}
//        };
//
//        LogicalAssessor solver = new LogicalAssessor();
//        solver.solve(sudokuGrid1);
//        HashMap<String, Integer> sMap = solver.getStrategyMap();
//        int eliminationCount = sMap.get("Basic Elimination");
//        System.out.println(eliminationCount);

//        int[][] sudokuGrid1 = {
//                {0, 2, 8, 0, 0, 7, 0, 0, 0},
//                {0, 1, 6, 0, 8, 3, 0, 7, 0},
//                {0, 0, 0, 0, 2, 0, 8, 5, 1},
//                {1, 3, 7, 2, 9, 0, 0, 0, 0},
//                {0, 0, 0, 7, 3, 0, 0, 0, 0},
//                {0, 0, 0, 0, 4, 6, 3, 0, 7},
//                {2, 9, 0, 0, 7, 0, 0, 0, 0},
//                {0, 0, 0, 8, 6, 0, 1, 4, 0},
//                {0, 0, 0, 3, 0, 0, 7, 0, 0}
//        };
//
//        LogicalAssessor solver = new LogicalAssessor();
//        solver.solve(sudokuGrid1);
//        HashMap<String, Integer> sMap = solver.getStrategyMap();
//        int eliminationCount = sMap.get("Hidden Single");
//        System.out.println(eliminationCount);
//        solver.printCandidatesGrid();

//        int[][] sudokuGrid1 = {
//                {4, 0, 0, 0, 0, 0, 9, 3, 8},
//                {0, 3, 2, 0, 9, 4, 1, 0, 0},
//                {0, 9, 5, 3, 0, 0, 2, 4, 0},
//                {3, 7, 0, 6, 0, 9, 0, 0, 4},
//                {5, 2, 9, 0, 0, 1, 6, 7, 3},
//                {6, 0, 4, 7, 0, 3, 0, 9, 0},
//                {9, 5, 7, 0, 0, 8, 3, 0, 0},
//                {0, 0, 3, 9, 0, 0, 4, 0, 0},
//                {2, 4, 0, 0, 3, 0, 7, 0, 9}
//        };
//
//        LogicalAssessor solver = new LogicalAssessor();
//        solver.solve(sudokuGrid1);
//        HashMap<String, Integer> sMap = solver.getStrategyMap();
//        int eliminationCount = sMap.get("Naked Pair");
//        System.out.println(eliminationCount);
//        GridGenerator gridGen = new GridGenerator();
//        // validate solved grid by converting candidates grid to int nested array and checking validity
//        int[][] gridSolved = new int[9][9];
//        for (int row = 0; row < 9; row++) {
//            for (int col = 0; col < 9; col++) {
//                gridSolved[row][col] = solver.candidatesGrid[row][col].get(0);
//            }
//        }
//        System.out.println(Arrays.deepToString(gridSolved).replace("], ", "]\n"));
//        System.out.println(gridGen.validateGrid(gridSolved));
//        solver.printCandidatesGrid();

//        int[][] sudokuGrid1 = {
//                {2, 9, 4, 5, 1, 3, 0, 0, 6},
//                {6, 0, 0, 8, 4, 2, 3, 1, 9},
//                {3, 0, 0, 6, 9, 7, 2, 5, 4},
//                {0, 0, 0, 0, 5, 6, 0, 0, 0},
//                {0, 4, 0, 0, 8, 0, 0, 6, 0},
//                {0, 0, 0, 4, 7, 0, 0, 0, 0},
//                {7, 3, 0, 1, 6, 4, 0, 0, 5},
//                {9, 0, 0, 7, 3, 5, 0, 0, 1},
//                {4, 0, 0, 9, 2, 8, 6, 3, 7}
//        };
//
//        LogicalAssessor solver = new LogicalAssessor();
//        solver.solve(sudokuGrid1);
//        HashMap<String, Integer> sMap = solver.getStrategyMap();
//        int eliminationCount = sMap.get("Naked Triple");
//        System.out.println(eliminationCount);
//        GridGenerator gridGen = new GridGenerator();
//        int[][] gridSolved = new int[9][9];
//        for (int row = 0; row < 9; row++) {
//            for (int col = 0; col < 9; col++) {
//                gridSolved[row][col] = solver.candidatesGrid[row][col].get(0);
//            }
//        }
//        System.out.println(Arrays.deepToString(gridSolved).replace("], ", "]\n"));
//        System.out.println("Board validity:");
//        System.out.println(gridGen.validateGrid(gridSolved));
//        solver.printCandidatesGrid();

//        int[][] sudokuGrid1 = {
//                {7, 2, 0, 4, 0, 8, 0, 3, 0},
//                {0, 8, 0, 0, 0, 0, 0, 4, 7},
//                {4, 0, 1, 0, 7, 6, 8, 0, 2},
//                {8, 1, 0, 7, 3, 9, 0, 0, 0},
//                {0, 0, 0, 8, 5, 1, 0, 0, 0},
//                {0, 0, 0, 2, 6, 4, 0, 8, 0},
//                {2, 0, 9, 6, 8, 0, 4, 1, 3},
//                {3, 4, 0, 0, 0, 0, 0, 0, 8},
//                {1, 6, 8, 9, 4, 3, 2, 7, 5}
//        };
//
//        LogicalAssessor solver = new LogicalAssessor();
//        solver.solve(sudokuGrid1);
//        HashMap<String, Integer> sMap = solver.getStrategyMap();
//        int eliminationCount = sMap.get("Hidden Pair");
//        System.out.println(eliminationCount);
//        solver.printCandidatesGrid();

        int[][] sudokuGrid1 = {
                {0, 0, 0, 0, 0, 1, 0, 3, 0},
                {2, 3, 1, 0, 9, 0, 0, 0, 0},
                {0, 6, 5, 0, 0, 3, 1, 0, 0},
                {6, 7, 8, 9, 2, 4, 3, 0, 0},
                {1, 0, 3, 0, 5, 0, 0, 0, 6},
                {0, 0, 0, 1, 3, 6, 7, 0, 0},
                {0, 0, 9, 3, 6, 0, 5, 7, 0},
                {0, 0, 6, 0, 1, 9, 8, 4, 3},
                {3, 0, 0, 0, 0, 0, 0, 0, 0}
        };

        LogicalAssessor solver = new LogicalAssessor();
        solver.solve(sudokuGrid1);
        HashMap<String, Integer> sMap = solver.getStrategyMap();
        int eliminationCount = sMap.get("Hidden Triple");
        System.out.println(eliminationCount);
        GridGenerator gridGen = new GridGenerator();
        // validate solved grid by converting candidates grid to int nested array and checking validity
        int[][] gridSolved = new int[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                gridSolved[row][col] = solver.candidatesGrid[row][col].get(0);
            }
        }
        System.out.println(Arrays.deepToString(gridSolved).replace("], ", "]\n"));
        System.out.println(gridGen.validateGrid(gridSolved));
        solver.printCandidatesGrid();

    }
}
