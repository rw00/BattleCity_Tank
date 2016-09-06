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

  void build() {
    state =  BaseState.STANDING;
    img = baseImg;
  }

  void destroy() {
    state = BaseState.DESTROYED;
    img = destroyedBaseImg;
  }

  void display() {
    image(img, x, y);
  }
}