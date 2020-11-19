/**
 * Prepares the player's game board
 */
function drawBoard() {
  let board = "",
    x,
    y;

  // Draw the cells
  for (let i = 0; i < 10; i++) {
    for (let j = 0; j < 10; j++) {
      x = 75 * i + 50;
      y = 75 * j + 20;

      board += `<rect id="cell_${i}${j}" 
                        x="${x}" 
                        y="${y}" 
                        width="75" 
                        height="75" 
                        stroke-width="5" 
                        stroke="#d2eeff" 
                        fill="#0077be" />`;
    }
  }

  /*
   * Draw the ships:
   *   Carrier     (5)
   *   Battleship  (4)
   *   Destroyer   (3)
   *   Submarine   (3)
   *   Patrol Boat (2)
   */
  drawShip("#carrier", 5, 25, "#44BBA4");
  drawShip("#battleship", 4, 125, "#ffae42");
  drawShip("#destroyer", 3, 225, "#114B5F");
  drawShip("#submarine", 3, 325, "#E94F37");
  drawShip("#boat", 2, 425, "#F6F7EB");

  document.querySelector("#board").innerHTML = board;
}

function drawShip(id, length, y, fillColor) {
  let ship = "";
  let x;

  for (let i = 0; i < length; i++) {
    x = 75 * i + 1200;

    ship += `<rect x="${x}" 
                    y="${y}"
                    width="75"
                    height="75"
                    stroke-width="5"
                    stroke="black"
                    fill="${fillColor}" />`;
  }

  document.querySelector(id).innerHTML = ship;
}
