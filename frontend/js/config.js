const isStandaloneDevelopmentServer =
    window.location.protocol === "file:" ||
    window.location.origin === "null" ||
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.hostname.startsWith("192.168.");

const backendOrigin = isStandaloneDevelopmentServer &&
    window.location.port !== "8080"
    ? `http://${window.location.hostname}:8080`
    : window.location.origin;

export const API_BASE_URL = backendOrigin;
export const WEBSOCKET_URL = `${backendOrigin}/ws`;
export const MODEL_BASE_URL = `${backendOrigin}/assets/models`;
