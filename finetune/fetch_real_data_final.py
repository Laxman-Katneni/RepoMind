import hashlib
import json
import re
import ast
from datasets import load_dataset

OUTPUT_FILE = "real_world_train.jsonl"

# CONFIGURATION
# We fetch a bit more to allow for strict filtering
GITHUB_TARGETS = ["python", "java", "javascript", "typescript"]
GITHUB_N_PER_LANG = 60  # Increased target to ensure we hit ~50 valid ones
CYBER_N_PER_LANG = 50 

MAX_INPUT_CHARS = 3000 

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

# ---------------------------------------------------------
# 🧠 AST VISITORS (True Depth & Analysis)
# ---------------------------------------------------------
class MaxNestingVisitor(ast.NodeVisitor):
    def __init__(self):
        self.max_depth = 0
        self.current_depth = 0

    def visit(self, node):
        if isinstance(node, (ast.If, ast.For, ast.While, ast.Try, ast.FunctionDef)):
            self.current_depth += 1
            self.max_depth = max(self.max_depth, self.current_depth)
            self.generic_visit(node)
            self.current_depth -= 1
        else:
            self.generic_visit(node)

def analyze_python_ast(code):
    issues = []
    fixes = []
    signals = []
    evidence = []
    
    try:
        tree = ast.parse(code)
    except SyntaxError:
        return None

    # 1. True Nesting Depth
    visitor = MaxNestingVisitor()
    visitor.visit(tree)
    if visitor.max_depth > 4:
        issues.append(f"Deep nesting detected (depth {visitor.max_depth})")
        fixes.append("Refactor deeply nested logic using guard clauses or extract methods.")
        signals.append("complexity")
        evidence.append(f"Max depth: {visitor.max_depth}")

    # 2. God Class (Method Count)
    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef):
            methods = [n for n in node.body if isinstance(n, ast.FunctionDef)]
            if len(methods) > 10: # Stricter threshold for snippets
                issues.append(f"Class '{node.name}' has too many methods ({len(methods)})")
                fixes.append(f"Break '{node.name}' into smaller classes with single responsibilities.")
                signals.append("cohesion")
                evidence.append(f"class {node.name}")

    # 3. Global State
    for node in ast.walk(tree):
        if isinstance(node, ast.Global):
            issues.append("Explicit global state usage")
            fixes.append("Pass dependencies explicitly instead of using 'global'.")
            signals.append("side-effects")
            evidence.append(f"global {', '.join(node.names)}")

    # 4. Bare Except
    for node in ast.walk(tree):
        if isinstance(node, ast.ExceptHandler):
            if node.type is None: 
                issues.append("Bare 'except:' block")
                fixes.append("Catch specific exceptions (e.g., ValueError) to avoid hiding bugs.")
                signals.append("error-handling")
                evidence.append("except:")

    return issues, fixes, signals, evidence

# ---------------------------------------------------------
# 🧠 REGEX HEURISTICS (Polyglot)
# ---------------------------------------------------------
def analyze_regex_heuristics(code, lang):
    issues = []
    fixes = []
    signals = []
    evidence = []
    code_lower = code.lower()

    # --- UNIVERSAL: Hardcoded Secrets (Improved) ---
    # Matches: api_key = "...", String key = "...", const TOKEN = "..."
    # Excludes: os.getenv, process.env
    secret_pattern = r'(?i)(api_key|password|secret|token)\s*[:=]\s*["\'][a-zA-Z0-9_\-]{8,}["\']'
    match = re.search(secret_pattern, code)
    if match and "env" not in code_lower and "getenv" not in code_lower:
        issues.append("Hardcoded secret detected")
        fixes.append("Use environment variables (e.g., os.getenv, process.env).")
        signals.append("security")
        evidence.append(match.group(0)[:50] + "...")

    # --- JAVA ---
    if lang == "java":
        if "System.out.print" in code:
            issues.append("Use of System.out for logging")
            fixes.append("Use a Logger (SLF4J/Log4j) for better control and performance.")
            signals.append("observability")
            evidence.append("System.out.print")
        
        if "@RestController" in code and ("java.sql" in code or "EntityManager" in code):
            issues.append("Controller mixing API and DB logic")
            fixes.append("Move data access to a @Repository class.")
            signals.append("separation-of-concerns")
            evidence.append("@RestController + SQL")

    # --- JS/TS ---
    if lang in ["javascript", "typescript"]:
        # Callback Hell (Nesting check via regex)
        if code.count("})") > 3 or code.count(").then") > 3:
            issues.append("Excessive callback/promise chaining")
            fixes.append("Refactor to 'async/await' for linear readability.")
            signals.append("readability")
            evidence.append("Multiple }).then chains")
        
        if lang == "typescript" and ": any" in code:
            issues.append("Usage of 'any' type")
            fixes.append("Replace 'any' with a specific interface or 'unknown'.")
            signals.append("type-safety")
            evidence.append(": any")

    return issues, fixes, signals, evidence

# ---------------------------------------------------------
# MAIN LOGIC
# ---------------------------------------------------------
def analyze_code(code, lang):
    # Try AST for Python (High Confidence)
    if lang == "python":
        res = analyze_python_ast(code)
        if res:
            issues, fixes, signals, evidence = res
            if issues: return issues, fixes, signals, evidence, "high"
    
    # Fallback to Regex (Medium Confidence)
    issues, fixes, signals, evidence = analyze_regex_heuristics(code, lang)
    if issues:
        return issues, fixes, signals, evidence, "medium"
        
    return None

print("🚀 Starting Grandmaster Data Extraction...")

final_data = []
seen_hashes = set()

# ---------------------------------------------------------
# 1) FETCH CYBERSEC EVAL (Rich Security)
# ---------------------------------------------------------
print("⬇️  Downloading CyberSecEval (Security)...")
for lang in ["python", "java", "javascript"]:
    try:
        ds = load_dataset("walledai/CyberSecEval", "instruct", split=f"{lang}[:{CYBER_N_PER_LANG}]")
        for row in ds:
            code_snippet = (row.get("origin_code") or row.get("prompt") or row.get("input") or "").strip()
            if len(code_snippet) < 50: continue
            
            h = stable_hash(code_snippet)
            if h in seen_hashes: continue
            seen_hashes.add(h)

            # Rich Metadata
            cwe = row.get("cwe_identifier", "Unknown")
            desc = row.get("description", "Vulnerability detected")
            
            entry = {
                "instruction": f"Analyze this {lang.capitalize()} code for security vulnerabilities. Return strict JSON.",
                "input": code_snippet[:MAX_INPUT_CHARS],
                "output": format_repo_mind_json(
                    severity="CRITICAL",
                    category="SECURITY",
                    language=lang.capitalize(),
                    message=f"{desc} (CWE: {cwe})",
                    fix="Apply secure coding practices: Input validation, parameterized queries, and output encoding.",
                    extra={"confidence": "high", "signals": ["security", f"cwe-{cwe}"]}
                ),
            }
            final_data.append(entry)
    except: pass

# ---------------------------------------------------------
# 2) FETCH GITHUB (Polyglot + AST)
# ---------------------------------------------------------
print("⬇️  Downloading GitHub Code (Architecture)...")
try:
    # We use the MIT subset for safety. If it yields low Java/JS, the loop simply skips them.
    # To get more variety, we scan more shards.
    part_ids = list(range(0, 8)) 
    shard_urls = [f"https://huggingface.co/datasets/codeparrot/github-code/resolve/refs%2Fconvert%2Fparquet/all-mit/partial-train/{i:04d}.parquet" for i in part_ids]
    ds = load_dataset("parquet", data_files=shard_urls, split="train", streaming=True)

    counts = {l: 0 for l in GITHUB_TARGETS}
    
    for row in ds:
        # Stop if we have enough of EVERYTHING
        if all(c >= GITHUB_N_PER_LANG for c in counts.values()): break
        
        row_lang = (row.get("language") or row.get("lang") or "").strip().lower()
        if row_lang not in GITHUB_TARGETS: continue
        if counts[row_lang] >= GITHUB_N_PER_LANG: continue

        content = str(row.get("code", "")).strip()
        # Filter for "meaty" code files
        if not (150 <= len(content) <= MAX_INPUT_CHARS): continue

        h = stable_hash(content)
        if h in seen_hashes: continue
        seen_hashes.add(h)

        # 🧠 INTELLIGENT ANALYSIS
        result = analyze_code(content, row_lang)
        
        if result:
            issues, fixes, signals, evidence, confidence = result
            
            entry = {
                "instruction": f"Analyze this {row_lang.capitalize()} code for architectural quality. Return strict JSON.",
                "input": content,
                "output": format_repo_mind_json(
                    severity="WARNING",
                    category="ARCHITECTURE",
                    language=row_lang.capitalize(),
                    message=f"Architectural issues detected: {'; '.join(issues)}",
                    fix=f"Suggestions: {'; '.join(fixes)}",
                    extra={
                        "signals": signals,
                        "evidence": evidence, 
                        "confidence": confidence,
                        "license": "mit"
                    }
                ),
            }
            final_data.append(entry)
            counts[row_lang] += 1
            if len(final_data) % 20 == 0: print(f"   ...collected {len(final_data)} examples so far...")

    print(f"   ✅ Added Architecture Examples: {counts}")

except Exception as e:
    print(f"   ⚠️ GitHub code failed: {e}")

# ---------------------------------------------------------
# 3) SAVE
# ---------------------------------------------------------
print(f"💾 Saving {len(final_data)} total examples to {OUTPUT_FILE}...")
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    for item in final_data:
        f.write(json.dumps(item) + "\n")