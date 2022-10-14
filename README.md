
# BlueRock LightsOut Application

 A java application which allows users to find out answers of lights out games.

## Descriptions

- Simply assign the path in the main method of LightsOutService, and you can search out the answer of question assigned.
- Generally speaking, I translate the pieces into int array, find out the combinations possible, then match them with the board.
- To advanced to level 6, I have to cut off some pieces with most combinations so that I can deduct the calculating numbers. And in the end I add those deductedPieces back.
- By deduting pieces, I advanced to level 6 eventually.
- The idea of deducting pieces is: 
1. Deduct the pieces with most combinations so numbers for calculating could be reduced drastically.
2. Calculate the "X" numbers in deducted pieces, and filter out those combinations calculated by remained pieces and have different "X" numbers.
3. For instance, if the "X" numbers of deducted pieces are 5, we could deduct all combinations that have "X" numbers other than 5.
4. The number of deducted pieces is related to the depth.

### Application Structure
- It is a spring boot structure, and there are two main classes: LightsOutService and LightDto
- Sample questions files are put in the resoureces/samples path.

## Tech Stack

- Java 14
- Maven
- Spring Boot
- Lombok


## Installation

### Requirements
- Java 14
- Maven 3.X up

### Setup
- Simply download it from Github and run the main method of LightsOutService.
- You could assign different sample questions by modifying the different path.
## Objective

The output should be printed to the standard out. Only one solution is required even though multiple might exist. The solution should be printed using the coordinate of each piece separated by a space. Each coordinate is formatted with “x,y” where the top-left corner of the board would be “0,0”. The coordinate of the piece is always the top-left corner of the piece on the board, even if the top-left corner of the piece is empty. The order of the coordinates should be the same as the order of the pieces in the input file.
One possible solution for the above input is:

0,1 0,2 1,0
## Appendix

Here are the results of first 6 questions. I also print all the pieces and its coordinates so it's easier to read and debug.

### Q1
0,0 1,0 1,0 0,0 1,0 1,0 0,1

.XX,XX.:01

XX,X.:10

XX.,.X.,.XX:00

.X,XX:10

X,X,X:10

..X,XXX,X..:00

XX:10

spent:16 milliseconds

### Q2
1,0 0,1 0,1 2,2 1,0 1,0 0,2

..XX,XXX.:02

XX,XX,.X,.X:10

XX:10

XXX:01

X...,XXXX:01

X.,XX,XX:10

X,X:22

spent:43 milliseconds

### Q3
0,0 1,3 0,2 2,0 1,0 0,4 1,2 0,0 0,0 0,2 1,0

.X.,.XX,.X.,.X.,XXX:10

X,X,X,X:02

.X..,.XX.,X.X.,XXXX:00

..XX,..XX,XXX.:00

.X.,.X.,XX.,XXX:12

XX..,.XXX:04

XX.,.XX,.X.,.X.:10

XXX,X.X:13

X...,X.X.,XXXX,XX..:00

XX,X.:02

XX,.X,.X:20

spent:1 seconds

### Q4
1,0 3,2 0,0 2,0 0,0 0,0 0,0 1,2 0,0 2,1 3,0

.X.,XXX,.XX,.X.:21

X.,X.,XX,XX:00

X.X.,XXXX,X...:12

.XX.,.XX.,XXX.,..XX:00

XX.,.XX,.XX,.X.,XX.:00

.X...,XXXXX,...X.,...X.,...X.:00

X..,XX.,XXX,XXX,X..:20

..X,.XX,XX.,X..,X..:00

.X..,.XX.,XXX.,..X.,..XX:10

.X,XX,XX:30

..X,XXX,..X:32

spent:1 seconds

### Q5
2,0 3,1 0,0 4,0 2,3 1,0 0,1 3,2 1,0 2,1 0,0 1,0

..X..,XXXXX:10

XX,X.,XX:00

X..,XXX,..X:21

.X.,XX.,.XX,XX.:10

XXX,X..:01

.X,.X,XX:10

XX,.X,.X,.X:40

XXXXX,X...X:00

..X,XXX,..X:31

..X,..X,..X,XXX:20

XXX:23

XX.,.XX:32

spent:122 seconds

### Q6
1,0 0,2 0,3 0,3 0,0 0,2 2,3 2,3 0,1 1,2 0,0 0,1

.X..,XX..,.XXX:01

.X.,.XX,.XX,XX.,.X.:00

.XX,.X.,XXX,XXX,..X:12

.X..,XX..,XXXX,.X..:01

.XX.,.XXX,XXX.,.X..:00

...XX,...XX,...X.,XXXX.:03

XX.XX,XXXX.,..XX.:03

X.XX.,XX.XX,XXXX.:02

XXX.,.X..,.XXX,..X.,..X.:10

.X,XX,X.:23

.X,XX,X.:23

XXXX,...X:02

spent:18 seconds


