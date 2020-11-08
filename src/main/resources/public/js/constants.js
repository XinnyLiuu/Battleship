const MESSAGE_TYPE_CHAT = "CHAT";
const MESSAGE_TYPE_SYSTEM = "SYSTEM";

const MESSAGE_TYPE_CHALLENGE = "CHALLENGE";
const MESSAGE_TYPE_ACCEPT = "ACCEPT";
const MESSAGE_TYPE_DECLINE = "DECLINE";

const WAITING_ROOM_WEBSOCKET_URL = `ws://${location.hostname}:${location.port}/waiting-room`;
const GAME_ROOM_WEBSOCKET_URL = `ws://${location.hostname}:${location.port}/game-room`;