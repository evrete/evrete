let SOCKET;
// Page initialization
if ('WebSocket' in window || 'MozWebSocket' in window) {

    // Create websocket connection
    SOCKET = _createWebSocket();

    // The rest will be initialized upon socket events
} else {
    $("body").html('<div class="cell small-12 callout alert">Web Sockets are not supported by your browser.</div>');
    throw new Error('WebSocket is not supported');
}
const SVG_NS = "http://www.w3.org/2000/svg";
const SVG = document.getElementById('plot');

const ELEMENT_LOG = $('#logs');


let tmp = 0;

function _onBoardClick(x, y, cell) {
    console.log(x, y, cell);
    if (cell['queen'] || cell['hits'] === 0) {
        // Place a new queen or remove existing one
        SOCKET.send(JSON.stringify({
            'type': 'TOGGLE',
            'x': x,
            'y': y
        }));
    }
}


function onUiReset() {
    SOCKET.send(JSON.stringify({
        'type': 'RESET',
        'text': $('#board-size').val()
    }));
}

function onUiRun() {
    $('#button-play').prop('disabled', true);
    let m = {
        'type': 'RUN'
    };
    tmp = 0;
    SOCKET.send(JSON.stringify(m));
}

function onMessage(evt) {
    let msg = JSON.parse(evt.data);
    //console.log(msg);
    let playButton = $('#button-play');
    switch (msg['type']) {
        case 'BOARD':
            _drawBoard(SVG, msg['board']);
            playButton.prop('disabled', false);
            break;
        case 'FOUND':
            playButton.prop('disabled', false);
            tmp++;
            break;
        case 'STOPPED':
            playButton.prop('disabled', false);
            console.log("Found", tmp)
            break;
        case 'ERROR':
            ELEMENT_LOG.append('<li class="ERROR">' + msg.text + '</li>');
            break;
    }
}

function _drawBoard(svg, board) {
    const size = board.cells.length;
    $('#board-size').val(size);

    // Clean the board
    $(svg).find('g').remove();


    let surface = document.createElementNS(SVG_NS, 'g');
    let pieces = document.createElementNS(SVG_NS, 'g');

    const svgSize = Number.parseInt(svg.getAttribute('width'));
    let step = svgSize / size;

    for (let x = 0; x < size; x++) {
        for (let y = 0; y < size; y++) {
            const cell = board.cells[x][y];

            const clickFunction = function () {
                _onBoardClick(x, y, cell);
            }

            let dark = (x + y) % 2 === 0;
            let rect = document.createElementNS(SVG_NS, "rect");

            let styleClass = dark ? 'cell-dark' : 'cell-white';

            if (cell['hits'] > 0) {
                styleClass = styleClass + ' unavailable';
            }

            let svgX = x * step;
            // Y axis is inverted in SVG
            let svgY = (size - 1 - y) * step;

            rect.setAttribute('class', styleClass);
            rect.setAttribute('x', svgX.toString());
            rect.setAttribute('y', svgY.toString());
            rect.setAttribute('width', step.toString());
            rect.setAttribute('height', step.toString());
            rect.onclick = clickFunction;
            surface.appendChild(rect);

            // Draw queen if present
            if (cell['queen']) {
                let queen = document.createElementNS(SVG_NS, "use");
                queen.setAttribute('href', '#queen');


                let scaledWidth = 0.618034 * step;
                let pad = (step - scaledWidth) / 2;
                let qx = svgX + pad;
                let qy = svgY + pad;
                queen.setAttribute('width', scaledWidth.toString());
                queen.setAttribute('height', scaledWidth.toString());
                queen.setAttribute('x', qx.toString());
                queen.setAttribute('y', qy.toString());
                queen.setAttribute('class', 'queen');
                queen.onclick = clickFunction;
                pieces.appendChild(queen);

            }
        }
    }
    svg.appendChild(surface);
    svg.appendChild(pieces);


}

function _createWebSocket() {
    let url = window.location.href.replace(/[^/]*$/, '') + 'ws/socket';
    url = url.replace('http', 'ws');
    const webSocket = new WebSocket(url, ['protocolOne', 'protocolTwo']);
    webSocket.onmessage = onMessage;
    webSocket.onerror = function () {
        ELEMENT_LOG.append('<li class="ERROR">Network error</li>');
    };

    webSocket.onclose = function (event) {
        ELEMENT_LOG.append('<li>Connection closed, reason: "' + event.reason + '". Please reload the page.</li>');
    };

    // Optional. Some browsers (or networks) may not support ping/pong messaging,
    // so we make sure our socket keeps the connection alive
    setInterval(function () {
        if (webSocket.OPEN) {
            let ping = {
                type: 'PING',
                payload: 'I am alive!'
            };

            webSocket.send(JSON.stringify(ping));
        }
    }, 19391);
    return webSocket;
}


