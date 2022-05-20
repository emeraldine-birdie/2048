/*
Класс Model будет содержать игровую логику и хранить игровое поле.
*/

import java.util.*;

public class Model {
    private static final int FIELD_WIDTH = 4;
    private Tile[][] gameTiles;
    int score = 0;
    int maxTile = 0;

    private Stack<Tile[][]> previousStates = new Stack<>();
    private Stack<Integer> previousScores = new Stack<>();

    private boolean isSaveNeeded = true;

    public Model(){
        resetGameTiles();
    }

    public Tile[][] getGameTiles(){
        return gameTiles;
    }

    void resetGameTiles(){
        gameTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < gameTiles.length; i++){
            for (int j = 0; j < gameTiles[0].length; j++){
                gameTiles[i][j] = new Tile();
            }
        }
        addTile();
        addTile();
    }

    private void addTile(){
        List<Tile> emptyTiles = getEmptyTiles();
        if (!emptyTiles.isEmpty()){
            Tile tile = emptyTiles.get((int) (emptyTiles.size() * Math.random()));
            tile.value = Math.random() < 0.9 ? 2 : 4;
        }
    }

    private List<Tile> getEmptyTiles(){
        final List<Tile> list = new ArrayList<Tile>();
        for (Tile[] tileArray : gameTiles) {
            for (Tile t : tileArray)
                if (t.isEmpty()) {
                    list.add(t);
                }
        }
        return list;
    }

    private boolean compressTiles(Tile[] tiles){
        boolean compressed = false;
        int insertPosition = 0;
        for (int i = 0; i < FIELD_WIDTH; i++) {
            if (!tiles[i].isEmpty()) {
                if (i != insertPosition) {
                    tiles[insertPosition] = tiles[i];
                    tiles[i] = new Tile();
                    compressed = true;
                }
                insertPosition++;
            }
        }
        return compressed;
    }

    private boolean mergeTiles(Tile[] tiles){
        boolean merged = false;
        LinkedList<Tile> tilesList = new LinkedList<>();
        for (int i = 0; i < FIELD_WIDTH; i++) {
            if (tiles[i].isEmpty()) {
                continue;
            }

            if (i < FIELD_WIDTH - 1 && tiles[i].value == tiles[i + 1].value) {
                int updatedValue = tiles[i].value * 2;
                if (updatedValue > maxTile) {
                    maxTile = updatedValue;
                }
                score += updatedValue;
                tilesList.addLast(new Tile(updatedValue));
                tiles[i + 1].value = 0;
                merged = true;
            } else {
                tilesList.addLast(new Tile(tiles[i].value));
            }
            tiles[i].value = 0;
        }

        for (int i = 0; i < tilesList.size(); i++) {
            tiles[i] = tilesList.get(i);
        }
        return merged;
    }

    public boolean canMove(){
        boolean canMove;
        List<Tile> empty = getEmptyTiles();
        if (!empty.isEmpty()){
            canMove = true;
        }
        else {
            canMove = false;
        }

        for (int i = 0; i < gameTiles.length-1; i++){
            for (int j = 0; j < gameTiles[0].length-1; j++){
                if (gameTiles[i][j].value == gameTiles[i][j+1].value |
                        gameTiles[i][j].value == gameTiles[i+1][j].value){
                    canMove = true;
                }
            }
        }

        return canMove;
    }

    private Tile[][] turnRight(Tile[][] tiles){
        Tile[][] result = new Tile[tiles.length][tiles[0].length];
        for (int i = 0; i < tiles.length; i++){
            for (int j = 0; j < tiles[0].length; j++){
                result[i][j] = tiles[tiles.length-1-j][i];
            }
        }
        return result;
    }

    public void left(){
        if (isSaveNeeded){
            saveState(gameTiles);
        }
        boolean moveFlag = false;
        for (int i = 0; i < FIELD_WIDTH; i++) {
            if (compressTiles(gameTiles[i]) | mergeTiles(gameTiles[i])) {
                moveFlag = true;
            }
        }
        if (moveFlag) {
            addTile();
            isSaveNeeded = true;
        }
    }

    public void right(){
        saveState(gameTiles);
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
        left();
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
    }

    public void up(){
        saveState(gameTiles);
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
        left();
        gameTiles = turnRight(gameTiles);
    }

    public void down(){
        saveState(gameTiles);
        gameTiles = turnRight(gameTiles);
        left();
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
        gameTiles = turnRight(gameTiles);
    }

    public void randomMove(){
        int n = ((int)(Math.random()*100)) % 4;
        switch (n) {
            case 0:
                up();
                break;
            case 1:
                down();
                break;
            case 2:
                right();
                break;
            case 3:
                left();
                break;
        }
    }

    boolean hasBoardChanged(){
        boolean result = false;

        int weightNow = 0;
        int weightPrev = 0;

        for (int i = 0; i < FIELD_WIDTH; i++){
            for (int j = 0; j < FIELD_WIDTH; j++){
                weightNow += gameTiles[i][j].value;
                weightPrev+= previousStates.peek()[i][j].value;
            }
        }
        if (weightPrev!=weightNow){
            result = true;
        }
        return result;
    }

    private MoveEfficiency getMoveEfficiency(Move move){
        move.move();
        MoveEfficiency moveEfficiency = new MoveEfficiency(getEmptyTiles().size(), score, move);
        if (!hasBoardChanged()){
            moveEfficiency = new MoveEfficiency(-1, 0, move);
        }
        rollback();
        return moveEfficiency;
    }

    public void autoMove(){
        PriorityQueue<MoveEfficiency> queue = new PriorityQueue<>(4, Collections.reverseOrder());
        queue.offer(getMoveEfficiency(this::left));
        queue.offer(getMoveEfficiency(this::right));
        queue.offer(getMoveEfficiency(this::up));
        queue.offer(getMoveEfficiency(this::down));
        queue.peek().getMove().move();
    }

    private void saveState(Tile[][] tiles){
        Tile[][] tempTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < FIELD_WIDTH; i++){
            for (int j = 0; j < FIELD_WIDTH; j++){
                tempTiles[i][j] = new Tile(tiles[i][j].value);
            }
        }
        previousStates.push(tempTiles);
        previousScores.push(score);
        isSaveNeeded = false;
    }

    public void rollback(){
        if (!previousStates.isEmpty() && !previousScores.isEmpty()){
            gameTiles = previousStates.pop();
            score = previousScores.pop();
        }
    }
}
