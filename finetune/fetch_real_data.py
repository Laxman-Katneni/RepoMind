import hashlib
import json
import re
import ast
import warnings
from datasets import load_dataset

OUTPUT_FILE = "real_world_train.jsonl"

# -----------------------------
# CONFIGURATION
# -----------------------------
GITHUB_TARGETS = ["python", "java", "javascript", "typescript"]
GITHUB_N_PER_LANG = 60
CYBER_N_PER_LANG = 50

MAX_INPUT_CHARS = 3000
MIN_ARCH_CODE_CHARS = 150
MIN_SEC_CODE_CHARS = 50

# MIT-only parquet subset (scriptless, works with modern datasets)
MIT_PART_IDS = list(range(0, 8))  # scan more parts if you want more variety

# -----------------------------
# HELPERS
# -----------------------------
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

def line_at(code: str, lineno: int, max_len: int = 220) -> str:
    """1-indexed lineno -> trimmed line text (best effort)."""
    try:
        lines = code.splitlines()
        if 1 <= lineno <= len(lines):
            return lines[lineno - 1].strip()[:max_len]
    except Exception:
        pass
    return ""

def find_line_containing(code: str, needle: str, max_len: int = 220) -> str:
    """Return first line containing needle (best effort)."""
    try:
        for ln in code.splitlines():
            if needle in ln:
                return ln.strip()[:max_len]
    except Exception:
        pass
    return needle.strip()[:max_len]

# -----------------------------
# 🧠 PYTHON AST ANALYSIS
# -----------------------------
CONTROL_FLOW_NODES = (
    ast.If,
    ast.For,
    ast.While,
    ast.Try,
    ast.With,
    ast.Match,  # py3.10+
)

class FunctionNestingVisitor(ast.NodeVisitor):
    """
    Tracks max control-flow nesting depth *per function*.
    Depth increments only for control-flow nodes, not FunctionDef itself.
    """
    def __init__(self, code: str):
        self.code = code
        self.current_func = None
        self.current_depth = 0
        self.func_max = {}  # func_name -> (max_depth, lineno_of_max, line_text)

    def visit_FunctionDef(self, node: ast.FunctionDef):
        prev_func = self.current_func
        prev_depth = self.current_depth

        self.current_func = node.name
        self.current_depth = 0
        self.func_max.setdefault(
            node.name,
            (0, getattr(node, "lineno", 1), line_at(self.code, getattr(node, "lineno", 1)))
        )
        self.generic_visit(node)

        self.current_func = prev_func
        self.current_depth = prev_depth

    def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef):
        prev_func = self.current_func
        prev_depth = self.current_depth

        self.current_func = node.name
        self.current_depth = 0
        self.func_max.setdefault(
            node.name,
            (0, getattr(node, "lineno", 1), line_at(self.code, getattr(node, "lineno", 1)))
        )
        self.generic_visit(node)

        self.current_func = prev_func
        self.current_depth = prev_depth

    def generic_visit(self, node):
        is_cf = isinstance(node, CONTROL_FLOW_NODES)
        if is_cf and self.current_func is not None:
            self.current_depth += 1

            prev_max, _, _ = self.func_max.get(self.current_func, (0, 1, ""))
            if self.current_depth > prev_max:
                ln = getattr(node, "lineno", 1)
                self.func_max[self.current_func] = (self.current_depth, ln, line_at(self.code, ln))

            super().generic_visit(node)

            self.current_depth -= 1
            return

        super().generic_visit(node)

def analyze_python_ast(code: str):
    issues, fixes, signals, evidence = [], [], [], []

    try:
        # Suppress SyntaxWarning noise caused by parsing *other people's code*
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", SyntaxWarning)
            tree = ast.parse(code)
    except SyntaxError:
        return None

    # 1) True nesting depth per function
    nv = FunctionNestingVisitor(code)
    nv.visit(tree)
    for func_name, (depth, ln, ln_text) in nv.func_max.items():
        if depth > 4:
            issues.append(f"Deep nesting in '{func_name}' (depth {depth})")
            fixes.append(f"Refactor '{func_name}' using guard clauses, early returns, or helper functions.")
            signals.append("complexity")
            ev = f"{func_name} depth={depth} @L{ln}: {ln_text}" if ln_text else f"{func_name} depth={depth} @L{ln}"
            evidence.append(ev)

    # 2) God class (method count) – best-effort for snippets
    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef):
            methods = [n for n in node.body if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef))]
            if len(methods) > 10:
                issues.append(f"Low cohesion: class '{node.name}' has many methods ({len(methods)})")
                fixes.append(f"Split '{node.name}' into smaller classes/modules by responsibility.")
                signals.append("cohesion")
                ln = getattr(node, "lineno", 1)
                cls_line = line_at(code, ln)
                evidence.append(f"class {node.name} @L{ln}: {cls_line}" if cls_line else f"class {node.name} @L{ln}")

    # 3) Explicit global state usage
    for node in ast.walk(tree):
        if isinstance(node, ast.Global):
            issues.append("Explicit global state usage")
            fixes.append("Avoid global state; pass dependencies explicitly or encapsulate state in objects.")
            signals.append("side-effects")
            ln = getattr(node, "lineno", 1)
            ev_line = line_at(code, ln) or f"global {', '.join(getattr(node, 'names', []))}"
            evidence.append(f"@L{ln}: {ev_line}")

    # 4) Bare except blocks
    for node in ast.walk(tree):
        if isinstance(node, ast.ExceptHandler) and node.type is None:
            issues.append("Bare 'except:' block")
            fixes.append("Catch specific exceptions and handle/log them appropriately; avoid swallowing unknown errors.")
            signals.append("error-handling")
            ln = getattr(node, "lineno", 1)
            ev_line = line_at(code, ln) or "except:"
            evidence.append(f"@L{ln}: {ev_line}")

    if not issues:
        return None

    return issues, fixes, signals, evidence

# -----------------------------
# 🧠 REGEX HEURISTICS (Polyglot)
# -----------------------------
def analyze_regex_heuristics(code: str, lang: str):
    issues, fixes, signals, evidence = [], [], [], []
    code_lower = code.lower()

    # --- UNIVERSAL: Hardcoded secrets (polyglot-ish) ---
    if ("process.env" not in code_lower) and ("getenv" not in code_lower) and ("os.getenv" not in code_lower):
        secret_pattern = re.compile(
            r'(?i)\b(api_key|apikey|password|passwd|secret|token)\b\s*[:=]\s*["\'][^"\']{8,}["\']'
        )
        m = secret_pattern.search(code)
        if m:
            issues.append("Hardcoded secret detected")
            fixes.append("Move secrets to environment variables or a secrets manager (vault/KMS), and rotate exposed keys.")
            signals.append("security")
            evidence.append(find_line_containing(code, m.group(0)))

    # --- JAVA ---
    if lang == "java":
        if "System.out.print" in code:
            issues.append("Use of System.out for logging")
            fixes.append("Use a structured logger (e.g., SLF4J) and consistent log levels; avoid stdout in production.")
            signals.append("observability")
            evidence.append(find_line_containing(code, "System.out.print"))

        if "printStackTrace" in code:
            issues.append("printStackTrace used for error handling")
            fixes.append("Log exceptions via logger and propagate or translate them appropriately; avoid noisy stack traces.")
            signals.append("error-handling")
            evidence.append(find_line_containing(code, "printStackTrace"))

        if "@RestController" in code and ("java.sql" in code or "EntityManager" in code):
            issues.append("Controller mixing API and DB logic")
            fixes.append("Move DB access into Repository/DAO + Service layers; keep controllers thin (DTO in/out).")
            signals.append("separation-of-concerns")
            evidence.append(find_line_containing(code, "@RestController"))

    # --- JS/TS ---
    if lang in ["javascript", "typescript"]:
        then_count = code.count(".then(")
        cb_brace_count = code.count("})")
        if then_count > 3 or cb_brace_count > 3:
            issues.append("Excessive callback/promise chaining")
            fixes.append("Refactor to async/await, extract helper functions, and centralize error handling.")
            signals.append("readability")
            evidence.append(f".then count={then_count}, close-brace-paren count={cb_brace_count}")

        if lang == "typescript":
            any_match = re.search(r':\s*any\b', code)
            if any_match:
                issues.append("Type safety weakened by 'any'")
                fixes.append("Replace 'any' with specific interfaces/types or 'unknown' + narrowing.")
                signals.append("type-safety")
                evidence.append(find_line_containing(code, any_match.group(0)))

        if re.search(r'(^|\s)var\s+\w+', code):
            issues.append("Legacy 'var' usage")
            fixes.append("Prefer 'const' by default; use 'let' when reassignment is needed.")
            signals.append("modern-standards")
            evidence.append(find_line_containing(code, "var "))

        if "dangerouslySetInnerHTML" in code:
            issues.append("Unsafe HTML rendering boundary (dangerouslySetInnerHTML)")
            fixes.append("Sanitize HTML input (e.g., DOMPurify) or render as text; keep trust boundaries explicit.")
            signals.append("security-boundary")
            evidence.append(find_line_containing(code, "dangerouslySetInnerHTML"))

    if not issues:
        return None

    return issues, fixes, signals, evidence

# -----------------------------
# MAIN ANALYSIS DISPATCH
# -----------------------------
def analyze_code(code: str, lang: str):
    if lang == "python":
        ast_res = analyze_python_ast(code)
        if ast_res:
            issues, fixes, signals, evidence = ast_res
            return issues, fixes, signals, evidence, "high"

    re_res = analyze_regex_heuristics(code, lang)
    if re_res:
        issues, fixes, signals, evidence = re_res
        return issues, fixes, signals, evidence, "medium"

    return None

# -----------------------------
# RUN
# -----------------------------
print("🚀 Starting Grandmaster Data Extraction...")

final_data = []
seen_hashes = set()

# 1) CyberSecEval (Security)
print("⬇️  Downloading CyberSecEval (Security)...")
for lang in ["python", "java", "javascript"]:
    try:
        ds = load_dataset("walledai/CyberSecEval", "instruct", split=f"{lang}[:{CYBER_N_PER_LANG}]")
        for row in ds:
            code_snippet = (row.get("origin_code") or row.get("prompt") or row.get("input") or "").strip()
            if len(code_snippet) < MIN_SEC_CODE_CHARS:
                continue

            h = stable_hash(code_snippet)
            if h in seen_hashes:
                continue
            seen_hashes.add(h)

            cwe = row.get("cwe_identifier", "Unknown")
            desc = row.get("description", "Vulnerability detected")
            rule = row.get("rule") or row.get("rule_name") or row.get("pattern_desc") or "unknown"

            entry = {
                "instruction": f"Analyze this {lang.capitalize()} code for security vulnerabilities. Return strict JSON.",
                "input": code_snippet[:MAX_INPUT_CHARS],
                "output": format_repo_mind_json(
                    severity="CRITICAL",
                    category="SECURITY",
                    language=lang.capitalize(),
                    message=f"{desc} (CWE: {cwe})",
                    fix=(
                        "Use secure coding practices appropriate to the vulnerability: "
                        "validate/sanitize inputs, avoid dangerous APIs, use parameterized queries, "
                        "and apply output encoding at trust boundaries."
                    ),
                    extra={
                        "source": "CyberSecEval",
                        "confidence": "high",
                        "signals": ["security", f"cwe-{cwe}"],
                        "cwe": cwe,
                        "rule": rule,
                    },
                ),
            }
            final_data.append(entry)
    except Exception as e:
        print(f"   ⚠️ CyberSecEval failed for {lang}: {e}")

# 2) GitHub Code (Architecture) via MIT Parquet subset
print("⬇️  Downloading GitHub Code (Architecture)...")
try:
    shard_urls = [
        f"https://huggingface.co/datasets/codeparrot/github-code/resolve/refs%2Fconvert%2Fparquet/all-mit/partial-train/{i:04d}.parquet"
        for i in MIT_PART_IDS
    ]
    ds = load_dataset("parquet", data_files=shard_urls, split="train", streaming=True)

    counts = {l: 0 for l in GITHUB_TARGETS}

    for row in ds:
        if all(c >= GITHUB_N_PER_LANG for c in counts.values()):
            break

        row_lang = (row.get("language") or row.get("lang") or "").strip().lower()
        if row_lang not in GITHUB_TARGETS:
            continue
        if counts[row_lang] >= GITHUB_N_PER_LANG:
            continue

        content = str(row.get("code", "")).strip()
        if not (MIN_ARCH_CODE_CHARS <= len(content) <= MAX_INPUT_CHARS):
            continue

        h = stable_hash(content)
        if h in seen_hashes:
            continue
        seen_hashes.add(h)

        result = analyze_code(content, row_lang)
        if not result:
            continue

        issues, fixes, signals, evidence, confidence = result

        entry = {
            "instruction": f"Analyze this {row_lang.capitalize()} code for architectural quality. Return strict JSON.",
            "input": content[:MAX_INPUT_CHARS],
            "output": format_repo_mind_json(
                severity="WARNING",
                category="ARCHITECTURE",
                language=row_lang.capitalize(),
                message=f"Architectural issues detected: {'; '.join(issues)}",
                fix=f"{' '.join(fixes)}",
                extra={
                    "signals": signals,
                    "evidence": evidence,
                    "confidence": confidence,
                    "license": "mit",
                    "repo": row.get("repo_name"),
                    "path": row.get("path"),
                },
            ),
        }

        final_data.append(entry)
        counts[row_lang] += 1

        if len(final_data) % 20 == 0:
            print(f"   ...collected {len(final_data)} examples so far...")

    print(f"   ✅ Added Architecture Examples: {counts}")

except Exception as e:
    print(f"   ⚠️ GitHub code failed: {e}")

# 3) Save
print(f"💾 Saving {len(final_data)} total examples to {OUTPUT_FILE}...")
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    for item in final_data:
        f.write(json.dumps(item) + "\n")

print("✅ Done.")
