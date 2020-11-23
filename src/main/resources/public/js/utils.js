/** The following are cookie related methods **/
function getCookie(name) {
  const v = document.cookie.match("(^|;) ?" + name + "=([^;]*)(;|$)");
  return v ? v[2] : null;
}

function setCookie(name, value, days) {
  const d = new Date();
  d.setTime(d.getTime() + 24 * 60 * 60 * 1000 * days);
  document.cookie =
    name + "=" + value + ";path=/;expires=" + d.toGMTString() + ";secure;";
}

function deleteCookie(name) {
  setCookie(name, "", -1);
}

/**
 * Show the alert modal with the text
 *
 * @param {String} text
 */
function showAlertModal(text) {
  document.querySelector("#alert-modal-body").innerHTML = text;
  $("#alert-modal").modal({
    backdrop: "static",
    keyboard: false,
  });
}

/**
 * Creates a chat message HTML element
 *
 * @param {Object} data
 */
function createMessageElement(data) {
  const messageElement = document.createElement("p");
  messageElement.innerHTML = data.message;

  if (data.type === MESSAGE_TYPE_SYSTEM) {
    messageElement.style.color = "#343a40";
    messageElement.style.fontWeight = "bold";
  } else {
    messageElement.style.color = data.color;
  }

  return messageElement;
}
