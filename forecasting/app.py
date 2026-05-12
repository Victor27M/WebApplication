"""
Improved ARIMA forecasting service.

Key improvements over v1:
  1. Outlier removal  — clips extreme spikes so they don't skew the model
  2. Rolling smoothing — smooths noisy daily data before fitting
  3. Auto parameter search — tries multiple (p,d,q) combos and picks lowest AIC
  4. Ensemble average — averages top 3 models to reduce single-model overfit
  5. Confidence interval clamped to 0 — no negative revenue predictions
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.tsa.stattools import adfuller
import numpy as np
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings("ignore")

app = Flask(__name__)
CORS(app)


def remove_outliers(y, sigma=2.5):
    mean, std = y.mean(), y.std()
    clipped = y.copy()
    clipped[np.abs(y - mean) > sigma * std] = mean
    return clipped


def smooth(y, window=5):
    if len(y) < window:
        return y
    kernel = np.ones(window) / window
    s = np.convolve(y, kernel, mode='same')
    half = window // 2
    s[:half]  = y[:half]
    s[-half:] = y[-half:]
    return s


def is_stationary(y):
    try:
        _, p, *_ = adfuller(y, autolag='AIC')
        return p < 0.05
    except Exception:
        return False


def top_n_models(y, n=3):
    d = 0 if is_stationary(y) else 1
    results = []
    for p in range(0, 4):
        for q in range(0, 4):
            if p + q == 0:
                continue
            try:
                fitted = ARIMA(y, order=(p, d, q)).fit()
                results.append((fitted.aic, fitted))
            except Exception:
                continue
    results.sort(key=lambda x: x[0])
    return [r[1] for r in results[:n]]


@app.route("/predict", methods=["POST"])
def predict():
    body = request.get_json(force=True)
    data = body.get("data", [])
    days = int(body.get("days", 14))

    if len(data) < 10:
        return jsonify({"error": "Need at least 10 data points."}), 400

    raw = np.array([float(p["revenue"]) for p in data], dtype=float)

    try:
        last_date = datetime.strptime(data[-1]["date"][:10], "%Y-%m-%d")
    except (ValueError, KeyError):
        last_date = datetime.today()

    try:
        y = smooth(remove_outliers(raw, sigma=2.5), window=5)

        models = top_n_models(y, n=3)
        if not models:
            return jsonify({"error": "No ARIMA model converged."}), 500

        all_means, all_lowers, all_uppers = [], [], []
        for m in models:
            fc = m.get_forecast(steps=days)
            ci = fc.conf_int(alpha=0.05)
            all_means.append(np.maximum(0, fc.predicted_mean))
            all_lowers.append(np.maximum(0, ci[:, 0]))
            all_uppers.append(np.maximum(0, ci[:, 1]))

        avg_mean  = np.mean(all_means,  axis=0)
        avg_lower = np.mean(all_lowers, axis=0)
        avg_upper = np.mean(all_uppers, axis=0)

        predictions = []
        for i in range(days):
            future_date = last_date + timedelta(days=i + 1)
            predictions.append({
                "date":      future_date.strftime("%Y-%m-%d"),
                "predicted": round(float(avg_mean[i]),  2),
                "lower":     round(float(avg_lower[i]), 2),
                "upper":     round(float(avg_upper[i]), 2),
            })

        best = models[0]
        residuals = best.resid

        return jsonify({
            "predictions": predictions,
            "model": {
                "type":            "ARIMA ensemble",
                "order":           str(best.model.order),
                "aic":             round(float(best.aic), 2),
                "bic":             round(float(best.bic), 2),
                "mae":             round(float(np.mean(np.abs(residuals))), 2),
                "rmse":            round(float(np.sqrt(np.mean(residuals ** 2))), 2),
                "ensemble_size":   len(models),
                "training_points": len(data),
            },
        })

    except Exception as e:
        return jsonify({"error": "Forecast failed.", "details": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": "ARIMA ensemble"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)