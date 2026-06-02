(function () {
    const CANVAS_SIZE = 280;
    const MNIST_SIZE = 28;
    const NORMALIZED_CONTENT_SIZE = 20;
    const samplePixels = readSamplePixels();

    const drawCanvas = document.getElementById("drawCanvas");
    const previewCanvas = document.getElementById("previewCanvas");
    const brushSizeInput = document.getElementById("brushSize");
    const clearButton = document.getElementById("clearButton");
    const predictButton = document.getElementById("predictButton");
    const statusText = document.getElementById("statusText");
    const predictionValue = document.getElementById("predictionValue");
    const probabilityChart = document.getElementById("probabilityChart");
    const sampleButtons = Array.from(document.querySelectorAll(".sample-button"));

    const drawContext = drawCanvas.getContext("2d");
    const previewContext = previewCanvas.getContext("2d");

    let drawing = false;
    let lastPoint = null;

    initialize();

    function initialize() {
        clearDrawing();
        renderChart(new Array(10).fill(0), null);
        bindCanvasEvents();
        bindControls();
    }

    function bindCanvasEvents() {
        drawCanvas.addEventListener("pointerdown", (event) => {
            drawing = true;
            lastPoint = getCanvasPoint(event);
            drawCanvas.setPointerCapture(event.pointerId);
            drawDot(lastPoint);
        });

        drawCanvas.addEventListener("pointermove", (event) => {
            if (!drawing) {
                return;
            }

            const nextPoint = getCanvasPoint(event);
            drawLine(lastPoint, nextPoint);
            lastPoint = nextPoint;
        });

        drawCanvas.addEventListener("pointerup", endDrawing);
        drawCanvas.addEventListener("pointercancel", endDrawing);
        drawCanvas.addEventListener("pointerleave", endDrawing);
    }

    function bindControls() {
        clearButton.addEventListener("click", () => {
            clearDrawing();
            resetResult();
            setStatus("대기 중");
        });

        predictButton.addEventListener("click", () => {
            predictCurrentCanvas();
        });

        sampleButtons.forEach((button) => {
            button.addEventListener("click", () => {
                loadSample(Number(button.dataset.digit));
            });
        });
    }

    function endDrawing(event) {
        if (drawing && event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId);
        }
        drawing = false;
        lastPoint = null;
    }

    function getCanvasPoint(event) {
        const rect = drawCanvas.getBoundingClientRect();
        return {
            x: (event.clientX - rect.left) * (drawCanvas.width / rect.width),
            y: (event.clientY - rect.top) * (drawCanvas.height / rect.height)
        };
    }

    function drawDot(point) {
        drawContext.fillStyle = "#111827";
        drawContext.beginPath();
        drawContext.arc(point.x, point.y, Number(brushSizeInput.value) / 2, 0, Math.PI * 2);
        drawContext.fill();
    }

    function drawLine(from, to) {
        drawContext.strokeStyle = "#111827";
        drawContext.lineWidth = Number(brushSizeInput.value);
        drawContext.lineCap = "round";
        drawContext.lineJoin = "round";
        drawContext.beginPath();
        drawContext.moveTo(from.x, from.y);
        drawContext.lineTo(to.x, to.y);
        drawContext.stroke();
    }

    function clearDrawing() {
        drawContext.fillStyle = "#ffffff";
        drawContext.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        clearPreview();
        sampleButtons.forEach((button) => button.classList.remove("is-active"));
    }

    function clearPreview() {
        previewContext.fillStyle = "#ffffff";
        previewContext.fillRect(0, 0, previewCanvas.width, previewCanvas.height);
    }

    async function loadSample(digit) {
        setBusy(true);
        setStatus(`${digit} 샘플 입력 중`);
        sampleButtons.forEach((button) => button.classList.toggle("is-active", Number(button.dataset.digit) === digit));

        try {
            const pixels = getSamplePixels(digit);
            drawPixelsToCanvas(pixels);
            drawPreview(pixels);
            await predictPixels(pixels);
        } catch (error) {
            setStatus(error.message, true);
        } finally {
            setBusy(false);
        }
    }

    async function predictCurrentCanvas() {
        setBusy(true);

        try {
            const pixels = normalizeCanvas();
            drawPreview(pixels);
            await predictPixels(pixels);
        } catch (error) {
            setStatus(error.message, true);
        } finally {
            setBusy(false);
        }
    }

    async function predictPixels(pixels) {
        setStatus("예측 중");
        const response = await fetch("/api/mnist/predict", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({pixels})
        });

        const result = await readJson(response);
        const probabilities = probabilitiesFromMap(result.probabilities || result);
        const prediction = Number.isInteger(result.prediction) ? result.prediction : findMaxIndex(probabilities);
        predictionValue.textContent = String(prediction);
        renderChart(probabilities, prediction);
        setStatus("예측 완료");
    }

    async function readJson(response) {
        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json") ? await response.json() : null;
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("예측 API가 아직 구현되지 않았습니다.");
            }
            throw new Error(payload?.message || "요청 처리 중 오류가 발생했습니다.");
        }
        return payload;
    }

    function normalizeCanvas() {
        const sourceData = drawContext.getImageData(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        const bounds = findInkBounds(sourceData);
        if (!bounds) {
            throw new Error("숫자를 먼저 입력하세요.");
        }

        const source = document.createElement("canvas");
        source.width = CANVAS_SIZE;
        source.height = CANVAS_SIZE;
        source.getContext("2d").putImageData(sourceData, 0, 0);

        const normalized = document.createElement("canvas");
        normalized.width = MNIST_SIZE;
        normalized.height = MNIST_SIZE;
        const normalizedContext = normalized.getContext("2d");
        normalizedContext.fillStyle = "#ffffff";
        normalizedContext.fillRect(0, 0, MNIST_SIZE, MNIST_SIZE);

        const width = bounds.maxX - bounds.minX + 1;
        const height = bounds.maxY - bounds.minY + 1;
        const scale = NORMALIZED_CONTENT_SIZE / Math.max(width, height);
        const targetWidth = Math.max(1, Math.round(width * scale));
        const targetHeight = Math.max(1, Math.round(height * scale));
        const targetX = Math.floor((MNIST_SIZE - targetWidth) / 2);
        const targetY = Math.floor((MNIST_SIZE - targetHeight) / 2);

        normalizedContext.imageSmoothingEnabled = true;
        normalizedContext.drawImage(
            source,
            bounds.minX,
            bounds.minY,
            width,
            height,
            targetX,
            targetY,
            targetWidth,
            targetHeight
        );

        return imageDataToPixels(normalizedContext.getImageData(0, 0, MNIST_SIZE, MNIST_SIZE));
    }

    function findInkBounds(imageData) {
        const data = imageData.data;
        let minX = CANVAS_SIZE;
        let minY = CANVAS_SIZE;
        let maxX = -1;
        let maxY = -1;

        for (let y = 0; y < CANVAS_SIZE; y++) {
            for (let x = 0; x < CANVAS_SIZE; x++) {
                const index = (y * CANVAS_SIZE + x) * 4;
                const intensity = 1 - ((data[index] + data[index + 1] + data[index + 2]) / 3 / 255);
                if (intensity > 0.05) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < 0 || maxY < 0) {
            return null;
        }

        return {minX, minY, maxX, maxY};
    }

    function imageDataToPixels(imageData) {
        const data = imageData.data;
        const pixels = [];
        for (let index = 0; index < data.length; index += 4) {
            const average = (data[index] + data[index + 1] + data[index + 2]) / 3;
            pixels.push(clamp((255 - average) / 255));
        }
        return pixels;
    }

    function drawPixelsToCanvas(pixels) {
        const source = pixelsToCanvas(pixels, MNIST_SIZE);
        drawContext.fillStyle = "#ffffff";
        drawContext.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        drawContext.imageSmoothingEnabled = false;
        drawContext.drawImage(source, 0, 0, CANVAS_SIZE, CANVAS_SIZE);
    }

    function getSamplePixels(digit) {
        const pixels = samplePixels[String(digit)] || samplePixels[digit];
        if (!Array.isArray(pixels) || pixels.length !== 784) {
            throw new Error("샘플 숫자는 0부터 9까지 선택할 수 있습니다.");
        }

        return pixels.map(Number);
    }

    function readSamplePixels() {
        const samplesElement = document.getElementById("mnistSamplesJson");
        if (!samplesElement) {
            return {};
        }

        try {
            return JSON.parse(samplesElement.textContent || "{}");
        } catch (error) {
            return {};
        }
    }

    function drawPreview(pixels) {
        const source = pixelsToCanvas(pixels, MNIST_SIZE);
        previewContext.fillStyle = "#ffffff";
        previewContext.fillRect(0, 0, previewCanvas.width, previewCanvas.height);
        previewContext.imageSmoothingEnabled = false;
        previewContext.drawImage(source, 0, 0, previewCanvas.width, previewCanvas.height);
    }

    function pixelsToCanvas(pixels, size) {
        const canvas = document.createElement("canvas");
        canvas.width = size;
        canvas.height = size;
        const context = canvas.getContext("2d");
        const imageData = context.createImageData(size, size);

        pixels.forEach((pixel, index) => {
            const color = Math.round(255 - clamp(pixel) * 255);
            const dataIndex = index * 4;
            imageData.data[dataIndex] = color;
            imageData.data[dataIndex + 1] = color;
            imageData.data[dataIndex + 2] = color;
            imageData.data[dataIndex + 3] = 255;
        });

        context.putImageData(imageData, 0, 0);
        return canvas;
    }

    function probabilitiesFromMap(probabilityMap) {
        return Array.from({length: 10}, (_, digit) => Number(probabilityMap[digit] || probabilityMap[String(digit)] || 0));
    }

    function findMaxIndex(values) {
        return values.reduce((maxIndex, value, index) => value > values[maxIndex] ? index : maxIndex, 0);
    }

    function renderChart(probabilities, prediction) {
        probabilityChart.innerHTML = "";
        probabilities.forEach((probability, digit) => {
            const row = document.createElement("div");
            row.className = "probability-row";
            if (digit === prediction) {
                row.classList.add("is-top");
            }

            const label = document.createElement("span");
            label.className = "probability-digit";
            label.textContent = String(digit);

            const track = document.createElement("div");
            track.className = "probability-track";

            const fill = document.createElement("div");
            fill.className = "probability-fill";
            fill.style.width = `${Math.max(0, Math.min(100, probability * 100))}%`;

            const value = document.createElement("span");
            value.className = "probability-value";
            value.textContent = `${(probability * 100).toFixed(1)}%`;

            track.appendChild(fill);
            row.append(label, track, value);
            probabilityChart.appendChild(row);
        });
    }

    function resetResult() {
        predictionValue.textContent = "-";
        renderChart(new Array(10).fill(0), null);
    }

    function setStatus(message, error = false) {
        statusText.textContent = message;
        statusText.classList.toggle("is-error", error);
    }

    function setBusy(busy) {
        predictButton.disabled = busy;
        clearButton.disabled = busy;
        sampleButtons.forEach((button) => {
            button.disabled = busy;
        });
    }

    function clamp(value) {
        return Math.max(0, Math.min(1, value));
    }
})();
