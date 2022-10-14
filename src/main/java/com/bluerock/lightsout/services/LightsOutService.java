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
                "01" +
                ".txt";
        lightsOutService.doLightsOut(path);
    }

    private int xCount = 0;
    private LightDto board = null;
    private int depth = -1;
    private List<List<LightDto>> puzzles = new ArrayList<>();
    private List<List<LightDto>> deductedPuzzles = new ArrayList<>();
    private List<List<Integer>> possiblePermutations = new ArrayList<>();
    private final int bucketSize = 10 * 1000;

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
        String board = "board";

        depth = Integer.parseInt(depthStr);
        calculateSquareLength(boardStr);
        int[] result = transStringToIntArray(boardStr);
        this.board = new LightDto(board, board, result);
        String[] inputRows = input.split(" ");

        puzzles = interpretPuzzles(inputRows);
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
        List<LightDto> resultLightDtos = findLights(possiblePermutations);
        printResult(resultLightDtos, inputRows);

        endTime = System.currentTimeMillis();
        System.out.println("spend:" + (endTime - startTime) / 1000 + " seconds");
    }

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
        String[] columns = boardStr.split(",");
        boardRowsLength = columns.length;
        boardColumnsLength = columns[0].length();

        String remove = StringUtils.delete(boardStr, ",");
        arrayLength = remove.length();
    }

    private int[] transStringToIntArray(String boardStr) {
        String remove = StringUtils.delete(boardStr, ",");
        //boardStringToIntArray
        int[] result = new int[remove.length()];
        for (int i = 0; i < remove.length(); i++) {
            result[i] = Character.getNumericValue(remove.charAt(i));
        }
        return result;
    }

    private List<List<LightDto>> interpretPuzzles(String[] rows) {
        List<List<LightDto>> puzzles = new ArrayList<>();
        for (String rowString : rows) {
            // XX,XX,XX
            String[] columns = rowString.split(",");
            int columnsLength = columns[0].length();
            int rowsLength = columns.length;
            List<LightDto> onePuzzleLightDtos = interpretOnePuzzle(rowString, columnsLength, rowsLength);
            puzzles.add(onePuzzleLightDtos);
        }

        // todo: remove this print for debugging
        puzzles.forEach(puzzle -> {
            for (LightDto lightDto : puzzle) {
                System.out.print(lightDto.getPuzzleName() + "-" + lightDto.getCoordinates() + ":");
                System.out.println(Arrays.toString(lightDto.getIntArray()));
            }
            System.out.println("==============");
        });

        return puzzles;
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
        return addBottom(lightDtos, boardRowsLength - rowLength, boardColumnsLength);
    }

    private int[] addRight(String input, int addNumber) {
        //    XX,XX,XX --> XX0,XX0,XX0 --> 110,110,110
        String[] rows = input.split(",");
        int[] array = new int[arrayLength];
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
        sb.insert(1, ",");
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

    private void deductPuzzles() {
        // 1. find the puzzle with most permutations
        // puzzleQueue: [index, List<LightDto> ]
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

    private void filterPossiblePermutations(List<List<Integer>> intPermutations) {
        for (List<Integer> permutation : intPermutations) {
            int[] ints = getLightDtoInts(permutation);

            if (isPuzzleMatched(ints, depth)) {
                possiblePermutations.add(permutation);
            }
        }
    }

    private int[] getLightDtoInts(List<Integer> permutation) {
        List<LightDto> lightDtos = new ArrayList<>();
        for (int i = 0; i < permutation.size(); i++) {
            int currentInt = permutation.get(i);
            LightDto lightDto = puzzles.get(i).get(currentInt);
            lightDtos.add(lightDto);
        }
        lightDtos.add(board);
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
            possiblePermutations = genCombinedPermutations(possiblePermutations, deductedPuzzleSizes);
            puzzles.add(deductedPuzzle);
        }
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

    private List<LightDto> findLights(List<List<Integer>> intPermutations) {
        for (List<Integer> permutation : intPermutations) {
            List<LightDto> lightDtos = new ArrayList<>();
            for (int i = 0; i < permutation.size(); i++) {
                int currentInt = permutation.get(i);
                LightDto lightDto = puzzles.get(i).get(currentInt);
                lightDtos.add(lightDto);
            }
            lightDtos.add(board);
            int[] intArr = addAllArrays(lightDtos);

            if (isAllZero(intArr, depth)) {
                return lightDtos;
            }
        }
        return null;
    }

    private void printResult(List<LightDto> lightDtos, String[] inputRows ) {
        Map<String, LightDto> lightDtoMap = lightDtos.stream()
                .collect(HashMap::new, (map, lightDto) -> map.put(
                        lightDto.getPuzzleName(),
                        lightDto
                ), HashMap::putAll);
        StringBuilder sb = new StringBuilder();
        for (String inputRow : inputRows) {
            String coordinates = lightDtoMap.get(inputRow).getCoordinates();
            sb.append(coordinates);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.lastIndexOf(" "));
        String resultString = sb.toString();
        resultString = resultString.replace("00", "0,0");
        System.out.println(resultString);

        // For debugging
        System.out.println("#### Found it!! #####");
        for (LightDto lightDto : lightDtos) {
            if (board.equals(lightDto.getPuzzleName())) {
                continue;
            }
            System.out.println(lightDto.getPuzzleName() + ":" + lightDto.getCoordinates());
        }
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
