const SVG_HEIGHT = 320;
const SVG_WIDTH = 640;
const paddingL = 32;
const paddingR = 1;
const paddingT = 1;
const paddingB = 5;
const MAX_LENGTH = 256;
const PALETTE = [
    '#2593BB',
    '#FE990B',
    '#5448C8',
    '#B6863E',
    '#6C5E6E',
    '#9F4146',
    '#6D667A',
    '#FF8247'
];


const NS = "http://www.w3.org/2000/svg";

class SvgOHLC {

    constructor() {
        //this.svg = document.getElementById(element);
        // OHLC Group
        this.ohlcGroup = document.getElementById('ohlc');
        // Paths
        this.pathsGroup = document.getElementById('paths');

        // Coordinate grid
        this.grid = document.getElementById('grid');

        this.lines = {};
        this.mapping = {};
        this.outliers = {};

        this.shades = document.getElementById('shades');
    }

    init(data) {
        // Calculate plot parameters
        let minPrice = Number.MAX_VALUE;
        let maxPrice = Number.MIN_VALUE;
        this.mapping.xDelta = (SVG_WIDTH - paddingL - paddingR) / (Math.min(data.length, MAX_LENGTH) - 1);
        for (let i = 0; i < data.length && i < MAX_LENGTH; i++) {
            let ohlc = data[i];
            minPrice = Math.min(minPrice, ohlc.low);
            maxPrice = Math.max(maxPrice, ohlc.high);
        }

        this.mapping.count = data.length;
        this.mapping.minPrice = minPrice;
        this.mapping.maxPrice = maxPrice;
        this.mapping.denominatorY =
            ((maxPrice - minPrice) * SVG_HEIGHT) / (SVG_HEIGHT - paddingT - paddingB);


        this.clearOhlcGroup();
        this.hideShades();
        this.drawGrid();

        for (let i = 0; i < data.length && i < MAX_LENGTH; i++) {
            let x = this.mapX(i);
            let ohlc = data[i];
            let h = this.mapY(ohlc["high"]);
            let l = this.mapY(ohlc["low"]);
            let o = this.mapY(ohlc["open"]);
            let c = this.mapY(ohlc["close"]);

            let hlLine = newSvgLine(x, h, x, l, "ohlc-hl");
            this.ohlcGroup.appendChild(hlLine);

            // Open/Close line
            let styleClass = o < c ? "ohlc-short" : "ohlc-long";
            let ocLine = newSvgLine(x, o, x, c, styleClass, this.mapping.xDelta * .75);
            this.ohlcGroup.appendChild(ocLine);
        }
    }

    clearOhlcGroup() {
        while (this.ohlcGroup.lastChild) {
            this.ohlcGroup.removeChild(this.ohlcGroup.lastChild);
        }
    }

    hideShades() {
        this.shades.setAttribute('x', (SVG_WIDTH * 2.0).toString());
    }

    clearPathsGroup() {
        while (this.pathsGroup.lastChild) {
            this.pathsGroup.removeChild(this.pathsGroup.lastChild);
        }
        this.lines = {};
        this.outliers = {};
    }

    nextEntry(entry) {
        let idx = entry.id;
        let shadesX = this.mapX(idx);
        this.shades.setAttribute('x', shadesX.toString());
        let data = entry.data;
        if (data) {
            for (let key in data) {
                if (data.hasOwnProperty(key)) {
                    let val = data[key];
                    if (isNaN(val)) {
                        // The value is assumed of type String

                        let close = this.mapY(entry.close);
                        let top = close > (SVG_HEIGHT / 2);
                        this.drawMarker(key, idx, val, top);
                    } else {
                        // The value is a Number
                        this.drawLine(key, idx, val, onNewDataCategory);
                    }
                }
            }
        }
    }

    drawLine(name, id, value, legendListener) {
        // Map value to plot coordinates
        let x = this.mapX(id);
        let y = this.mapY(value);
        if (x >= 0 && x <= SVG_WIDTH && y >= 0 && y <= SVG_HEIGHT) {
            // A valid line point, checking if this line had outliers before
            if (this.outliers[name]) {
                return;
            }
        } else {
            // An outlier, we wont draw it, and mark it for the future
            this.outliers[name] = true;
            return;
        }

        let lineData = this.lines[name];
        if (!lineData) {
            let totalLines = Object.keys(this.lines).length;

            let color = PALETTE[totalLines % PALETTE.length];
            if (legendListener) {
                legendListener(name, color);
            }
            let path = newSvgPath(name, color);
            this.pathsGroup.appendChild(path);

            lineData = {
                'path': path,
                'points': []
            }
            this.lines[name] = lineData;
        }
        let newPoint = [];
        newPoint[0] = x;
        newPoint[1] = y;
        lineData.points.push(newPoint);
        lineData.path.setAttribute('d', svgPathRender(lineData.points));
    }

    drawMarker(key, id, value, top) {
        let x = this.mapX(id);

        let color;
        if (!this.lines[key]) {
            let totalLines = Object.keys(this.lines).length;
            color = PALETTE[totalLines % PALETTE.length];
            this.lines[key] = {'color': color};
        } else {
            color = this.lines[key].color;
        }

        let line = newSvgLine(x, 0, x, SVG_HEIGHT, "marker");
        line.setAttribute("stroke", color);
        this.pathsGroup.appendChild(line);

        let label = newSvgText(value);
        label.setAttribute('x', 0);
        label.setAttribute('y', 0);
        let y = top ? 20 : SVG_HEIGHT - 20;

        label.setAttribute('class', top ? 'label top' : 'label bottom');
        label.setAttribute('transform', 'translate(' + (x - 3) + ',' + y + ') rotate(270)');
        this.pathsGroup.appendChild(label);

    }

    mapY(y) {
        return paddingT + (SVG_HEIGHT * (this.mapping.maxPrice - y)) / this.mapping.denominatorY;
    }

    mapX(id) {
        return paddingL + id * this.mapping.xDelta;
    }

    drawGrid() {
        // Clear existing elements
        while (this.grid.lastChild) {
            this.grid.removeChild(this.grid.lastChild);
        }

        // X axis grid
        let idx = 0;
        let x = this.mapX(idx);
        while (x < SVG_WIDTH) {
            let xTick = use('x-grid-line', this.grid);
            xTick.setAttribute('x', x);


            let xLabel = newSvgText(idx.toString());
            xLabel.setAttribute('class', 'x-label');
            xLabel.setAttribute('x', x);
            xLabel.setAttribute('y', (SVG_HEIGHT - 5).toString());
            this.grid.appendChild(xLabel);
            idx += 10;
            x = this.mapX(idx);
        }

        // Y axis grid
        let diff = this.mapping.maxPrice - this.mapping.minPrice;
        let yCount = 10;

        let log = Math.log10(diff / yCount);
        let options = [Math.floor(log), Math.ceil(log)];
        options.sort(function (o1, o2) {
            let n1 = Math.pow(10, o1);
            let n2 = Math.pow(10, o2);
            let cnt1 = Math.abs(Math.round(diff / n1) - yCount);
            let cnt2 = Math.abs(Math.round(diff / n2) - yCount);
            return cnt2 < cnt1 ? 1 : -1;
        })
        let selectedPow = options[0];
        let step = Math.pow(10, selectedPow);

        let min1 = this.mapping.minPrice;
        let y = Math.round(min1 / step) * step;
        while (y < this.mapping.maxPrice) {
            let mappedY = this.mapY(y);
            let yTick = use('y-grid-line', this.grid);
            yTick.setAttribute('y', mappedY);

            let yLabel = newSvgText(y.toString());
            yLabel.setAttribute('class', 'y-label');
            yLabel.setAttribute('x', '4');
            yLabel.setAttribute('y', (mappedY - 3).toString());
            this.grid.appendChild(yLabel);


            y = y + step;
        }
    }
}

/**
 * SVG <use> element
 */
function use(id, parent) {
    const element = document.createElementNS(NS, 'use');
    element.setAttributeNS(null, 'href', '#' + id);
    parent.appendChild(element);
    return element;
}

function newSvgLine(x1, y1, x2, y2, styleClass, strokeWidth) {
    const element = document.createElementNS(NS, 'line');
    element.setAttributeNS(null, 'x1', x1);
    element.setAttributeNS(null, 'x2', x2);
    element.setAttributeNS(null, 'y1', y1);
    element.setAttributeNS(null, 'y2', y2);
    element.setAttributeNS(null, 'class', styleClass);
    if (strokeWidth) {
        element.setAttributeNS(null, 'stroke-width', strokeWidth);
    }
    return element;
}


function newSvgPath(id, stroke) {
    const element = document.createElementNS(NS, 'path');
    element.setAttributeNS(null, 'id', id);
    element.setAttribute('stroke', stroke);
    return element;
}

function newSvgText(content) {
    const element = document.createElementNS(NS, 'text');
    element.textContent = content;
    return element;
}

const lineProperties = (pointA, pointB) => {
    const lengthX = pointB[0] - pointA[0]
    const lengthY = pointB[1] - pointA[1]
    return {
        length: Math.sqrt(Math.pow(lengthX, 2) + Math.pow(lengthY, 2)),
        angle: Math.atan2(lengthY, lengthX)
    }
}

const controlPointCalc = (current, previous, next, reverse) => {
    const c = current
    const p = previous ? previous : c
    const n = next ? next : c
    const smoothing = 0.2
    const o = lineProperties(p, n)
    const rev = reverse ? Math.PI : 0

    const x = c[0] + Math.cos(o.angle + rev) * o.length * smoothing
    const y = c[1] + Math.sin(o.angle + rev) * o.length * smoothing

    return [x, y]
}

const svgPathRender = points => {
    const d = points.reduce((acc, e, i, a) => {
        if (i > 0) {
            const cs = controlPointCalc(a[i - 1], a[i - 2], e)
            const ce = controlPointCalc(e, a[i - 1], a[i + 1], true)
            return `${acc} C ${cs[0]},${cs[1]} ${ce[0]},${ce[1]} ${e[0]},${e[1]}`
        } else {
            return `${acc} M ${e[0]},${e[1]}`
        }
    }, '')

    return `${d}`;
}