import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 
import java.util.Arrays; 
import java.util.List; 
import java.util.Random; 
import java.util.Scanner; 
import java.awt.Point; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class BattleCity_Tank extends PApplet {








/**
 @author Raafat
 
 Description: This game means a lot to me as it was my favorite childhood game: 
 Tank 1990 or BattleCity Tank
 
 Known Bugs and Issues:
 - Shovel Bonus is not implemented.
 */

/*
 imageMode(CORNER) for Tiles and Bonuses but imageMode(CENTER) for Tanks and Shells
 rectMode(CORNER) for Tiles and Bonuses but rectMode(CENTER) for Tanks and Shells
 int[][] matrix = new int[columns][rows]; // because mother of everything!
 // and matrix.length := rows, matrix[0].length := columns
 */

Player player1Tank, player2Tank;
List<Enemy> enemyTanks;
List<Shell> shells;
List<Bonus> bonuses;
TileType[][] tiles;
boolean[][] obstacleTiles;
boolean keys[]; // shoot and move simultaneously!

int stage = 1; // level number
Base base;
Level level;
int levelIndex = stage - 1;
GameState gameState;
boolean gameOver = false;
final static int MAX_LEVEL = 10;
PImage baseImg;
PImage basicTankImg, fastTankImg, powerfulTankImg, armoredTankImg;
PImage shellExplosionImg, tankExplosionImg, baseExplosionImg;
PImage brickTileImg, steelTileImg, grassTileImg, waterTileImg, snowTileImg;
PImage arrowImg;
PImage tankCursor;
final int CURSOR_X = 130;
final int CURSOR_Y = 228;
int cursorY = CURSOR_Y;

final static int TILE_SIZE = 16;
final static int OBJECT_SIZE = TILE_SIZE*2;
final static int DEFAULT_HI_SCORE = 20000;

final static int WIDTH = 480;
final static int HEIGHT = 416;
final static int SIDE_PANEL_SIZE = 64;
final static int ACTUAL_WIDTH = WIDTH - SIDE_PANEL_SIZE;
final static int MARGIN = 1;
final int DEFAULT_FONT_SIZE = 14;
final int SMALL_FONT_SIZE = 10;
final int FPS = 50;

int scoreWaitTimer;
final int SCORE_WAIT_TIME = 8000; // show score screen for a while
int gameOverWaitTimer;
final int GAME_OVER_WAIT_TIME = 2500; // wait a little before announcing game over
int bonusGenerationTimer;
final int BONUS_GENERATION_TIME = 10000; // generate bonus every while
int enemyGenerationTimer;
final int ENEMY_GENERATION_TIME = 2500;

int baseProtectionTimer;
final int BASE_PROTECTION_TIME = 1500; // TODO

int[][] enemyTanksInfo; // number of tanks in each level and of each type
int[] remainingEnemies; // in each level
int[] killedEnemies; // in each level

PImage allObjectsImage;
PImage introImg, gameOverImg;
PFont defaultFont;
int animationIdx;
final int UPDATE_ANIMATION_TIME = 100;

boolean playSounds = true;
Minim minim;
AudioPlayer[] sounds; // TODO

public void settings() {
  size(WIDTH, HEIGHT);
}

public void setup() {
  // GENERAL IMAGES //
  allObjectsImage = loadImage("all_objects.gif");
  allObjectsImage.resize(allObjectsImage.width*2, allObjectsImage.height*2);
  introImg = loadImage("intro.png");
  tankCursor = loadImage("tank_cursor.png");
  gameOverImg = loadImage("game_over.png");
  arrowImg = allObjectsImage.get(5*OBJECT_SIZE, 3*OBJECT_SIZE, OBJECT_SIZE, TILE_SIZE);
  if (!hasScreenSize(introImg)) {
    introImg.resize(width, height);
  }
  if (!hasScreenSize(gameOverImg)) {
    gameOverImg.resize(width, height);
  }
  loadImages();
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // SOUND THINGS // 
  minim = new Minim(this);
  loadSounds();
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // {BASIC, FAST, POWERFUL, ARMORED}
  enemyTanksInfo = new int[][]{{18, 2, 0, 0}, {14, 4, 0, 2}, {14, 4, 0, 2}, {2, 5, 10, 3}, {8, 5, 5, 2}, {9, 2, 7, 2}, {7, 4, 6, 3}, {7, 4, 7, 2}, {6, 4, 7, 3}, {12, 2, 4, 2}};
  keys = new boolean[5]; // 0-space, 1-up, 2-right, 3-down, 4-left

  level = new Level();
  setGameState(GameState.INTRO);

  // FONT INFO // 
  defaultFont = loadFont("fonts/PressStart.vlw");
  textFont(defaultFont);
  textSize(DEFAULT_FONT_SIZE);
  textAlign(CENTER, CENTER);
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  
  frameRate(FPS);
}

public void draw() {
  if (gameState == GameState.INTRO) { // just animate main page
    animateIntro();
  } else if (gameState == GameState.OFF) { // stop animation..
    // TODO: Fix!
    background(introImg);
    image(tankCursor, CURSOR_X, cursorY);
  } else if (gameState == GameState.LOADING) {
    background(127);
    fill(0);
    text("STAGE " +stage, WIDTH/2, HEIGHT/2);
  } else if (gameState == GameState.PAUSED) {
    // not looping
  } else if (gameState == GameState.RUNNING) {
    background(0);
    level.displaySideInfo(); // and checks game status
    base.display();
    player1Tank.display();
    player1Tank.control();

    if (gameOver) { // game lost. wait a little..
      playSound(sounds[1], false); // don't rewind because it will restart!
    }

    checkEnemyTanks();

    level.display(); // TODO: fix SNOW

    checkFiredShells();

    if (level.activeEnemies < Level.MAX_ACTIVE_ENEMIES) {
      spawnNewEnemy();
    }

    checkActivateBonus();
    checkBonuses();

    if (gameOver) { // wait and show game over page
      checkGameOver();
    }
  } else if (gameState == GameState.SCORE) {
    checkScore();
  } else if (gameState == GameState.OVER) {
    background(gameOverImg);
  } else if (gameState == GameState.CONSTRUCTION) {
    constructingNewLevel();
  }
}

public void init() {
  level = new Level(stage);
  level.loadLevel();

  loadGame();
}

public void loadGame() {
  player1Tank = new Player();
  player2Tank = null; // TODO
  enemyTanks = new ArrayList();
  shells = new ArrayList();
  bonuses = new ArrayList();
  base = new Base();
  gameOver = false;

  int l = levelIndex;
  if (l >= enemyTanksInfo.length || l < 0) { // no info about enemy tanks number in this level
    l = 0;
  }
  remainingEnemies = new int[enemyTanksInfo[0].length]; // or 4
  for (int i = 0; i < remainingEnemies.length; i++) {
    remainingEnemies[i] = enemyTanksInfo[l][i];
  }
  killedEnemies = new int[remainingEnemies.length]; // initialized to 0 by default

  rewindSounds(); // such as game_over
}

public void initEmptyLevelTiles() {
  // matrix = [columns][rows]
  tiles = new TileType[HEIGHT/TILE_SIZE][ACTUAL_WIDTH/TILE_SIZE];
  obstacleTiles = new boolean[HEIGHT/TILE_SIZE][ACTUAL_WIDTH/TILE_SIZE];
}

public void animateIntro() {
  if (animationIdx > 0) {
    background(0);
    image(introImg, 0, animationIdx-=3);
  } else {
    gameState = GameState.OFF; // stop animation
  }
}

public void checkEnemyTanks() {
  for (int i = 0; i < enemyTanks.size(); i++) {
    Enemy enemy = enemyTanks.get(i); 
    if (enemy.state == TankState.DEAD) {
      enemyTanks.remove(i);
      level.activeEnemies--;
    } else {
      enemy.display();
      enemy.playRandomly();
    }
  }
}

public void checkFiredShells() {
  for (int i = 0; i < shells.size(); i++) {
    Shell shell = shells.get(i);
    if (shell.state == ShellState.REMOVED) {
      shells.remove(i);
    } else {
      shell.display();
      shell.move();
    }
  }
}

public void checkBonuses() {
  if (millis() - bonusGenerationTimer > BONUS_GENERATION_TIME) {
    bonuses.add(new Bonus());
    bonusGenerationTimer = millis();
  }
  for (int i = 0; i < bonuses.size(); i++) {
    Bonus bonus = bonuses.get(i);
    if (!bonus.active) {
      bonuses.remove(i);
    } else {
      bonus.toggleVisibility();
      bonus.display();
    }
  }
}

public void checkGameOver() {
  if (millis() - gameOverWaitTimer > GAME_OVER_WAIT_TIME) {
    setGameState(GameState.SCORE);
  } else {
    fill(127);
    text("GAME\nOVER", ACTUAL_WIDTH/2, animationIdx-=3);
    if (animationIdx < HEIGHT/2) {
      animationIdx = HEIGHT/2;
    }
  }
}

public void checkScore() {
  if (millis() - scoreWaitTimer > SCORE_WAIT_TIME) {
    if (gameOver) {
      gameState = GameState.OVER;
    } else {
      wonLevel();
    }
  } else {
    showScore();
  }
}

public void keyPressed() {
  if (gameState == GameState.INTRO) {
    if (keyCode == ENTER) { // skip animation
      gameState = GameState.OFF;
    }
  } else if (gameState == GameState.OFF) {
    showStartMenu();
  } else if (gameState == GameState.LOADING) {
    if (keyCode == ENTER) {
      setGameState(GameState.RUNNING);
    }
  } else if (gameState == GameState.RUNNING) {
    if (key == 'p' || key == 'P') {
      gameState = GameState.PAUSED;
      noLoop();
    }
    if (key == ' ') {
      keys[0] = true;
    } 
    if (keyCode == UP) {
      keys[1] = true;
    } 
    if (keyCode == RIGHT) {
      keys[2] = true;
    } 
    if (keyCode == DOWN) {
      keys[3] = true;
    } 
    if (keyCode == LEFT) {
      keys[4] = true;
    }
  } else if (gameState == GameState.PAUSED) {
    if (key == 'p' || key == 'P') {
      gameState = GameState.RUNNING;
      loop();
    }
  } else if (gameState == GameState.OVER) {
    if (key == ' ' || keyCode == ENTER) {
      setGameState(GameState.INTRO);
    }
  } else if (gameState == GameState.CONSTRUCTION) {
    if (key == 's' || key == 'S') {
      saveConstructedLevel();
    } else if (keyCode == ENTER) {
      loadConstructedLevel();
      loadGame();
      setGameState(GameState.LOADING);
    }
  }
}

public void keyReleased() {
  if (key == ' ') {
    keys[0] = false;
  } 
  if (keyCode == UP) {
    keys[1] = false;
  } 
  if (keyCode == RIGHT) {
    keys[2] = false;
  } 
  if (keyCode == DOWN) {
    keys[3] = false;
  }  
  if (keyCode == LEFT) {
    keys[4] = false;
  }
}

public int getEnemyTankValue(Enemy tank) {
  if (tank.type == EnemyTankType.BASIC) {
    return 100;
  }
  if (tank.type == EnemyTankType.FAST) {
    return 200;
  }
  if (tank.type == EnemyTankType.POWERFUL) {
    return 300;
  }
  if (tank.type == EnemyTankType.ARMORED) {
    return 400;
  }
  return 0;
}

// x, y are center coordinates
public void displayRotatedImg(PImage image, int x, int y, Direction direction) {
  pushMatrix();
  imageMode(CENTER);
  translate(x, y);
  if (direction == Direction.EAST) {
    rotate(PI/2.0f);
  } else if (direction == Direction.SOUTH) {
    rotate(PI);
  } else if (direction == Direction.WEST) {
    rotate(3.0f * PI/2.0f);
  } else if (direction == Direction.NORTH) {
  }
  image(image, 0, 0);
  imageMode(CORNER);
  popMatrix();
  /*
  rectMode(CENTER);
   fill(255, 127);
   rect(x, y, image.width, image.height);
   rectMode(CORNER);
   */
}

/*
  TODO: Check Player2
 */
public void checkActivateBonus() {
  for (Bonus bonus : bonuses) {
    if (squaresOverlap(player1Tank.x - Player.SIZE/2, player1Tank.y - Player.SIZE/2, Player.SIZE, bonus.x, bonus.y, OBJECT_SIZE)) {
      activateBonus(player1Tank, bonus);
      break;
    }
  }
}

public void activateBonus(Player player, Bonus bonus) {
  bonus.deactivate(); // used, then removed
  playSound(sounds[5], true);
  if (bonus.type == BonusType.GRENADE) { // destroys all tanks
    for (Enemy enemy : enemyTanks) {
      enemy.explode(true);
      player.updateScore(enemy);
    }
  } else if (bonus.type == BonusType.HELMET) { // gives temporary shield
    player.getShield();
  } else if (bonus.type == BonusType.SHOVEL) { // TODO: Not implemented
    baseProtectionTimer = millis();
  } else if (bonus.type == BonusType.STAR) { // gives power-up upgrade
    player.upgradeShellPowers();
  } else if (bonus.type == BonusType.TANK) { // adds life!
    player.lives++;
  } else if (bonus.type == BonusType.TIMER) { // freezes enemies temporarily
    for (Enemy enemy : enemyTanks) {
      enemy.freeze();
    }
  }
}

/**
 Returns true whether two squares overlap and false otherwise; x, y are corner position
 */
public boolean squaresOverlap(int x0, int y0, int size0, int x1, int y1, int size1) {
  if (x0 < x1 + size1 && x0 + size0 > x1 && y0 < y1 + size1 && y0 + size0 > y1) {
    return true;
  }
  return false;
}

/*
pos0 and pos1 are center positions
 */
public boolean segmentsOverlay(int pos0, int size0, int pos1, int size1) {
  return pos0 - size0/2 - MARGIN <= pos1 + size1/2 && pos0 + size0/2 + MARGIN >= pos1 - size1/2;
}

public void loadConstructedLevel() {
  stage = 0; // one constructed level only. ZERO  
  levelIndex = 0;
  cursor(ARROW);
}

public void wonLevel() {
  nextLevel();
  int lives = player1Tank.lives;
  int score = player1Tank.score;
  int shellExtraPowers = player1Tank.extraPowers;
  init();
  // copy player info
  player1Tank.lives = lives;
  player1Tank.score = score;
  player1Tank.extraPowers = shellExtraPowers;
  gameState = GameState.LOADING;
}

public void showScore() {
  background(0);
  fill(127);
  textSize(DEFAULT_FONT_SIZE);
  text("HI-SCORE", 8*TILE_SIZE, 2*TILE_SIZE);
  text(getHiScore(), 21*TILE_SIZE, 2*TILE_SIZE);
  text("STAGE   " + stage, 15*TILE_SIZE, 4*TILE_SIZE);
  text("I-PLAYER", 7*TILE_SIZE, 7*TILE_SIZE);
  text(player1Tank.score, 9*TILE_SIZE, 9*TILE_SIZE);
  fill(255);
  int t1 = killedEnemies[0];
  int t2 = killedEnemies[1];
  int t3 = killedEnemies[2];
  int t4 = killedEnemies[3];
  textAlign(LEFT);
  imageMode(CENTER);
  text(String.format("%5d  PTS", t1*100), .5f*TILE_SIZE, 11*TILE_SIZE);
  text(String.format("%2d", t1), 11*TILE_SIZE, 11*TILE_SIZE);
  image(arrowImg, 13.5f*TILE_SIZE, 10.5f*TILE_SIZE);
  image(basicTankImg, 15.5f*TILE_SIZE, 10.5f*TILE_SIZE);

  text(String.format("%5d  PTS", t2*200), .5f*TILE_SIZE, 14*TILE_SIZE);
  text(String.format("%2d", t2), 11*TILE_SIZE, 14*TILE_SIZE);
  image(arrowImg, 13.5f*TILE_SIZE, 13.5f*TILE_SIZE);
  image(fastTankImg, 15.5f*TILE_SIZE, 13.5f*TILE_SIZE);

  text(String.format("%5d  PTS", t3*300), .5f*TILE_SIZE, 17*TILE_SIZE);
  text(String.format("%2d", t3), 11*TILE_SIZE, 17*TILE_SIZE);
  image(arrowImg, 13.5f*TILE_SIZE, 16.5f*TILE_SIZE);
  image(powerfulTankImg, 15.5f*TILE_SIZE, 16.5f*TILE_SIZE);

  text(String.format("%5d  PTS", t4*400), .5f*TILE_SIZE, 20*TILE_SIZE);
  text(String.format("%2d", t4), 11*TILE_SIZE, 20*TILE_SIZE);
  image(arrowImg, 13.5f*TILE_SIZE, 19.5f*TILE_SIZE);
  image(armoredTankImg, 15.5f*TILE_SIZE, 19.5f*TILE_SIZE);

  fill(255);
  rect(11*TILE_SIZE, 21*TILE_SIZE, 9*TILE_SIZE, TILE_SIZE/2);
  text("TOTAL ", 4*TILE_SIZE, 23*TILE_SIZE);
  text(String.format("%2d", t1 + t2 + t3 + t4), 11*TILE_SIZE, 23*TILE_SIZE);
  imageMode(CORNER);
  textAlign(CENTER);
  textSize(DEFAULT_FONT_SIZE);
  saveHiScore();
}

public void nextLevel() {
  if (stage == MAX_LEVEL) {
    playSound(sounds[10], true);
  } else {
    if (stage == 0 && levelIndex == 0) { // constructed level
      levelIndex = -1;
    }
    stage++;
    levelIndex++;
  }
}

///////////////////////////////////////////////////////////////////////////////////////////////////
public void loadImages() {
  baseImg = allObjectsImage.get(0*TILE_SIZE, 2*TILE_SIZE, 2*TILE_SIZE, 2*TILE_SIZE-MARGIN);

  basicTankImg = allObjectsImage.get(4*TILE_SIZE, 0, Enemy.SIZE, Enemy.SIZE);
  fastTankImg = allObjectsImage.get(6*TILE_SIZE, 0, Enemy.SIZE, Enemy.SIZE);
  powerfulTankImg = allObjectsImage.get(8*TILE_SIZE, 0, Enemy.SIZE, Enemy.SIZE);
  armoredTankImg = allObjectsImage.get(10*TILE_SIZE, 0, Enemy.SIZE, Enemy.SIZE);

  shellExplosionImg = allObjectsImage.get(TILE_SIZE, 5*OBJECT_SIZE + TILE_SIZE, OBJECT_SIZE, OBJECT_SIZE);
  tankExplosionImg = allObjectsImage.get(2*OBJECT_SIZE + TILE_SIZE, 5*OBJECT_SIZE + TILE_SIZE, 2*TILE_SIZE, 2*TILE_SIZE);
  baseExplosionImg = allObjectsImage.get(4*OBJECT_SIZE, 5*OBJECT_SIZE, 3*TILE_SIZE, 3*TILE_SIZE);

  // brickTileImg = allObjectsImage.get(3*OBJECT_SIZE, 4*OBJECT_SIZE, TILE_SIZE, TILE_SIZE);
  // steelTileImg = allObjectsImage.get(3*OBJECT_SIZE, 4*OBJECT_SIZE + TILE_SIZE, TILE_SIZE, TILE_SIZE);
  grassTileImg = allObjectsImage.get(3*OBJECT_SIZE + TILE_SIZE, 4*OBJECT_SIZE + TILE_SIZE, TILE_SIZE, TILE_SIZE);
  // snowTileImg = allObjectsImage.get(4*OBJECT_SIZE, 4*OBJECT_SIZE + TILE_SIZE, TILE_SIZE, TILE_SIZE);

  brickTileImg = loadImage("images/brick.gif");
  steelTileImg = loadImage("images/steel.gif");
  // grassTileImg = loadImage("images/grass.gif");
  waterTileImg = loadImage("images/water.gif");
  snowTileImg = loadImage("images/snow.gif");
}

///////////////////////////////////////////////////////////////////////////////////////////////////
public void loadSounds() {
  sounds = new AudioPlayer[11];
  sounds[0] = minim.loadFile("sounds/game_on.mp3");
  sounds[1] = minim.loadFile("sounds/game_over.mp3");
  sounds[2] = minim.loadFile("sounds/tank_fire.mp3");
  sounds[3] = minim.loadFile("sounds/shell_explosion_brick.mp3");
  sounds[4] = minim.loadFile("sounds/shell_explosion_steel.mp3");
  sounds[5] = minim.loadFile("sounds/player_bonus.mp3");
  sounds[6] = minim.loadFile("sounds/tank_explosion.mp3");
  sounds[10] = minim.loadFile("ending_GoT_music.mp3");
}

public void rewindSounds() {
  for (AudioPlayer sound : sounds) {
    if (sound != null) {
      sound.rewind();
    }
  }
}

public void playSound(AudioPlayer sound, boolean rewind) {
  if (playSounds) {
    sound.play();
    if (rewind) {
      sound.rewind();
    }
  }
}

public void repeatSound(AudioPlayer sound) {
  if (playSounds && !sound.isPlaying()) {
    sound.rewind();
    sound.play();
  }
}

public void stop() {
  // stop AudioPlayers?
  minim.stop();
}
///////////////////////////////////////////////////////////////////////////////////////////////////

/*
// TODO: Make 5 positions
 TODO: Improve algorithm
 */
public Point getSpawningPosition() {
  int r = PApplet.parseInt(random(0, 3)); // left, center, right. 
  int x = TILE_SIZE + MARGIN, y = x;

  if (r == 0) {
    if (noTanksOn(x, y)) {
      return new Point(x, y);
    }
  } else if (r == 1) {
    if (noTanksOn(ACTUAL_WIDTH/2, y)) {
      return new Point(ACTUAL_WIDTH/2, y);
    }
  } else if (r == 2) {
    if (noTanksOn(ACTUAL_WIDTH - x, y)) {
      return new Point(ACTUAL_WIDTH - x, y);
    }
  }
  return null;
}

// TODO: Fix. Not accurate
/*
  Checks whether any tanks are occupying that position
 x, y are center coordinates
 */
public boolean noTanksOn(int x, int y) {
  for (Enemy enemy : enemyTanks) {
    if (squaresOverlap(x - Tank.SIZE/2, y - Tank.SIZE/2, Tank.SIZE, enemy.x - Enemy.SIZE/2, enemy.y - Enemy.SIZE/2, Enemy.SIZE)) {
      return false;
    }
  }
  if (squaresOverlap(x - Tank.SIZE/2, y - Tank.SIZE/2, Tank.SIZE, player1Tank.x - Player.SIZE/2, player1Tank.y - Player.SIZE/2, Player.SIZE)) {
    return false;
  }
  return true;
}

public void setGameState(GameState state) {
  if (state == GameState.INTRO) {
    gameState = GameState.INTRO; // for animation things
    animationIdx = HEIGHT - MARGIN;
  } else if (state == GameState.OFF) {
    // nextLevel(); // for testing
    gameState = GameState.OFF;
  } else if (state == GameState.LOADING) {
    gameState = GameState.LOADING;
  } else if (state == GameState.RUNNING) {
    gameState = GameState.RUNNING;
    bonusGenerationTimer = millis();
    enemyGenerationTimer = millis();
    playSound(sounds[0], true); // game start music!
  } else if (state == GameState.SCORE) {
    gameState = GameState.SCORE;
    scoreWaitTimer = millis();
  } else if (state == GameState.OVER) { // mmm
    gameOver = true;
    gameOverWaitTimer = millis();
    animationIdx = HEIGHT - TILE_SIZE;
    stage = 1;
    levelIndex = 0;
  } else if (state == GameState.CONSTRUCTION) {
    initEmptyLevelTiles();
    gameState = GameState.CONSTRUCTION;
  }
}

/*
Spawns new enemy randomly.
 */
public void spawnNewEnemy() {
  if (millis() - enemyGenerationTimer > ENEMY_GENERATION_TIME) { // time to spawn new enemy tank
    RandomEnum<EnemyTankType> enemyTankEnumRandomizer = new RandomEnum<EnemyTankType>(EnemyTankType.class);
    int t = PApplet.parseInt(random(0, enemyTankEnumRandomizer.getValuesLength())); // int(random(0, 5));
    if (remainingEnemies[t] <= 0) {
      t = 0;
      while (t < remainingEnemies.length && remainingEnemies[t] <= 0) { // should be only equal but for reasons
        t++;
      }
      if (t >= remainingEnemies.length) {
        return;
      }
    }
    Point pos = getSpawningPosition();
    if (pos != null) {
      enemyTanks.add(new Enemy(pos.x, pos.y, enemyTankEnumRandomizer.getConstantAtIndex(t)));
      remainingEnemies[t]--;
      level.activeEnemies++;
    }
    enemyGenerationTimer = millis();
  }
}

public void displayLevelTiles() {
  for (int i=0; i < tiles.length; i++) {
    for (int j=0; j < tiles[i].length; j++) {
      if (tiles[i][j] != null) { // switch crashes on null. lel
        switch(tiles[i][j]) {
        case BRICK:
          image(brickTileImg, i*TILE_SIZE, j*TILE_SIZE);
          break;
        case STEEL:
          image(steelTileImg, i*TILE_SIZE, j*TILE_SIZE);
          break;
        case GRASS:
          image(grassTileImg, i*TILE_SIZE, j*TILE_SIZE);
          break;
        case WATER:
          // animateWaves();
          image(waterTileImg, i*TILE_SIZE, j*TILE_SIZE);
          break;
        case SNOW:
          image(snowTileImg, i*TILE_SIZE, j*TILE_SIZE);
          break;
        default:
        }
      }
    }
  }
}

public void showStartMenu() {
  int diff = 28;
  if (keyCode == DOWN) {
    cursorY += diff;
  }
  if (keyCode == UP) {
    cursorY -= diff;
  }

  cursorY = constrain(cursorY, CURSOR_Y, CURSOR_Y + 2*diff);

  if (keyCode == ENTER) {
    if (cursorY == CURSOR_Y) {
      initEmptyLevelTiles();
      init();
      setGameState(GameState.LOADING);
    } else if (cursorY == CURSOR_Y + 2*diff) {
      setGameState(GameState.CONSTRUCTION);
    } else if (cursorY == CURSOR_Y + diff) {
      // player2Tank = new Player(); // TODO
    }
  }
}

public boolean hasScreenSize(PImage image) {
  return image.width == width && image.height == height;
}

// TODO: Check Player2
public void saveHiScore() {
  int player1Score = player1Tank.score; 
  int hiScore = getHiScore();
  if (player1Score > hiScore) {
    hiScore = player1Score;
  }
  setHiScore(hiScore);
}

public int getHiScore() {
  int hiScore = DEFAULT_HI_SCORE;
  File f = new File(dataPath("hi-score"));
  if (!f.exists()) {
    return hiScore;
  }
  Scanner scanner = null;
  try {
    scanner = new Scanner(f);
    hiScore = scanner.nextInt();
    if (hiScore > 10000000 || hiScore < 0) {
      hiScore = DEFAULT_HI_SCORE;
    }
  } 
  catch (Exception e) {
  } 
  finally {
    if (scanner != null) {
      scanner.close();
    }
  }
  return hiScore;
}

public void setHiScore(int hiScore) {
  File f = new File(dataPath("hi-score"));
  try {
    if (!f.exists()) {
      f.createNewFile();
    }
    PrintWriter out = new PrintWriter(f);
    out.println(hiScore);
    out.close();
  } 
  catch(Exception e) {
  }
}

public int getArraySum(int[] a) {
  int sum = 0;
  for (int e : a) {
    sum += e;
  }
  return sum;
}
class Base {
  int x, y; // corner coordinates for the base position
  PImage img; // displayed image
  PImage destroyedBaseImg;
  BaseState state;

  Base() {
    destroyedBaseImg = allObjectsImage.get(2*TILE_SIZE, 2*TILE_SIZE, 2*TILE_SIZE, 2*TILE_SIZE-1);

    this.x = ACTUAL_WIDTH/2 - TILE_SIZE;
    this.y = HEIGHT - OBJECT_SIZE + 1;

    build();
  }

  public void build() {
    state =  BaseState.STANDING;
    img = baseImg;
  }

  public void destroy() {
    state = BaseState.DESTROYED;
    img = destroyedBaseImg;
  }

  public void display() {
    image(img, x, y);
  }
}
class Bonus {
  int x, y; // corner coordinates
  BonusType type;
  boolean active;
  boolean visible;
  PImage img;
  PImage grenadeBonusImg, helmetBonusImg, shovelBonusImg, starBonusImg, tankBonusImg, timerBonusImg;
  int bonusActiveTimer;
  final int BONUS_ACTIVE_TIME = 8000;

  int updateAnimationTimer;

  Bonus() {
    grenadeBonusImg = allObjectsImage.get(0, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);
    helmetBonusImg = allObjectsImage.get(OBJECT_SIZE, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);
    shovelBonusImg = allObjectsImage.get(2*OBJECT_SIZE, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);
    starBonusImg = allObjectsImage.get(3*OBJECT_SIZE, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);
    tankBonusImg = allObjectsImage.get(4*OBJECT_SIZE, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);
    timerBonusImg = allObjectsImage.get(5*OBJECT_SIZE, 2*OBJECT_SIZE, OBJECT_SIZE, OBJECT_SIZE);

    active = true;
    visible = true;
    x = PApplet.parseInt(random(MARGIN, ACTUAL_WIDTH - OBJECT_SIZE - MARGIN));
    y = PApplet.parseInt(random(MARGIN, HEIGHT - OBJECT_SIZE - MARGIN));

    RandomEnum<BonusType> bonusEnumRandomizer = new RandomEnum<BonusType>(BonusType.class);
    type = bonusEnumRandomizer.getRandomEnum();
    if (type == BonusType.GRENADE) {
      img = grenadeBonusImg;
    } else if (type == BonusType.HELMET) {
      img = helmetBonusImg;
    } else if (type == BonusType.SHOVEL) {
      img = shovelBonusImg;
    } else if (type == BonusType.STAR) {
      img = starBonusImg;
    } else if (type == BonusType.TANK) {
      img = tankBonusImg;
    } else if (type == BonusType.TIMER) {
      img = timerBonusImg;
    }

    bonusActiveTimer = millis();
    updateAnimationTimer = millis();
  }

  // TODO: Use Timer
  public void toggleVisibility() {
    if (frameCount % 10 == 0) {
      visible = !visible;
    }
  }

  public void deactivate() {
    active = false;
    visible = false;
  }

  public void display() {
    if (millis() - bonusActiveTimer > BONUS_ACTIVE_TIME) {
      deactivate();
    }
    if (visible) {
      image(img, x, y);
    }
  }
}

final int CONTROLS_X = ACTUAL_WIDTH + TILE_SIZE;
final int CONTROLS_Y = 3*TILE_SIZE;
PImage img;
boolean erase = false; // TODO: Use an image of eraser

public void constructingNewLevel() {
  background(0);
  fill(127);
  rect(ACTUAL_WIDTH, 0, SIDE_PANEL_SIZE, HEIGHT);
  image(brickTileImg, CONTROLS_X, CONTROLS_Y*0 + TILE_SIZE);
  image(steelTileImg, CONTROLS_X, CONTROLS_Y*1 + TILE_SIZE);
  image(grassTileImg, CONTROLS_X, CONTROLS_Y*2 + TILE_SIZE);
  image(waterTileImg, CONTROLS_X, CONTROLS_Y*3 + TILE_SIZE);
  image(snowTileImg, CONTROLS_X, CONTROLS_Y*4 + TILE_SIZE);
  fill(255);
  rect(CONTROLS_X, CONTROLS_Y*5 + TILE_SIZE, TILE_SIZE, TILE_SIZE); // eraser

  fill(0);
  textFont(createFont("Arial", 9, true));
  text("S to Save", ACTUAL_WIDTH + SIDE_PANEL_SIZE/2, 23*TILE_SIZE);
  text("ENTER to Play", ACTUAL_WIDTH + SIDE_PANEL_SIZE/2, 25*TILE_SIZE);
  textFont(defaultFont);
  textSize(DEFAULT_FONT_SIZE);
  image(baseImg, ACTUAL_WIDTH/2 - TILE_SIZE, HEIGHT - OBJECT_SIZE + 1);
  displayLevelTiles();
}

public void saveConstructedLevel() {
  File f = new File(dataPath("levels/0"));
  try {
    if (!f.exists()) {
      f.createNewFile();
    }
    PrintWriter out = new PrintWriter(f);
    for (int j = 0; j < tiles[0].length; j++) {
      for (int i = 0; i < tiles.length; i++) {
        if (tiles[i][j] != null) {
          switch(tiles[i][j]) {
          case BRICK: 
            out.print("#");
            break;
          case STEEL:
            out.print("x");
            break;
          case WATER:
            out.print("~");
            break;
          case GRASS:
            out.print(",");
            break;
          case SNOW:
            out.print("-");
            break;
          default:
            out.print(" ");
          }
        } else {
          out.print(" ");
        }
      }
      out.println();
    }
    out.close();
  } 
  catch (Exception e) {
    e.printStackTrace();
  }
}

public void mousePressed() {
  if (gameState == GameState.CONSTRUCTION) {
    if (mouseX < ACTUAL_WIDTH && mouseX > 0 && mouseY < HEIGHT - OBJECT_SIZE - MARGIN && mouseY > 0) {
      if (img != null) {
        image(img, mouseX, mouseY);
        if (img==brickTileImg) {
          tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.BRICK;
          obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = true;
        } else if (img==steelTileImg) {
          tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.STEEL;
          obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = true;
        } else if (img==grassTileImg) {
          tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.GRASS;
          obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = false;
        } else if (img==waterTileImg) {
          tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.WATER;
          obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = true;
        } else if (img==snowTileImg) {
          tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.SNOW;
          obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = false;
        }
        cursor(img);
      } else if (erase) {
        tiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = TileType.EMPTY;
        obstacleTiles[mouseX/TILE_SIZE][mouseY/TILE_SIZE] = false;
        cursor(HAND);
      } else {
        cursor(ARROW);
      }
    }
  }
}

public void mouseDragged() {
  mousePressed();
}

public void mouseClicked() {
  if (gameState == GameState.CONSTRUCTION) {
    if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*0 + TILE_SIZE && mouseY < CONTROLS_Y*0 + TILE_SIZE + TILE_SIZE) {
      img = brickTileImg;
      erase = false;
      cursor(img);
    } else if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*1 + TILE_SIZE && mouseY < CONTROLS_Y*1 + TILE_SIZE + TILE_SIZE) {
      img = steelTileImg;
      erase = false;
      cursor(img);
    } else if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*2 + TILE_SIZE && mouseY < CONTROLS_Y*2 + TILE_SIZE + TILE_SIZE) {
      img = grassTileImg;
      erase = false;
      cursor(img);
    } else if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*3 + TILE_SIZE && mouseY < CONTROLS_Y*3 + TILE_SIZE + TILE_SIZE) {
      img = waterTileImg;
      erase = false;
      cursor(img);
    } else if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*4 + TILE_SIZE && mouseY < CONTROLS_Y*4 + TILE_SIZE + TILE_SIZE) {
      img = snowTileImg;
      erase = false;
      cursor(img);
    } else if (mouseX > CONTROLS_X && mouseX < CONTROLS_X + TILE_SIZE && mouseY > CONTROLS_Y*5 + TILE_SIZE && mouseY < CONTROLS_Y*5 + TILE_SIZE + TILE_SIZE) {
      erase = true;
      img = null;
      cursor(HAND);
    } else if (mouseX > ACTUAL_WIDTH && mouseX < WIDTH && mouseY > 0 && mouseY < HEIGHT) {
      erase = false;
      img = null;
      cursor(ARROW);
    }
  }
}
/// DATA TYPES ///
enum GameState {
  INTRO, OFF, LOADING, RUNNING, PAUSED, SCORE, CONSTRUCTION, OVER;
};

enum Direction {
  NORTH, EAST, SOUTH, WEST;
};

enum TileType {
  EMPTY, BRICK, STEEL, WATER, GRASS, SNOW;
};

enum BaseState {
  STANDING, DESTROYED, EXPLODING;
};

enum TankState {
  SPAWNING, ALIVE, DEAD, EXPLODING;
};

enum ShellState {
  ACTIVE, REMOVED, EXPLODING;
};

enum EnemyTankType {
  BASIC, FAST, POWERFUL, ARMORED;
};

enum BonusType {
  GRENADE, HELMET, SHOVEL, STAR, TANK, TIMER;
};

static class RandomEnum<E extends Enum> {
  static final Random RAND = new Random();
  final E[] VALUES;

  RandomEnum(Class<E> enumerationClass) {
    VALUES = enumerationClass.getEnumConstants();
  }

  public E getRandomEnum() {
    return VALUES[RAND.nextInt(VALUES.length)];
  }

  public int getRandomEnumIndex() {
    E e = getRandomEnum();
    for (int i = 0; i < VALUES.length; i++) {
      if (VALUES[i] == e) return i;
    }
    return -1;
  }

  public E getConstantAtIndex(int idx) {
    return VALUES[idx];
  }

  public int getValuesLength() {
    return VALUES.length;
  }
}
class Enemy extends Tank {
  EnemyTankType type;

  int freezingTimer;
  final int FREEZING_TIME = 10000;
  int shootingTimer;
  final int SHOOTING_TIME = 1500; // 0 to always shoot when ready

  final float AI_PROBABILITY = 0.60f; // get artificially-intelligent direction. lel
  // if > 0.7, it becomes artificially stupid. lel

  Enemy(int x, int y, EnemyTankType enemyTankType) {
    super(x, y);
    this.type = enemyTankType;
    this.dir = Direction.SOUTH;

    if (this.type == EnemyTankType.BASIC) {
      this.img = basicTankImg;
      this.speed = 1;
    } else if (this.type == EnemyTankType.FAST) {
      this.img = fastTankImg;
      this.speed = 3;
    } else if (this.type == EnemyTankType.POWERFUL) {
      this.img = powerfulTankImg;
      this.damage = 2;
    } else if (this.type == EnemyTankType.ARMORED) {
      this.img = armoredTankImg;
      this.hp = 400;
    }

    this.shootingTimer = millis();
  }

  public void playRandomly() {
    if (!this.frozen) { // wait a little before firing
      if (millis() - this.shootingTimer > SHOOTING_TIME) {
        this.fire();
        this.shootingTimer = millis();
      }
      this.moveRandomly();
    } else {
      if (millis() - this.freezingTimer > FREEZING_TIME) {
        this.frozen = false;
      }
    }
  }

  public void moveRandomly() {
    move();
    if (isObstacleNextTile() || willCollide()) {
      turnRandomly();
    } else if (dir == Direction.WEST && x <= TILE_SIZE + MARGIN 
      || dir == Direction.EAST && x >= ACTUAL_WIDTH - TILE_SIZE - MARGIN
      || dir == Direction.NORTH && y <= TILE_SIZE + MARGIN
      || dir == Direction.SOUTH && y >= HEIGHT - TILE_SIZE - MARGIN) {
      turnRandomly();
    }
  }

  public void turnRandomly() {
    RandomEnum<Direction> directionRandomizer = new RandomEnum<Direction>(Direction.class);
    Direction preferredDir = getPreferredDirection();
    if (preferredDir != null && random(1) <= AI_PROBABILITY) {
      this.dir = preferredDir;
    } else {
      this.dir =  directionRandomizer.getRandomEnum();
    }
  }

  /*
  Not very intelligent tho
   */
  public Direction getPreferredDirection() {
    if (x < ACTUAL_WIDTH/2) { // on the left, go EAST
      if (!obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y)/TILE_SIZE] && !obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y - SIZE/2 - 2*MARGIN)/TILE_SIZE] && !obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE]) {
        return Direction.EAST;
      }
    }
    if (x > ACTUAL_WIDTH/2) { // on the right, go WEST
      if (!obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y)/TILE_SIZE] && !obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y - SIZE/2 - 2*MARGIN)/TILE_SIZE] && !obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE]) {
        return Direction.WEST;
      }
    }
    // Base is located SOUTH!
    if (!obstacleTiles[(x)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE] && !obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE] && !obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE]) {
      return Direction.SOUTH;
    }
    return null;
  }

  public void freeze() {
    frozen = true;
    freezingTimer = millis();
  }
}
class Level {
  PImage brickTileImg, steelTileImg, grassTileImg, waterTileImg, snowTileImg;
  PImage waterTile1Img, waterTile2Img;
  PImage enemyScoreTankImg, playerScoreTankImg, flagImg;
  final static int MAX_ACTIVE_ENEMIES = 4;
  int activeEnemies = 0;
  int l; // level number

  Level() {
    this(1);
  }

  Level(int x) {
    this.l = x;

    waterTile1Img = allObjectsImage.get(4*OBJECT_SIZE, 4*OBJECT_SIZE, TILE_SIZE, TILE_SIZE);
    waterTile2Img = allObjectsImage.get(4*OBJECT_SIZE + TILE_SIZE, 4*OBJECT_SIZE, TILE_SIZE, TILE_SIZE);
    enemyScoreTankImg = allObjectsImage.get(5*OBJECT_SIZE, 3*OBJECT_SIZE + TILE_SIZE, TILE_SIZE, TILE_SIZE);
    playerScoreTankImg = allObjectsImage.get(5*OBJECT_SIZE + TILE_SIZE, 3*OBJECT_SIZE + TILE_SIZE, TILE_SIZE, TILE_SIZE);
    flagImg = allObjectsImage.get(4*OBJECT_SIZE, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);
  }

  public boolean loadLevel() {
    File f = new File(dataPath("levels/" + l+ ""));
    if (!f.exists()) {
      println("Level File doesn't exist");
      exit();
      return false;
    }
    try {
      BufferedReader reader = new BufferedReader(new java.io.FileReader(f));
      String line = null;
      int j = 0;
      while ((line = reader.readLine()) != null) {
        for (int i = 0; i < line.length(); i++) {
          switch(line.charAt(i)) {
          case '#': 
            obstacleTiles[i][j] = true;
            tiles[i][j] = TileType.BRICK;
            break;
          case 'x':
            obstacleTiles[i][j] = true;
            tiles[i][j] = TileType.STEEL;
            break;
          case '~':
            obstacleTiles[i][j] = true;
            tiles[i][j] = TileType.WATER;
            break;
          case ',':
            obstacleTiles[i][j] = false;
            tiles[i][j]= TileType.GRASS;
            break;
          case '-':
            obstacleTiles[i][j] = false;
            tiles[i][j] = TileType.SNOW;
            break;
          default:
            obstacleTiles[i][j] = false;
            tiles[i][j] = TileType.EMPTY;
          }
        }
        j++;
      }
      reader.close();
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }

  // TODO: Cause of high FPS not working. Use timer!
  public void animateWaves() {
    if (waterTileImg == waterTile1Img) {
      waterTileImg = waterTile2Img;
    } else {
      waterTileImg = waterTile1Img;
    }
  }

  public void display() {
    if (gameState == GameState.RUNNING) {
      displayLevelTiles();
    }
  }

  public void displaySideInfo() {
    int numberOfRemainingEnemiesInLevel = getArraySum(remainingEnemies);
    if (numberOfRemainingEnemiesInLevel <= 0 && enemyTanks.size() <= 0) {
      setGameState(GameState.SCORE); // game won!
    } else {
      fill(127);
      rect(ACTUAL_WIDTH, 0, WIDTH - ACTUAL_WIDTH, HEIGHT);
      image(flagImg, WIDTH - OBJECT_SIZE - TILE_SIZE, 18*TILE_SIZE);
      image(playerScoreTankImg, WIDTH - OBJECT_SIZE - TILE_SIZE, 15.5f*TILE_SIZE);
      fill(0);
      text(player1Tank.lives, WIDTH - TILE_SIZE, 16*TILE_SIZE);
      text(l, WIDTH - OBJECT_SIZE, 21*TILE_SIZE);
      int i = 0, j = 0;
      for (int k = 0; k < numberOfRemainingEnemiesInLevel; k++) {
        image(enemyScoreTankImg, ACTUAL_WIDTH + i*TILE_SIZE + 13*MARGIN, j);
        if ((k+1) % 2 == 0) {
          j += TILE_SIZE + 2*MARGIN;
          i = 0;
        } else {
          i++;
        }
      }
    }
    fill(0);
    textFont(createFont("Arial", 9, true));
    text("P to Pause", ACTUAL_WIDTH + SIDE_PANEL_SIZE/2, 24*TILE_SIZE);
    text("SPACE to Fire", ACTUAL_WIDTH + SIDE_PANEL_SIZE/2, 25*TILE_SIZE);
    textFont(defaultFont);
    textSize(DEFAULT_FONT_SIZE);
  }
}
class Player extends Tank {
  int score = 0;
  int lives = 3;
  final static int SIZE = Tank.SIZE - 2*MARGIN;
  int firingTimer;
  final int FIRING_TIME = 100; // fixes fire spamming

  Player() {
    super(ACTUAL_WIDTH/2 - OBJECT_SIZE - TILE_SIZE, HEIGHT - TILE_SIZE - MARGIN);
    this.img = allObjectsImage.get(0, 0, SIZE, SIZE); 
    this.dir = Direction.NORTH;
  }

  public void control() {
    if (this.state == TankState.ALIVE) {
      if (!gameOver) {
        if (keys[0]) {
          if (millis() - firingTimer > FIRING_TIME) {
            fire();
            firingTimer = millis();
          }
        }
        if (keys[1]) {
          this.dir = Direction.NORTH;
        }
        if (keys[2]) {
          this.dir = Direction.EAST;
        }
        if (keys[3]) {
          this.dir = Direction.SOUTH;
        }
        if (keys[4]) {
          this.dir = Direction.WEST;
        }
        if (keys[1] || keys[2] || keys[3] || keys[4]) {
          move();
        }
      }
    }
  }

  public void updateScore(Enemy enemy) {
    if (enemy.type == EnemyTankType.BASIC) {
      this.score += 100;
      killedEnemies[0]++;
    } else if (enemy.type == EnemyTankType.FAST) {
      this.score += 200;
      killedEnemies[1]++;
    } else if (enemy.type == EnemyTankType.POWERFUL) {
      this.score += 300;
      killedEnemies[2]++;
    } else if (enemy.type == EnemyTankType.ARMORED) {
      this.score += 400;
      killedEnemies[3]++;
    }
  }

  public void getShield() {
    shielded = true;
    shieldingTime = BONUS_SHIELDING_TIME;
    spawnAndShieldTimer = millis();
  }
}
class Shell {
  int x, y; // center corner coordinates
  PImage img;
  Direction dir;
  int speed = 5;
  int power = 1; // 1 is normal shell, 2 can destroy steel
  ShellState state = ShellState.ACTIVE;
  Tank ownerTank;
  final static int SIZE = TILE_SIZE - 5*MARGIN;
  int updateAnimationTimer;

  Shell(int x, int y, Direction dir, Tank ownerTank) {
    // not very smart to get it every time. TODO
    this.img = allObjectsImage.get(OBJECT_SIZE*4 + TILE_SIZE + 4*MARGIN, OBJECT_SIZE*4 + TILE_SIZE + 3*MARGIN, SIZE, SIZE);
    this.x =x;
    this.y =y;
    this.dir = dir;
    this.ownerTank = ownerTank;
  }

  public void move() {
    if (state == ShellState.EXPLODING) {
      checkExploded();
    } else if (x > ACTUAL_WIDTH - 4*MARGIN || x < 0 + 4*MARGIN || y > HEIGHT - 4*MARGIN || y < 0 + 4*MARGIN) {
      this.state = ShellState.EXPLODING;
      updateAnimationTimer = millis();
    } else {
      if (dir == Direction.NORTH) {
        y -= speed;
      } else if (dir == Direction.EAST) {
        x += speed;
      } else if (dir == Direction.SOUTH) {
        y += speed;
      } else if (dir == Direction.WEST) {
        x -= speed;
      }
      checkHit();
    }
  }

  public void checkHit() {
    if (this.state == ShellState.ACTIVE) {
      checkHitTile();
      checkHitAnotherShell();
      checkHitBase();
      checkHitTank();
    }
  }

  /*
  No idea why this code works
   */
  public void checkHitTile() {
    if (state == ShellState.ACTIVE) {
      if (x < ACTUAL_WIDTH - SIZE/2 && x > -SIZE/2 && y < HEIGHT - SIZE/2 && y > -SIZE/2) { 
        // remove two horizontal tiles in one hit
        if (tiles[(x - SIZE/2)/TILE_SIZE][(y)/TILE_SIZE] == TileType.BRICK && tiles[(x + SIZE/2)/TILE_SIZE][(y)/TILE_SIZE] == TileType.BRICK) {
          removeTile(x - SIZE/2, y);
          removeTile(x + SIZE/2, y);
          state = ShellState.REMOVED;
        }
        // remove two vertical tiles in one hit   
        if (state == ShellState.ACTIVE) {
          if (tiles[(x)/TILE_SIZE][(y - SIZE/2)/TILE_SIZE] == TileType.BRICK && tiles[(x)/TILE_SIZE][(y + SIZE/2)/TILE_SIZE] == TileType.BRICK) { 
            removeTile(x, y - SIZE/2);
            removeTile(x, y + SIZE/2);
            state = ShellState.REMOVED;
          }
        }
        if (state == ShellState.ACTIVE) {
          // remove two horizontal steel tiles in one hit
          if (tiles[(x - SIZE/2)/TILE_SIZE][(y)/TILE_SIZE] == TileType.STEEL && tiles[(x + SIZE/2)/TILE_SIZE][(y)/TILE_SIZE] == TileType.STEEL) {
            if (power == 2) {
              removeTile(x - SIZE/2, y);
              removeTile(x + SIZE/2, y);
            }
            state = ShellState.REMOVED;
          }
          // remove two vertical steel tiles in one hit   
          if (state == ShellState.ACTIVE) {
            if (tiles[(x)/TILE_SIZE][(y - SIZE/2)/TILE_SIZE] == TileType.STEEL && tiles[(x)/TILE_SIZE][(y + SIZE/2)/TILE_SIZE] == TileType.STEEL) { 
              if (power == 2) {
                removeTile(x, y - SIZE/2);
                removeTile(x, y + SIZE/2);
              }
              state = ShellState.REMOVED;
            }
          }
        }
      }
    }
  }

  public void removeTile(int x, int y) {
    if (tiles[x/TILE_SIZE][y/TILE_SIZE] == TileType.BRICK) {
      playSound(sounds[3], true);
    } else if (tiles[x/TILE_SIZE][y/TILE_SIZE] == TileType.STEEL) {  
      playSound(sounds[4], true);
    }
    tiles[x/TILE_SIZE][y/TILE_SIZE] = TileType.EMPTY;
    obstacleTiles[x/TILE_SIZE][y/TILE_SIZE] = false;
  }

  public void checkHitAnotherShell() {
    if (state == ShellState.ACTIVE) {
      // check for collision with other shells
      for (Shell shell : shells) {
        if (!this.equals(shell)) {
          if (squaresOverlap(this.x - SIZE/2, this.y - SIZE/2, SIZE, shell.x - SIZE/2, shell.y - SIZE/2, SIZE)) {
            shell.state = ShellState.REMOVED;
            this.state = ShellState.REMOVED;
            break;
          }
        }
      }
    }
  }

  public void checkHitBase() {
    if (state == ShellState.ACTIVE) {
      if (x > base.x && y > base.y && x < base.x + base.img.width && y < base.y + base.img.height && !gameOver) {
        state = ShellState.REMOVED;
        base.destroy();
        setGameState(GameState.OVER);
      }
    }
  }

  public void checkHitTank() {
    if (!(this.ownerTank instanceof Player) && squaresOverlap(this.x - SIZE/2, this.y - SIZE/2, SIZE - 2*MARGIN, player1Tank.x - Player.SIZE/2, player1Tank.y - Player.SIZE/2, Player.SIZE)) {
      this.state = ShellState.REMOVED;
      player1Tank.shellImpact(this);
    } else {
      for (Enemy enemy : enemyTanks) {
        if (!enemy.equals(this.ownerTank)) {
          if (squaresOverlap(x - SIZE/2, y - SIZE/2, SIZE - 2*MARGIN, enemy.x - Enemy.SIZE/2, enemy.y - Enemy.SIZE/2, Enemy.SIZE)) {
            this.state = ShellState.REMOVED;
            enemy.shellImpact(this);
          }
        }
      }
    }
  }

  public void display() {
    displayRotatedImg(img, x, y, dir);
  }

  public void checkExploded() {
    if (millis() - updateAnimationTimer > UPDATE_ANIMATION_TIME) {
      state = ShellState.REMOVED;
    } else {
      imageMode(CENTER);
      image(shellExplosionImg, x, y);
      imageMode(CORNER);
    }
  }
}
class Tank {
  PImage img; // image of the tank
  int x, y; // center position coordinates
  Direction dir; // current direction
  TankState state; // spawning, alive or dead
  final static int SIZE = 2 * TILE_SIZE - 3*MARGIN; // dimensions size
  int speed = 2; // movement speed
  int hp = 100; // health points
  int damage = hp; // damaging power; one hit one kill normally
  boolean shielded = true; // shield that protects temporarily
  boolean frozen = false; // frozen means cannot move or fire
  int maxFiringShells = 1; // max number of shells it can fire
  int extraPowers = 0; 
  /* 
   0- basic shells, 
   1- shells are faster, 
   2- tank can fire 2 shells, 
   3- shells can destroy steel */

  int spawnAndShieldTimer;
  int shieldingTime;
  final int SPAWNING_TIME = 2000, SPAWN_SHIELDING_TIME = 3000, BONUS_SHIELDING_TIME = 10000;
  PImage shieldImgs[];
  int shieldImgIdx = 0;
  PImage spawnImgs[];
  int spawnImgIdx = 0;
  int updateAnimationTimer;

  // cannot instantiate Tank
  private Tank(int x, int y) {
    shieldImgs = getShieldImages();
    spawnImgs = getSpawnImages();
    this.x = x;
    this.y = y;
    if (extraPowers >= 2) {
      maxFiringShells = 2;
    }
    this.state = TankState.SPAWNING;
    spawnAndShieldTimer = millis();
    updateAnimationTimer = millis();
    shieldingTime = SPAWN_SHIELDING_TIME;
  }

  public void display() {
    if (state == TankState.SPAWNING) {
      checkSpawnAndShieldAnimation();
      displayRotatedImg(spawnImgs[spawnImgIdx], x, y, dir);
      checkEndSpawnAndShieldAnimation();
      if (millis() - spawnAndShieldTimer > SPAWNING_TIME) {
        endSpawning();
      }
    } else if (state == TankState.ALIVE) {
      if (shielded) {
        checkSpawnAndShieldAnimation();
        displayRotatedImg(shieldImgs[shieldImgIdx], x, y, dir);
        if (millis() - spawnAndShieldTimer > shieldingTime) {
          endShielding();
        }
      }
      displayRotatedImg(img, x, y, dir);
    } else if (state == TankState.EXPLODING) {
      if (millis() - updateAnimationTimer > 3*UPDATE_ANIMATION_TIME) {
        state = TankState.DEAD;
        updateAnimationTimer = millis();
      }
      imageMode(CENTER);
      image(tankExplosionImg, x, y);
      imageMode(CORNER);
      if (this instanceof Enemy) { // show points earned
        fill(255);
        textSize(SMALL_FONT_SIZE);
        text(getEnemyTankValue((Enemy)this), x + SIZE, y);
        textSize(DEFAULT_FONT_SIZE);
      }
    }
  }

  public void checkSpawnAndShieldAnimation() {
    if (state == TankState.SPAWNING) {
      if (millis() - updateAnimationTimer > UPDATE_ANIMATION_TIME) {
        spawning();
        updateAnimationTimer = millis();
      }
    } else if (state == TankState.ALIVE) {
      if (shielded) {
        if (millis() - updateAnimationTimer > UPDATE_ANIMATION_TIME) {
          shielding();
          updateAnimationTimer = millis();
        }
      }
    }
  }

  public void checkEndSpawnAndShieldAnimation() {
  }

  public void spawning() {
    if (state == TankState.SPAWNING) {
      spawnImgIdx++;
      if (spawnImgIdx >= spawnImgs.length) {
        spawnImgIdx = 0;
      }
    }
  }

  public void endSpawning() {
    state = TankState.ALIVE;
    shielded = true;
    spawnAndShieldTimer = millis();
  }

  public void shielding() {
    if (state == TankState.ALIVE && shielded) {
      shieldImgIdx++;
      if (shieldImgIdx >= shieldImgs.length) {
        shieldImgIdx = 0;
      }
    }
  }

  public void endShielding() {
    // state = TankState.ALIVE;
    shielded = false;
    // spawnAndShieldTimer = millis();
  }

  public void explode(boolean forceExplode) {
    if (state == TankState.ALIVE || forceExplode) {
      state = TankState.EXPLODING;
      updateAnimationTimer = millis();
      playSound(sounds[6], true);
    }
  }

  public void upgradeShellPowers() {
    extraPowers++;
    extraPowers = constrain(extraPowers, 0, 3);
    if (extraPowers >= 2) {
      maxFiringShells = 2;
    }
  }

  public void move() {
    if (state == TankState.ALIVE) {
      if (isInBattleField()) {
        if (!isObstacleNextTile() && !willCollide()) {
          if (dir == Direction.NORTH) {
            y -= speed;
          } else if (dir == Direction.EAST) {
            x += speed;
          } else if (dir == Direction.SOUTH) {
            y += speed;
          } else if (dir == Direction.WEST) {
            x -= speed;
          }
        }
      }
    }
    x = constrain(x, TILE_SIZE, ACTUAL_WIDTH - TILE_SIZE - MARGIN);
    y = constrain(y, TILE_SIZE, HEIGHT - TILE_SIZE - MARGIN);
  }

  public void fire() {
    if (state == TankState.ALIVE) {
      int c = 0;
      for (Shell shell : shells) {
        if (shell.ownerTank.equals(this)) {
          c++;
          if (c >= maxFiringShells) {
            return;
          }
        }
      }
      int i = this.x, j = this.y; // postition it on the cannon
      if (this.dir == Direction.EAST) {
        i += SIZE/2;
      } else if (this.dir == Direction.WEST) {
        i -= SIZE/2;
      } else if (this.dir == Direction.SOUTH) {
        j += SIZE/2;
      } else if (this.dir == Direction.NORTH) {
        j -= SIZE/2;
      }
      Shell shell = new Shell(i, j, this.dir, this);
      if (this.extraPowers > 0) {
        shell.speed = 8;
      }
      if (this.extraPowers > 2) {
        shell.power = 2;
      }
      shells.add(shell);
      if (this instanceof Player) { // firing sound for players only
        playSound(sounds[2], true);
      }
    }
  }

  public boolean willCollide() {
    for (Enemy enemy : enemyTanks) {
      if (dir == Direction.NORTH) {
        if ((y - SIZE/2 - 3*MARGIN <= enemy.y + Enemy.SIZE/2 && y - SIZE/2 - MARGIN >= enemy.y + Enemy.SIZE/2)  && segmentsOverlay(x, SIZE, enemy.x, Enemy.SIZE)) {
          return true;
        }
      } else if (dir == Direction.SOUTH) {
        if ((y + SIZE/2 + 3*MARGIN >= enemy.y - Enemy.SIZE/2 && y + SIZE/2 + MARGIN <= enemy.y - Enemy.SIZE/2) && segmentsOverlay(x, SIZE, enemy.x, Enemy.SIZE)) {
          return true;
        }
      } else if (dir == Direction.EAST) {
        if ((x + SIZE/2 + 3*MARGIN >= enemy.x - Enemy.SIZE/2 && x + SIZE/2 + MARGIN <= enemy.x - Enemy.SIZE/2) && segmentsOverlay(y, SIZE, enemy.y, Enemy.SIZE)) {
          return true;
        }
      } else if (dir == Direction.WEST) {
        if ((x - SIZE/2 - 3*MARGIN <= enemy.x + Enemy.SIZE/2 && x - SIZE/2 - MARGIN >= enemy.x + Enemy.SIZE/2) && segmentsOverlay(y, SIZE, enemy.y, Enemy.SIZE)) {
          return true;
        }
      }
    }
    if (this instanceof Enemy) {
      if (dir == Direction.NORTH) {
        if ((y - SIZE/2 - 3*MARGIN <= player1Tank.y + Player.SIZE/2 && y - SIZE/2 - MARGIN >= player1Tank.y + Player.SIZE/2)  && x - SIZE/2 - MARGIN <= player1Tank.x + Player.SIZE/2 && x + SIZE/2 + MARGIN >= player1Tank.x - Player.SIZE/2) {
          return true;
        }
      } else if (dir == Direction.SOUTH) {
        if ((y + SIZE/2 + 3*MARGIN >= player1Tank.y - Player.SIZE/2 && y + SIZE/2 + MARGIN <= player1Tank.y - Player.SIZE/2) && x - SIZE/2 - MARGIN <= player1Tank.x + Player.SIZE/2 && x + SIZE/2 + MARGIN >= player1Tank.x - Player.SIZE/2) {
          return true;
        }
      } else if (dir == Direction.EAST) {
        if ((x + SIZE/2 + 3*MARGIN >= player1Tank.x - Player.SIZE/2 && x + SIZE/2 + MARGIN <= player1Tank.x - Player.SIZE/2) && y - SIZE/2 - MARGIN <= player1Tank.y + Player.SIZE/2 && y + SIZE/2 + MARGIN >= player1Tank.y - Player.SIZE/2) {
          return true;
        }
      } else if (dir == Direction.WEST) {
        if ((x - SIZE/2 - 3*MARGIN <= player1Tank.x + Player.SIZE/2 && x - SIZE/2 - MARGIN >= player1Tank.x + Player.SIZE/2) && y - SIZE/2 - MARGIN <= player1Tank.y + Player.SIZE/2 && y + SIZE/2 + MARGIN >= player1Tank.y - Player.SIZE/2) {
          return true;
        }
      }
    }
    return false;
  }

  public void shellImpact(Shell shell) {
    if (!this.shielded) {  
      if ((this instanceof Player) && !gameOver) {
        getDamaged(shell);
        if (state == TankState.DEAD || state == TankState.EXPLODING) {
          state = TankState.DEAD; // make sure it's dead
          Player player = (Player) this;
          player.lives--;
          if (player.lives <= 0) {
            player.lives = 0;
            setGameState(GameState.OVER);
          } else {
            int lives = player.lives;
            int score = player.score;
            if (player.equals(player1Tank)) {
              player = new Player();
              player.lives = lives;
              player.score = score;
              player1Tank = player;
            } else if (player.equals(player2Tank)) {
              player = new Player();
              player.lives = lives;
              player.score = score;
              player2Tank = player;
            }
          }
        }
      } else if (shell.ownerTank instanceof Player && this instanceof Enemy) { 
        // because enemies can't destroy each other
        // player shooting enemy
        if (this.state == TankState.ALIVE) {
          Player player = (Player) shell.ownerTank;
          player.updateScore((Enemy) this);
          getDamaged(shell);
        }
      }
    }
  }

  public void getDamaged(Shell shell) {
    this.hp -= shell.ownerTank.damage;
    if (this.hp <= 0) {
      explode(false);
    }
  }

  public void turnInOppositeDirection() {
    if (state == TankState.ALIVE) {
      if (dir == Direction.NORTH) {
        dir = Direction.SOUTH;
      } else if (dir == Direction.SOUTH) {
        dir = Direction.NORTH;
      } else if (dir == Direction.EAST) {
        dir = Direction.WEST;
      } else if (dir == Direction.WEST) {
        dir = Direction.EAST;
      }
    }
  }

  public boolean isObstacleNextTile() {
    if (dir == Direction.SOUTH) {
      if (obstacleTiles[(x)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE] || obstacleTiles[(x - SIZE/2)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE] || obstacleTiles[(x + SIZE/2)/TILE_SIZE][(y + SIZE/2 + 2*MARGIN)/TILE_SIZE]) {
        return true;
      }
    } else if (dir == Direction.EAST) {
      if (obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y - SIZE/2)/TILE_SIZE] || obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y)/TILE_SIZE] || obstacleTiles[(x + SIZE/2 + 2*MARGIN)/TILE_SIZE][(y + SIZE/2)/TILE_SIZE]) {
        return true;
      }
    } else if (dir == Direction.NORTH) {
      if (obstacleTiles[(x)/TILE_SIZE][(y - SIZE/2 - 2*MARGIN)/TILE_SIZE] || obstacleTiles[(x - SIZE/2)/TILE_SIZE][(y - SIZE/2 - 2*MARGIN)/TILE_SIZE] || obstacleTiles[(x + SIZE/2)/TILE_SIZE][(y - SIZE/2 - 2*MARGIN)/TILE_SIZE]) {
        return true;
      }
    } else if (dir == Direction.WEST) {
      if (obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y - SIZE/2)/TILE_SIZE] || obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y)/TILE_SIZE] || obstacleTiles[(x - SIZE/2 - 2*MARGIN)/TILE_SIZE][(y + SIZE/2)/TILE_SIZE]) {
        return true;
      }
    }
    return false;
  }

  public boolean isInBattleField() {
    if (x >= TILE_SIZE && y >= TILE_SIZE && x <= ACTUAL_WIDTH - TILE_SIZE && y <= HEIGHT - TILE_SIZE) {
      return true;
    }
    return false;
  }

  public PImage[] getShieldImages() {
    PImage[] shieldingImgs = new PImage[2];
    shieldingImgs[0] = allObjectsImage.get(MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);
    shieldingImgs[1] = allObjectsImage.get(OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);

    return shieldingImgs;
  }

  public PImage[] getSpawnImages() {
    PImage[] spawningImgs = new PImage[2];
    spawningImgs[0] = allObjectsImage.get(2*OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);
    spawningImgs[1] = allObjectsImage.get(3*OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);

    return spawningImgs;
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "BattleCity_Tank" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
