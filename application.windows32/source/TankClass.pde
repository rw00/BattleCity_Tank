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

  void display() {
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

  void checkSpawnAndShieldAnimation() {
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

  void checkEndSpawnAndShieldAnimation() {
  }

  void spawning() {
    if (state == TankState.SPAWNING) {
      spawnImgIdx++;
      if (spawnImgIdx >= spawnImgs.length) {
        spawnImgIdx = 0;
      }
    }
  }

  void endSpawning() {
    state = TankState.ALIVE;
    shielded = true;
    spawnAndShieldTimer = millis();
  }

  void shielding() {
    if (state == TankState.ALIVE && shielded) {
      shieldImgIdx++;
      if (shieldImgIdx >= shieldImgs.length) {
        shieldImgIdx = 0;
      }
    }
  }

  void endShielding() {
    // state = TankState.ALIVE;
    shielded = false;
    // spawnAndShieldTimer = millis();
  }

  void explode(boolean forceExplode) {
    if (state == TankState.ALIVE || forceExplode) {
      state = TankState.EXPLODING;
      updateAnimationTimer = millis();
      playSound(sounds[6], true);
    }
  }

  void upgradeShellPowers() {
    extraPowers++;
    extraPowers = constrain(extraPowers, 0, 3);
    if (extraPowers >= 2) {
      maxFiringShells = 2;
    }
  }

  void move() {
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

  void fire() {
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

  boolean willCollide() {
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

  void shellImpact(Shell shell) {
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

  void getDamaged(Shell shell) {
    this.hp -= shell.ownerTank.damage;
    if (this.hp <= 0) {
      explode(false);
    }
  }

  void turnInOppositeDirection() {
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

  boolean isObstacleNextTile() {
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

  boolean isInBattleField() {
    if (x >= TILE_SIZE && y >= TILE_SIZE && x <= ACTUAL_WIDTH - TILE_SIZE && y <= HEIGHT - TILE_SIZE) {
      return true;
    }
    return false;
  }

  PImage[] getShieldImages() {
    PImage[] shieldingImgs = new PImage[2];
    shieldingImgs[0] = allObjectsImage.get(MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);
    shieldingImgs[1] = allObjectsImage.get(OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);

    return shieldingImgs;
  }

  PImage[] getSpawnImages() {
    PImage[] spawningImgs = new PImage[2];
    spawningImgs[0] = allObjectsImage.get(2*OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);
    spawningImgs[1] = allObjectsImage.get(3*OBJECT_SIZE + MARGIN, 3*OBJECT_SIZE, OBJECT_SIZE - MARGIN, OBJECT_SIZE - MARGIN);

    return spawningImgs;
  }
}