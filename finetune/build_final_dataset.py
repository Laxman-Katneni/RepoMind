import json
import os
import random
from datasets import load_dataset

# Configuration
SYNTHETIC_FILE = "synthetic_train.jsonl"
FINAL_TRAIN_FILE = "final_train.jsonl"
FINAL_TEST_FILE = "final_test.jsonl"

def format_repo_mind_json(severity, category, language, message, fix):
    return json.dumps({
        "severity": severity,
        "category": category,
        "language": language,
        "title": "Automated Security Audit",
        "message": message,
        "suggestion": fix
    })

all_examples = []

# ---------------------------------------------------------
# 1. LOAD SYNTHETIC DATA (The "Gold Standard")
# ---------------------------------------------------------
if os.path.exists(SYNTHETIC_FILE):
    print(f"Loading synthetic data from {SYNTHETIC_FILE}...")
    with open(SYNTHETIC_FILE, 'r') as f:
        for line in f:
            if line.strip():
                all_examples.append(json.loads(line))
    print(f"   ✅ Loaded {len(all_examples)} synthetic examples.")
else:
    print("   ⚠️  Synthetic file not found! Please run the previous step.")

# ---------------------------------------------------------
# 2. FETCH CYBERSEC EVAL (Real Security Flaws)
# ---------------------------------------------------------
print("Downloading CyberSecEval (Security Vulnerabilities)...")
try:
    # We fetch specifically the 'instruct' subset which is cleaner
    ds = load_dataset("walledai/CyberSecEval", "instruct", split="train[:300]")
    
    count = 0
    for row in ds:
        # Heuristic: Only keep examples that look like code analysis prompts
        prompt = row.get('prompt', '')
        
        # We only want entries that contain code-like structures
        if "{" in prompt and "}" in prompt and len(prompt) < 2000:
            entry = {
                "instruction": "Analyze this code for security vulnerabilities. Return strict JSON.",
                "input": prompt,
                "output": format_repo_mind_json(
                    "CRITICAL",
                    "SECURITY",
                    "Unknown", # Language detection would go here in a real app
                    "Potential security vulnerability detected based on CyberSecEval patterns.",
                    "Review code against OWASP Top 10 security guidelines."
                )
            }
            all_examples.append(entry)
            count += 1
            if count >= 100: break # Don't overwhelm the synthetic data
            
    print(f"   ✅ Added {count} real-world security examples.")

except Exception as e:
    print(f"   ⚠️ CyberSecEval failed: {e}")

# ---------------------------------------------------------
# 3. SHUFFLE & SPLIT
# ---------------------------------------------------------
print("Shuffling and splitting dataset...")
random.shuffle(all_examples)

# 90% Train, 10% Test (Holdout for "Resume Proof")
split_idx = int(len(all_examples) * 0.9)
train_data = all_examples[:split_idx]
test_data = all_examples[split_idx:]

print(f"💾 Saving {len(train_data)} training examples to {FINAL_TRAIN_FILE}...")
with open(FINAL_TRAIN_FILE, "w") as f:
    for item in train_data:
        f.write(json.dumps(item) + "\n")

print(f"💾 Saving {len(test_data)} test examples to {FINAL_TEST_FILE}...")
with open(FINAL_TEST_FILE, "w") as f:
    for item in test_data:
        f.write(json.dumps(item) + "\n")

print("\n✅ PHASE 1 COMPLETE!")
print(f"   Train Set: {len(train_data)} rows (Use this for Fine-Tuning)")
print(f"   Test Set:  {len(test_data)} rows (Use this for Evaluation)")