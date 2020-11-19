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
  drawShip("#carrier", 5, "#44BBA4", "default", null, 25);
  drawShip("#battleship", 4, "#ffae42", "default", null, 125);
  drawShip("#destroyer", 3, "#114B5F", "default", null, 225);
  drawShip("#submarine", 3, "#E94F37", "default", null, 325);
  drawShip("#boat", 2, "#F6F7EB", "default", null, 425);

  localStorage.setItem("shipsDirection", "default");

  document.querySelector("#board").innerHTML = board;
}

/**
 * Prepares the ships for the game
 * @param {String} id
 * @param {Number} length
 * @param {String} fillColor
 * @param {String} direction
 * @param {Number} defaultX
 * @param {Number} defaultY
 */
function drawShip(id, length, fillColor, direction, defaultX, defaultY) {
  let ship = "";
  let x, y;

  for (let i = 0; i < length; i++) {
    if (direction === "default") {
      x = 75 * i + 1200;
      y = defaultY;
    } else if (direction === "rotated") {
      x = defaultX;
      y = 75 * i + 25;
    }

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

/**
 * Rotates the ships
 */
function rotateShips() {
  // Get the stored ship direction value
  const direction = localStorage.getItem("shipsDirection");

  // Clear existing ships
  document.querySelector("#carrier").innerHTML = "";
  document.querySelector("#battleship").innerHTML = "";
  document.querySelector("#destroyer").innerHTML = "";
  document.querySelector("#submarine").innerHTML = "";
  document.querySelector("#boat").innerHTML = "";

  if (direction === "default") {
    drawShip("#carrier", 5, "#44BBA4", "default", null, 25);
    drawShip("#battleship", 4, "#ffae42", "default", null, 125);
    drawShip("#destroyer", 3, "#114B5F", "default", null, 225);
    drawShip("#submarine", 3, "#E94F37", "default", null, 325);
    drawShip("#boat", 2, "#F6F7EB", "default", null, 425);
  } else if (direction === "rotated") {
    drawShip("#carrier", 5, "#44BBA4", "rotated", 1100, null);
    drawShip("#battleship", 4, "#ffae42", "rotated", 1200, null);
    drawShip("#destroyer", 3, "#114B5F", "rotated", 1300, null);
    drawShip("#submarine", 3, "#E94F37", "rotated", 1400, null);
    drawShip("#boat", 2, "#F6F7EB", "rotated", 1500, null);
  }
}
