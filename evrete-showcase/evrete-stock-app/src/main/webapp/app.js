let SOCKET;
// Page initialization
if ('WebSocket' in window || 'MozWebSocket' in window) {
    // Position delay value label
    positionDelayRange();

    // Create websocket connection
    SOCKET = _createWebSocket();

    // The rest will be initialized upon socket events
} else {
    $("body").html('<div class="cell small-12 callout alert">Web Sockets are not supported by your browser.</div>');
    throw new Error('WebSocket is not supported');
}

const EDITOR_RULES = _createEditor('rule-editor', 'ace/mode/java');
const EDITOR_DATA = _createEditor('stocks-editor', 'ace/mode/json');
const ELEMENT_LOG = $('#logs');
const ELEMENT_MONITOR = $('#rule-monitor');
const SVG_OHLC = new SvgOHLC();


// This will hold the price feed state
let iterationState = {
    currentIndex: 0,
    prices: []
}

function onMessage(evt) {
    let msg = JSON.parse(evt.data);
    switch (msg['type']) {
        case 'CONFIG':
            // Initial greeting with default rule and price history
            iterationState.prices = msg['prices'];
            // Init rule editor
            EDITOR_RULES.setValue(msg['rules'], -1);
            // Init stocks editor
            EDITOR_DATA.setValue(JSON.stringify(iterationState.prices, null, 2), -1);
            // Draw stock history
            SVG_OHLC.init(iterationState.prices);
            setControls([true, false]);
            ELEMENT_LOG.append('<li>Default rules and data received from the server</li>');

            break;
        case 'LOG':
            ELEMENT_LOG.append('<li>' + msg.text + '</li>');
            break
        case 'RULE_COMPILED':
            ELEMENT_LOG.append('<li>\'' + msg.text + '\' compiled.</li>');
            ELEMENT_MONITOR.append('<li data-rule-name="' + msg.text + '">' + msg.text + '</li>');
            break
        case 'RULE_EXECUTED':
            highlightCurrentRule(msg.text);
            break
        case 'ERROR':
            ELEMENT_LOG.append('<li class="ERROR">Error: <pre>' + msg.text + '</pre></li>');
            setControls([true, false]);
            break;
        case 'READY_FOR_DATA':
            //Clear paths
            SVG_OHLC.clearPathsGroup();
            // Feeding the prices
            iterationState.currentIndex = 0;
            ELEMENT_LOG.append('<li>Rules compiled, feeding prices...</li>');
            sendNextOHLC();
            break;
        case 'STOPPED':
            sessionEnd();
            break;
        case 'OHLC_INSERTED':
            // Draw the modified price data
            SVG_OHLC.nextEntry(JSON.parse(msg.text));
            if (!sendNextOHLC()) {
                sessionEnd();
            }
            break;
    }
}

function sessionEnd() {
    setControls([true, false]);
    ELEMENT_LOG.append('<li>Session ended.</li>');
    const idx = iterationState.currentIndex;
    if (idx === iterationState.prices.length) {
        // The last index, meaning session hasn't been interrupted
        SVG_OHLC.hideShades();
        highlightCurrentRule();
    }
}

function highlightCurrentRule(rule) {
    const currentClass = 'current';
    // noinspection JSUnresolvedFunction
    ELEMENT_MONITOR.children('li').each(function (index, element) {
        let ruleName = element.getAttribute('data-rule-name');
        if (rule && ruleName === rule) {
            element.classList.add(currentClass);
        } else {
            element.classList.remove(currentClass);
        }
    });
}

function setControls(arr) {
    $('#run-button').prop('disabled', !arr[0]);
    $('#stop-button').prop('disabled', !arr[1]);
}


function positionDelayRange() {
    const delayEl = $('#delay');
    const delayValueEl = $('#delay-value');
    const currentValue = Number.parseInt(delayEl.val(), 10);
    const maxValue = Number.parseInt(delayEl.attr('max'), 10);

    delayValueEl.text(currentValue + 'ms');
    let padding = 8 + (currentValue * (delayEl.width() - 16) / maxValue) - (delayValueEl.width() / 2);
    padding = Math.max(padding, 0);
    padding = Math.min(padding, delayEl.width() - delayValueEl.width() - 4);
    delayValueEl.css('margin-left', padding.toString() + 'px');
}

function onNewDataCategory(name, color) {
    $('#legend').append('<li><span class="l" style="color:' + color + ';">&#x2501;&#x2501;</span><span class="n">' + name + '</span></li>');
}

function sendNextOHLC() {
    const idx = iterationState.currentIndex;
    if (idx === iterationState.prices.length) return false;

    let ohlc = iterationState.prices[idx];
    if (!ohlc) {
        throw new Error();
    }
    let m = {
        'type': 'OHLC',
        'ohlc': ohlc
    };
    SOCKET.send(JSON.stringify(m));
    iterationState.currentIndex = idx + 1;
    return true;

}

function stopSession() {
    let m = {
        'type': 'STOP'
    };
    SOCKET.send(JSON.stringify(m));
    ELEMENT_LOG.append('<li>STOP signal sent</li>');
}

function runSession() {
    // Clearing the logs
    $('.clearable').empty();

    setControls([false, true]);

    // Sending to the server current rules for compilation
    SOCKET.send(JSON.stringify({
        type: 'RUN_COMMAND',
        rules: EDITOR_RULES.getValue(),
        delay: $('#delay').val()
    }));
}

function togglePriceEditor() {
    let e = $('#prices-toggle');
    e.toggleClass('editor-hidden');
    $('#rules-button').prop('disabled', !e.hasClass('editor-hidden'));
}

function toggleRulesEditor() {
    let e = $('#rules-toggle');
    e.toggleClass('editor-hidden');
    $('#data-button').prop('disabled', !e.hasClass('editor-hidden'));
}

function saveStockData() {
    let prices = JSON.parse(EDITOR_DATA.getValue());
    iterationState.prices = prices;
    SVG_OHLC.clearPathsGroup();
    SVG_OHLC.init(prices);
    return false;
}

function _createWebSocket() {
    let url = window.location.href + 'ws/socket';
    url = url.replace('http', 'ws');
    const webSocket = new WebSocket(url, ['protocolOne', 'protocolTwo']);
    webSocket.onmessage = onMessage;
    webSocket.onerror = function (event) {
        ELEMENT_LOG.append('<li class="ERROR">Error: <pre>' + event.toString() + '</pre></li>');
        sessionEnd();
    };

    webSocket.onclose = function (event) {
        ELEMENT_LOG.append('<li>Connection closed, please reload the page.</li>');
        sessionEnd();
        setControls([false, false]);
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
        //fontSize: 16,
        theme: 'ace/theme/xcode'
    })
}