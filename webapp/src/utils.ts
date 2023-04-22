interface BasePath {
    path?: string
}

let basePath: BasePath

export function getBasePath() {
    if (basePath === undefined) {
        const path = resolveBasePath()
        basePath = {path: path}
    }
    return basePath.path
}

function resolveBasePath() {
    const attr = document.querySelector("meta[name='base-path']")?.getAttribute("content");
    if (attr) {
        const normalized = attr.endsWith("/") ? attr.slice(0, -1) : attr;
        return normalized.length > 0 ? normalized : undefined
    }
    return undefined
}