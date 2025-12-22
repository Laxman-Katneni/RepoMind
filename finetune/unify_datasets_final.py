import json
import os
import random
import hashlib

# CONFIGURATION
SYNTHETIC_FILE = "synthetic_train.jsonl"
REAL_WORLD_FILE = "real_world_train.jsonl"
FINAL_TRAIN = "train.jsonl"
FINAL_TEST = "test.jsonl"

def stable_hash(text: str) -> str:
    # We hash the input code to find duplicates
    return hashlib.sha256(text.encode("utf-8", errors="ignore")).hexdigest()

def is_valid_repo_mind_json(entry):
    """Checks if the entry matches our schema"""
    try:
        # Must have instruction, input, output
        if not all(k in entry for k in ["instruction", "input", "output"]):
            return False
        
        # Verify output is a valid JSON string (the model needs to learn this format)
        json.loads(entry["output"]) 
        return True
    except:
        return False

merged_data = []
seen_hashes = set()
stats = {
    "synthetic_loaded": 0,
    "real_loaded": 0,
    "duplicates_dropped": 0,
    "invalid_dropped": 0
}

print("🚀 Starting Final Dataset Unification...")

# ---------------------------------------------------------
# 1. LOAD SYNTHETIC DATA
# ---------------------------------------------------------
if os.path.exists(SYNTHETIC_FILE):
    print(f"📦 Processing {SYNTHETIC_FILE}...")
    with open(SYNTHETIC_FILE, 'r') as f:
        for line in f:
            if line.strip():
                try:
                    entry = json.loads(line)
                    if is_valid_repo_mind_json(entry):
                        # Deduplication Check
                        h = stable_hash(entry["input"])
                        if h in seen_hashes:
                            stats["duplicates_dropped"] += 1
                            continue
                        
                        seen_hashes.add(h)
                        merged_data.append(entry)
                        stats["synthetic_loaded"] += 1
                    else:
                        stats["invalid_dropped"] += 1
                except:
                    stats["invalid_dropped"] += 1

# ---------------------------------------------------------
# 2. LOAD REAL WORLD DATA
# ---------------------------------------------------------
if os.path.exists(REAL_WORLD_FILE):
    print(f"📦 Processing {REAL_WORLD_FILE}...")
    with open(REAL_WORLD_FILE, 'r') as f:
        for line in f:
            if line.strip():
                try:
                    entry = json.loads(line)
                    if is_valid_repo_mind_json(entry):
                        # Deduplication Check
                        h = stable_hash(entry["input"])
                        if h in seen_hashes:
                            stats["duplicates_dropped"] += 1
                            continue
                        
                        seen_hashes.add(h)
                        merged_data.append(entry)
                        stats["real_loaded"] += 1
                    else:
                        stats["invalid_dropped"] += 1
                except:
                    stats["invalid_dropped"] += 1

# ---------------------------------------------------------
# 3. SHUFFLE & SPLIT
# ---------------------------------------------------------
random.shuffle(merged_data)

print("\n📊 MERGE REPORT:")
print(f"   ✅ Synthetic Unique:  {stats['synthetic_loaded']}")
print(f"   ✅ Real-World Unique: {stats['real_loaded']}")
print(f"   🗑️  Duplicates:       {stats['duplicates_dropped']}")
print(f"   ❌ Invalid JSON:      {stats['invalid_dropped']}")
print(f"   --------------------------------")
print(f"   🔥 TOTAL DATASET:     {len(merged_data)}")

# Split 90/10 for Training/Validation
split_index = int(len(merged_data) * 0.9)
train_set = merged_data[:split_index]
test_set = merged_data[split_index:]

with open(FINAL_TRAIN, 'w') as f:
    for entry in train_set:
        f.write(json.dumps(entry) + "\n")

with open(FINAL_TEST, 'w') as f:
    for entry in test_set:
        f.write(json.dumps(entry) + "\n")

print(f"\n🚀 SAVED TO FILES:")
print(f"   - {FINAL_TRAIN} ({len(train_set)} rows)")
print(f"   - {FINAL_TEST} ({len(test_set)} rows)")
print("\n👉 NEXT STEP: Upload 'train.jsonl' and 'test.jsonl' to Google Colab.")