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
    x = int(random(MARGIN, ACTUAL_WIDTH - OBJECT_SIZE - MARGIN));
    y = int(random(MARGIN, HEIGHT - OBJECT_SIZE - MARGIN));

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
  void toggleVisibility() {
    if (frameCount % 10 == 0) {
      visible = !visible;
    }
  }

  void deactivate() {
    active = false;
    visible = false;
  }

  void display() {
    if (millis() - bonusActiveTimer > BONUS_ACTIVE_TIME) {
      deactivate();
    }
    if (visible) {
      image(img, x, y);
    }
  }
}