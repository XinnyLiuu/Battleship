let selectedShip, selectedShipType;

// Map of ship type to length
const SHIPS = {
  carrier: {
    size: 5,
    color: "#44BBA4",
  },
  battleship: {
    size: 4,
    color: "#ffae42",
  },
  destroyer: {
    size: 3,
    color: "#114B5F",
  },
  submarine: {
    size: 3,
    color: "#E94F37",
  },
  boat: {
    size: 2,
    color: "#F6F7EB",
  },
};

/**
 * Prepares the player's game board
 */
function drawBoard() {
  let board = "",
    x,
    y;

  const boardCellIds = [];

  // Draw the cells
  for (let i = 0; i < 10; i++) {
    for (let j = 0; j < 10; j++) {
      y = 50 * i + 20;
      x = 50 * j + 50;

      const id = `cell_${i}_${j}`;
      boardCellIds.push(id);

      board += `<rect id="${id}" 
                        data-row="${i}"
                        data-col="${j}"
                        x="${x}" 
                        y="${y}" 
                        width="50" 
                        height="50" 
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
  drawShip(CARRIER, "default", null, 25);
  drawShip(BATTLESHIP, "default", null, 125);
  drawShip(DESTROYER, "default", null, 225);
  drawShip(SUBMARINE, "default", null, 325);
  drawShip(BOAT, "default", null, 425);

  localStorage.setItem("shipsDirection", "default");

  const boardSvgElement = document.querySelector("#board");
  boardSvgElement.innerHTML = board;

  // Prepare event listener for each cell
  for (const id of boardCellIds) {
    const cellSvgElement = document.querySelector(`#${id}`);

    cellSvgElement.addEventListener("mouseenter", (e) => {
      if (selectedShip) {
        e.target.style.fill = "red";
      }
    });

    cellSvgElement.addEventListener("mouseleave", (e) => {
      e.target.style.fill = "#0077be";
    });
  }
}

/**
 * Prepares the ships for the game
 * @param {String} type
 * @param {String} direction
 * @param {Number} defaultX
 * @param {Number} defaultY
 */
function drawShip(type, direction, defaultX, defaultY) {
  let ship = "",
    x,
    y;

  const shipData = SHIPS[type];

  for (let i = 0; i < shipData.size; i++) {
    if (direction === "default") {
      x = 50 * i + 900;
      y = defaultY;
    } else if (direction === "rotated") {
      x = defaultX;
      y = 50 * i + 25;
    }

    ship += `<rect x="${x}" 
                    y="${y}"
                    width="50"
                    height="50"
                    stroke-width="5"
                    stroke="black"
                    fill="${shipData.color}" />`;
  }

  const shipSvgElement = document.querySelector(`#${type}`);
  shipSvgElement.innerHTML = ship;

  shipSvgElement.addEventListener("click", () => {
    selectedShip = shipSvgElement;
    selectedShipType = type;

    document.querySelector(
      "#selected-piece"
    ).innerHTML = `Currently selected ${type}`;
  });
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
    drawShip(CARRIER, "default", null, 25);
    drawShip(BATTLESHIP, "default", null, 125);
    drawShip(DESTROYER, "default", null, 225);
    drawShip(SUBMARINE, "default", null, 325);
    drawShip(BOAT, "default", null, 425);
  } else if (direction === "rotated") {
    drawShip(CARRIER, "rotated", 900, null);
    drawShip(BATTLESHIP, "rotated", 1000, null);
    drawShip(DESTROYER, "rotated", 1100, null);
    drawShip(SUBMARINE, "rotated", 1200, null);
    drawShip(BOAT, "rotated", 1300, null);
  }
}
