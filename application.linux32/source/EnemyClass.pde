class Enemy extends Tank {
  EnemyTankType type;

  int freezingTimer;
  final int FREEZING_TIME = 10000;
  int shootingTimer;
  final int SHOOTING_TIME = 1500; // 0 to always shoot when ready

  final float AI_PROBABILITY = 0.60; // get artificially-intelligent direction. lel
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

  void playRandomly() {
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

  void moveRandomly() {
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

  void turnRandomly() {
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
  Direction getPreferredDirection() {
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

  void freeze() {
    frozen = true;
    freezingTimer = millis();
  }
}