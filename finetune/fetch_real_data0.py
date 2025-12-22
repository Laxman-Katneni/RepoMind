import json
import os
from datasets import load_dataset

OUTPUT_FILE = "real_world_train.jsonl"

def format_json_output(severity, category, message, fix):
    return json.dumps({
        "severity": severity,
        "category": category,
        "title": "Automated Code Review",
        "message": message,
        "suggestion": fix
    })

real_data = []

print("🚀 Starting Real-World Data Extraction (V3 - Fixed)...")

# ---------------------------------------------------------
# 1. FETCH DEFECTS4J (Logic Bugs / Refactoring)
# ---------------------------------------------------------
print("   ⬇️  Downloading Defects4J (Java Logic Bugs)...")
try:
    # This dataset serves as our "Refactoring/Logic" training
    ds = load_dataset("rufimelo/defects4j", split="train[:100]")
    
    for row in ds:
        buggy_code = row.get('func_before')
        fixed_code = row.get('func_after')
        
        if buggy_code and fixed_code:
            entry = {
                "instruction": "Analyze this Java code for logic errors and architectural flaws. Return a strict JSON review.",
                "input": buggy_code,
                "output": format_json_output(
                    "WARNING",
                    "LOGIC_ERROR",
                    "Defect detected in method logic (derived from Defects4J).",
                    f"Refactored Fix:\n{fixed_code}"
                )
            }
            real_data.append(entry)
            
    print(f"   ✅ Added {len(real_data)} Defects4J examples.")

except Exception as e:
    print(f"   ⚠️ Defects4J failed: {e}")

# ---------------------------------------------------------
# 2. FETCH CYBERSEC EVAL (Security) - FIXED
# ---------------------------------------------------------
print("   ⬇️  Downloading CyberSecEval (Security)...")
try:
    # FIX: Added "instruct" as the second argument
    languages = ['java', 'python'] 
    security_count = 0
    
    for lang in languages:
        print(f"      - Fetching {lang} split...")
        try:
            # We explicitly request the 'instruct' config
            ds = load_dataset("walledai/CyberSecEval", "instruct", split=f"{lang}[:50]")
            
            for row in ds:
                # The 'instruct' config puts the code in 'prompt' or 'input'
                # We check multiple fields to be safe
                prompt = row.get('prompt') or row.get('input') or row.get('question')
                
                # Filter for actual code snippets (heuristic)
                if prompt and ("public" in prompt or "def " in prompt or "{" in prompt):
                    entry = {
                        "instruction": f"Analyze this {lang} code for security vulnerabilities.",
                        "input": prompt,
                        "output": format_json_output(
                            "CRITICAL",
                            "SECURITY",
                            f"Security vulnerability detected in {lang} code (CyberSecEval).",
                            "Review against OWASP guidelines and sanitize inputs."
                        )
                    }
                    real_data.append(entry)
                    security_count += 1
        except Exception as lang_e:
            print(f"      ⚠️ Failed to load {lang}: {lang_e}")

    print(f"   ✅ Added {security_count} Security examples.")

except Exception as e:
    print(f"   ⚠️ CyberSecEval Global failed: {e}")

# ---------------------------------------------------------
# 3. SAVE
# ---------------------------------------------------------
print(f"💾 Saving {len(real_data)} total examples to {OUTPUT_FILE}...")
with open(OUTPUT_FILE, "w") as f:
    for item in real_data:
        f.write(json.dumps(item) + "\n")

print("Done! NOW you are ready to run 'unify_datasets.py'.")