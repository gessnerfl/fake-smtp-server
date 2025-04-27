interface BasePath {
    path?: string
}

export const TEST_BASE_PATH = "https://example.com"

let basePath: BasePath

export function getBasePath() {
    if (basePath === undefined) {
        const path = resolveBasePath()
        basePath = {path: path}
    }
    return basePath.path
}

function resolveBasePath() {
    if (process.env.NODE_ENV === "test") {
        return TEST_BASE_PATH
    }
    const path = window.location.pathname;
    if (path) {
        return path.substring(0, path.lastIndexOf("/"));
    }
    return undefined
}