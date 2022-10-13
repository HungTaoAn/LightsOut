package com.bluerock.lightsout.services;

import com.bluerock.lightsout.entities.LightDto;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

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
                "07" +
                ".txt";
        lightsOutService.doLightsOut(path);

        // new LightsOut().getIntPermutationsTest();
        // lightsOutService.xCount = 9;
        // int[] arr = new int[]{2,2,1,1,1,2};
        // boolean puzzleMatched = lightsOutService.isPuzzleMatched(arr, 3);
        // System.out.println(puzzleMatched);


        // List<Integer> a = new ArrayList<>(Arrays.asList(0));
        // List<Integer> b = new ArrayList<>(Arrays.asList(0, 1));
        // List<Integer> c = new ArrayList<>(Arrays.asList(0,1,2));
        // List<Integer> d = new ArrayList<>(Arrays.asList(0,1,2,3));
        // List<List<Integer>> list = new ArrayList<>(Arrays.asList(a, b,c, d));
        // List<List<Integer>> buckets = new ArrayList<>();
        // List<Integer> combinations = new ArrayList<>();
        // recGetNumbers(list, buckets, combinations, 0, list.size());

    }

    private int xCount = 0;
    private LightDto board = null;
    private int depth = -1;
    private List<List<LightDto>> puzzles = new ArrayList<>();
    private List<List<LightDto>> deductedPuzzles = new ArrayList<>();
    private List<List<Integer>> possiblePermutations = new ArrayList<>();
    private final int bucketSize = 10 * 1000;
    // private int bucketSize = 1;

    private int boardColumnsLength;
    private int boardRowsLength;
    private int arrayLength;
    long startTime, endTime;


    private void doLightsOut(String filePath) {
        startTime = System.currentTimeMillis();

        // parameters
        String[] txtArr = readTxtFile(filePath);
        String depthStr = txtArr[0];
        String boardStr = txtArr[1];
        String input = txtArr[2];
        String name = "board";

        depth = Integer.parseInt(depthStr);
        calculateSquareLength(boardStr);
        int[] result = stringToIntArray(boardStr);
        board = new LightDto(name, name, result);

        puzzles = interpretPuzzles(input);
        deductPuzzles();
        calculateXCount();

        List<List<Integer>> puzzleSizes = getPuzzleSizes(puzzles);

        List<List<Integer>> buckets = new ArrayList<>();
        Map<Integer, Integer> combinations = new HashMap<>();
        buckets = recGetNumbers(puzzleSizes, buckets, combinations, 0, puzzleSizes.size());

        if (!ObjectUtils.isEmpty(buckets)) {
            System.out.println("dump buckets");
            filterPossiblePermutations(buckets);
        }

        addDeductedPuzzles();
        findLights(possiblePermutations);

    }

    private void addDeductedPuzzles() {
        for (List<LightDto> deductedPuzzle : deductedPuzzles) {
            List<Integer> deductedPuzzleSizes = getPuzzleSizes(
                    new ArrayList<>(Collections.singletonList(deductedPuzzle))).get(0);
            possiblePermutations = genCombinedPermutations(possiblePermutations, deductedPuzzleSizes);
            puzzles.add(deductedPuzzle);
        }
    }

    private void deductPuzzles() {
        // 1. find the puzzle with most opportunities
        // [index, List<LightDto> ]
        PriorityQueue<Object[]> puzzleQueue = new PriorityQueue<>((pair1, pair2) ->
                ((List<LightDto>) pair2[1]).size() - ((List<LightDto>) pair1[1]).size());

        for (int i = 0; i < puzzles.size(); i++) {
            puzzleQueue.add(new Object[]{i, puzzles.get(i)});
        }

        List<Integer> removeIndexes = new ArrayList<>();
        int count = depth - 1;
        while (count-- > 0) {
            Object[] poll = puzzleQueue.poll();
            int index = (int) poll[0];
            removeIndexes.add(index);
            List<LightDto> puzzle = (List<LightDto>) poll[1];
            deductedPuzzles.add(puzzle);
        }
        List<List<LightDto>> tempPuzzles = new ArrayList<>();
        for (int i = puzzles.size() - 1; i >= 0; i--) {
            if (!removeIndexes.contains(i)) {
                tempPuzzles.add(puzzles.get(i));
            }
        }
        puzzles = tempPuzzles;
    }

    private void calculateXCount() {
        for (List<LightDto> deductedPuzzle : deductedPuzzles) {
            String puzzleName = deductedPuzzle.get(0).getPuzzleName();
            for (int i = 0; i < puzzleName.length(); i++) {
                if (puzzleName.charAt(i) == 'X') {
                    xCount++;
                }
            }
        }
    }

    public static String[] readTxtFile(String filePath) {
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

    private List<List<Integer>> genCombinedPermutations(List<List<Integer>> permutations, List<Integer> deductedPuzzleSizes) {
        List<List<Integer>> combinedPermutations = new ArrayList<>();
        for (Integer integer : deductedPuzzleSizes) {
            List<Integer> tempNumbers;
            for (List<Integer> numbers : permutations) {
                tempNumbers = new ArrayList<>(numbers);
                tempNumbers.add(integer);
                combinedPermutations.add(tempNumbers);
            }
        }
        return combinedPermutations;
    }

    private List<List<Integer>> recGetNumbers(List<List<Integer>> list, List<List<Integer>> buckets, Map<Integer, Integer> combinations, int index, int listSize) {
        List<Integer> integers = list.get(index);
        if (index == listSize - 1) {
            // last integers
            Map<Integer, Integer> tempCombinations = new HashMap<>(combinations);
            for (Integer integer : integers) {

                tempCombinations.put(index, integer);
                buckets.add(new ArrayList<>(tempCombinations.values()));

                // set a maximum size
                if (buckets.size() > bucketSize) {
                    filterPossiblePermutations(buckets);
                    buckets = new ArrayList<>();
                }
            }
        } else {
            // 1. take one integer from integers
            for (Integer integer : integers) {
                combinations.put(index, integer);

                // call rec
                buckets = recGetNumbers(list, buckets, combinations, index + 1, listSize);

            }
            combinations.remove(index);
        }
        return buckets;
    }

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

    private boolean findLights(List<List<Integer>> intPermutations) {
        // List<List<Integer>> intPermutations = getIntPermutations(puzzles);

        for (List<Integer> permutation : intPermutations) {
            List<LightDto> lightDtos = new ArrayList<>();
            for (int i = 0; i < permutation.size(); i++) {
                int currentInt = permutation.get(i);
                LightDto lightDto = puzzles.get(i).get(currentInt);
                lightDtos.add(lightDto);
            }
            lightDtos.add(board);
            int[] ints = addAllArrays(lightDtos);

            if (isAllZero(ints, depth)) {
                System.out.println("####################");
                System.out.println("####################");
                System.out.println("####################");
                System.out.println("####################");
                System.out.println("####################");
                System.out.println("#### Found it!! #####");
                // System.out.println(Arrays.toString(ints));
                for (LightDto lightDto : lightDtos) {
                    if (lightDto.getPuzzleName().equals("board")) {
                        continue;
                    }
                    System.out.println(lightDto.getPuzzleName() + ":" + lightDto.getCoordinates());
                }
                endTime = System.currentTimeMillis();
                System.out.println("spend:" + (endTime - startTime) / 1000 + " seconds");
                return true;
            }
        }
        return false;
    }

    private void filterPossiblePermutations(List<List<Integer>> intPermutations) {
        for (List<Integer> permutation : intPermutations) {
            List<LightDto> lightDtos = new ArrayList<>();
            for (int i = 0; i < permutation.size(); i++) {
                int currentInt = permutation.get(i);
                LightDto lightDto = puzzles.get(i).get(currentInt);
                lightDtos.add(lightDto);
            }
            lightDtos.add(board);
            int[] ints = addAllArrays(lightDtos);

            if (isPuzzleMatched(ints, depth)) {
                possiblePermutations.add(permutation);
            }
        }
    }

    // private void recCalculate(List<List<LightDto>> separatedTask){
    //     List<List<List<LightDto>>> separatedTasks2 = separateTasks(separatedTask);
    //     for (List<List<LightDto>> separatedTask2 : separatedTasks2) {
    //         if (findLights(depth, board, separatedTask2)) {
    //             return;
    //         }
    //     }
    // }

    private void calculateSquareLength(String boardStr) {
        // calculateSquareLength
        String[] columns = boardStr.split(",");
        boardRowsLength = columns.length;
        boardColumnsLength = columns[0].length();

        String remove = StringUtils.delete(boardStr, ",");
        arrayLength = remove.length();
    }

    private List<List<List<LightDto>>> separateTasks(List<List<LightDto>> puzzles) {
        int tempSize = 0;
        int index = -1;
        for (int i = 0; i < puzzles.size(); i++) {
            List<LightDto> lightDtos = puzzles.get(i);
            int size = lightDtos.size();
            if (size > tempSize) {
                index = i;
                tempSize = size;
            }
        }
        List<LightDto> targetLightDtos = puzzles.get(index);
        // puzzles.remove(index);
        List<List<LightDto>> listWithoutTargets = new ArrayList<>(puzzles);
        listWithoutTargets.remove(targetLightDtos);

        List<List<List<LightDto>>> separatedTasks = new ArrayList<>();
        for (LightDto lightDto : targetLightDtos) {
            List<List<LightDto>> tempList = new ArrayList<>(listWithoutTargets);
            tempList.add(Collections.singletonList(lightDto));
            separatedTasks.add(tempList);
        }
        return separatedTasks;
    }

    private int[] stringToIntArray(String boardStr) {
        String remove = StringUtils.delete(boardStr, ",");
        //boardStringToIntArray
        int[] result = new int[remove.length()];
        for (int i = 0; i < remove.length(); i++) {
            result[i] = Character.getNumericValue(remove.charAt(i));
        }
        return result;
    }

    private List<List<LightDto>> getPermutations(LightDto board, Map<String, List<LightDto>> puzzleMap) {
        List<List<LightDto>> publicList = new ArrayList<>();
        // add board
        publicList.add(Collections.singletonList(board));

        List<String> permutations = getPermutations(new ArrayList<>(puzzleMap.values()));
        List<int[]> intPermutations = permutations.stream()
                .map(this::stringToIntArray)
                .collect(Collectors.toList());

        // for (Map.Entry<String, List<LightDto>> entry : puzzleMap.entrySet()) {
        //     List<LightDto> lightDtos = entry.getValue();
        //     publicList = passValue(publicList, lightDtos);
        // }
        return publicList;
    }

    private List<LightDto> interpretOnePuzzle(String rowString, int columnLength, int rowLength) {
        // input = XX
        List<LightDto> lightDtos = new ArrayList<>();
        // List<int[]> arrayList = new ArrayList<>();
        // 1. addRight
        int rightAddTimes = boardColumnsLength - columnLength;
        int[] rowFirstArray = addRight(rowString, rightAddTimes);
        LightDto lightDto = new LightDto(rowString, "00", rowFirstArray);
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
        List<LightDto> resultList = addBottom(lightDtos, boardRowsLength - rowLength, boardColumnsLength);
        return resultList;
    }

    private List<List<LightDto>> interpretPuzzles(String input) {
        String[] rows = input.split(" ");
        List<List<LightDto>> puzzles = new ArrayList<>();
        for (String rowString : rows) {
            // XX,XX,XX
            String[] columns = rowString.split(",");
            int columnsLength = columns[0].length();
            int rowsLength = columns.length;
            List<LightDto> onePuzzleLightDtos = interpretOnePuzzle(rowString, columnsLength, rowsLength);
            puzzles.add(onePuzzleLightDtos);
        }

        // todo: remove this print
        puzzles.forEach(puzzle -> {
            for (LightDto lightDto : puzzle) {
                System.out.print(lightDto.getPuzzleName() + "-" + lightDto.getCoordinates() + ":");
                System.out.println(Arrays.toString(lightDto.getIntArray()));
            }
            System.out.println("==============");
        });

        return puzzles;
    }

    private List<LightDto> addBottom(List<LightDto> lightDtos, int addTimes, int partitionNumber) {
        List<LightDto> newList = new ArrayList<>(lightDtos);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addTimes; i++) {
            for (int j = 0; j < partitionNumber; j++) {
                sb.append("0");
            }
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

    private int[] addRight(String input, int addNumber) {
        //    XX,XX,XX --> XX0,XX0,XX0 --> 110,110,110
        String[] rows = input.split(",");
        int[] array = new int[arrayLength];
        // Arrays.fill(array, 0);
        // XX
        // XX
        // XX
        int count = 0;
        for (String row : rows) {
            for (int i = 0; i < row.length() + addNumber; i++) {
                if (i < row.length()) {
                    if (row.charAt(i) == 'X') {
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
     * @param lightDto
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

    /**
     * Giving two lists, combine permutations for these two lists
     *
     * @param aList
     * @param bList
     * @return
     */
    private List<List<LightDto>> passValue(List<List<LightDto>> aList, List<LightDto> bList) {
        List<List<LightDto>> resultList = new ArrayList<>();
        for (List<LightDto> aLightDtos : aList) {
            for (LightDto bLightDto : bList) {
                List<LightDto> tempLightDtos = new ArrayList<>(aLightDtos);
                tempLightDtos.add(bLightDto);
                resultList.add(tempLightDtos);
            }
        }
        return resultList;
    }

    private int recGetPosition(int index, int[] positionArr, int size) {
        if (index == 0) {
        } else if (index == size - 1) {

        } else {

        }
        return -1;
    }

    // private int getPermutations(List<List<LightDto>> puzzles, int i) {
    //     List<int[]> results = new ArrayList<>();
    //     int size = puzzles.size();
    //     int[] sizeArr = new int[size];
    //     int count = 0;
    //     for (List<LightDto> lightDtos : puzzles) {
    //         sizeArr[count++] = lightDtos.size();
    //     }
    //     count = 0;
    //     for (int i1 : sizeArr) {
    //
    //     }
    //     for (int i = 0; i < 1; i++) {
    //         for (int j = 0; j < 2; j++) {
    //             for (int k = 0; k < 3; k++) {
    //                 int[] ints = new int[3];
    //                 ints[count++] = i;
    //                 ints[count++] = j;
    //                 ints[count] = k;
    //                 count = 0;
    //                 results.add(ints);
    //             }
    //         }
    //     }
    //     for (int[] ints : results) {
    //         System.out.println(Arrays.toString(ints));
    //     }
    //     return -1;
    // }
    private void getPermutations() {
        List<int[]> results = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    int[] ints = new int[3];
                    ints[count++] = i;
                    ints[count++] = j;
                    ints[count] = k;
                    count = 0;
                    results.add(ints);
                }
            }
        }
        for (int[] ints : results) {
            System.out.println(Arrays.toString(ints));
        }
    }

    private List<String> getPermutations(List<List<LightDto>> puzzles) {
        // 7, 4, 2
        List<List<String>> puzzleSizes = new ArrayList<>();
        for (List<LightDto> puzzle : puzzles) {
            int size = puzzle.size();
            List<String> sizes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                sizes.add(String.valueOf(i));
            }
            puzzleSizes.add(sizes);
        }

        List<String> permutations = new ArrayList<>();
        for (int i = 0; i < puzzleSizes.size(); i++) {
            if (i == 0) {
                permutations = puzzleSizes.get(i);
            } else {
                permutations = passStringValue(permutations, puzzleSizes.get(i));
            }
        }
        return permutations;
    }

    private List<List<Integer>> getIntPermutations(List<List<LightDto>> puzzles) {
        // 7, 4, 2
        List<List<Integer>> puzzleSizes = getPuzzleSizes(puzzles);

        List<List<Integer>> permutations = new ArrayList<>();
        for (int i = 0; i < puzzleSizes.size(); i++) {
            if (i == 0) {
                for (Integer integer : puzzleSizes.get(0)) {
                    List<Integer> list = new ArrayList<>();
                    list.add(integer);
                    permutations.add(list);
                }
            } else {
                permutations = passIntValue(permutations, puzzleSizes.get(i));
            }
        }
        return permutations;
    }

    private List<List<Integer>> passIntValue(List<List<Integer>> publicList, List<Integer> bList) {
        List<List<Integer>> resultList = new ArrayList<>();
        for (List<Integer> aList : publicList) {
            for (Integer bInteger : bList) {
                List<Integer> tempList = new ArrayList<>(aList);
                tempList.add(bInteger);
                resultList.add(tempList);
            }
        }
        return resultList;
    }

    private List<String> passStringValue(List<String> aList, List<String> bList) {
        List<String> resultList = new ArrayList<>();
        for (String aString : aList) {
            for (String bString : bList) {
                resultList.add(aString + "," + bString);
            }
        }
        return resultList;
    }

    private int[][] grid;

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

    private boolean isPuzzleMatched(int[] array, int depth) {
        int count = 0;
        for (int i : array) {
            int mod = i % depth;
            if (mod != 0) {
                count += (depth - mod);
            }
        }
        if (count == xCount) {
            return true;
        }
        return false;
    }

    // todo remove below methods
    private int[][] modifySpot(int x, int y) {
        grid[x][y] += 1;
        return grid;
    }

    public void lightsOutGrid(int size) {
        grid = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = 0;
            }
        }
    }

    public String printGrid(int[][] grid) {
        String s = "";
        for (int[] ints : grid) {
            for (int anInt : ints) {
                s += anInt;
            }
            s += "\n";
        }
        return s;
    }

    @Override
    public String toString() {
        String s = "";
        for (int[] ints : grid) {
            for (int anInt : ints) {
                s += anInt;
            }
            s += "\n";
        }
        return s;
    }
}
