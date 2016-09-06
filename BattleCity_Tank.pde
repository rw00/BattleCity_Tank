import ddf.minim.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.awt.Point;

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

void settings() {
  size(WIDTH, HEIGHT);
}

void setup() {
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

  smooth();
  frameRate(FPS);
}

void draw() {
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

void init() {
  level = new Level(stage);
  level.loadLevel();

  loadGame();
}

void loadGame() {
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

void initEmptyLevelTiles() {
  // matrix = [columns][rows]
  tiles = new TileType[HEIGHT/TILE_SIZE][ACTUAL_WIDTH/TILE_SIZE];
  obstacleTiles = new boolean[HEIGHT/TILE_SIZE][ACTUAL_WIDTH/TILE_SIZE];
}

void animateIntro() {
  if (animationIdx > 0) {
    background(0);
    image(introImg, 0, animationIdx-=3);
  } else {
    gameState = GameState.OFF; // stop animation
  }
}

void checkEnemyTanks() {
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

void checkFiredShells() {
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

void checkBonuses() {
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

void checkGameOver() {
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

void checkScore() {
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

void keyPressed() {
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

void keyReleased() {
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

int getEnemyTankValue(Enemy tank) {
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
void displayRotatedImg(PImage image, int x, int y, Direction direction) {
  pushMatrix();
  imageMode(CENTER);
  translate(x, y);
  if (direction == Direction.EAST) {
    rotate(PI/2.0);
  } else if (direction == Direction.SOUTH) {
    rotate(PI);
  } else if (direction == Direction.WEST) {
    rotate(3.0 * PI/2.0);
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
void checkActivateBonus() {
  for (Bonus bonus : bonuses) {
    if (squaresOverlap(player1Tank.x - Player.SIZE/2, player1Tank.y - Player.SIZE/2, Player.SIZE, bonus.x, bonus.y, OBJECT_SIZE)) {
      activateBonus(player1Tank, bonus);
      break;
    }
  }
}

void activateBonus(Player player, Bonus bonus) {
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
boolean squaresOverlap(int x0, int y0, int size0, int x1, int y1, int size1) {
  if (x0 < x1 + size1 && x0 + size0 > x1 && y0 < y1 + size1 && y0 + size0 > y1) {
    return true;
  }
  return false;
}

/*
pos0 and pos1 are center positions
 */
boolean segmentsOverlay(int pos0, int size0, int pos1, int size1) {
  return pos0 - size0/2 - MARGIN <= pos1 + size1/2 && pos0 + size0/2 + MARGIN >= pos1 - size1/2;
}

void loadConstructedLevel() {
  stage = 0; // one constructed level only. ZERO  
  levelIndex = 0;
  cursor(ARROW);
}

void wonLevel() {
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

void showScore() {
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
  text(String.format("%5d  PTS", t1*100), .5*TILE_SIZE, 11*TILE_SIZE);
  text(String.format("%2d", t1), 11*TILE_SIZE, 11*TILE_SIZE);
  image(arrowImg, 13.5*TILE_SIZE, 10.5*TILE_SIZE);
  image(basicTankImg, 15.5*TILE_SIZE, 10.5*TILE_SIZE);

  text(String.format("%5d  PTS", t2*200), .5*TILE_SIZE, 14*TILE_SIZE);
  text(String.format("%2d", t2), 11*TILE_SIZE, 14*TILE_SIZE);
  image(arrowImg, 13.5*TILE_SIZE, 13.5*TILE_SIZE);
  image(fastTankImg, 15.5*TILE_SIZE, 13.5*TILE_SIZE);

  text(String.format("%5d  PTS", t3*300), .5*TILE_SIZE, 17*TILE_SIZE);
  text(String.format("%2d", t3), 11*TILE_SIZE, 17*TILE_SIZE);
  image(arrowImg, 13.5*TILE_SIZE, 16.5*TILE_SIZE);
  image(powerfulTankImg, 15.5*TILE_SIZE, 16.5*TILE_SIZE);

  text(String.format("%5d  PTS", t4*400), .5*TILE_SIZE, 20*TILE_SIZE);
  text(String.format("%2d", t4), 11*TILE_SIZE, 20*TILE_SIZE);
  image(arrowImg, 13.5*TILE_SIZE, 19.5*TILE_SIZE);
  image(armoredTankImg, 15.5*TILE_SIZE, 19.5*TILE_SIZE);

  fill(255);
  rect(11*TILE_SIZE, 21*TILE_SIZE, 9*TILE_SIZE, TILE_SIZE/2);
  text("TOTAL ", 4*TILE_SIZE, 23*TILE_SIZE);
  text(String.format("%2d", t1 + t2 + t3 + t4), 11*TILE_SIZE, 23*TILE_SIZE);
  imageMode(CORNER);
  textAlign(CENTER);
  textSize(DEFAULT_FONT_SIZE);
  saveHiScore();
}

void nextLevel() {
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
void loadImages() {
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
void loadSounds() {
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

void rewindSounds() {
  for (AudioPlayer sound : sounds) {
    if (sound != null) {
      sound.rewind();
    }
  }
}

void playSound(AudioPlayer sound, boolean rewind) {
  if (playSounds) {
    sound.play();
    if (rewind) {
      sound.rewind();
    }
  }
}

void repeatSound(AudioPlayer sound) {
  if (playSounds && !sound.isPlaying()) {
    sound.rewind();
    sound.play();
  }
}

void stop() {
  // stop AudioPlayers?
  minim.stop();
}
///////////////////////////////////////////////////////////////////////////////////////////////////

/*
// TODO: Make 5 positions
 TODO: Improve algorithm
 */
Point getSpawningPosition() {
  int r = int(random(0, 3)); // left, center, right. 
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
boolean noTanksOn(int x, int y) {
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

void setGameState(GameState state) {
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
void spawnNewEnemy() {
  if (millis() - enemyGenerationTimer > ENEMY_GENERATION_TIME) { // time to spawn new enemy tank
    RandomEnum<EnemyTankType> enemyTankEnumRandomizer = new RandomEnum<EnemyTankType>(EnemyTankType.class);
    int t = int(random(0, enemyTankEnumRandomizer.getValuesLength())); // int(random(0, 5));
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

void displayLevelTiles() {
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

void showStartMenu() {
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

boolean hasScreenSize(PImage image) {
  return image.width == width && image.height == height;
}

// TODO: Check Player2
void saveHiScore() {
  int player1Score = player1Tank.score; 
  int hiScore = getHiScore();
  if (player1Score > hiScore) {
    hiScore = player1Score;
  }
  setHiScore(hiScore);
}

int getHiScore() {
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

void setHiScore(int hiScore) {
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

int getArraySum(int[] a) {
  int sum = 0;
  for (int e : a) {
    sum += e;
  }
  return sum;
}
