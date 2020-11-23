const MESSAGE_TYPE_CHAT = "CHAT";
const MESSAGE_TYPE_SYSTEM = "SYSTEM";

const MESSAGE_TYPE_CHALLENGE = "CHALLENGE";
const MESSAGE_TYPE_ACCEPT = "ACCEPT";
const MESSAGE_TYPE_DECLINE = "DECLINE";

const MESSAGE_TYPE_END = "END";

const COOKIE_USERNAME = "username";
const COOKIE_CHAT_COLOR = "chatColor";
const COOKIE_GAME_ROOM_ID = "gameRoomId";

const WAITING_ROOM_WEBSOCKET_URL = `ws://${location.hostname}:${location.port}/waiting-room`;
const GAME_ROOM_WEBSOCKET_URL = `ws://${location.hostname}:${location.port}/game-room`;

const CARRIER = "carrier";
const BATTLESHIP = "battleship";
const DESTROYER = "destroyer";
const SUBMARINE = "submarine";
const BOAT = "boat";