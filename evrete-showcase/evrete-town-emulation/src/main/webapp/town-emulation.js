const SVG_NS = "http://www.w3.org/2000/svg";
const SVG = document.getElementById('map-svg');
const IMG_SIZE = 2048;
const SVG_DUMMY_POINT = SVG.createSVGPoint();
const MAX_ZOOM = 3;
//const CONFIG_EDITOR = _createEditor('xml-editor', 'ace/mode/xml');
const VIEW = {
    center: {
        x: IMG_SIZE / 2,
        y: IMG_SIZE / 2
    },

    zoom: 0
}
const ELEMENT_LOG = $('#logs');


if ('WebSocket' in window || 'MozWebSocket' in window) {

    // Create websocket connection
    SOCKET = _createWebSocket();

    // The rest will be initialized upon socket events
} else {
    $("body").html('<div class="cell small-12 callout alert">Web Sockets are not supported by your browser.</div>');
    throw new Error('WebSocket is not supported');
}

_updateView();

SVG.onclick = function (ev) {
    SVG_DUMMY_POINT.x = ev.clientX;
    SVG_DUMMY_POINT.y = ev.clientY;

    let converted = SVG_DUMMY_POINT.matrixTransform(SVG.getScreenCTM().inverse());


    onImageClick(Math.round(converted.x), Math.round(converted.y));

}

function onImageClick(x, y) {
    VIEW.center = {
        x: x,
        y: y
    }
    _updateView();
    sendViewport();
}

function sessionStart() {
    $('.clearable').empty();
    SOCKET.send(JSON.stringify({
        type: 'START',
        //config: CONFIG_EDITOR.getValue(),
        interval: $('#interval').val()
    }));
    $('#start-button').prop('disabled', true);
    $('#stop-button').prop('disabled', false);
}

function sessionStop() {
    SOCKET.send(JSON.stringify({
        type: 'STOP',
    }));
}

function zoomIn() {
    _zoom(1);
}

function zoomOut() {
    _zoom(-1);
}

function _zoom(delta) {
    let currentZoom = VIEW.zoom;
    if (delta < 0 && currentZoom === 0) {
        return;
    }
    if (delta > 0 && currentZoom === MAX_ZOOM) {
        return;
    }
    VIEW.zoom = currentZoom + delta;
    _updateView();
    sendViewport();
}

function _updateView() {
    let factor = 1 << VIEW.zoom;


    let halfSize = Math.round(0.5 * IMG_SIZE / factor);
    let size = halfSize * 2;
    let imgX = Math.round(VIEW.center.x - halfSize);
    let imgY = Math.round(VIEW.center.y - halfSize);

    if (imgX < 0) {
        VIEW.center.x = halfSize;
        imgX = 0;
    }

    if (imgX + size > IMG_SIZE) {
        VIEW.center.x = IMG_SIZE - halfSize;
        imgX = IMG_SIZE - size;
    }

    if (imgY < 0) {
        VIEW.center.y = halfSize;
        imgY = 0;
    }

    if (imgY + size > IMG_SIZE) {
        VIEW.center.y = IMG_SIZE - halfSize;
        imgY = IMG_SIZE - size;
    }

    SVG.viewBox.baseVal.width = size;
    SVG.viewBox.baseVal.height = size;
    SVG.viewBox.baseVal.x = imgX;
    SVG.viewBox.baseVal.y = imgY;
}

function onMessage(evt) {
    let msg = JSON.parse(evt.data);
    const startButton = $('#start-button');
    const stopButton = $('#stop-button');

    switch (msg['type']) {
        case 'CONFIG':
            //CONFIG_EDITOR.setValue(msg['xml'], -1);
            break;
        case 'ERROR':
            ELEMENT_LOG.append('<li class="ERROR">Error: <pre>' + msg.text + '</pre></li>');
            startButton.prop('disabled', false);
            stopButton.prop('disabled', true);
            break;
        case 'LOG':
            ELEMENT_LOG.append('<li>' + msg.text + '</li>');
            break;
        case 'END':
            startButton.prop('disabled', false);
            stopButton.prop('disabled', true);
            break;
        case 'STATE':
            _drawState(msg);
            break;
    }
}

function _drawState(state) {
    const layersGroup = document.getElementById('svg-layers');
    if (state['reset']) {
        // Clearing all layers
        while (layersGroup.lastChild) {
            layersGroup.removeChild(layersGroup.lastChild);
        }
    }

    drawSummary(state['total'], state['time'])

    let cellSize = state['cellSize'];
    // clear current status
    let layers = state['layers'];
    for (const key in layers) {
        if (layers.hasOwnProperty(key)) {
            let svgLayer = document.getElementById(key);

            if (!svgLayer) {
                // Create a new one
                svgLayer = document.createElementNS(SVG_NS, 'g');
                svgLayer.setAttribute('id', key);
                layersGroup.appendChild(svgLayer);
            }

            let cells = layers[key];
            for (let i = 0; i < cells.length; i++) {
                const cell = cells[i];
                const cellId = cell['id'];
                let rect = document.getElementById(cellId);
                if (!rect) {
                    rect = document.createElementNS(SVG_NS, 'rect');
                    rect.setAttribute('id', cellId);
                    rect.setAttribute("x", cell.x);
                    rect.setAttribute("y", cell.y);
                    rect.setAttribute("width", cellSize);
                    rect.setAttribute("height", cellSize);
                    svgLayer.appendChild(rect);
                }
                rect.setAttribute("fill-opacity", cell.opacity);
            }
        }
    }
}


function sendViewport() {
    SOCKET.send(JSON.stringify(
        {
            type: 'VIEWPORT',
            x: SVG.viewBox.baseVal.x,
            y: SVG.viewBox.baseVal.y,
            zoom: VIEW.zoom
        }
    ));
}

function drawSummary(data, time) {
    const pieGroup = document.getElementById('chart-pie');
    const legendGroup = document.getElementById('chart-legend');

    while (pieGroup.lastChild) {
        pieGroup.removeChild(pieGroup.lastChild);
    }
    while (legendGroup.lastChild) {
        legendGroup.removeChild(legendGroup.lastChild);
    }

    // Set time
    document.getElementById('chart-time').innerHTML = time;

    const radius = 50;
    let currentAngle = Math.PI / 2;
    let currentX = 0;
    let currentY = -radius;
    let legendY = 15;
    for (const key in data) {
        if (data.hasOwnProperty(key)) {
            const val = data[key];
            const angle = 2.0 * Math.PI * val;
            const largeArc = angle > Math.PI ? ' 1 ' : ' 0 ';

            let path = 'M 0 0 ' + currentX + ' ' + currentY + ' A ' + radius + ' ' + radius + ' 0 ' + largeArc + ' 1 ';
            currentAngle = currentAngle - angle;
            currentX = radius * Math.cos(currentAngle);
            currentY = -radius * Math.sin(currentAngle);
            path = path + ' ' + currentX + ' ' + currentY + ' Z';
            const pieSector = document.createElementNS(SVG_NS, 'path');
            pieSector.setAttribute('d', path);
            pieSector.setAttribute('class', key);
            pieGroup.appendChild(pieSector);

            const legendRect = document.createElementNS(SVG_NS, 'rect');
            legendRect.setAttribute("class", key);
            legendRect.setAttribute("width", "10");
            legendRect.setAttribute("height", "16");
            legendRect.setAttribute("y", legendY.toString());
            legendRect.setAttribute("x", "0");

            legendGroup.appendChild(legendRect);


            const legendText = document.createElementNS(SVG_NS, 'text');
            legendText.setAttribute("class", key);
            legendText.setAttribute("y", (legendY + 14).toString());
            legendText.setAttribute("x", "16");
            legendText.innerHTML = key + ' - ' + Math.round(val * 100) + '%';
            legendGroup.appendChild(legendText);

            legendY += 20;

        }
    }
}


function _createWebSocket() {
    let url = window.location.href.replace(/[^/]*$/, '') + 'ws/socket';
    url = url.replace('http', 'ws');
    const webSocket = new WebSocket(url);
    webSocket.onmessage = onMessage;
    webSocket.onerror = function () {
        ELEMENT_LOG.append('<li class="ERROR">Network error</li>');
    };

    webSocket.onclose = function (event) {
        ELEMENT_LOG.append('<li>Connection closed, reason: "' + event.reason + '". Please reload the page.</li>');
    };

    webSocket.onopen = function () {
        sendViewport();

    }

    // Optional. Some browsers (or networks) may not support ping/pong messaging,
    // so we make sure our socket keeps the connection alive
    const pingMessage = JSON.stringify({
        type: 'PING',
        payload: 'I am alive!'
    });
    setInterval(function () {
        if (webSocket.readyState === webSocket.OPEN) {
            webSocket.send(pingMessage);
        }
    }, 19391);
    return webSocket;
}
