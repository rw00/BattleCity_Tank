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

  void move() {
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

  void checkHit() {
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
  void checkHitTile() {
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

  void removeTile(int x, int y) {
    if (tiles[x/TILE_SIZE][y/TILE_SIZE] == TileType.BRICK) {
      playSound(sounds[3], true);
    } else if (tiles[x/TILE_SIZE][y/TILE_SIZE] == TileType.STEEL) {  
      playSound(sounds[4], true);
    }
    tiles[x/TILE_SIZE][y/TILE_SIZE] = TileType.EMPTY;
    obstacleTiles[x/TILE_SIZE][y/TILE_SIZE] = false;
  }

  void checkHitAnotherShell() {
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

  void checkHitBase() {
    if (state == ShellState.ACTIVE) {
      if (x > base.x && y > base.y && x < base.x + base.img.width && y < base.y + base.img.height && !gameOver) {
        state = ShellState.REMOVED;
        base.destroy();
        setGameState(GameState.OVER);
      }
    }
  }

  void checkHitTank() {
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

  void display() {
    displayRotatedImg(img, x, y, dir);
  }

  void checkExploded() {
    if (millis() - updateAnimationTimer > UPDATE_ANIMATION_TIME) {
      state = ShellState.REMOVED;
    } else {
      imageMode(CENTER);
      image(shellExplosionImg, x, y);
      imageMode(CORNER);
    }
  }
}