import { build } from "esbuild";
import { cp, mkdir } from "node:fs/promises";

await mkdir("dist", { recursive: true });
await build({
    entryPoints: ["js/main.js"],
    bundle: true,
    format: "esm",
    minify: true,
    outfile: "dist/js/main.js",
    sourcemap: true
});
await cp("index.html", "dist/index.html");
await cp("css", "dist/css", { recursive: true });
