#!/usr/bin/env python3
"""Convert newer Yes Steve Model folders to this mod's legacy folder layout."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any, Dict, Iterable, List, Sequence, Tuple


OUTPUT_DIR_NAME = "ysmu_convert"

MAIN_MODEL_NAME = "main.json"
ARM_MODEL_NAME = "arm.json"
MAIN_ANIMATION_NAME = "main.animation.json"
ARM_ANIMATION_NAME = "arm.animation.json"
EXTRA_ANIMATION_NAME = "extra.animation.json"


class ConvertError(Exception):
    pass


def load_json(path: Path) -> Any:
    try:
        with path.open("r", encoding="utf-8-sig") as fp:
            return json.load(fp)
    except json.JSONDecodeError as exc:
        raise ConvertError(f"{path} is not valid JSON: {exc}") from exc
    except OSError as exc:
        raise ConvertError(f"failed to read {path}: {exc}") from exc


def write_json(path: Path, data: Any) -> None:
    try:
        with path.open("w", encoding="utf-8", newline="\n") as fp:
            json.dump(data, fp, ensure_ascii=False, indent="\t")
            fp.write("\n")
    except OSError as exc:
        raise ConvertError(f"failed to write {path}: {exc}") from exc


def as_dict(value: Any, label: str) -> Dict[str, Any]:
    if not isinstance(value, dict):
        raise ConvertError(f"{label} must be an object")
    return value


def nested(data: Dict[str, Any], keys: Sequence[str]) -> Any:
    current: Any = data
    for key in keys:
        if not isinstance(current, dict) or key not in current:
            return None
        current = current[key]
    return current


def is_relative_to(child: Path, parent: Path) -> bool:
    try:
        child.relative_to(parent)
        return True
    except ValueError:
        return False


def resolve_declared_file(model_root: Path, raw_path: Any, fallback: str, label: str) -> Path:
    if raw_path is None:
        raw_path = fallback
    if not isinstance(raw_path, str) or not raw_path.strip():
        raise ConvertError(f"{label} path is missing or not a string")

    relative = Path(raw_path.replace("\\", "/"))
    if relative.is_absolute():
        raise ConvertError(f"{label} path must be relative: {raw_path}")

    root = model_root.resolve()
    resolved = (model_root / relative).resolve()
    if not is_relative_to(resolved, root):
        raise ConvertError(f"{label} path leaves the model folder: {raw_path}")
    if not resolved.is_file():
        raise ConvertError(f"{label} file does not exist: {raw_path}")
    return resolved


def resolve_optional_declared_file(
    model_root: Path, raw_path: Any, fallback: str, label: str
) -> Path | None:
    if raw_path is None:
        raw_path = fallback
    if not isinstance(raw_path, str) or not raw_path.strip():
        return None

    relative = Path(raw_path.replace("\\", "/"))
    if relative.is_absolute():
        raise ConvertError(f"{label} path must be relative: {raw_path}")

    root = model_root.resolve()
    resolved = (model_root / relative).resolve()
    if not is_relative_to(resolved, root):
        raise ConvertError(f"{label} path leaves the model folder: {raw_path}")
    if not resolved.is_file():
        return None
    return resolved


def discover_model_roots(input_dir: Path, recursive: bool) -> List[Path]:
    output_dir = (input_dir / OUTPUT_DIR_NAME).resolve()

    if (input_dir / "ysm.json").is_file():
        return [input_dir]

    if recursive:
        roots = []
        for ysm_json in input_dir.rglob("ysm.json"):
            parent = ysm_json.parent.resolve()
            if parent == output_dir or is_relative_to(parent, output_dir):
                continue
            roots.append(ysm_json.parent)
        return sorted(roots)

    roots = []
    for child in sorted(input_dir.iterdir()):
        if child.is_dir() and child.name != OUTPUT_DIR_NAME and (child / "ysm.json").is_file():
            roots.append(child)
    return roots


def text_or_default(value: Any, default: str = "") -> str:
    if value is None:
        return default
    return str(value)


def extract_authors(metadata: Dict[str, Any]) -> List[str]:
    raw_authors = metadata.get("authors")
    if raw_authors is None:
        return []

    if isinstance(raw_authors, dict):
        raw_authors = [raw_authors]
    if not isinstance(raw_authors, list):
        raise ConvertError("metadata.authors must be an array or object")

    authors = []
    for author in raw_authors:
        if isinstance(author, dict):
            name = author.get("name")
        else:
            name = author
        if name is not None and str(name).strip():
            authors.append(str(name))
    return authors


def extract_license(metadata: Dict[str, Any]) -> str:
    raw_license = metadata.get("license")
    if isinstance(raw_license, dict):
        return text_or_default(raw_license.get("type"), "All Rights Reserved")
    return text_or_default(raw_license, "All Rights Reserved")


def inject_legacy_description(main_model_path: Path, ysm_data: Dict[str, Any]) -> Dict[str, Any]:
    main_model = as_dict(load_json(main_model_path), f"{MAIN_MODEL_NAME}")
    geometries = main_model.get("minecraft:geometry")
    if not isinstance(geometries, list) or not geometries:
        raise ConvertError(f"{MAIN_MODEL_NAME} must contain a non-empty minecraft:geometry array")
    first_geometry = as_dict(geometries[0], "minecraft:geometry[0]")

    description = first_geometry.get("description")
    if description is None:
        description = {}
        first_geometry["description"] = description
    description = as_dict(description, "minecraft:geometry[0].description")

    metadata = ysm_data.get("metadata") or {}
    metadata = as_dict(metadata, "metadata")
    description["ysm_extra_info"] = {
        "name": text_or_default(metadata.get("name"), main_model_path.parent.name),
        "tips": text_or_default(metadata.get("tips")),
        "authors": extract_authors(metadata),
        "license": extract_license(metadata),
    }

    properties = ysm_data.get("properties")
    if isinstance(properties, dict):
        if "height_scale" in properties:
            description["ysm_height_scale"] = properties["height_scale"]
        if "width_scale" in properties:
            description["ysm_width_scale"] = properties["width_scale"]

    return main_model


def texture_entries(player_files: Dict[str, Any]) -> List[Any]:
    raw_textures = player_files.get("texture")
    if raw_textures is None:
        raise ConvertError("files.player.texture is missing")
    if isinstance(raw_textures, list):
        return raw_textures
    return [raw_textures]


def declared_texture_paths(model_root: Path, player_files: Dict[str, Any]) -> List[Path]:
    paths: List[Path] = []
    for index, entry in enumerate(texture_entries(player_files)):
        if isinstance(entry, dict):
            raw_path = entry.get("uv")
        else:
            raw_path = entry
        path = resolve_declared_file(model_root, raw_path, "", f"files.player.texture[{index}].uv")
        if path.suffix.lower() != ".png":
            raise ConvertError(f"texture is not a PNG file: {raw_path}")
        paths.append(path)
    if not paths:
        raise ConvertError("files.player.texture does not contain any textures")
    return paths


def unique_texture_map(textures: Iterable[Path]) -> Dict[str, Path]:
    copied_texture_names: Dict[str, Path] = {}
    for texture in textures:
        lowered_name = texture.name.casefold()
        previous = copied_texture_names.get(lowered_name)
        if previous is not None and previous.resolve() != texture.resolve():
            raise ConvertError(
                f"texture file names collide after flattening: {previous} and {texture}"
            )
        if previous is None:
            copied_texture_names[lowered_name] = texture
    return copied_texture_names


def copy_file(src: Path, dst: Path, dry_run: bool) -> None:
    if dry_run:
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    try:
        shutil.copy2(src, dst)
    except OSError as exc:
        raise ConvertError(f"failed to copy {src} to {dst}: {exc}") from exc


def clear_output_dir(path: Path, output_root: Path) -> None:
    resolved = path.resolve()
    root = output_root.resolve()
    if resolved == root or not is_relative_to(resolved, root):
        raise ConvertError(f"refusing to remove unexpected output path: {path}")
    shutil.rmtree(path)


def choose_output_names(model_roots: Iterable[Path]) -> List[Tuple[Path, str]]:
    used: Dict[str, int] = {}
    result = []
    for root in model_roots:
        base = root.name or "unnamed"
        key = base.casefold()
        index = used.get(key, 0) + 1
        used[key] = index
        output_name = base if index == 1 else f"{base}_{index}"
        result.append((root, output_name))
    return result


def convert_one(model_root: Path, output_root: Path, output_name: str, overwrite: bool, dry_run: bool) -> int:
    ysm_path = model_root / "ysm.json"
    ysm_data = as_dict(load_json(ysm_path), "ysm.json")

    player_files = nested(ysm_data, ("files", "player"))
    player_files = as_dict(player_files, "files.player")

    model_main = resolve_declared_file(
        model_root,
        nested(player_files, ("model", "main")),
        "models/main.json",
        "files.player.model.main",
    )
    model_arm = resolve_declared_file(
        model_root,
        nested(player_files, ("model", "arm")),
        "models/arm.json",
        "files.player.model.arm",
    )
    animation_main = resolve_optional_declared_file(
        model_root,
        nested(player_files, ("animation", "main")),
        "animations/main.animation.json",
        "files.player.animation.main",
    )
    animation_arm = resolve_optional_declared_file(
        model_root,
        nested(player_files, ("animation", "arm")),
        "animations/arm.animation.json",
        "files.player.animation.arm",
    )
    animation_extra = resolve_optional_declared_file(
        model_root,
        nested(player_files, ("animation", "extra")),
        "animations/extra.animation.json",
        "files.player.animation.extra",
    )
    textures = declared_texture_paths(model_root, player_files)
    unique_textures = unique_texture_map(textures)

    out_dir = output_root / output_name
    if out_dir.exists():
        if not overwrite:
            raise ConvertError(f"output already exists: {out_dir} (use --overwrite)")
        if not dry_run:
            clear_output_dir(out_dir, output_root)

    if not dry_run:
        out_dir.mkdir(parents=True, exist_ok=False)

    legacy_main = inject_legacy_description(model_main, ysm_data)
    if not dry_run:
        write_json(out_dir / MAIN_MODEL_NAME, legacy_main)

    copy_file(model_arm, out_dir / ARM_MODEL_NAME, dry_run)
    if animation_main is not None:
        copy_file(animation_main, out_dir / MAIN_ANIMATION_NAME, dry_run)
    if animation_arm is not None:
        copy_file(animation_arm, out_dir / ARM_ANIMATION_NAME, dry_run)
    if animation_extra is not None:
        copy_file(animation_extra, out_dir / EXTRA_ANIMATION_NAME, dry_run)

    for texture in unique_textures.values():
        copy_file(texture, out_dir / texture.name, dry_run)

    return len(unique_textures)


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Convert newer Yes Steve Model folders into the legacy YSMU folder layout. "
            f"Output is written to <input>/{OUTPUT_DIR_NAME}."
        )
    )
    parser.add_argument(
        "input_dir",
        type=Path,
        help="A model folder containing ysm.json, or a folder containing model subfolders.",
    )
    parser.add_argument(
        "--recursive",
        action="store_true",
        help="Search recursively for ysm.json when input_dir is a collection folder.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help=f"Replace existing model folders under {OUTPUT_DIR_NAME}.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate inputs and print planned output without writing files.",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    input_dir = args.input_dir.resolve()
    if not input_dir.is_dir():
        print(f"error: input directory does not exist: {input_dir}", file=sys.stderr)
        return 2

    model_roots = discover_model_roots(input_dir, args.recursive)
    if not model_roots:
        print(f"error: no ysm.json found under {input_dir}", file=sys.stderr)
        return 2

    output_root = input_dir / OUTPUT_DIR_NAME
    if not args.dry_run:
        output_root.mkdir(parents=True, exist_ok=True)

    converted = 0
    failed = 0
    for model_root, output_name in choose_output_names(model_roots):
        out_dir = output_root / output_name
        try:
            texture_count = convert_one(model_root, output_root, output_name, args.overwrite, args.dry_run)
        except ConvertError as exc:
            failed += 1
            print(f"[ERROR] {model_root}: {exc}", file=sys.stderr)
            continue

        converted += 1
        action = "DRY" if args.dry_run else "OK"
        print(f"[{action}] {model_root} -> {out_dir} ({texture_count} texture(s))")

    print(f"done: {converted} converted, {failed} failed. output: {output_root}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
