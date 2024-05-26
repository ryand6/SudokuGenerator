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
    private List<List<Cell>> rowHouses;
    private List<List<Cell>> colHouses;
    private List<List<Cell>> blockHouses;
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

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    public LogicalAssessor() {
        initialiseAllHouses();
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

    private void initialiseAllHouses() {
        // Each nested list represents a house (row) which contains each of the cells in that house
        this.rowHouses = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            List<Cell> rowHouse = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                Cell cell = new Cell(row, col);
                rowHouse.add(cell);
            }
            rowHouses.add(rowHouse);
        }

        // Each nested list represents a house (column) which contains each of the cells in that house
        this.colHouses = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            List<Cell> colHouse = new ArrayList<>();
            for (int row = 0; row < 9; row++) {
                Cell cell = new Cell(row, col);
                colHouse.add(cell);
            }
            colHouses.add(colHouse);
        }

        // Each nested list represents a house (block) which contains each of the cells in that house
        this.blockHouses = new ArrayList<>();
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

    /*
    Strategy 7) Covers both pointing pairs and box line reduction
     */
    private boolean intersectionRemoval() {
        strategyMap.putIfAbsent("Intersection", 0);
        boolean candidatesEliminated = false;
        List<List<Cell>> lines = new ArrayList<>();
        lines.addAll(rowHouses);
        lines.addAll(colHouses);
        for (List<Cell> block : blockHouses) {
            for (List<Cell> line : lines) {
                HashSet<Cell> intersection = new HashSet<>(block);
                intersection.retainAll(line);
                // If no cells can be found to intersect both the block and the line, skip to next
                if (intersection.isEmpty()) {
                    continue;
                }
                HashSet<Cell> nonIntersectBlockCells = new HashSet<>(block);
                nonIntersectBlockCells.removeAll(intersection);
                List<Cell> blockOnlyCells = new ArrayList<>(nonIntersectBlockCells);
                HashSet<Cell> nonIntersectLineCells = new HashSet<>(line);
                nonIntersectLineCells.removeAll(intersection);
                List<Cell> lineOnlyCells = new ArrayList<>(nonIntersectLineCells);

                List<Integer> intersectCandidates = getUniqueCandidatesInCells(new ArrayList<>(intersection));
                List<Integer> blockOnlyCandidates = getUniqueCandidatesInCells(blockOnlyCells);
                List<Integer> lineOnlyCandidates = getUniqueCandidatesInCells(lineOnlyCells);
                int eliminatedCount = 0;
                for (int i = 1; i < 10; i++) {
                    // if true, box line reduction can potentially be applied - if 'i' is present in the intersection more than once,
                    // any occurrence of 'i' in the box where the cells don't intersect, will be removed
                    if (intersectCandidates.contains(i) && blockOnlyCandidates.contains(i) && !lineOnlyCandidates.contains(i)) {
                        eliminatedCount += cleanCellsOfSingleCandidate(i, blockOnlyCells);
                        // if true, pointing pair / triple has potentially been found - if 'i' is present in the intersection more than once,
                        // any occurrence of 'i' in the intersecting line where the cells don't intersect, will be removed
                    } else if (intersectCandidates.contains(i) && lineOnlyCandidates.contains(i) && !blockOnlyCandidates.contains(i)) {
                        eliminatedCount += cleanCellsOfSingleCandidate(i, lineOnlyCells);
                    }
                }
                if (eliminatedCount != 0) {
                    int strategyCount = strategyMap.get("Intersection");
                    strategyMap.put("Intersection", (strategyCount + eliminatedCount));
                    candidatesEliminated = true;
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for intersectionRemoval used to identify the candidates found in a list of cells,
    // that can be compared to the cells in an intersecting house/list of cells
    private List<Integer> getUniqueCandidatesInCells(List<Cell> cells) {
        HashSet<Integer> foundCandidates = new HashSet<>();
        for (Cell cell : cells) {
            foundCandidates.addAll(candidatesGrid[cell.row][cell.col]);
        }
        return new ArrayList<>(foundCandidates);
    }

    // Helper function for removing all occurrences of a single candidate from a list of cells
    private int cleanCellsOfSingleCandidate(int num, List<Cell> cells) {
        int counter = 0;
        for (Cell cell : cells) {
            if (candidatesGrid[cell.row][cell.col].contains(num)) {
                candidatesGrid[cell.row][cell.col].remove((Integer) num);
                counter++;
            }
        }
        return counter;
    }

    /*
    Strategy 8) X-Wing
     */
    private boolean xWing() {
        strategyMap.putIfAbsent("X-Wing", 0);
        boolean candidatesEliminated = false;
        for (int row1 = 0; row1 < 9; row1++) {
            for (int row2 = row1 + 1; row2 < 9; row2++) {
                for (int col1 = 0; col1 < 9; col1++) {
                    for (int col2 = col1 + 1; col2 < 9; col2++) {
                        HashSet<Cell> rowCells = new HashSet<>(rowHouses.get(row1));
                        rowCells.addAll(rowHouses.get(row2));
                        HashSet<Cell> colCells = new HashSet<>(colHouses.get(col1));
                        colCells.addAll(colHouses.get(col2));
                        // Get set of cells that intersect the rows and cols
                        HashSet<Cell> intersection = new HashSet<>(rowCells);
                        intersection.retainAll(colCells);
                        // There must be 4 intersecting cells where the horizontal and vertical houses cross
                        if (intersection.size() != 4) {
                            continue;
                        }
                        // Get cells in the two rows that aren't part of the intersection
                        HashSet<Cell> rowOnlyCellsSet = new HashSet<>(rowCells);
                        rowOnlyCellsSet.removeAll(intersection);
                        List<Cell> rowOnlyCells = new ArrayList<>(rowOnlyCellsSet);
                        // Get cells in the two cols that aren't part of the intersection
                        HashSet<Cell> colOnlyCellsSet = new HashSet<>(colCells);
                        List<Cell> colOnlyCells = new ArrayList<>(colOnlyCellsSet);
                        colOnlyCells.removeAll(intersection);

                        // Get list of candidates (including dupes) that are found in each of the lists of cells
                        List<Integer> intersectCandidates = getCandidatesInCells(new ArrayList<>(intersection));
                        List<Integer> rowOnlyCandidates = getCandidatesInCells(rowOnlyCells);
                        List<Integer> colOnlyCandidates = getCandidatesInCells(colOnlyCells);

                        int eliminatedCount = 0;
                        for (int i = 1; i < 10; i++) {
                            // Candidate being tested must appear in all 4x intersecting cells
                            if (Collections.frequency(intersectCandidates, i) != 4) {
                                continue;
                            }
                            // Remove candidate occurrences if they're found in one set of directional lines but not the other
                            if (rowOnlyCandidates.contains(i) && !colOnlyCandidates.contains(i)) {
                                eliminatedCount += cleanCellsOfSingleCandidate(i, rowOnlyCells);
                            } else if (colOnlyCandidates.contains(i) && !rowOnlyCandidates.contains(i)) {
                                eliminatedCount += cleanCellsOfSingleCandidate(i, colOnlyCells);
                            }
                        }
                        if (eliminatedCount != 0) {
                            int strategyCount = strategyMap.get("X-Wing");
                            strategyMap.put("X-Wing", (strategyCount + eliminatedCount));
                            candidatesEliminated = true;
                        }
                    }
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for xWing to get all candidates in a list of cells, including dupes
    private List<Integer> getCandidatesInCells(List<Cell> cells) {
        ArrayList<Integer> foundCandidates = new ArrayList<>();
        for (Cell cell : cells) {
            foundCandidates.addAll(candidatesGrid[cell.row][cell.col]);
        }
        return foundCandidates;
    }

    /*
    Strategy 9) Simple Colours
     */
    private boolean simpleColours() {
        strategyMap.putIfAbsent("Simple Colouring", 0);
        boolean candidatesEliminated = false;
        for (int i = 1; i < 10; i++) {
            List<HashSet<Cell>> linkedCells = getAllLinkedCells(i);
            List<List<List<Cell>>> chainsOfLinkedCells = separateIntoChains(linkedCells);
            for (List<List<Cell>> chain : chainsOfLinkedCells) {
                List<List<Cell>> colourGroups = colourChains(chain);
                List<Cell> colourGroup1 = colourGroups.get(0);
                List<Cell> colourGroup2 = colourGroups.get(1);
                int eliminatedCount = 0;
                eliminatedCount += twoSameColourInHouse(i, colourGroup1);
                eliminatedCount += twoSameColourInHouse(i, colourGroup2);
                eliminatedCount += candidateSpottedByTwoColours(i, colourGroup1, colourGroup2);
                if (eliminatedCount != 0) {
                    int strategyCount = strategyMap.get("Simple Colouring");
                    strategyMap.put("Simple Colouring", (strategyCount + eliminatedCount));
                    candidatesEliminated = true;
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function for simpleColours used to return a list of all linked cell pairs
    private List<HashSet<Cell>> getAllLinkedCells(int num) {
        List<HashSet<Cell>> linkedCells = new ArrayList<>();
        for (List<Cell> house : allHouses) {
            HashSet<Cell> linkedPair = getLinkedPair(num, house);
            if (linkedPair != null && !linkedCells.contains(linkedPair)) {
                linkedCells.add(linkedPair);
            }
        }
        return linkedCells;
    }

    // Helper function used to return a linked pair within a house, if one exists
    private HashSet<Cell> getLinkedPair(int num, List<Cell> house) {
        HashSet<Cell> linkedPair = new HashSet<>();
        int counter = 0;
        for (Cell cell : house) {
            if (candidatesGrid[cell.row][cell.col].size() > 1 && candidatesGrid[cell.row][cell.col].contains(num)) {
                linkedPair.add(cell);
                counter++;
            }
        }
        if (counter == 2) {
            return linkedPair;
        }
        return null;
    }

    // Helper function used to separate all cell links into their corresponding link chain
    private List<List<List<Cell>>> separateIntoChains(List<HashSet<Cell>> linkedCells) {
        List<List<List<Cell>>> chains = new ArrayList<>();
        while (!linkedCells.isEmpty()) {
            List<List<Cell>> chain = new ArrayList<>();
            chain.add(linkedCells.get(0).stream().toList());
            linkedCells.remove(0);
            boolean continueChain = true;
            while (continueChain) {
                continueChain = false;
                for (List<Cell> chainPair : chain) {
                    // Using iterator so elements can be deleted whilst iterating
                    Iterator<HashSet<Cell>> iterator = linkedCells.iterator();
                    while (iterator.hasNext()) {
                        HashSet<Cell> linkedPair = iterator.next();
                        List<Cell> pair = linkedPair.stream().toList();
                        // Check if one of the cells in the link matches one of the cells in the other link, forming a chain
                        if (chainPair.contains(pair.get(0)) || chainPair.contains(pair.get(1))) {
                            chain.add(pair);
                            iterator.remove();
                            continueChain = true;
                        }
                    }
                }
            }
            chains.add(chain);
        }
        return chains;
    }

    // Used to separate a chain into two colour groups
    private List<List<Cell>> colourChains(List<List<Cell>> chain) {
        List<List<Cell>> colourGroups = new ArrayList<>();
        List<Cell> group1 = new ArrayList<>();
        List<Cell> group2 = new ArrayList<>();
        boolean continueDownChain = true;
        while (continueDownChain) {
            continueDownChain = false;
            for (List<Cell> link : chain) {
                // add first values to colour groups
                if (group1.isEmpty() && group2.isEmpty()) {
                    group1.add(link.get(0));
                    group2.add(link.get(1));
                    continueDownChain = true;
                }
                // logic used to separate cells into colour groups depending on what colour group their linked cell is in
                if (group1.contains(link.get(0)) && !group2.contains(link.get(1))) {
                    group2.add(link.get(1));
                    continueDownChain = true;
                }
                if (group1.contains(link.get(1)) && !group2.contains(link.get(0))) {
                    group2.add(link.get(0));
                    continueDownChain = true;
                }
                if (group2.contains(link.get(0)) && !group1.contains(link.get(1))) {
                    group1.add(link.get(1));
                    continueDownChain = true;
                }
                if (group2.contains(link.get(1)) && !group1.contains(link.get(0))) {
                    group1.add(link.get(0));
                    continueDownChain = true;
                }
            }
        }
        colourGroups.add(group1);
        colourGroups.add(group2);
        return colourGroups;
    }

    // Used to detect if more than one cell from the same colour group is present in a house, and if so remove the specified candidate from all
    // cells in the colour group as it cannot be contained in that group
    private int twoSameColourInHouse(int num, List<Cell> group) {
        int counter = 0;
        int candidatesEliminated = 0;
        for (List<Cell> house : allHouses) {
            for (Cell cell : house) {
                if (group.contains(cell) && candidatesGrid[cell.row][cell.col].contains(num)) {
                    counter++;
                }
            }
        }
        // if more than 1x cell that contains the specified candidate is present in the colour group for that house, neither cell
        // in the group can contain that candidate
        if (counter > 1) {
            for (Cell cell : group) {
                candidatesGrid[cell.row][cell.col].remove((Integer) num);
                candidatesEliminated++;
            }
        }
        return candidatesEliminated;
    }

    // Used to detect if a non-coloured cell is spotted by a cell from each coloured group, and if so the checked candidate is removed from the non-coloured cell
    private int candidateSpottedByTwoColours(int num, List<Cell> group1, List<Cell> group2) {
        int candidatesEliminated = 0;
        for (List<Cell> house : blockHouses) {
            for (Cell cell : house) {
                // Make sure cell contains the candidate and is not a coloured cell
                if (candidatesGrid[cell.row][cell.col].contains(num) && !group1.contains(cell) && !group2.contains(cell)) {
                    boolean spottedGroup1 = checkCellSpottedByGroup(cell, group1, house);
                    if (!spottedGroup1) {
                        continue;
                    }
                    boolean spottedGroup2 = checkCellSpottedByGroup(cell, group2, house);
                    if (!spottedGroup2) {
                        continue;
                    }
                    candidatesGrid[cell.row][cell.col].remove((Integer) num);
                    candidatesEliminated++;
                }
            }
        }
        return candidatesEliminated;
    }

    // Helper function used to check if a cell can be spotted by another cell that exists in a group
    private boolean checkCellSpottedByGroup(Cell cell, List<Cell> group, List<Cell> blockHouse) {
        for (Cell groupCell : group) {
            // check if cell is in same row, column or block as the coloured cell
            if (cell.row == groupCell.row || cell.col == groupCell.col || blockHouse.contains(groupCell)) {
                return true;
            }

        }
        return false;
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
            System.out.println("BASIC ELIMINATION CALLED");
            printCandidatesGrid();
            System.out.println();
            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenSingle();
                System.out.println("HIDDEN SINGLE CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = nakedPair();
                System.out.println("NAKED PAIR CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = nakedTriple();
                System.out.println("NAKED TRIPLE CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenPair();
                System.out.println("HIDDEN PAIR CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = hiddenTriple();
                System.out.println("HIDDEN TRIPLE CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = intersectionRemoval();
                System.out.println("INTERSECTION REMOVAL CALLED");
                printCandidatesGrid();
                System.out.println();
            }
            if (!techniqueSuccessful) {
                techniqueSuccessful = xWing();
                System.out.println("X-WING CALLED");
                printCandidatesGrid();
                System.out.println();
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

        int[][] sudokuGrid1 = {
                {1, 0, 0, 0, 0, 0, 5, 6, 9},
                {4, 9, 2, 0, 5, 6, 1, 0, 8},
                {0, 5, 6, 1, 0, 9, 2, 4, 0},
                {0, 0, 9, 6, 4, 0, 8, 0, 1},
                {0, 6, 4, 0, 1, 0, 0, 0, 0},
                {2, 1, 8, 0, 3, 5, 6, 0, 4},
                {0, 4, 0, 5, 0, 0, 0, 1, 6},
                {9, 0, 5, 0, 6, 1, 4, 0, 2},
                {6, 2, 1, 0, 0, 0, 0, 0, 5}
        };

        LogicalAssessor solver = new LogicalAssessor();
        solver.solve(sudokuGrid1);
        HashMap<String, Integer> sMap = solver.getStrategyMap();
        int eliminationCount = sMap.get("X-Wing");
        System.out.println(eliminationCount);
        GridGenerator gridGen = new GridGenerator();
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
