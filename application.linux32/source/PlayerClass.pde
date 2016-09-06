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

  void control() {
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

  void updateScore(Enemy enemy) {
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

  void getShield() {
    shielded = true;
    shieldingTime = BONUS_SHIELDING_TIME;
    spawnAndShieldTimer = millis();
  }
}