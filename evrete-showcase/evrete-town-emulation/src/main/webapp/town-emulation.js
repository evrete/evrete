const SVG_NS = "http://www.w3.org/2000/svg";
const SVG = document.getElementById('svg');
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

const TMP_LOCATIONS = [];

SVG.onclick = function (ev) {
    SVG_DUMMY_POINT.x = ev.clientX;
    SVG_DUMMY_POINT.y = ev.clientY;

    let converted = SVG_DUMMY_POINT.matrixTransform(SVG.getScreenCTM().inverse());


    onImageClick(Math.round(converted.x), Math.round(converted.y));

}

function onImageClick(x, y) {

    const mode = _tmpMode();
    switch (mode) {
        case 'normal':
            VIEW.center = {
                x: x,
                y: y
            }
            _updateView();
            sendViewport();
            break;
        case 'add_place':
            TMP_LOCATIONS.push({
                x: x,
                y: y
            });
            $('#tmp_locations').text(JSON.stringify(TMP_LOCATIONS));
            addHome("businesses", x, y);
            break;
    }

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
        case 'MAP_DATA':
            let points = msg['points'];
            for (let i = 0; i < points.length; i++) {
                addHome(msg['category'], points[i].x, points[i].y);
            }
            break;

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
    if (state['full']) {
        // Clearing all layers
        while (layersGroup.lastChild) {
            layersGroup.removeChild(layersGroup.lastChild);
        }
    }

    $('#world-time').text(state['time']);

    let cellSize = state['cellSize'];
    // clear current status
    let layers = state['layers'];
    for (const key in layers) {
        if (layers.hasOwnProperty(key)) {
            let svgLayer = document.getElementById(key);
            const maxCount = state['maxCounts'][key];

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
                const count = cell.count;
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
                const opacity = count === 0 ? 0 : 0.4 * (count / maxCount);
                rect.setAttribute("fill-opacity", opacity.toString());
            }
        }
    }
}

function _tmpMode() {
    const radios = document.getElementsByName('tmp_mode');

    for (let i = 0, length = radios.length; i < length; i++) {
        if (radios[i].checked) {
            return radios[i].value;
        }
    }
    return null;
}

function addHome(group, x, y) {
    let homesUiGroup = document.getElementById(group);
    const element = document.createElementNS(SVG_NS, 'circle');
    element.setAttributeNS(null, 'cx', x);
    element.setAttributeNS(null, 'cy', y);
    element.setAttributeNS(null, 'r', "4");
    homesUiGroup.appendChild(element);
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

/*
function _createEditor(element, mode) {
    return ace.edit(element, {
        mode: mode,
        maxLines: 30,
        wrap: true,
        autoScrollEditorIntoView: true,
        showPrintMargin: false,
        vScrollBarAlwaysVisible: true,
        minLines: 8,
        scrollPastEnd: 0.5,
        theme: 'ace/theme/xcode'
    })
}*/
