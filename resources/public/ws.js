const ws = new WebSocket("ws://localhost:8890/ws");

ws.onopen = (event) => {
    ws.send("Connected to the server");
}

ws.onmessage = (event) => {
    console.log("Event from server!", event.data);
    const msg = JSON.parse(event.data);
}
