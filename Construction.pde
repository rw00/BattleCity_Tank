
final int CONTROLS_X = ACTUAL_WIDTH + TILE_SIZE;
final int CONTROLS_Y = 3*TILE_SIZE;
PImage img;
boolean erase = false; // TODO: Use an image of eraser

void constructingNewLevel() {
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

void saveConstructedLevel() {
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

void mousePressed() {
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

void mouseDragged() {
  mousePressed();
}

void mouseClicked() {
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