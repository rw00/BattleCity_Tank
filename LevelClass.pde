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

  boolean loadLevel() {
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
  void animateWaves() {
    if (waterTileImg == waterTile1Img) {
      waterTileImg = waterTile2Img;
    } else {
      waterTileImg = waterTile1Img;
    }
  }

  void display() {
    if (gameState == GameState.RUNNING) {
      displayLevelTiles();
    }
  }

  void displaySideInfo() {
    int numberOfRemainingEnemiesInLevel = getArraySum(remainingEnemies);
    if (numberOfRemainingEnemiesInLevel <= 0 && enemyTanks.size() <= 0) {
      setGameState(GameState.SCORE); // game won!
    } else {
      fill(127);
      rect(ACTUAL_WIDTH, 0, WIDTH - ACTUAL_WIDTH, HEIGHT);
      image(flagImg, WIDTH - OBJECT_SIZE - TILE_SIZE, 18*TILE_SIZE);
      image(playerScoreTankImg, WIDTH - OBJECT_SIZE - TILE_SIZE, 15.5*TILE_SIZE);
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