package com.bluerock.lightsout.services;

import com.bluerock.lightsout.entities.LightDto;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author DanHung
 * @version V1.0
 * @Package com.bluerock.lightsout.services
 * @date 10/13/22 9:50 PM
 */
public class LightsOutService {
    public static void main(String[] args) {
        LightsOutService lightsOutService = new LightsOutService();
        String path = "/Users/DanHung/Desktop/2022履歷/blueRock/samples/" +
                "06" +
                ".txt";
        lightsOutService.doLightsOut(path);
    }

    private int xCount = 0;
    private LightDto boardLightDto = null;
    private int depth = -1;
    private List<List<LightDto>> puzzles = new ArrayList<>();
    private List<List<LightDto>> deductedPuzzles = new ArrayList<>();
    private List<List<Integer>> publicPossibleCombinations = new ArrayList<>();

    private int boardColumnsLength;
    private int boardRowsLength;
    private int arrayLength;
    private long startTime;

    private final int BUCKET_SIZE = 10 * 1000;
    private final String SPACE_STRING = " ";
    private final String COMMA_STRING = ",";
    private final char X_CHAR = 'X';


    private void doLightsOut(String filePath) {
        startTime = System.currentTimeMillis();

        // parameters
        String[] txtArr = readTxtFile(filePath);
        String depthStr = txtArr[0];
        String boardStr = txtArr[1];
        String input = txtArr[2];
        String boardString = "board";

        depth = Integer.parseInt(depthStr);
        // calculate the square length of board
        calculateSquareLength(boardStr);
        // trans board string to intArray
        int[] result = transStringToIntArray(boardStr);
        boardLightDto = new LightDto(boardString, boardString, result);
        String[] inputRows = input.split(SPACE_STRING);

        // Each "puzzle" means each "pieces"
        //      Here it trans the rowString(e.g. "XX,XX,XX") to LightDtos, then put them in a puzzle list
        puzzles = interpretPuzzles(inputRows);

        // Here we cut off some puzzles with most combinations so that we could deduct the calculating numbers
        //    We will add these deductedPuzzles back later
        deductPuzzles();
        // calculate the "X" numbers in deductedPuzzles
        calculateXCount();

        // Get sizes of puzzles so later we could use it to generate combinations
        List<List<Integer>> puzzleSizes = getPuzzleSizes(puzzles);

        // Recursively generate combinations
        List<List<Integer>> publicBuckets = new ArrayList<>();
        Map<Integer, Integer> combinations = new HashMap<>();
        publicBuckets = recGetCombinations(puzzleSizes, publicBuckets, combinations, 0);

        // dump publicBuckets: there might be some combinations left in the buckets
        if (!ObjectUtils.isEmpty(publicBuckets)) {
            // Filter out those combinations which contain different number of xCount
            //      By doing this we could reduce the numbers of calculating
            filterPossibleCombinations(publicBuckets);
        }

        // add back puzzles deducted in the beginning
        addDeductedPuzzles();
        // match all the puzzle combinations
        List<LightDto> resultLightDtos = findLights(publicPossibleCombinations);
        printResult(resultLightDtos, inputRows);
        printSpentTime();

    }

    private void printSpentTime() {
        long spendTime = System.currentTimeMillis() - startTime;
        if (spendTime < 1000) {
            System.out.println("spent:" + spendTime + " milliseconds");
        } else {
            System.out.println("spent:" + (spendTime) / 1000 + " seconds");
        }
    }

    /**
     * @param filePath the path where sample questions locate
     * @return String array contents
     */
    public String[] readTxtFile(String filePath) {
        String[] strArr = new String[3];
        try {
            String encoding = "UTF8";
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt;
                int count = 0;
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    strArr[count++] = lineTxt;
                    // System.out.println(lineTxt);
                }
                read.close();
            } else {
                System.out.println("no such file");
            }
        } catch (Exception e) {
            System.out.println("cannot read content");
            e.printStackTrace();
        }
        return strArr;
    }

    private void calculateSquareLength(String boardStr) {
        String[] columns = boardStr.split(COMMA_STRING);
        boardRowsLength = columns.length;
        boardColumnsLength = columns[0].length();

        String boardStringWithoutComma = StringUtils.delete(boardStr, COMMA_STRING);
        arrayLength = boardStringWithoutComma.length();
    }

    private int[] transStringToIntArray(String boardStr) {
        String remove = StringUtils.delete(boardStr, COMMA_STRING);
        int[] result = new int[remove.length()];
        for (int i = 0; i < remove.length(); i++) {
            result[i] = Character.getNumericValue(remove.charAt(i));
        }
        return result;
    }

    /**
     * Each "puzzle" means each "pieces."
     *      Here it trans the rowString(e.g. "XX,XX,XX") to LightDtos, then put them in a puzzle list
     */
    private List<List<LightDto>> interpretPuzzles(String[] rows) {
        List<List<LightDto>> puzzles = new ArrayList<>();
        for (String rowString : rows) {
            String[] columns = rowString.split(COMMA_STRING);
            int columnsLength = columns[0].length();
            int rowsLength = columns.length;
            List<LightDto> onePuzzleLightDtos = interpretOnePuzzle(rowString, columnsLength, rowsLength);
            puzzles.add(onePuzzleLightDtos);
        }

        /* print for debugging
        puzzles.forEach(puzzle -> {
            for (LightDto lightDto : puzzle) {
                System.out.print(lightDto.getPuzzleName() + "-" + lightDto.getCoordinates() + ":");
                System.out.println(Arrays.toString(lightDto.getIntArray()));
            }
            System.out.println("==============");
        });
        */

        return puzzles;
    }

    private List<LightDto> interpretOnePuzzle(String rowString, int columnLength, int rowLength) {
        List<LightDto> lightDtos = new ArrayList<>();
        // 1. addRight
        int rightAddTimes = boardColumnsLength - columnLength;
        int[] rowFirstArray = addRight(rowString, rightAddTimes);
        String doubleZeroString = "00";
        LightDto lightDto = new LightDto(rowString, doubleZeroString, rowFirstArray);
        lightDtos.add(lightDto);

        // 2. addLeft. Move this piece to right spot, this means add one zero on left side each time
        for (int i = 0; i < rightAddTimes; i++) {
            String oldCoordinates = lightDto.getCoordinates();
            lightDto = addLeft(lightDto, "0");
            String newCoordinates = addCoordinates(oldCoordinates, true, 1);
            lightDto.setCoordinates(newCoordinates);
            lightDto.setPuzzleName(rowString);
            lightDtos.add(lightDto);
        }

        // 3. addBottom. Move piece to below spot, this means add [partitionNumber] zeros on the left side each time.
        if (rowLength == boardRowsLength) {
            // you cannot addBottom, just return it
            return lightDtos;
        }
        return addBottom(lightDtos, boardRowsLength - rowLength, boardColumnsLength);
    }

    private int[] addRight(String input, int addNumber) {
        //    XX,XX,XX --> XX0,XX0,XX0 --> 110,110,110
        String[] rows = input.split(COMMA_STRING);
        int[] array = new int[arrayLength];
        int count = 0;
        for (String row : rows) {
            for (int i = 0; i < row.length() + addNumber; i++) {
                if (i < row.length()) {
                    if (row.charAt(i) == X_CHAR) {
                        array[count] = 1;
                    }
                } else {
                    array[count] = 0;
                }
                count++;
            }
        }
        return array;
    }

    /**
     * @param lightDto dto that contains puzzleName, coordinates and intArray
     * @param input    add input to the left of this arrays
     * @return [110110000], "00"
     * [001101100]
     */
    private LightDto addLeft(LightDto lightDto, String input) {
        int[] arrays = lightDto.getIntArray();
        int length = input.length();
        int arrayLength = arrays.length;

        int[] result = new int[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            if (i < length) {
                result[i] = Character.getNumericValue(input.charAt(i));
            } else {
                result[i] = arrays[i - length];
            }
        }
        return new LightDto(null, null, result);
    }

    private String addCoordinates(String coordinates, boolean isAddLeft, int addNumber) {
        // (x,y)
        char xCoordinates = coordinates.charAt(0);
        char yCoordinates = coordinates.charAt(1);
        StringBuilder sb = new StringBuilder();
        if (isAddLeft) {
            // add x
            int xInt = Character.getNumericValue(xCoordinates);
            xInt += addNumber;
            sb.append(xInt).append(yCoordinates);
        } else {
            int yInt = Character.getNumericValue(yCoordinates);
            yInt += addNumber;
            sb.append(xCoordinates).append(yInt);
        }
        return sb.toString();
    }

    private List<LightDto> addBottom(List<LightDto> lightDtos, int addTimes, int partitionNumber) {
        List<LightDto> newList = new ArrayList<>(lightDtos);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addTimes; i++) {
            sb.append("0".repeat(Math.max(0, partitionNumber)));
            for (LightDto lightDto : lightDtos) {
                String name = lightDto.getCoordinates();
                LightDto addLeftDto = addLeft(lightDto, sb.toString());
                name = addCoordinates(name, false, i + 1);
                addLeftDto.setCoordinates(name);
                addLeftDto.setPuzzleName(lightDto.getPuzzleName());
                newList.add(addLeftDto);
            }
        }
        return newList;
    }

    /**
     * Find puzzles with most combinations and deduct them from puzzles
     */
    private void deductPuzzles() {
        // 1. find the puzzle with most combinations
        // puzzleQueue: [index, List<LightDto> ]
        PriorityQueue<Object[]> puzzleQueue = new PriorityQueue<>((pair1, pair2) ->
                ((List<LightDto>) pair2[1]).size() - ((List<LightDto>) pair1[1]).size());

        for (int i = 0; i < puzzles.size(); i++) {
            puzzleQueue.add(new Object[]{i, puzzles.get(i)});
        }

        // 2. add removeIndexes and put puzzle into deductedPuzzles
        List<Integer> removeIndexes = new ArrayList<>();
        int count = depth - 1;
        while (count-- > 0) {
            Object[] poll = puzzleQueue.poll();
            int index = (int) poll[0];
            removeIndexes.add(index);
            List<LightDto> puzzle = (List<LightDto>) poll[1];
            deductedPuzzles.add(puzzle);
        }
        // 3. remove puzzle from puzzles
        List<List<LightDto>> tempPuzzles = new ArrayList<>();
        for (int i = puzzles.size() - 1; i >= 0; i--) {
            if (!removeIndexes.contains(i)) {
                tempPuzzles.add(puzzles.get(i));
            }
        }
        puzzles = tempPuzzles;
    }

    /**
     * Calculate the "X" numbers in deductedPuzzles
     */
    private void calculateXCount() {
        for (List<LightDto> deductedPuzzle : deductedPuzzles) {
            String puzzleName = deductedPuzzle.get(0).getPuzzleName();
            for (int i = 0; i < puzzleName.length(); i++) {
                if (puzzleName.charAt(i) == X_CHAR) {
                    xCount++;
                }
            }
        }
    }

    /**
     * Get a list of sizes of puzzles.
     * e.g. {{0}, {0,1}, {0,1,2,3}, {0,1,2,3,4,5}}
     * This is for later generating the combinations
     */
    private List<List<Integer>> getPuzzleSizes(List<List<LightDto>> puzzles) {
        List<List<Integer>> puzzleSizes = new ArrayList<>();
        for (List<LightDto> puzzle : puzzles) {
            int size = puzzle.size();
            List<Integer> sizes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                sizes.add(i);
            }
            puzzleSizes.add(sizes);
        }
        return puzzleSizes;
    }

    /**
     * Recursively generate combinations
     */
    private List<List<Integer>> recGetCombinations(List<List<Integer>> puzzleSizes, List<List<Integer>> publicBuckets, Map<Integer, Integer> combinations, int index) {
        List<Integer> integers = puzzleSizes.get(index);
        if (index == puzzleSizes.size() - 1) {
            // last integers
            Map<Integer, Integer> tempCombinations = new HashMap<>(combinations);
            for (Integer integer : integers) {

                tempCombinations.put(index, integer);
                publicBuckets.add(new ArrayList<>(tempCombinations.values()));

                // set a maximum size
                if (publicBuckets.size() > BUCKET_SIZE) {
                    filterPossibleCombinations(publicBuckets);
                    publicBuckets = new ArrayList<>();
                }
            }
        } else {
            // 1. take one integer from integers
            for (Integer integer : integers) {
                combinations.put(index, integer);

                // call rec
                publicBuckets = recGetCombinations(puzzleSizes, publicBuckets, combinations, index + 1);

            }
            combinations.remove(index);
        }
        return publicBuckets;
    }

    /**
     * Filter out those combinations contain different number of xCount
     *      Through this we could deduct the combinations
     */
    private void filterPossibleCombinations(List<List<Integer>> combinations) {
        for (List<Integer> combination : combinations) {
            int[] ints = getLightDtoInts(combination);

            if (isPuzzleMatched(ints, depth)) {
                publicPossibleCombinations.add(combination);
            }
        }
    }

    private int[] getLightDtoInts(List<Integer> combination) {
        List<LightDto> lightDtos = new ArrayList<>();
        for (int i = 0; i < combination.size(); i++) {
            int currentInt = combination.get(i);
            LightDto lightDto = puzzles.get(i).get(currentInt);
            lightDtos.add(lightDto);
        }
        lightDtos.add(boardLightDto);
        return addAllArrays(lightDtos);
    }

    private int[] addAllArrays(List<LightDto> lightDtos) {
        int[] result = new int[arrayLength];
        for (LightDto lightDto : lightDtos) {
            result = arrayAdd(result, lightDto.getIntArray());
        }
        return result;
    }

    private int[] arrayAdd(int[] a, int[] b) {
        int i = 0;
        int[] result = new int[a.length];
        for (int aNumber : a) {
            result[i] = aNumber + b[i];
            i++;
        }
        return result;
    }

    /**
     * Check if the array contains the same number of xCount
     */
    private boolean isPuzzleMatched(int[] array, int depth) {
        int count = 0;
        for (int i : array) {
            int mod = i % depth;
            if (mod != 0) {
                count += (depth - mod);
            }
        }
        return count == xCount;
    }

    private void addDeductedPuzzles() {
        for (List<LightDto> deductedPuzzle : deductedPuzzles) {
            List<Integer> deductedPuzzleSizes = getPuzzleSizes(
                    new ArrayList<>(Collections.singletonList(deductedPuzzle))).get(0);
            publicPossibleCombinations = genCombinedCombinations(publicPossibleCombinations, deductedPuzzleSizes);
            puzzles.add(deductedPuzzle);
        }
    }

    private List<List<Integer>> genCombinedCombinations(List<List<Integer>> combinations, List<Integer> deductedPuzzleSizes) {
        List<List<Integer>> combinedCombinations = new ArrayList<>();
        for (Integer integer : deductedPuzzleSizes) {
            List<Integer> tempNumbers;
            for (List<Integer> numbers : combinations) {
                tempNumbers = new ArrayList<>(numbers);
                tempNumbers.add(integer);
                combinedCombinations.add(tempNumbers);
            }
        }
        return combinedCombinations;
    }

    private List<LightDto> findLights(List<List<Integer>> combinations) {
        for (List<Integer> combination : combinations) {
            List<LightDto> lightDtos = new ArrayList<>();
            for (int i = 0; i < combination.size(); i++) {
                int currentInt = combination.get(i);
                LightDto lightDto = puzzles.get(i).get(currentInt);
                lightDtos.add(lightDto);
            }
            lightDtos.add(boardLightDto);
            int[] intArr = addAllArrays(lightDtos);

            if (isAllZero(intArr, depth)) {
                return lightDtos;
            }
        }
        return null;
    }

    private void printResult(List<LightDto> lightDtos, String[] inputRows ) {
        if (ObjectUtils.isEmpty(lightDtos)) {
            System.out.println("Cannot find results");
            return;
        }

        Map<String, LightDto> lightDtoMap = lightDtos.stream()
                .collect(HashMap::new, (map, lightDto) -> map.put(
                        lightDto.getPuzzleName(),
                        lightDto
                ), HashMap::putAll);
        StringBuilder sb = new StringBuilder();
        for (String inputRow : inputRows) {
            String coordinates = lightDtoMap.get(inputRow).getCoordinates();
            sb.append(coordinates.charAt(0))
                    .append(COMMA_STRING)
                    .append(coordinates.charAt(1))
                    .append(SPACE_STRING);
        }
        sb.deleteCharAt(sb.lastIndexOf(SPACE_STRING));
        System.out.println(sb.toString());

        /* print for debugging
        System.out.println("#### Found it!! #####");
        for (LightDto lightDto : lightDtos) {
            if (boardLightDto.equals(lightDto.getPuzzleName())) {
                continue;
            }
            System.out.println(lightDto.getPuzzleName() + ":" + lightDto.getCoordinates());
        }

         */

    }

    private boolean isAllZero(int[] array, int depth) {
        // int i = 0;
        // int[] result = new int[array.length];
        for (int aNumber : array) {
            if (aNumber % depth != 0) {
                return false;
            }
        }
        return true;
    }
}
