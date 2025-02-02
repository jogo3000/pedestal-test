const KEEPALIVE_RATE = 10000;
const ws = new WebSocket("ws://localhost:8890/ws");
let timeoutId = null;


const keepalive = () => {
    if (timeoutId) {
        console.log("Sending ping");
        ws.send("ping");
        timeoutId = setTimeout(keepalive, KEEPALIVE_RATE);
    }
}

ws.onopen = (event) => {
    ws.send("Connected to the server");

    timeoutId = setTimeout(keepalive, KEEPALIVE_RATE);
}

ws.onclose = (event) => {
    clearTimeout(timeoutId);
    timeoutId = null;
}


ws.onmessage = (event) => {
    console.log("Event from server!", event.data);
    const msg = JSON.parse(event.data);

    switch (msg.type) {
    case "REPLACE":
        const el = document.getElementById(msg.id)
        el.setHTMLUnsafe(msg.html)
        break;
    default:
        console.log(`Unknown message type ${msg.type}`)
    }
}
