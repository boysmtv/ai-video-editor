# Download TFLite models for AI features
# Requires: Invoke-WebRequest (PowerShell 5+)

$AssetsDir = Join-Path $PSScriptRoot "app\src\main\assets"
$ModelsDir = Join-Path $AssetsDir "models"
New-Item -ItemType Directory -Force -Path $ModelsDir | Out-Null

$models = @(
    @{
        Name = "deeplabv3_257_mv_gpu.tflite"
        Url = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/gpu/deeplabv3_257_mv_gpu.tflite"
        Desc = "Portrait Segmentation (DeepLab V3+)"
    },
    @{
        Name = "face_landmark.tflite"
        Url = "https://storage.googleapis.com/mediapipe-assets/face_landmark.tflite"
        Desc = "Face Landmark Detection (MediaPipe 468 points)"
    },
    @{
        Name = "movenet.tflite"
        Url = "https://tfhub.dev/google/lite-model/movenet/singlepose/lightning/tflite/float16/4?lite-format=tflite"
        Desc = "Pose/Object Detection (MoveNet Lightning)"
    }
)

function Test-TfliteFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $false }
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 8) { return $false }
    return $bytes[4] -eq 0x54 -and $bytes[5] -eq 0x46 -and $bytes[6] -eq 0x4C -and $bytes[7] -eq 0x33
}

Write-Host "Downloading TFLite models to: $ModelsDir" -ForegroundColor Cyan

foreach ($model in $models) {
    $dest = Join-Path $ModelsDir $model.Name
    if (Test-TfliteFile $dest) {
        Write-Host "  [SKIP] $($model.Name) already exists and is valid" -ForegroundColor Yellow
        continue
    }
    if (Test-Path $dest) {
        Remove-Item -LiteralPath $dest -Force
    }
    Write-Host "  [DOWNLOAD] $($model.Name) - $($model.Desc)" -ForegroundColor Green
    try {
        Invoke-WebRequest -Uri $model.Url -OutFile $dest -UseBasicParsing -ErrorAction Stop
        if (-not (Test-TfliteFile $dest)) {
            throw "Downloaded file is not a valid TFLite flatbuffer"
        }
        Write-Host "    -> Done ($([Math]::Round((Get-Item $dest).Length / 1MB, 2)) MB)" -ForegroundColor Green
    } catch {
        Write-Host "    -> FAILED: $_" -ForegroundColor Red
        if (Test-Path $dest) {
            Remove-Item -LiteralPath $dest -Force
        }
    }
}

Write-Host ""
Write-Host "Model download pass complete." -ForegroundColor Cyan
