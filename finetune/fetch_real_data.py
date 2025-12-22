import hashlib
import json
from datasets import load_dataset

OUTPUT_FILE = "real_world_train.jsonl"

# CONFIGURATION
CYBER_LANGS = ["python", "java", "javascript"]
CYBER_N_PER_LANG = 50 

GITHUB_LANGS = ["Python"]
GITHUB_N = 100 # Fetch more, we will filter heavily
# Strict Permissive Licenses Only
GITHUB_LICENSE_ALLOWLIST = ["mit", "apache-2.0", "bsd-2-clause", "bsd-3-clause", "isc"]

MIN_CODE_CHARS = 50
MAX_INPUT_CHARS = 2000 

def format_repo_mind_json(severity, category, language, message, fix, extra=None):
    payload = {
        "severity": severity,
        "category": category,
        "language": language,
        "title": "Automated Code Review",
        "message": message,
        "suggestion": fix,
    }
    if extra:
        payload["extra"] = extra
    return json.dumps(payload, ensure_ascii=True)

def stable_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="ignore")).hexdigest()

# 🧠 SMART HEURISTICS (The ChatGPT Suggestion)
def analyze_python_architecture(code):
    issues = []
    fixes = []
    
    # Heuristic 1: Bare Except
    if "except:" in code or "except Exception:" in code:
        issues.append("broad exception handling")
        fixes.append("Catch specific exceptions (e.g., ValueError) instead of bare 'except:'.")
        
    # Heuristic 2: Global State
    if "global " in code:
        issues.append("usage of global state")
        fixes.append("Avoid 'global' state; pass dependencies as arguments.")

    # Heuristic 3: Missing Docstrings
    if '"""' not in code and "'''" not in code:
        issues.append("missing documentation")
        fixes.append("Add docstrings to classes and functions.")

    # Heuristic 4: Complexity
    if len(code.split('\n')) > 60:
        issues.append("high cyclomatic complexity")
        fixes.append("Refactor long functions into smaller, single-responsibility units.")

    if not issues:
        # Default fallback if code looks clean
        return "Code structure appears sound.", "Maintain current coding standards."
        
    return (
        f"Architectural issues detected: {', '.join(issues)}.",
        " ".join(fixes)
    )

print("🚀 Starting Smart Data Extraction...")

final_data = []
seen_hashes = set()

# ---------------------------------------------------------
# 1) FETCH CYBERSEC EVAL (Security)
# ---------------------------------------------------------
print("⬇️  Downloading CyberSecEval (Security)...")
for lang in CYBER_LANGS:
    try:
        ds = load_dataset("walledai/CyberSecEval", "instruct", split=f"{lang}[:{CYBER_N_PER_LANG}]")
        for row in ds:
            # Fallback chain for field names
            code_snippet = (
                row.get("origin_code") or 
                row.get("prompt") or 
                row.get("input") or 
                row.get("question") or ""
            )
            code_snippet = str(code_snippet).strip()

            if len(code_snippet) < MIN_CODE_CHARS: continue
            
            h = stable_hash(code_snippet)
            if h in seen_hashes: continue
            seen_hashes.add(h)

            cwe = row.get("cwe_identifier", "Unknown")
            description = row.get("description", "Potential security vulnerability.")

            entry = {
                "instruction": f"Analyze the following {lang.capitalize()} code for security vulnerabilities. Return strict JSON.",
                "input": code_snippet[:MAX_INPUT_CHARS],
                "output": format_repo_mind_json(
                    severity="CRITICAL",
                    category="SECURITY",
                    language=lang.capitalize(),
                    message=f"Security Alert: {description} (CWE: {cwe})",
                    fix="Apply secure coding practices: validate inputs, use parameterized queries, and sanitize data.",
                    extra={"cwe": cwe}
                ),
            }
            final_data.append(entry)
    except Exception as e:
        print(f"   ⚠️  Skipping {lang}: {e}")

# ---------------------------------------------------------
# 2) FETCH GITHUB CODE (Smart Architecture)
# ---------------------------------------------------------
print("⬇️  Downloading GitHub Code (Architecture)...")
try:
    # MIT-only subset (no dataset script needed). :contentReference[oaicite:1]{index=1}
    part_ids = list(range(0, 3))  # 0000..0002; increase if you want more variety
    shard_urls = [
        f"https://huggingface.co/datasets/codeparrot/github-code/resolve/refs%2Fconvert%2Fparquet/all-mit/partial-train/{i:04d}.parquet"
        for i in part_ids
    ]

    ds = load_dataset("parquet", data_files=shard_urls, split="train", streaming=True)

    count = 0
    scanned = 0
    for row in ds:
        if scanned >= 300000 or count >= GITHUB_N:
            break
        scanned += 1

        # Normalize language key + case
        lang = (row.get("language") or row.get("lang") or "").strip().lower()
        if lang != "python":
            continue

        content = str(row.get("code", "")).strip()
        if not (100 <= len(content) <= 2000):
            continue
        if "def " not in content and "class " not in content:
            continue

        h = stable_hash(content)
        if h in seen_hashes:
            continue
        seen_hashes.add(h)

        msg, fix = analyze_python_architecture(content)

        entry = {
            "instruction": "Analyze this Python code for architectural quality. Return strict JSON.",
            "input": content,
            "output": format_repo_mind_json(
                severity="INFO" if "sound" in msg else "WARNING",
                category="ARCHITECTURE",
                language="Python",
                message=msg,
                fix=fix,
                extra={
                    "license": "mit",  # implied by the subset :contentReference[oaicite:2]{index=2}
                    "repo": row.get("repo_name"),
                    "path": row.get("path"),
                },
            ),
        }
        final_data.append(entry)
        count += 1

    print(f"   ✅ Added {count} Smart Architecture examples.")

except Exception as e:
    print(f"   ⚠️ GitHub code failed: {e}")



# ---------------------------------------------------------
# 3) ADD CURATED EXAMPLES
# ---------------------------------------------------------
# ... (Keep your handcrafted list here if you want extra precision) ...

# ---------------------------------------------------------
# 4) SAVE
# ---------------------------------------------------------
print(f"💾 Saving {len(final_data)} examples to {OUTPUT_FILE}...")
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    for item in final_data:
        f.write(json.dumps(item) + "\n")