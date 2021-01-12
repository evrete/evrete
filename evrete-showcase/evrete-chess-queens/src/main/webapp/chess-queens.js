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
const SVG = document.getElementById('main-board');

const LOG = $('#logs');

const BUTTON_PLAY = $('#button-play');
const BUTTON_RESET = $('#button-reset');

function _onBoardClick(x, y, cell) {
    if (BUTTON_PLAY.prop('disabled')) {
        // A task is already running
        return;
    }

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
    $('#solutions').empty();
    LOG.empty();
}

function onUiRun() {
    BUTTON_PLAY.prop('disabled', true);
    BUTTON_RESET.prop('disabled', true);
    LOG.empty();
    let m = {
        'type': 'RUN'
    };
    $('#solutions').empty();
    SOCKET.send(JSON.stringify(m));
}

function onMessage(evt) {
    let msg = JSON.parse(evt.data);
    switch (msg['type']) {
        case 'BOARD':
            let board = msg['board'];
            _drawBoard(SVG, board, true);
            const size = board.cells.length;
            $('#board-size').val(size);
            BUTTON_PLAY.prop('disabled', false);
            BUTTON_RESET.prop('disabled', false);
            break;
        case 'SOLUTION':
            _drawSolution(msg['id'], msg['board'])
            break;
        case 'STOPPED':
            BUTTON_PLAY.prop('disabled', false);
            BUTTON_RESET.prop('disabled', false);
            break;
        case 'ERROR':
            LOG.append('<li class="ERROR">' + msg.text + '</li>');
            break;
        case 'INFO':
            LOG.append('<li>' + msg.text + '</li>');
            break;
        case 'PONG':
            // Ping response, doing nothing
            break;
        default:
            LOG.append('<li  class="ERROR">Unknown message <pre>' + evt.data + '</pre></li>');
    }
}


function _drawSolution(id, board) {
    let svgId = 'solution' + id;
    $('#solutions').append('<div class="cell small-3 medium-3"><div><svg id="' + svgId + '" class="solution" width="100" height="100" viewBox="0 0 100 100" preserveAspectRatio="none"></svg></div></div>');

    let svg = document.getElementById(svgId);
    _drawBoard(svg, board, false);
}


function _drawBoard(svg, board, listener) {

    // Clean the board
    $(svg).find('g').remove();


    let surface = document.createElementNS(SVG_NS, 'g');
    let pieces = document.createElementNS(SVG_NS, 'g');

    const svgSize = Number.parseInt(svg.getAttribute('width'));
    const size = board.cells.length;
    let step = svgSize / size;

    for (let col of board.cells) {
        for (let cell of col) {
            let x = cell.x;
            let y = cell.y;

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
            if (listener) {
                rect.onclick = clickFunction;
            }
            surface.appendChild(rect);

            // Draw queen if present
            if (cell['queen']) {
                let queen = document.createElementNS(SVG_NS, "use");
                queen.setAttribute('href', '#queen');

                let styleClass = dark ? 'queen queen-on-dark' : 'queen queen-on-white';

                let scaledWidth = 0.618034 * step;
                let pad = (step - scaledWidth) / 2;
                let qx = svgX + pad;
                let qy = svgY + pad;
                queen.setAttribute('width', scaledWidth.toString());
                queen.setAttribute('height', scaledWidth.toString());
                queen.setAttribute('x', qx.toString());
                queen.setAttribute('y', qy.toString());
                queen.setAttribute('class', styleClass);
                if (listener) {
                    queen.onclick = clickFunction;
                }
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
    const webSocket = new WebSocket(url);
    webSocket.onmessage = onMessage;
    webSocket.onerror = function () {
        LOG.append('<li class="ERROR">Network error</li>');
    };

    webSocket.onclose = function (event) {
        LOG.append('<li>Connection closed, reason: "' + event.reason + '". Please reload the page.</li>');
    };

    // Optional. Some browsers (or networks) may not support ping/pong messaging,
    // so we make sure our socket keeps the connection alive
    setInterval(function () {
        if (webSocket.readyState === webSocket.OPEN) {
            let ping = {
                type: 'PING',
                payload: 'I am alive!'
            };

            webSocket.send(JSON.stringify(ping));
        }
    }, 19391);
    return webSocket;
}


