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

  E getRandomEnum() {
    return VALUES[RAND.nextInt(VALUES.length)];
  }

  int getRandomEnumIndex() {
    E e = getRandomEnum();
    for (int i = 0; i < VALUES.length; i++) {
      if (VALUES[i] == e) return i;
    }
    return -1;
  }

  E getConstantAtIndex(int idx) {
    return VALUES[idx];
  }

  int getValuesLength() {
    return VALUES.length;
  }
}