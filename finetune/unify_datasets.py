import json
import os
import random

# CONFIGURATION
SYNTHETIC_FILE = "synthetic_train.jsonl"
REAL_WORLD_FILE = "real_world_train.jsonl"
FINAL_TRAIN = "train.jsonl"
FINAL_TEST = "test.jsonl"

def is_valid_repo_mind_json(entry):
    """Checks if the entry matches our schema"""
    try:
        if not all(k in entry for k in ["instruction", "input", "output"]):
            return False
        
        # Check if output is a valid JSON string
        out = json.loads(entry["output"])
        if not all(k in out for k in ["severity", "category", "message"]):
            return False
            
        return True
    except:
        return False

merged_data = []
stats = {
    "synthetic_loaded": 0,
    "real_loaded": 0,
    "skipped_length": 0,
    "skipped_invalid_json": 0,
    "duplicates_removed": 0
}

print("🚀 Starting Dataset Unification...")

# 1. Load Synthetic Data
if os.path.exists(SYNTHETIC_FILE):
    print(f"📦 Processing {SYNTHETIC_FILE}...")
    with open(SYNTHETIC_FILE, 'r') as f:
        for line in f:
            if line.strip():
                try:
                    entry = json.loads(line)
                    if is_valid_repo_mind_json(entry):
                        merged_data.append(entry)
                        stats["synthetic_loaded"] += 1
                    else:
                        stats["skipped_invalid_json"] += 1
                except:
                    stats["skipped_invalid_json"] += 1

# 2. Load Real World Data
if os.path.exists(REAL_WORLD_FILE):
    print(f"📦 Processing {REAL_WORLD_FILE}...")
    with open(REAL_WORLD_FILE, 'r') as f:
        for line in f:
            if line.strip():
                try:
                    entry = json.loads(line)
                    # FIX: Lowered threshold from 50 to 10 to save Security Snippets
                    if len(entry.get("input", "")) < 10:
                        stats["skipped_length"] += 1
                        continue

                    if is_valid_repo_mind_json(entry):
                        merged_data.append(entry)
                        stats["real_loaded"] += 1
                    else:
                        stats["skipped_invalid_json"] += 1
                except:
                    stats["skipped_invalid_json"] += 1

# 3. Deduplicate
initial_count = len(merged_data)
# We use the 'input' (code) as the unique key to prevent duplicate code snippets
unique_data = {entry["input"].strip(): entry for entry in merged_data}.values()
final_list = list(unique_data)
stats["duplicates_removed"] = initial_count - len(final_list)

# Shuffle
random.shuffle(final_list)

# 4. Report & Save
print("\n📊 DIAGNOSTIC REPORT:")
print(f"   ✅ Synthetic Loaded:  {stats['synthetic_loaded']}")
print(f"   ✅ Real-World Loaded: {stats['real_loaded']}")
print(f"   ❌ Skipped (Too Short < 10): {stats['skipped_length']}")
print(f"   ❌ Skipped (Bad JSON):       {stats['skipped_invalid_json']}")
print(f"   🗑️  Duplicates Removed:      {stats['duplicates_removed']}")
print(f"   --------------------------------")
print(f"   🔥 FINAL DATASET SIZE: {len(final_list)}")

# Split 90/10
split_index = int(len(final_list) * 0.9)
train_set = final_list[:split_index]
test_set = final_list[split_index:]

with open(FINAL_TRAIN, 'w') as f:
    for entry in train_set:
        f.write(json.dumps(entry) + "\n")

with open(FINAL_TEST, 'w') as f:
    for entry in test_set:
        f.write(json.dumps(entry) + "\n")

print(f"\n🚀 SAVED TO FILES:")
print(f"   - {FINAL_TRAIN} ({len(train_set)} rows)")
print(f"   - {FINAL_TEST} ({len(test_set)} rows)")